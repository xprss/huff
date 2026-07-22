#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck source=/dev/null
  . "${ENV_FILE}"
  set +a
fi

IMAGE_NAME="${IMAGE_NAME:-huff-italian-wordle}"
CONTAINER_NAME="${CONTAINER_NAME:-huff-wordle}"
HOST_PORT="${HOST_PORT:-8083}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
DOCKER_NETWORK="${DOCKER_NETWORK:-huff-wordle}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
POSTGRES_DB="${POSTGRES_DB:-huff_wordle}"
POSTGRES_USER="${POSTGRES_USER:-huff}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

mkdir -p "${DATA_DIR}" "${POSTGRES_DATA_DIR}"

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "Creating Docker network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}" >/dev/null
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
    "${POSTGRES_IMAGE}" >/dev/null
fi

echo "Waiting for PostgreSQL readiness"
for attempt in $(seq 1 45); do
  if docker exec "${POSTGRES_CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "PostgreSQL did not become ready in time" >&2
    exit 1
  fi
  sleep 1
done

echo "Building ${IMAGE_NAME}:latest from ${PROJECT_ROOT}"
PREVIOUS_IMAGE_ID=""
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  PREVIOUS_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${CONTAINER_NAME}")"
fi

docker build -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"
NEW_IMAGE_ID="$(docker image inspect --format '{{.Id}}' "${IMAGE_NAME}:latest")"

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  echo "Stopping and removing existing container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}"
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
  -e "PORT=${CONTAINER_PORT}" \
  -e "POSTGRES_HOST=${POSTGRES_CONTAINER_NAME}" \
  -e "POSTGRES_PORT=${POSTGRES_PORT}" \
  -e "POSTGRES_DB=${POSTGRES_DB}" \
  -e "POSTGRES_USER=${POSTGRES_USER}" \
  -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_NAME}:latest"

if [[ -n "${PREVIOUS_IMAGE_ID}" && "${PREVIOUS_IMAGE_ID}" != "${NEW_IMAGE_ID}" ]]; then
  echo "Removing previously used image: ${PREVIOUS_IMAGE_ID}"
  docker image rm "${PREVIOUS_IMAGE_ID}" >/dev/null 2>&1 || true
fi

echo "Deployment complete"
docker ps --filter "name=^(${CONTAINER_NAME}|${POSTGRES_CONTAINER_NAME})$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
