#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

usage() {
  cat <<'EOF'
Usage: scripts/deploy-image.sh --environment staging|production \
  --image-ref namespace/repository@sha256:digest \
  --app-version MAJOR.MINOR.PATCH+gha.RUN.SHA \
  --git-commit FULL_SHA

The image is pulled and deployed by immutable digest. The script never builds an
image and never changes VERSION, frontend/src/version.ts, or the Git checkout.
Set ENV_FILE to select the runtime environment file and DIAGNOSTICS_DIR to keep
sanitized inspect data, the readiness response, and application logs.
EOF
}

ENVIRONMENT=""
IMAGE_REF=""
APP_VERSION=""
GIT_COMMIT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --environment)
      [[ $# -ge 2 ]] || { echo "--environment requires a value" >&2; exit 2; }
      ENVIRONMENT="$2"
      shift 2
      ;;
    --image-ref)
      [[ $# -ge 2 ]] || { echo "--image-ref requires a value" >&2; exit 2; }
      IMAGE_REF="$2"
      shift 2
      ;;
    --app-version)
      [[ $# -ge 2 ]] || { echo "--app-version requires a value" >&2; exit 2; }
      APP_VERSION="$2"
      shift 2
      ;;
    --git-commit)
      [[ $# -ge 2 ]] || { echo "--git-commit requires a value" >&2; exit 2; }
      GIT_COMMIT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ "${ENVIRONMENT}" == "staging" || "${ENVIRONMENT}" == "production" ]] || { usage >&2; exit 2; }
[[ -n "${IMAGE_REF}" && -n "${APP_VERSION}" && -n "${GIT_COMMIT}" ]] || { usage >&2; exit 2; }

if [[ "${ENVIRONMENT}" == "staging" ]]; then
  DEFAULT_ENV_FILE="${PROJECT_ROOT}/.env.staging"
  DEFAULT_CONTAINER_NAME="huff-hexaquot-staging"
  DEFAULT_HOST_PORT="8084"
  DEFAULT_DATA_DIR="${PROJECT_ROOT}/data/staging"
  DEFAULT_DOCKER_NETWORK="huff-hexaquot-staging"
  DEFAULT_POSTGRES_CONTAINER_NAME="huff-postgres-staging"
  DEFAULT_POSTGRES_DB="huff_hexaquot_staging"
  DEFAULT_POSTGRES_USER="huff_staging"
  DEFAULT_POSTGRES_PASSWORD="huff-staging"
  DEFAULT_APP_MEMORY="320m"
  DEFAULT_APP_MEMORY_SWAP="448m"
  DEFAULT_VAPID_SUBJECT="mailto:notifications@staging.huff.ottonovembre.it"
else
  DEFAULT_ENV_FILE="${PROJECT_ROOT}/.env"
  DEFAULT_CONTAINER_NAME="huff-hexaquot"
  DEFAULT_HOST_PORT="8083"
  DEFAULT_DATA_DIR="${PROJECT_ROOT}/data"
  DEFAULT_DOCKER_NETWORK="huff-hexaquot"
  DEFAULT_POSTGRES_CONTAINER_NAME="huff-postgres"
  DEFAULT_POSTGRES_DB="huff_hexaquot"
  DEFAULT_POSTGRES_USER="huff"
  DEFAULT_POSTGRES_PASSWORD="huff"
  DEFAULT_APP_MEMORY="384m"
  DEFAULT_APP_MEMORY_SWAP="512m"
  DEFAULT_VAPID_SUBJECT="mailto:notifications@huff.ottonovembre.it"
fi

ENV_FILE="${ENV_FILE:-${DEFAULT_ENV_FILE}}"
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=load-env-file.sh
  . "${SCRIPT_DIR}/load-env-file.sh"
  load_env_file "${ENV_FILE}"
else
  echo "Runtime environment file not found: ${ENV_FILE}" >&2
  exit 1
fi

CONTAINER_NAME="${CONTAINER_NAME:-${DEFAULT_CONTAINER_NAME}}"
HOST_PORT="${HOST_PORT:-${DEFAULT_HOST_PORT}}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
DATA_DIR="${DATA_DIR:-${DEFAULT_DATA_DIR}}"
DOCKER_NETWORK="${DOCKER_NETWORK:-${DEFAULT_DOCKER_NETWORK}}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-${DEFAULT_POSTGRES_CONTAINER_NAME}}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
POSTGRES_DB="${POSTGRES_DB:-${DEFAULT_POSTGRES_DB}}"
POSTGRES_USER="${POSTGRES_USER:-${DEFAULT_POSTGRES_USER}}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-${DEFAULT_POSTGRES_PASSWORD}}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_HOST_BIND="${POSTGRES_HOST_BIND:-127.0.0.1}"
POSTGRES_HOST_PORT="${POSTGRES_HOST_PORT:-}"
APP_MEMORY="${APP_MEMORY:-${DEFAULT_APP_MEMORY}}"
APP_MEMORY_SWAP="${APP_MEMORY_SWAP:-${DEFAULT_APP_MEMORY_SWAP}}"
POSTGRES_MEMORY="${POSTGRES_MEMORY:-160m}"
POSTGRES_MEMORY_SWAP="${POSTGRES_MEMORY_SWAP:-224m}"
COOKIE_SECURE="${COOKIE_SECURE:-true}"
AUTH_ENABLED="${AUTH_ENABLED:-true}"
VAPID_SUBJECT="${VAPID_SUBJECT:-${DEFAULT_VAPID_SUBJECT}}"
DIAGNOSTICS_DIR="${DIAGNOSTICS_DIR:-${PROJECT_ROOT}/deployment-diagnostics/${ENVIRONMENT}}"

[[ "${AUTH_ENABLED}" == "true" ]] || { echo "${ENVIRONMENT} deployment refused: AUTH_ENABLED must be true." >&2; exit 1; }
[[ -n "${GOOGLE_CLIENT_ID:-}" ]] || { echo "${ENVIRONMENT} deployment refused: GOOGLE_CLIENT_ID is required." >&2; exit 1; }

command -v docker >/dev/null 2>&1 || { echo "Docker is required but was not found in PATH." >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "curl is required but was not found in PATH." >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker is not running." >&2; exit 1; }

mkdir -p "${DATA_DIR}" "${POSTGRES_DATA_DIR}" "${DIAGNOSTICS_DIR}"
for diagnostic_file in inspect.json container.log health-response.json health-curl.log; do
  : > "${DIAGNOSTICS_DIR}/${diagnostic_file}"
done

capture_diagnostics() {
  local exit_code=$?
  set +e

  if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
    docker inspect --format \
      '{"name":{{json .Name}},"image":{{json .Config.Image}},"state":{{json .State}}}' \
      "${CONTAINER_NAME}" > "${DIAGNOSTICS_DIR}/inspect.json" 2>/dev/null
    docker logs --tail 500 "${CONTAINER_NAME}" > "${DIAGNOSTICS_DIR}/container.log" 2>&1
  fi

  curl --silent --show-error --max-time 10 \
    "http://127.0.0.1:${HOST_PORT}/q/health/ready" \
    > "${DIAGNOSTICS_DIR}/health-response.json" 2> "${DIAGNOSTICS_DIR}/health-curl.log"

  return "${exit_code}"
}
trap capture_diagnostics EXIT

"${SCRIPT_DIR}/version.sh" verify-image \
  --image-ref "${IMAGE_REF}" \
  --app-version "${APP_VERSION}" \
  --git-commit "${GIT_COMMIT}"

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "Creating Docker network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}" >/dev/null
fi

POSTGRES_PORT_ARGS=()
if [[ -n "${POSTGRES_HOST_PORT}" ]]; then
  POSTGRES_PORT_ARGS=(-p "${POSTGRES_HOST_BIND}:${POSTGRES_HOST_PORT}:5432")
fi

postgres_publish_matches() {
  local published
  published="$(docker port "${POSTGRES_CONTAINER_NAME}" 5432/tcp 2>/dev/null || true)"
  if [[ -z "${POSTGRES_HOST_PORT}" ]]; then
    [[ -z "${published}" ]]
  else
    [[ "${published}" == *"${POSTGRES_HOST_BIND}:${POSTGRES_HOST_PORT}"* ]]
  fi
}

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}" && ! postgres_publish_matches; then
  echo "Recreating PostgreSQL container with updated port publishing: ${POSTGRES_CONTAINER_NAME}"
  docker rm -f "${POSTGRES_CONTAINER_NAME}" >/dev/null
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    echo "Starting existing PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
    docker start "${POSTGRES_CONTAINER_NAME}" >/dev/null
  else
    echo "PostgreSQL container already running: ${POSTGRES_CONTAINER_NAME}"
  fi
else
  echo "Starting PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
  docker run -d \
    --name "${POSTGRES_CONTAINER_NAME}" \
    --restart unless-stopped \
    --network "${DOCKER_NETWORK}" \
    --memory "${POSTGRES_MEMORY}" \
    --memory-swap "${POSTGRES_MEMORY_SWAP}" \
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -v "${POSTGRES_DATA_DIR}:/var/lib/postgresql/data" \
    "${POSTGRES_PORT_ARGS[@]}" \
    "${POSTGRES_IMAGE}" >/dev/null
fi

echo "Waiting for PostgreSQL readiness"
for attempt in $(seq 1 45); do
  if docker exec "${POSTGRES_CONTAINER_NAME}" \
    pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "PostgreSQL did not become ready in time." >&2
    exit 1
  fi
  sleep 1
done

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  echo "Removing existing ${ENVIRONMENT} application container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi

echo "Starting ${CONTAINER_NAME} from immutable image ${IMAGE_REF}"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network "${DOCKER_NETWORK}" \
  --memory "${APP_MEMORY}" \
  --memory-swap "${APP_MEMORY_SWAP}" \
  --env-file "${ENV_FILE}" \
  -e "AUTH_ENABLED=${AUTH_ENABLED}" \
  -e "COOKIE_SECURE=${COOKIE_SECURE}" \
  -e "VAPID_SUBJECT=${VAPID_SUBJECT}" \
  -e "PORT=${CONTAINER_PORT}" \
  -e "POSTGRES_HOST=${POSTGRES_CONTAINER_NAME}" \
  -e "POSTGRES_PORT=${POSTGRES_PORT}" \
  -e "POSTGRES_DB=${POSTGRES_DB}" \
  -e "POSTGRES_USER=${POSTGRES_USER}" \
  -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_REF}" >/dev/null

echo "Waiting for database migrations"
for attempt in $(seq 1 45); do
  if docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -Atc \
      "SELECT to_regclass('public.users') IS NOT NULL;" 2>/dev/null | grep -Fxq "t"; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "Flyway did not create the database schema in time." >&2
    exit 1
  fi
  sleep 1
done

echo "Waiting for application readiness"
for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error --max-time 5 \
    "http://127.0.0.1:${HOST_PORT}/q/health/ready" \
    > "${DIAGNOSTICS_DIR}/health-response.json" 2> "${DIAGNOSTICS_DIR}/health-curl.log"; then
    break
  fi
  if [[ "${attempt}" -eq 60 ]]; then
    echo "Application did not become ready in time." >&2
    exit 1
  fi
  sleep 2
done

running_image="$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}")"
[[ "${running_image}" == "${IMAGE_REF}" ]] || { echo "Running image mismatch: ${running_image}" >&2; exit 1; }

trap - EXIT
capture_diagnostics

echo "${ENVIRONMENT} deployment complete: ${APP_VERSION} (${GIT_COMMIT})"
docker ps --filter "name=^(${CONTAINER_NAME}|${POSTGRES_CONTAINER_NAME})$" \
  --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
