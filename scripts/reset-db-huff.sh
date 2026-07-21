#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

CONTAINER_NAME="${CONTAINER_NAME:-huff-wordle}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
DB_FILE="${DB_FILE:-${DATA_DIR}/huff-wordle.sqlite}"
BACKUP_DIR="${BACKUP_DIR:-${DATA_DIR}/backups}"
STAMP="$(date -u +%Y%m%d%H%M%S)"
RESET_DIR="${BACKUP_DIR}/reset-${STAMP}"

mkdir -p "${RESET_DIR}"

echo "Stopping ${CONTAINER_NAME} before resetting the database"
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker rm -f "${CONTAINER_NAME}"
else
  echo "Container not found: ${CONTAINER_NAME}"
fi

found_db=false
for suffix in "" "-wal" "-shm"; do
  file="${DB_FILE}${suffix}"
  if [[ -e "${file}" ]]; then
    found_db=true
    mv "${file}" "${RESET_DIR}/$(basename "${file}")"
  fi
done

if [[ "${found_db}" == true ]]; then
  echo "Previous database files moved to ${RESET_DIR}"
else
  echo "No database files found at ${DB_FILE}; continuing with a fresh deploy"
  rmdir "${RESET_DIR}" 2>/dev/null || true
fi

echo "Starting Wordolino with a fresh database"
"${SCRIPT_DIR}/redeploy-huff.sh"
