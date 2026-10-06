#!/usr/bin/env bash
set -euo pipefail

RELEASE="${1:?release directory is required}"
ACTION="${2:-apply}"
BASE=/opt/my-fitness/monitoring
export MONITORING_SECRET_DIR="$BASE/secrets"
export MONITORING_DASHBOARD_DIR="$RELEASE/generated-dashboards"
COMPOSE=(docker compose -p my-fitness-monitoring-prod -f "$RELEASE/monitoring/docker-compose.prod.yml")

if [[ "$ACTION" == stop ]]; then
  "${COMPOSE[@]}" stop prometheus grafana alertmanager node-exporter
  exit 0
fi
if [[ "$ACTION" != apply && "$ACTION" != preflight ]]; then
  echo 'Action must be preflight, apply or stop' >&2
  exit 1
fi

mkdir -p "$MONITORING_DASHBOARD_DIR"
jq '.refresh = "1m" | .title = "My Fitness · Production Observability"' \
  "$RELEASE/monitoring/grafana/dashboards/my-fitness.json" > "$MONITORING_DASHBOARD_DIR/my-fitness.json"
chmod 755 "$MONITORING_DASHBOARD_DIR"
chmod 644 "$MONITORING_DASHBOARD_DIR/my-fitness.json"

if [[ "$ACTION" == preflight ]]; then
  "${COMPOSE[@]}" config --quiet
  "${COMPOSE[@]}" pull
  # One-off tools use staged source secrets; active credential volumes stay untouched.
  umask 077
  PREFLIGHT_CREDENTIALS="$(mktemp -d)"
  trap 'rm -rf "$PREFLIGHT_CREDENTIALS"' EXIT
  mkdir -m 700 "$PREFLIGHT_CREDENTIALS/prometheus" "$PREFLIGHT_CREDENTIALS/alertmanager"
  cp "$MONITORING_SECRET_DIR/app.monitoring.password" "$PREFLIGHT_CREDENTIALS/prometheus/metrics_password"
  cp "$MONITORING_SECRET_DIR/slack_webhook_url" "$PREFLIGHT_CREDENTIALS/alertmanager/slack_webhook_url"
  # Replace the credential directory; nested file mounts fail on an empty read-only volume.
  "${COMPOSE[@]}" run --rm --no-deps --user 0:0 --entrypoint promtool \
    -v "$PREFLIGHT_CREDENTIALS/prometheus:/credentials:ro" \
    prometheus check config /etc/prometheus/prometheus.yml
  "${COMPOSE[@]}" run --rm --no-deps --user 0:0 --entrypoint amtool \
    -v "$PREFLIGHT_CREDENTIALS/alertmanager:/credentials:ro" \
    alertmanager check-config /etc/alertmanager/alertmanager.yml
  if (( $(awk '/^MemAvailable:/ {print $2}' /proc/meminfo) < 153600 \
      || $(df -Pk /opt/my-fitness | awk 'END {print $4}') < 5242880 )); then
    echo 'Capacity preflight failed after image pull and configuration checks' >&2
    exit 1
  fi
  exit 0
fi
PREVIOUS="$(readlink -f "$BASE/current" 2>/dev/null || true)"
start_stack() {
  "${COMPOSE[@]}" run --rm secret-init || return 1
  "${COMPOSE[@]}" up -d --no-deps prometheus || return 1
  "${COMPOSE[@]}" up -d --no-deps alertmanager node-exporter || return 1
  "${COMPOSE[@]}" up -d --no-deps grafana || return 1
}
healthy() {
  for _ in $(seq 1 30); do
    if curl --fail --silent http://127.0.0.1:9090/-/ready >/dev/null \
        && curl --fail --silent http://127.0.0.1:9093/-/ready >/dev/null \
        && curl --fail --silent http://127.0.0.1:3001/api/health >/dev/null \
        && curl --fail --silent http://127.0.0.1:9100/metrics >/dev/null; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if start_stack && healthy; then
  ln -sfn "$RELEASE" "$BASE/current.next"
  mv -Tf "$BASE/current.next" "$BASE/current"
  echo 'Monitoring deployment succeeded; app remains independently managed'
  exit 0
fi

echo 'Monitoring deployment failed; app deployment is not rolled back' >&2
"${COMPOSE[@]}" stop prometheus grafana alertmanager node-exporter || true
if [[ "$PREVIOUS" != "$RELEASE" && -f "$PREVIOUS/monitoring/docker-compose.prod.yml" ]]; then
  export MONITORING_DASHBOARD_DIR="$PREVIOUS/generated-dashboards"
  docker compose -p my-fitness-monitoring-prod -f "$PREVIOUS/monitoring/docker-compose.prod.yml" \
    up -d --no-deps prometheus alertmanager node-exporter grafana
  echo 'Previous monitoring configuration restored; verify health separately' >&2
fi
exit 1
