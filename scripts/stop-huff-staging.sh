#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env.staging}"

usage() {
  cat <<EOF
Usage: scripts/stop-huff-staging.sh [--delete-db]

Stop the staging Huff application and PostgreSQL database.

Options:
  --delete-db  Remove the staging PostgreSQL container and delete its data
  -h, --help   Show this help

Staging defaults can be overridden in ${ENV_FILE}.
EOF
}

DELETE_DB="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --delete-db)
      DELETE_DB="true"
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

CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot-staging}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data/staging}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres-staging}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required but was not found in PATH." >&2
  exit 1
fi

container_exists() {
  docker ps -a --format '{{.Names}}' | grep -Fxq "$1"
}

stop_container() {
  local container_name="$1"

  if container_exists "${container_name}"; then
    if docker ps --format '{{.Names}}' | grep -Fxq "${container_name}"; then
      echo "Stopping container: ${container_name}"
      docker stop "${container_name}" >/dev/null
    else
      echo "Container already stopped: ${container_name}"
    fi
  else
    echo "Container not found: ${container_name}"
  fi
}

remove_container_with_volumes() {
  local container_name="$1"

  if container_exists "${container_name}"; then
    echo "Removing container and attached Docker volumes: ${container_name}"
    docker rm -f -v "${container_name}" >/dev/null
  else
    echo "Container not found: ${container_name}"
  fi
}

delete_postgres_data_dir() {
  if [[ -z "${POSTGRES_DATA_DIR}" || "${POSTGRES_DATA_DIR}" == "/" ]]; then
    echo "Refusing to delete unsafe POSTGRES_DATA_DIR: ${POSTGRES_DATA_DIR}" >&2
    exit 1
  fi

  if [[ "${POSTGRES_DATA_DIR}" != "${DATA_DIR}"/* ]]; then
    echo "Refusing to delete POSTGRES_DATA_DIR outside DATA_DIR: ${POSTGRES_DATA_DIR}" >&2
    exit 1
  fi

  if [[ -d "${POSTGRES_DATA_DIR}" ]]; then
    echo "Deleting PostgreSQL data directory: ${POSTGRES_DATA_DIR}"
    rm -rf "${POSTGRES_DATA_DIR}"
  else
    echo "PostgreSQL data directory not found: ${POSTGRES_DATA_DIR}"
  fi
}

stop_container "${CONTAINER_NAME}"

if [[ "${DELETE_DB}" == "true" ]]; then
  remove_container_with_volumes "${POSTGRES_CONTAINER_NAME}"
  delete_postgres_data_dir
else
  stop_container "${POSTGRES_CONTAINER_NAME}"
fi

echo "Staging stop complete"
