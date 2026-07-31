#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"

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

usage() {
  cat <<EOF
Usage: scripts/run-docker-local.sh [--no-build]

Build and run the application Docker image locally with PostgreSQL.
The application runs in the foreground at http://localhost:${HOST_PORT}.

Options:
  --no-build  Run the existing ${IMAGE_NAME}:latest image without rebuilding it
  -h, --help  Show this help

Local overrides:
  LOCAL_IMAGE_NAME, LOCAL_CONTAINER_NAME, LOCAL_HOST_PORT,
  LOCAL_CONTAINER_PORT, LOCAL_DOCKER_NETWORK,
  LOCAL_POSTGRES_CONTAINER_NAME, LOCAL_POSTGRES_IMAGE,
  LOCAL_POSTGRES_DB, LOCAL_POSTGRES_USER, LOCAL_POSTGRES_PASSWORD,
  LOCAL_POSTGRES_DATA_VOLUME, ENV_FILE
EOF
}

BUILD_IMAGE="true"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      BUILD_IMAGE="false"
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

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running." >&2
  exit 1
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
  docker build -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"
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
exec docker run -d --rm \
  --name "${CONTAINER_NAME}" \
  --network "${DOCKER_NETWORK}" \
  "${ENV_ARGS[@]}" \
  -e "PORT=${CONTAINER_PORT}" \
  -e "POSTGRES_HOST=${POSTGRES_CONTAINER_NAME}" \
  -e "POSTGRES_PORT=5432" \
  -e "POSTGRES_DB=${POSTGRES_DB}" \
  -e "POSTGRES_USER=${POSTGRES_USER}" \
  -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_NAME}:latest"
