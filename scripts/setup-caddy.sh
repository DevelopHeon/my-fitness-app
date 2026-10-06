#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:?domain is required}"
APP_UPSTREAM="${APP_UPSTREAM:-127.0.0.1:8080}"
CADDY_IMAGE="${CADDY_IMAGE:-caddy:2.11.4-alpine}"
CADDY_NAME="my-fitness-caddy"
BASE_DIR="/opt/my-fitness"
CADDYFILE="$BASE_DIR/Caddyfile"
NEXT_CADDYFILE="$CADDYFILE.next"
PREVIOUS_CADDYFILE="$CADDYFILE.previous"
trap 'rm -f "$NEXT_CADDYFILE"' EXIT

if ! [[ "$DOMAIN" =~ ^[a-zA-Z0-9.-]+$ ]]; then
  echo 'Invalid public domain' >&2
  exit 1
fi

mkdir -p "$BASE_DIR"

cat > "$NEXT_CADDYFILE" <<CADDY
$DOMAIN {
	encode zstd gzip

	@webmanifest path /manifest.webmanifest
	header @webmanifest >Content-Type "application/manifest+json"

	@privateActuator {
		path /actuator /actuator/*
		not path /actuator/health
	}
	respond @privateActuator 404

	reverse_proxy $APP_UPSTREAM
}
CADDY

docker pull "$CADDY_IMAGE"

docker run --rm --memory 48m --memory-swap 48m \
  -v "$NEXT_CADDYFILE:/etc/caddy/Caddyfile:ro" \
  "$CADDY_IMAGE" caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile

OLD_IMAGE="$(docker inspect --format '{{.Config.Image}}' "$CADDY_NAME" 2>/dev/null || true)"
OLD_MEMORY="$(docker inspect --format '{{.HostConfig.Memory}}' "$CADDY_NAME" 2>/dev/null || true)"
OLD_SWAP="$(docker inspect --format '{{.HostConfig.MemorySwap}}' "$CADDY_NAME" 2>/dev/null || true)"
: "${OLD_MEMORY:=0}"
: "${OLD_SWAP:=0}"
if [[ -f "$CADDYFILE" ]]; then
  cp -p "$CADDYFILE" "$PREVIOUS_CADDYFILE"
fi
mv "$NEXT_CADDYFILE" "$CADDYFILE"

start_caddy() {
  local image="$1"
  local memory="$2"
  local memory_swap="$3"
  docker rm -f "$CADDY_NAME" >/dev/null 2>&1 || true
  docker run -d --name "$CADDY_NAME" --restart unless-stopped --network host \
    --memory "$memory" --memory-swap "$memory_swap" \
    --log-driver local --log-opt max-size=10m --log-opt max-file=3 \
    -v "$CADDYFILE:/etc/caddy/Caddyfile:ro" \
    -v my-fitness-caddy-data:/data -v my-fitness-caddy-config:/config \
    "$image" >/dev/null
}

healthy() {
  for _ in $(seq 1 45); do
    if curl --fail --silent "https://$DOMAIN/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if start_caddy "$CADDY_IMAGE" 48m 48m && healthy; then
  echo "Caddy HTTPS setup succeeded: https://$DOMAIN"
  exit 0
fi

echo "Caddy HTTPS setup failed" >&2
if [[ -n "$OLD_IMAGE" && -f "$PREVIOUS_CADDYFILE" ]]; then
  cp -p "$PREVIOUS_CADDYFILE" "$CADDYFILE"
  start_caddy "$OLD_IMAGE" "$OLD_MEMORY" "$OLD_SWAP"
  healthy || echo 'Previous Caddy health check failed' >&2
fi
exit 1
