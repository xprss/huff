#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=load-env-file.sh
  . "${SCRIPT_DIR}/load-env-file.sh"
  load_env_file "${ENV_FILE}"
fi

IMAGE_NAME="${LOCAL_IMAGE_NAME:-huff-local}"
CONTAINER_NAME="${LOCAL_CONTAINER_NAME:-huff-local}"
HOST_PORT="${LOCAL_HOST_PORT:-8080}"
CONTAINER_PORT="${LOCAL_CONTAINER_PORT:-8080}"
DOCKER_NETWORK="${LOCAL_DOCKER_NETWORK:-huff-local}"
POSTGRES_CONTAINER_NAME="${LOCAL_POSTGRES_CONTAINER_NAME:-huff-postgres-local}"
POSTGRES_IMAGE="${LOCAL_POSTGRES_IMAGE:-postgres:16-alpine}"
POSTGRES_DB="${LOCAL_POSTGRES_DB:-huff_hexaquot}"
POSTGRES_USER="${LOCAL_POSTGRES_USER:-huff}"
POSTGRES_PASSWORD="${LOCAL_POSTGRES_PASSWORD:-huff}"
POSTGRES_DATA_VOLUME="${LOCAL_POSTGRES_DATA_VOLUME:-huff-postgres-local-data}"
# Local Docker runs use the authentication configured in .env by default.
# LOCAL_AUTH_ENABLED remains available for deliberately disabling it.
LOCAL_AUTH_ENABLED="${LOCAL_AUTH_ENABLED:-${AUTH_ENABLED:-true}}"
LOCAL_COOKIE_SECURE="${LOCAL_COOKIE_SECURE:-false}"
LOCAL_AGENT_DATA_DIR="${LOCAL_AGENT_DATA_DIR:-${PROJECT_ROOT}/data/local-agents}"
POSTGRES_MCP_USER="${POSTGRES_MCP_USER:-huff_stats_agent}"
POSTGRES_MCP_DATABASE="${POSTGRES_DB}"
POSTGRES_MCP_HOST="${POSTGRES_CONTAINER_NAME}"
POSTGRES_MCP_PORT="5432"

usage() {
  cat <<EOF
Usage: scripts/run-docker-local.sh [--no-build] [--fresh-db]

Build and run the application Docker image locally with PostgreSQL.
The application runs in the foreground at http://localhost:${HOST_PORT}.

Options:
  --no-build  Run the existing ${IMAGE_NAME}:latest image without rebuilding it
  --fresh-db  Recreate the disposable local PostgreSQL volume before starting
  -h, --help  Show this help

Local overrides:
  LOCAL_IMAGE_NAME, LOCAL_CONTAINER_NAME, LOCAL_HOST_PORT,
  LOCAL_CONTAINER_PORT, LOCAL_DOCKER_NETWORK,
  LOCAL_POSTGRES_CONTAINER_NAME, LOCAL_POSTGRES_IMAGE,
  LOCAL_POSTGRES_DB, LOCAL_POSTGRES_USER, LOCAL_POSTGRES_PASSWORD,
  LOCAL_POSTGRES_DATA_VOLUME, LOCAL_AUTH_ENABLED, LOCAL_COOKIE_SECURE, ENV_FILE
  POSTGRES_MCP_USER, POSTGRES_MCP_PASSWORD, OPENAI_API_KEY, GAME_STATS_AGENT_TOKEN,
  LOCAL_AGENT_DATA_DIR
EOF
}

BUILD_IMAGE="true"
FRESH_DB="false"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      BUILD_IMAGE="false"
      ;;
    --fresh-db)
      FRESH_DB="true"
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
  shift
done

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required but was not found in PATH." >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to check application readiness." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running." >&2
  exit 1
fi

if [[ "${LOCAL_AUTH_ENABLED}" != "true" && "${LOCAL_AUTH_ENABLED}" != "false" ]]; then
  echo "LOCAL_AUTH_ENABLED must be true or false." >&2
  exit 1
fi

if [[ "${LOCAL_AUTH_ENABLED}" == "true" && -z "${GOOGLE_CLIENT_ID:-}" ]]; then
  echo "Local Docker run refused: GOOGLE_CLIENT_ID is required when authentication is enabled." >&2
  exit 1
fi

if [[ -z "${OPENAI_API_KEY:-}" || -z "${GAME_STATS_AGENT_TOKEN:-}" || "${GAME_STATS_AGENT_TOKEN:-}" == replace-with-a-long-random-secret \
  || -z "${POSTGRES_MCP_PASSWORD:-}" || "${POSTGRES_MCP_PASSWORD:-}" == replace-with-a-separate-read-only-database-password \
  || "${POSTGRES_MCP_PASSWORD:-}" == SET_A_READ_ONLY_DATABASE_PASSWORD ]]; then
  echo "Local Docker run requires OPENAI_API_KEY, GAME_STATS_AGENT_TOKEN, and POSTGRES_MCP_PASSWORD in ENV_FILE." >&2
  exit 1
fi

mkdir -p "${LOCAL_AGENT_DATA_DIR}"

if [[ "${FRESH_DB}" == "true" ]]; then
  echo "Recreating disposable local database volume: ${POSTGRES_DATA_VOLUME}"
  if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
    docker rm -f "${CONTAINER_NAME}" >/dev/null
  fi
  if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    docker rm -f "${POSTGRES_CONTAINER_NAME}" >/dev/null
  fi
  if docker volume inspect "${POSTGRES_DATA_VOLUME}" >/dev/null 2>&1; then
    docker volume rm "${POSTGRES_DATA_VOLUME}" >/dev/null
  fi
fi

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "Creating Docker network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}" >/dev/null
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    echo "Starting existing PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
    docker start "${POSTGRES_CONTAINER_NAME}" >/dev/null
  fi
else
  echo "Starting PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
  docker run -d \
    --name "${POSTGRES_CONTAINER_NAME}" \
    --network "${DOCKER_NETWORK}" \
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -v "${POSTGRES_DATA_VOLUME}:/var/lib/postgresql/data" \
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

if [[ "${BUILD_IMAGE}" == "true" ]]; then
  echo "Building ${IMAGE_NAME}:latest"
  LOCAL_APP_VERSION="$("${SCRIPT_DIR}/version.sh" read)"
  LOCAL_GIT_COMMIT="$(git -C "${PROJECT_ROOT}" rev-parse HEAD)"
  DOCKER_BUILDKIT=1 docker build \
    --build-arg "APP_VERSION=${LOCAL_APP_VERSION}" \
    --build-arg "GIT_COMMIT=${LOCAL_GIT_COMMIT}" \
    --build-arg "GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:-}" \
    -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  echo "Removing existing local application container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi

ENV_ARGS=()
if [[ -f "${ENV_FILE}" ]]; then
  ENV_ARGS+=(--env-file "${ENV_FILE}")
fi

echo "Running ${IMAGE_NAME}:latest at http://localhost:${HOST_PORT} (Ctrl-C to stop)"
docker run -d --rm \
  --name "${CONTAINER_NAME}" \
  --network "${DOCKER_NETWORK}" \
  "${ENV_ARGS[@]}" \
  -e "AUTH_ENABLED=${LOCAL_AUTH_ENABLED}" \
  -e "COOKIE_SECURE=${LOCAL_COOKIE_SECURE}" \
  -e "PORT=${CONTAINER_PORT}" \
  -e "POSTGRES_HOST=${POSTGRES_CONTAINER_NAME}" \
  -e "POSTGRES_PORT=5432" \
  -e "POSTGRES_DB=${POSTGRES_DB}" \
  -e "POSTGRES_USER=${POSTGRES_USER}" \
  -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
  -e "POSTGRES_MCP_USER=${POSTGRES_MCP_USER}" \
  -e "POSTGRES_MCP_PASSWORD=${POSTGRES_MCP_PASSWORD}" \
  -e "POSTGRES_MCP_DATABASE=${POSTGRES_MCP_DATABASE}" \
  -e "POSTGRES_MCP_HOST=${POSTGRES_MCP_HOST}" \
  -e "POSTGRES_MCP_PORT=${POSTGRES_MCP_PORT}" \
  -e "GAME_STATS_AGENT_HOST=127.0.0.1" \
  -e "GAME_STATS_AGENT_PORT=4111" \
  -e "GAME_STATS_AGENT_URL=http://127.0.0.1:4111" \
  -e "GAME_STATS_AGENT_DB_URL=file:./data/mastra.db" \
  -v "${LOCAL_AGENT_DATA_DIR}:/app/agents/data" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_NAME}:latest"
trap 'docker stop "${CONTAINER_NAME}" >/dev/null 2>&1 || true' EXIT INT TERM

echo "Waiting for database migrations"
for attempt in $(seq 1 45); do
  if docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -Atc \
      "SELECT to_regclass('public.users') IS NOT NULL;" 2>/dev/null | grep -Fxq "t"; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "Flyway did not create the database schema in time." >&2
    docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
    exit 1
  fi
  sleep 1
done

echo "Waiting for application readiness"
for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error --max-time 5 "http://127.0.0.1:${HOST_PORT}/q/health/ready" >/dev/null 2>&1; then
    break
  fi
  if [[ "${attempt}" -eq 60 ]]; then
    echo "Application did not become ready in time." >&2
    exit 1
  fi
  sleep 2
done

POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME}" \
POSTGRES_DB="${POSTGRES_DB}" \
POSTGRES_USER="${POSTGRES_USER}" \
POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
POSTGRES_MCP_USER="${POSTGRES_MCP_USER}" \
POSTGRES_MCP_PASSWORD="${POSTGRES_MCP_PASSWORD}" \
  "${SCRIPT_DIR}/provision-game-stats-role.sh"

echo "Local app ready at http://localhost:${HOST_PORT} (Ctrl-C to stop)"
docker logs -f "${CONTAINER_NAME}"
