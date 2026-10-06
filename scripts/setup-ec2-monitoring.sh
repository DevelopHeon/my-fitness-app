#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ "$EUID" != 0 || "$(uname -s)" != Linux || "$(uname -m)" != aarch64 ]]; then
  echo 'Run as root on the ARM64 Linux application host' >&2
  exit 1
fi

BASE=/opt/my-fitness/monitoring
REGION="$(cat /opt/my-fitness/region)"
STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT

fetch_secret() {
  local parameter="$1"
  local file="$2"
  local value
  if ! value="$(aws ssm get-parameter --region "$REGION" --name "/my-fitness/prod/$parameter" \
      --with-decryption --query Parameter.Value --output text 2>/dev/null)"; then
    echo "Cannot load required parameter: $parameter" >&2
    return 1
  fi
  if [[ -z "$value" || "$value" =~ [[:space:]] ]]; then
    echo "Required parameter must be non-empty without whitespace: $parameter" >&2
    return 1
  fi
  printf '%s' "$value" > "$STAGING/$file"
}

# Validate all values before replacing any active credentials.
fetch_secret monitoring-password app.monitoring.password
fetch_secret grafana-admin-password grafana_admin_password
fetch_secret slack-webhook-url slack_webhook_url
if ! [[ "$(cat "$STAGING/slack_webhook_url")" =~ ^https://hooks\.slack\.com/services/[^[:space:]]+$ ]]; then
  echo 'Slack parameter must be an Incoming Webhook HTTPS URL' >&2
  exit 1
fi

AVAILABLE_KIB="$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)"
DISK_KIB="$(df -Pk /opt/my-fitness | awk 'END {print $4}')"
if (( AVAILABLE_KIB < 153600 || DISK_KIB < 5242880 )); then
  echo 'Preflight failed: require 150MiB available RAM and 5GiB free disk; stop monitoring first' >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  curl --fail --silent --show-error --location \
    https://github.com/docker/compose/releases/download/v5.5.1/docker-compose-linux-aarch64 \
    --output "$STAGING/docker-compose"
  printf '%s  %s\n' 732e3a84c1a0f67256ce80bc2598a24546b10ca05f9faa97efceb1171ece2ef7 \
    "$STAGING/docker-compose" | sha256sum --check --status
  install -D -m 755 "$STAGING/docker-compose" /usr/local/lib/docker/cli-plugins/docker-compose
fi
docker compose version

if [[ ! -e /swapfile ]]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
fi
if [[ ! -f /swapfile || -L /swapfile || "$(stat -c %s /swapfile)" != 2147483648 ]]; then
  echo 'Existing swapfile must be a regular 2GiB file; preserve and inspect it manually' >&2
  exit 1
fi
chown root:root /swapfile
chmod 600 /swapfile
if ! swapon --show=NAME --noheadings | grep -Fxq /swapfile; then
  swapon /swapfile
fi
if ! awk '$1 == "/swapfile" {found=1} END {exit !found}' /etc/fstab; then
  printf '/swapfile swap swap defaults 0 0\n' >> /etc/fstab
fi
mkdir -p "$BASE"
if [[ ! -f "$BASE/previous-swappiness" ]]; then
  sysctl -n vm.swappiness > "$BASE/previous-swappiness"
fi
printf 'vm.swappiness=10\n' > /etc/sysctl.d/99-my-fitness-monitoring.conf
sysctl -p /etc/sysctl.d/99-my-fitness-monitoring.conf >/dev/null
install -d -m 700 "$BASE/secrets"
for file in app.monitoring.password grafana_admin_password slack_webhook_url; do
  install -m 600 "$STAGING/$file" "$BASE/secrets/$file.next"
  mv "$BASE/secrets/$file.next" "$BASE/secrets/$file"
done
echo 'Host resources and monitoring secret files are ready'
