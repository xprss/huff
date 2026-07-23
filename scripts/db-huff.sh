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

DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres}"
POSTGRES_DB="${POSTGRES_DB:-huff_hexaquot}"
POSTGRES_USER="${POSTGRES_USER:-huff}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

usage() {
  cat <<EOF
Usage:
  scripts/db-huff.sh path
  scripts/db-huff.sh tables
  scripts/db-huff.sh schema [table]
  scripts/db-huff.sh query "SELECT * FROM users;"
  scripts/db-huff.sh dump [output.sql]
  scripts/db-huff.sh backup [output.sql]
  scripts/db-huff.sh shell

Environment:
  ENV_FILE                 Env file to load. Default: ${PROJECT_ROOT}/.env
  POSTGRES_CONTAINER_NAME  PostgreSQL container. Default: ${POSTGRES_CONTAINER_NAME}
  POSTGRES_DB              Database name. Default: ${POSTGRES_DB}
  POSTGRES_USER            Database user. Default: ${POSTGRES_USER}
  POSTGRES_PORT            Database port inside the Docker network. Default: ${POSTGRES_PORT}
EOF
}

require_postgres() {
  if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    echo "PostgreSQL container is not running: ${POSTGRES_CONTAINER_NAME}" >&2
    echo "Run scripts/redeploy-huff.sh first." >&2
    exit 1
  fi

  if ! docker exec -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
    echo "PostgreSQL is not ready in container: ${POSTGRES_CONTAINER_NAME}" >&2
    exit 1
  fi
}

psql_exec() {
  docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -P pager=off "$@"
}

pg_dump_exec() {
  docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --no-owner --no-privileges "$@"
}

command="${1:-help}"
shift || true

case "${command}" in
  help|-h|--help)
    usage
    ;;
  path)
    echo "postgresql://${POSTGRES_USER}@${POSTGRES_CONTAINER_NAME}:${POSTGRES_PORT}/${POSTGRES_DB}"
    ;;
  shell)
    require_postgres
    exec docker exec -it -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
      psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -P pager=off
    ;;
  tables)
    require_postgres
    psql_exec -Atc "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'public' ORDER BY tablename;"
    ;;
  schema)
    require_postgres
    if [[ $# -gt 0 ]]; then
      pg_dump_exec --schema-only --table "$1"
    else
      pg_dump_exec --schema-only
    fi
    ;;
  query)
    require_postgres
    query="${1:-}"
    if [[ -z "${query}" ]]; then
      query="$(cat)"
    fi
    if [[ -z "${query//[[:space:]]/}" ]]; then
      echo "Missing SQL query" >&2
      exit 2
    fi
    psql_exec -c "${query}"
    ;;
  dump)
    require_postgres
    output="${1:-}"
    if [[ -n "${output}" ]]; then
      mkdir -p "$(dirname "$(realpath -m "${output}")")"
      pg_dump_exec > "${output}"
      echo "${output}"
    else
      pg_dump_exec
    fi
    ;;
  backup)
    require_postgres
    output="${1:-}"
    if [[ -z "${output}" ]]; then
      stamp="$(date -u +%Y%m%d%H%M%S)"
      output="${DATA_DIR}/backups/huff-hexaquot-${stamp}.sql"
    fi
    mkdir -p "$(dirname "$(realpath -m "${output}")")"
    pg_dump_exec > "${output}"
    echo "${output}"
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
