#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env.staging}"

usage() {
  cat <<EOF
Usage: scripts/redeploy-huff-staging.sh [--no-build] [--skip-migrations]

Build and deploy the staging Huff application with an isolated PostgreSQL container.

Options:
  --no-build          Reuse the existing staging Docker image
  --skip-migrations  Do not run DB migration scripts after app startup
  -h, --help         Show this help

Staging defaults can be overridden in ${ENV_FILE}.
EOF
}

BUILD_IMAGE="true"
RUN_MIGRATIONS="true"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      BUILD_IMAGE="false"
      ;;
    --skip-migrations)
      RUN_MIGRATIONS="false"
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

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck source=/dev/null
  . "${ENV_FILE}"
  set +a
fi

IMAGE_NAME="${IMAGE_NAME:-huff-italian-hexaquot-staging}"
CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot-staging}"
HOST_PORT="${HOST_PORT:-8084}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data/staging}"
DOCKER_NETWORK="${DOCKER_NETWORK:-huff-hexaquot-staging}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres-staging}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
POSTGRES_DB="${POSTGRES_DB:-huff_hexaquot_staging}"
POSTGRES_USER="${POSTGRES_USER:-huff_staging}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff-staging}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_HOST_BIND="${POSTGRES_HOST_BIND:-127.0.0.1}"
POSTGRES_HOST_PORT="${POSTGRES_HOST_PORT:-}"
COOKIE_SECURE="${COOKIE_SECURE:-true}"
AUTH_ENABLED="${AUTH_ENABLED:-true}"
VAPID_SUBJECT="${VAPID_SUBJECT:-mailto:notifications@staging.huff.ottonovembre.it}"

if [[ "${AUTH_ENABLED}" != "true" ]]; then
  echo "Staging deployment refused: AUTH_ENABLED must be true." >&2
  exit 1
fi
if [[ "${COOKIE_SECURE}" != "true" ]]; then
  echo "Staging deployment refused: COOKIE_SECURE must be true." >&2
  exit 1
fi
if [[ -z "${GOOGLE_CLIENT_ID:-}" || -z "${GOOGLE_CLIENT_SECRET:-}" ]]; then
  echo "Staging deployment refused: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are required." >&2
  exit 1
fi

POSTGRES_PORT_ARGS=()
if [[ -n "${POSTGRES_HOST_PORT}" ]]; then
  POSTGRES_PORT_ARGS=(-p "${POSTGRES_HOST_BIND}:${POSTGRES_HOST_PORT}:5432")
fi

mkdir -p "${DATA_DIR}" "${POSTGRES_DATA_DIR}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required but was not found in PATH." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running." >&2
  exit 1
fi

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "Creating Docker network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}" >/dev/null
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

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  if ! postgres_publish_matches; then
    echo "Recreating PostgreSQL container with updated port publishing: ${POSTGRES_CONTAINER_NAME}"
    docker rm -f "${POSTGRES_CONTAINER_NAME}" >/dev/null
  fi
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
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -v "${POSTGRES_DATA_DIR}:/var/lib/postgresql/data" \
    "${POSTGRES_PORT_ARGS[@]}" \
    "${POSTGRES_IMAGE}" >/dev/null
fi

echo "Waiting for PostgreSQL readiness"
for attempt in $(seq 1 45); do
  if docker exec -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
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
  echo "Building ${IMAGE_NAME}:latest from ${PROJECT_ROOT}"
  DOCKER_BUILDKIT=1 docker build -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"
fi

PREVIOUS_IMAGE_ID=""
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  PREVIOUS_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${CONTAINER_NAME}")"
  echo "Stopping and removing existing staging container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi

ENV_ARGS=()
if [[ -f "${ENV_FILE}" ]]; then
  ENV_ARGS+=(--env-file "${ENV_FILE}")
fi

echo "Starting ${CONTAINER_NAME} on 127.0.0.1:${HOST_PORT}->${CONTAINER_PORT}"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network "${DOCKER_NETWORK}" \
  "${ENV_ARGS[@]}" \
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
  "${IMAGE_NAME}:latest" >/dev/null

echo "Waiting for application schema creation"
for attempt in $(seq 1 45); do
  if docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -Atc \
      "SELECT to_regclass('public.users') IS NOT NULL;" 2>/dev/null | grep -Fxq "t"; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "Application did not create the database schema in time." >&2
    exit 1
  fi
  sleep 1
done

if [[ "${RUN_MIGRATIONS}" == "true" ]]; then
  echo "Running staging database migrations"
  export ENV_FILE
  export POSTGRES_CONTAINER_NAME
  export POSTGRES_DB
  export POSTGRES_USER
  export POSTGRES_PASSWORD
  export POSTGRES_PORT
  ENV_FILE="${ENV_FILE}" "${PROJECT_ROOT}/scripts/migrate-game-modes-huff.sh"
  ENV_FILE="${ENV_FILE}" "${PROJECT_ROOT}/scripts/migrate-user-stars-huff.sh"
  ENV_FILE="${ENV_FILE}" "${PROJECT_ROOT}/scripts/migrate-user-profile-huff.sh"
  ENV_FILE="${ENV_FILE}" "${PROJECT_ROOT}/scripts/migrate-admin-users-huff.sh"
fi

NEW_IMAGE_ID="$(docker image inspect --format '{{.Id}}' "${IMAGE_NAME}:latest")"
if [[ -n "${PREVIOUS_IMAGE_ID}" && "${PREVIOUS_IMAGE_ID}" != "${NEW_IMAGE_ID}" ]]; then
  echo "Removing previously used staging image: ${PREVIOUS_IMAGE_ID}"
  docker image rm "${PREVIOUS_IMAGE_ID}" >/dev/null 2>&1 || true
fi

echo "Staging deployment complete"
docker ps --filter "name=^(${CONTAINER_NAME}|${POSTGRES_CONTAINER_NAME})$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
