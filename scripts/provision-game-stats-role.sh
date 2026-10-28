#!/usr/bin/env bash

set -euo pipefail

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
    -v "stats_database=${POSTGRES_DB}" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'stats_role', :'stats_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'stats_role')
\gexec

ALTER ROLE :"stats_role" WITH LOGIN PASSWORD :'stats_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
ALTER ROLE :"stats_role" SET default_transaction_read_only = on;
REVOKE ALL PRIVILEGES ON SCHEMA public FROM :"stats_role";
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :"stats_role";
GRANT CONNECT ON DATABASE :"stats_database" TO :"stats_role";
GRANT USAGE ON SCHEMA public TO :"stats_role";
GRANT SELECT (id, nickname, display_name) ON TABLE users TO :"stats_role";
GRANT SELECT (user_id, puzzle_date, status) ON TABLE
  hexaword_games, hexahack_games, hexasky_games, hexaflow_games,
  hexastar_games, hexaeco_games, hexatrap_games TO :"stats_role";
SQL

echo "PostgreSQL read-only statistics role ${POSTGRES_MCP_USER} is ready for ${POSTGRES_DB}."
