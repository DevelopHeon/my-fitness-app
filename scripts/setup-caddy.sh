#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:?domain is required}"
APP_UPSTREAM="${APP_UPSTREAM:-127.0.0.1:8080}"
CADDY_IMAGE="${CADDY_IMAGE:-caddy:2.11.4-alpine}"
CADDY_NAME="my-fitness-caddy"
BASE_DIR="/opt/my-fitness"
CADDYFILE="$BASE_DIR/Caddyfile"

mkdir -p "$BASE_DIR"

cat > "$CADDYFILE" <<CADDY
$DOMAIN {
	encode zstd gzip

	@webmanifest path /manifest.webmanifest
	header @webmanifest Content-Type "application/manifest+json"

	reverse_proxy $APP_UPSTREAM
}
CADDY

docker pull "$CADDY_IMAGE"

docker run --rm   --network host   -v "$CADDYFILE:/etc/caddy/Caddyfile:ro"   "$CADDY_IMAGE"   caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile

docker rm -f "$CADDY_NAME" >/dev/null 2>&1 || true

docker run -d   --name "$CADDY_NAME"   --restart unless-stopped   --network host   -v "$CADDYFILE:/etc/caddy/Caddyfile:ro"   -v my-fitness-caddy-data:/data   -v my-fitness-caddy-config:/config   "$CADDY_IMAGE" >/dev/null

for _ in $(seq 1 45); do
  if curl --fail --silent --show-error "https://$DOMAIN/actuator/health" >/dev/null 2>&1; then
    echo "Caddy HTTPS setup succeeded: https://$DOMAIN"
    exit 0
  fi
  sleep 2
done

echo "Caddy HTTPS setup failed" >&2
docker logs --tail 200 "$CADDY_NAME" >&2 || true
exit 1
