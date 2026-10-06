#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"
# promtool reads the mounted fixture directory as UID 65534 on Linux.
chmod 755 "$TEMP"
export MONITORING_SECRET_DIR="$TEMP/secrets"
PROJECT="my-fitness-monitoring-check-$$"
NETWORK="$PROJECT-notifications"
cleanup() {
  docker rm -fv "$PROJECT-receiver" "$PROJECT-alertmanager" "$PROJECT-caddy" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
  for environment in local prod; do
    docker compose -p "$PROJECT-$environment" -f "$ROOT/monitoring/docker-compose.$environment.yml" \
      down -v >/dev/null 2>&1 || true
  done
  rm -rf "$TEMP"
}
trap cleanup EXIT
mkdir -m 700 "$MONITORING_SECRET_DIR"
printf 'dummy-metrics\n' > "$MONITORING_SECRET_DIR/app.monitoring.password"
printf 'dummy-grafana\n' > "$MONITORING_SECRET_DIR/grafana_admin_password"
printf 'https://hooks.slack.com/services/dummy/test/not-a-real-webhook\n' > "$MONITORING_SECRET_DIR/slack_webhook_url"
chmod 600 "$MONITORING_SECRET_DIR"/*

for environment in local prod; do
  COMPOSE=(docker compose -p "$PROJECT-$environment" -f "$ROOT/monitoring/docker-compose.$environment.yml")
  "${COMPOSE[@]}" config --quiet
  "${COMPOSE[@]}" run --rm secret-init
  "${COMPOSE[@]}" run --rm --no-deps --entrypoint sh prometheus -ec \
    'test "$(id -u)" = 65534; test -r /credentials/metrics_password; test ! -r /credentials/grafana_admin_password; test ! -r /credentials/slack_webhook_url'
  "${COMPOSE[@]}" run --rm --no-deps --entrypoint sh grafana -ec \
    'test "$(id -u)" = 472; test -r /credentials/grafana_admin_password; test ! -r /credentials/metrics_password; test ! -r /credentials/slack_webhook_url'
  "${COMPOSE[@]}" run --rm --no-deps --entrypoint promtool prometheus check config /etc/prometheus/prometheus.yml
done
"${COMPOSE[@]}" run --rm --no-deps --entrypoint sh alertmanager -ec \
  'test "$(id -u)" = 65534; test -r /credentials/slack_webhook_url; test ! -r /credentials/metrics_password; test ! -r /credentials/grafana_admin_password'

# Same six behavioral fixtures, evaluated at the production 60-second interval.
cp "$ROOT/monitoring/prometheus/alerts.yml" "$TEMP/alerts.yml"
cp "$ROOT/monitoring/tests/alerts.test.yml" "$TEMP/alerts.test.yml"
cp "$ROOT/monitoring/tests/host-alerts.test.yml" "$TEMP/host-alerts.test.yml"
jq '.evaluation_interval = "1m"' "$ROOT/monitoring/tests/alerts.test.yml" > "$TEMP/alerts.prod.test.yml"
docker run --rm --entrypoint promtool -v "$TEMP:/tests:ro" \
  prom/prometheus:v3.15.0 test rules /tests/alerts.test.yml /tests/alerts.prod.test.yml /tests/host-alerts.test.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint amtool alertmanager \
  check-config /etc/alertmanager/alertmanager.yml
"${COMPOSE[@]}" run --rm --no-deps --entrypoint amtool alertmanager \
  config routes test --config.file=/etc/alertmanager/alertmanager.yml \
  --verify.receivers=slack service=my-fitness environment=prod alertname=HostMemoryPressure

sed -e 's/group_interval: 5m/group_interval: 1s/' \
  -e 's/repeat_interval: 4h/repeat_interval: 1m/' \
  -e 's@api_url_file: /credentials/slack_webhook_url@api_url: http://receiver:8080/@' \
  "$ROOT/monitoring/alertmanager/alertmanager.prod.yml" > "$TEMP/alertmanager.yml"
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
