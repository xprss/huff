#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env.staging}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=load-env-file.sh
  . "${SCRIPT_DIR}/load-env-file.sh"
  load_env_file "${ENV_FILE}"
fi

CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot-staging}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres-staging}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data/staging}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"
BACKUP_ROOT="${PROJECT_ROOT}/data/backups"

case "${POSTGRES_DATA_DIR}" in
  "${PROJECT_ROOT}"|"${PROJECT_ROOT}/"|"/"|"")
    echo "Unsafe PostgreSQL data directory: ${POSTGRES_DATA_DIR}" >&2
    exit 1
    ;;
esac

echo "Stopping the experimental staging application and database"
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi
if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  docker rm -f "${POSTGRES_CONTAINER_NAME}" >/dev/null
fi

if [[ -d "${POSTGRES_DATA_DIR}" ]]; then
  backup_dir="${BACKUP_ROOT}/staging-before-hexahack-$(date +%Y%m%d%H%M%S)"
  mkdir -p "${BACKUP_ROOT}"
  mv "${POSTGRES_DATA_DIR}" "${backup_dir}"
  echo "Previous experimental database moved to ${backup_dir}"
fi

echo "Recreating staging from Flyway V1-V12"
"${SCRIPT_DIR}/redeploy-huff-staging.sh"
