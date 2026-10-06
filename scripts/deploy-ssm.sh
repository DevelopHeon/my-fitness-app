#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE_TAG="${1:?verified commit SHA is required}"
if ! [[ "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo 'Expected a full commit SHA' >&2
  exit 1
fi
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
RELEASE="/opt/my-fitness/monitoring/releases/$IMAGE_TAG"

# Package the verified Git object, excluding local configuration and test sources.
git -C "$ROOT" archive "$IMAGE_TAG" \
  monitoring/docker-compose.prod.yml \
  monitoring/prometheus/prometheus.prod.yml monitoring/prometheus/alerts.yml \
  monitoring/grafana/dashboards/my-fitness.json \
  monitoring/grafana/provisioning/dashboards/dashboards.yml \
  monitoring/grafana/provisioning/datasources/prometheus.prod.yml \
  monitoring/alertmanager/alertmanager.prod.yml \
  scripts/deploy-ec2.sh scripts/setup-caddy.sh scripts/setup-ec2-monitoring.sh \
  scripts/deploy-monitoring.sh scripts/deploy-release.sh | gzip > "$TEMP/release.tar.gz"
ARCHIVE_B64="$(base64 < "$TEMP/release.tar.gz" | tr -d '\n')"
if (( ${#ARCHIVE_B64} > 48000 )); then
  echo 'Monitoring release archive exceeds the inline SSM delivery budget' >&2
  exit 1
fi
ARCHIVE_SHA="$(sha256sum "$TEMP/release.tar.gz" | cut -d ' ' -f 1)"
jq -n --arg release "$RELEASE" --arg archive "$ARCHIVE_B64" \
  --arg checksum "$ARCHIVE_SHA" --arg tag "$IMAGE_TAG" \
  '{commands: [
    "set -eu",
    ("mkdir -p " + $release),
    ("printf %s " + $archive + " | base64 -d > " + $release + "/release.tar.gz"),
    ("printf \"%s  %s\\n\" " + $checksum + " " + $release + "/release.tar.gz | sha256sum --check --status"),
    ("tar -xzf " + $release + "/release.tar.gz -C " + $release),
    ("bash " + $release + "/scripts/deploy-release.sh " + $tag)
  ], executionTimeout: ["900"]}' > "$TEMP/parameters.json"

INSTANCE_ID="$(aws ssm get-parameter --name /my-fitness/prod/instance-id \
  --query Parameter.Value --output text)"
COMMAND_ID="$(aws ssm send-command --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript --parameters "file://$TEMP/parameters.json" \
  --query Command.CommandId --output text)"

# Initial image pulls can exceed the standard AWS CLI waiter's timeout.
STATUS=Pending
for (( attempt=0; attempt<180; attempt++ )); do
  if STATUS="$(aws ssm get-command-invocation --command-id "$COMMAND_ID" \
      --instance-id "$INSTANCE_ID" --query Status --output text 2> "$TEMP/poll-error")"; then
    case "$STATUS" in
      Success|Failed|Cancelled|TimedOut) break ;;
    esac
  elif [[ "$(cat "$TEMP/poll-error")" != *InvocationDoesNotExist* ]]; then
    cat "$TEMP/poll-error" >&2
    exit 1
  fi
  sleep 5
done
aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}'
if [[ "$STATUS" != Success ]]; then
  echo "SSM deployment did not succeed: $STATUS" >&2
  exit 1
fi
