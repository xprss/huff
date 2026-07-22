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

CONTAINER_NAME="${CONTAINER_NAME:-huff-wordle}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"

echo "Stopping ${CONTAINER_NAME} before resetting the database"
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker rm -f "${CONTAINER_NAME}"
else
  echo "Container not found: ${CONTAINER_NAME}"
fi

echo "Stopping ${POSTGRES_CONTAINER_NAME} before deleting PostgreSQL data"
if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  docker rm -f "${POSTGRES_CONTAINER_NAME}"
else
  echo "Container not found: ${POSTGRES_CONTAINER_NAME}"
fi

if [[ -d "${POSTGRES_DATA_DIR}" ]]; then
  echo "Deleting PostgreSQL data directory: ${POSTGRES_DATA_DIR}"
  rm -rf "${POSTGRES_DATA_DIR}"
else
  echo "PostgreSQL data directory not found: ${POSTGRES_DATA_DIR}"
fi

echo "Deleting old SQLite database files, if present"
rm -f "${DATA_DIR}/huff-wordle.sqlite" "${DATA_DIR}/huff-wordle.sqlite-wal" "${DATA_DIR}/huff-wordle.sqlite-shm"

echo "Starting Indovena with a fresh database"
"${SCRIPT_DIR}/redeploy-huff.sh"
