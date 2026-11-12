#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${POSTGRES_CONTAINER_NAME:?POSTGRES_CONTAINER_NAME is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${POSTGRES_MCP_USER:=huff_stats_agent}"
: "${POSTGRES_MCP_PASSWORD:?POSTGRES_MCP_PASSWORD is required}"

docker exec -i \
  -e "PGPASSWORD=${POSTGRES_PASSWORD}" \
  "${POSTGRES_CONTAINER_NAME}" \
  psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 \
    -v "stats_role=${POSTGRES_MCP_USER}" \
    -v "stats_password=${POSTGRES_MCP_PASSWORD}" \
    -v "stats_database=${POSTGRES_DB}" < "${SCRIPT_DIR}/sql/game-stats-role.sql"

echo "PostgreSQL read-only statistics role ${POSTGRES_MCP_USER} is ready for ${POSTGRES_DB}."
