#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG="${1:?image tag is required}"
PARAM_PREFIX="/my-fitness/prod"
APP_NAME="my-fitness-app"
ENV_FILE="/opt/my-fitness/runtime.env"
REGION_FILE="/opt/my-fitness/region"

if [[ ! -f "$REGION_FILE" ]]; then
  echo "AWS region file not found: $REGION_FILE" >&2
  exit 1
fi

REGION="$(cat "$REGION_FILE")"

get_parameter() {
  aws ssm get-parameter \
    --region "$REGION" \
    --name "$1" \
    --query "Parameter.Value" \
    --output text
}

get_optional_parameter() {
  aws ssm get-parameter \
    --region "$REGION" \
    --name "$1" \
    --with-decryption \
    --query "Parameter.Value" \
    --output text 2>/dev/null || true
}

get_policy_parameter() {
  local result
  if result="$(aws ssm get-parameter --region "$REGION" --name "$1" --with-decryption --query "Parameter.Value" --output text 2>&1)"; then
    printf '%s' "$result"
  elif [[ "$result" == *ParameterNotFound* ]]; then
    return 0
  else
    echo "Unable to load AI policy parameter: $1" >&2
    return 1
  fi
}

REPOSITORY_URI="$(get_parameter "$PARAM_PREFIX/ecr-repository-uri")"
DB_HOST="$(get_parameter "$PARAM_PREFIX/db-host")"
DB_SECRET_ARN="$(get_parameter "$PARAM_PREFIX/db-secret-arn")"
DB_SECRET="$(aws secretsmanager get-secret-value \
  --region "$REGION" \
  --secret-id "$DB_SECRET_ARN" \
  --query SecretString \
  --output text)"

DB_USERNAME="$(printf '%s' "$DB_SECRET" | jq -r '.username')"
DB_PASSWORD="$(printf '%s' "$DB_SECRET" | jq -r '.password')"
GOOGLE_CLIENT_ID="$(get_optional_parameter "$PARAM_PREFIX/google-client-id")"
GOOGLE_CLIENT_SECRET="$(get_optional_parameter "$PARAM_PREFIX/google-client-secret")"
OPENAI_API_KEY="$(get_optional_parameter "$PARAM_PREFIX/openai-api-key")"
APP_BASE_URL="$(get_optional_parameter "$PARAM_PREFIX/app-base-url")"
AI_PROVIDER="$(get_optional_parameter "$PARAM_PREFIX/ai-provider")"

TYPESAFE_API_KEY="$(get_policy_parameter "$PARAM_PREFIX/typesafe-api-key")"
if [[ -z "$TYPESAFE_API_KEY" ]]; then
  echo "AI policy key is required for JEV policy validation" >&2
  exit 1
fi
AI_POLICY_MODEL="$(get_policy_parameter "$PARAM_PREFIX/ai-policy-model")"
AI_POLICY_VERSION="$(get_policy_parameter "$PARAM_PREFIX/ai-policy-version")"
: "${AI_POLICY_MODEL:=jev-1.13.0}"
: "${AI_POLICY_VERSION:=fitness-policy-v1}"

: "${AI_PROVIDER:=none}"

install -m 600 /dev/null "$ENV_FILE"
{
  printf 'SPRING_PROFILES_ACTIVE=prod\n'
  printf 'DB_URL=jdbc:postgresql://%s:5432/my_fitness\n' "$DB_HOST"
  printf 'DB_USERNAME=%s\n' "$DB_USERNAME"
  printf 'DB_PASSWORD=%s\n' "$DB_PASSWORD"
  printf 'DB_POOL_MAX_SIZE=5\n'
  printf 'DB_POOL_MIN_IDLE=1\n'
  printf 'AI_PROVIDER=%s\n' "$AI_PROVIDER"
  printf 'AI_POLICY_MODEL=%s\n' "$AI_POLICY_MODEL"
  printf 'AI_POLICY_VERSION=%s\n' "$AI_POLICY_VERSION"
  if [[ -n "$TYPESAFE_API_KEY" ]]; then
    printf 'TYPESAFE_API_KEY=%s\n' "$TYPESAFE_API_KEY"
  fi
  printf 'JAVA_TOOL_OPTIONS=-Xms128m -Xmx512m -XX:+UseG1GC\n'

  if [[ -n "$GOOGLE_CLIENT_ID" ]]; then
    printf 'GOOGLE_CLIENT_ID=%s\n' "$GOOGLE_CLIENT_ID"
  fi
  if [[ -n "$GOOGLE_CLIENT_SECRET" ]]; then
    printf 'GOOGLE_CLIENT_SECRET=%s\n' "$GOOGLE_CLIENT_SECRET"
  fi
  if [[ -n "$OPENAI_API_KEY" ]]; then
    printf 'OPENAI_API_KEY=%s\n' "$OPENAI_API_KEY"
  fi
  if [[ -n "$APP_BASE_URL" ]]; then
    printf 'FRONTEND_ORIGIN=%s\n' "$APP_BASE_URL"
    printf 'LOGIN_SUCCESS_URL=%s\n' "$APP_BASE_URL"
    printf 'SESSION_COOKIE_SECURE=true\n'
  else
    printf 'SESSION_COOKIE_SECURE=false\n'
  fi
} >> "$ENV_FILE"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${REPOSITORY_URI%%/*}"

NEW_IMAGE="${REPOSITORY_URI}:${IMAGE_TAG}"
OLD_IMAGE="$(docker inspect --format '{{.Config.Image}}' "$APP_NAME" 2>/dev/null || true)"

docker pull "$NEW_IMAGE"

start_container() {
  local image="$1"
  docker rm -f "$APP_NAME" >/dev/null 2>&1 || true
  docker run -d \
    --name "$APP_NAME" \
    --restart unless-stopped \
    --memory 700m \
    --env-file "$ENV_FILE" \
    -p 127.0.0.1:8080:8080 \
    "$image" >/dev/null
}

healthy() {
  for _ in $(seq 1 30); do
    if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then
      return 0
    fi
    sleep 2
  done
  return 1
}

start_container "$NEW_IMAGE"

if healthy; then
  printf '%s\n' "$IMAGE_TAG" > /opt/my-fitness/current-image
  docker image prune -f >/dev/null 2>&1 || true
  echo "Deployment succeeded: $NEW_IMAGE"
  exit 0
fi

echo "Health check failed for $NEW_IMAGE" >&2
docker logs --tail 200 "$APP_NAME" >&2 || true

if [[ -n "$OLD_IMAGE" ]]; then
  echo "Rolling back to $OLD_IMAGE" >&2
  start_container "$OLD_IMAGE"
  if healthy; then
    echo "Rollback succeeded: $OLD_IMAGE" >&2
  else
    echo "Rollback health check also failed" >&2
  fi
fi

exit 1
