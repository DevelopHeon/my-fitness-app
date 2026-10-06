#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"
export MONITORING_SECRET_DIR="$TEMP/secrets"
PROJECT="my-fitness-monitoring-check-$$"
COMPOSE=(docker compose -p "$PROJECT" -f "$ROOT/monitoring/docker-compose.prod.yml")
NETWORK="$PROJECT-notifications"
cleanup() {
  docker rm -fv "$PROJECT-receiver" "$PROJECT-alertmanager" "$PROJECT-caddy" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
  "${COMPOSE[@]}" down -v >/dev/null 2>&1 || true
  rm -rf "$TEMP"
}
trap cleanup EXIT
mkdir -m 700 "$MONITORING_SECRET_DIR"
printf 'dummy-metrics\n' > "$MONITORING_SECRET_DIR/app.monitoring.password"
printf 'dummy-grafana\n' > "$MONITORING_SECRET_DIR/grafana_admin_password"
printf 'https://hooks.slack.com/services/dummy/test/not-a-real-webhook\n' > "$MONITORING_SECRET_DIR/slack_webhook_url"
chmod 600 "$MONITORING_SECRET_DIR"/*

"${COMPOSE[@]}" config --quiet
"${COMPOSE[@]}" run --rm secret-init
"${COMPOSE[@]}" run --rm --no-deps --entrypoint sh prometheus -ec \
  'test -r /credentials/metrics_password; test ! -r /credentials/grafana_admin_password; test ! -r /credentials/slack_webhook_url'
"${COMPOSE[@]}" run --rm --no-deps --entrypoint sh grafana -ec \
  'test -r /credentials/grafana_admin_password; test ! -r /credentials/metrics_password; test ! -r /credentials/slack_webhook_url'
"${COMPOSE[@]}" run --rm --no-deps --entrypoint sh alertmanager -ec \
  'test -r /credentials/slack_webhook_url; test ! -r /credentials/metrics_password; test ! -r /credentials/grafana_admin_password'
"${COMPOSE[@]}" run --rm --no-deps --entrypoint promtool prometheus check config /etc/prometheus/prometheus.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint promtool prometheus test rules /etc/prometheus/alerts.test.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint promtool prometheus test rules /etc/prometheus/host-alerts.test.yml

# Same six behavioral fixtures, evaluated at the production 60-second interval.
cp "$ROOT/monitoring/prometheus/alerts.yml" "$TEMP/alerts.yml"
jq '.evaluation_interval = "1m"' "$ROOT/monitoring/prometheus/alerts.test.yml" > "$TEMP/alerts.test.yml"
docker run --rm --entrypoint promtool -v "$TEMP:/tests:ro" \
  prom/prometheus:v3.15.0 test rules /tests/alerts.test.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint amtool alertmanager \
  check-config /etc/alertmanager/alertmanager.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint amtool alertmanager \
  config routes test --config.file=/etc/alertmanager/alertmanager.yml \
  --verify.receivers=slack service=my-fitness environment=prod alertname=HostMemoryPressure

sed -e 's/group_wait: 30s/group_wait: 1s/' \
  -e 's/group_interval: 5m/group_interval: 1s/' \
  -e 's/repeat_interval: 4h/repeat_interval: 1m/' \
  -e 's@api_url_file: /credentials/slack_webhook_url@api_url: http://receiver:8080/@' \
  "$ROOT/monitoring/production/alertmanager.yml" > "$TEMP/alertmanager.yml"
# Exercise the actual Caddy template with an HTTP-only isolated test address.
awk '/^cat > .*<<CADDY$/ {copy=1; next} /^CADDY$/ {copy=0} copy' \
  "$ROOT/scripts/setup-caddy.sh" | sed -e 's/\$DOMAIN/:8081/' \
  -e 's/\$APP_UPSTREAM/receiver:8080/' > "$TEMP/Caddyfile"
docker run --rm -v "$TEMP/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2.11.4-alpine \
  caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
docker network create "$NETWORK" >/dev/null
docker run -d --name "$PROJECT-alertmanager" --network "$NETWORK" --network-alias alertmanager \
  -v "$TEMP/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" \
  prom/alertmanager:v0.34.1 --config.file=/etc/alertmanager/alertmanager.yml \
  --cluster.listen-address= --web.listen-address=0.0.0.0:9093 >/dev/null
docker run -d --name "$PROJECT-caddy" --network "$NETWORK" --network-alias caddy \
  -v "$TEMP/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2.11.4-alpine >/dev/null
docker run --name "$PROJECT-receiver" --network "$NETWORK" --network-alias receiver \
  -v "$ROOT/monitoring/tests/notifications.mjs:/test.mjs:ro" node:22-alpine node /test.mjs

echo 'Monitoring configuration, rules, routes and credential isolation passed'
