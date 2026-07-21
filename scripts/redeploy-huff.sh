#!/usr/bin/env bash

set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-huff-italian-wordle}"
CONTAINER_NAME="${CONTAINER_NAME:-huff-wordle}"
HOST_PORT="${HOST_PORT:-8083}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"

mkdir -p "${DATA_DIR}"

echo "Building ${IMAGE_NAME}:latest from ${PROJECT_ROOT}"
docker build -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"

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
  "${ENV_ARGS[@]}" \
  -e "PORT=${CONTAINER_PORT}" \
  -v "${DATA_DIR}:/data" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_NAME}:latest"

echo "Deployment complete"
docker ps --filter "name=^${CONTAINER_NAME}$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
