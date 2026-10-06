#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG="${1:?verified commit SHA is required}"
if ! [[ "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo 'Expected a full commit SHA' >&2
  exit 1
fi
RELEASE="/opt/my-fitness/monitoring/releases/$IMAGE_TAG"
REGION="$(cat /opt/my-fitness/region)"
APP_URL="$(aws ssm get-parameter --region "$REGION" --name /my-fitness/prod/app-base-url \
  --with-decryption --query Parameter.Value --output text 2>/dev/null)"
if ! [[ "$APP_URL" =~ ^https://([a-zA-Z0-9.-]+)/*$ ]]; then
  echo 'A public HTTPS app-base-url without a path is required' >&2
  exit 1
fi
DOMAIN="${BASH_REMATCH[1]}"

bash "$RELEASE/scripts/setup-ec2-monitoring.sh"
bash "$RELEASE/scripts/deploy-monitoring.sh" "$RELEASE" preflight
bash "$RELEASE/scripts/deploy-ec2.sh" "$IMAGE_TAG" preflight
# Protect public metrics before enabling the monitoring profile.
bash "$RELEASE/scripts/setup-caddy.sh" "$DOMAIN"
bash "$RELEASE/scripts/deploy-monitoring.sh" "$RELEASE" stop
if ! bash "$RELEASE/scripts/deploy-ec2.sh" "$IMAGE_TAG"; then
  echo 'App deployment failed; monitoring remains stopped to preserve app resources' >&2
  exit 1
fi
bash "$RELEASE/scripts/deploy-monitoring.sh" "$RELEASE" apply
