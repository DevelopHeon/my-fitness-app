#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRET_DIR="$TASK_ROOT/.local/monitoring/secrets"
umask 077
mkdir -p "$SECRET_DIR"
chmod 700 "$SECRET_DIR"
for name in app.monitoring.password grafana_admin_password; do
  if [[ ! -e "$SECRET_DIR/$name" ]]; then
    openssl rand -hex 32 > "$SECRET_DIR/$name"
  fi
  if [[ ! -s "$SECRET_DIR/$name" ]]; then
    echo "Empty monitoring secret file: $name" >&2
    exit 1
  fi
  chmod 600 "$SECRET_DIR/$name"
done
echo 'Local monitoring secret files are ready; existing values were preserved.'
