#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env.staging}"

usage() {
  cat <<EOF
Usage: scripts/grant-staging-stars-huff.sh [--dry-run] [--allow-non-staging]

Make every staging user eligible to use the star on the frontend.

The script is safe to rerun: it sets star_available=true and clears star_used_at
for every user, so a star consumed during testing can be restored by running it
again.

Options:
  --dry-run             Show what would be updated, then rollback
  --allow-non-staging   Permit targets whose DB/container name does not include "staging"
  -h, --help            Show this help

Environment:
  ENV_FILE                 Env file to load. Default: ${ENV_FILE}
  POSTGRES_CONTAINER_NAME  PostgreSQL container. Default: huff-postgres-staging
  POSTGRES_DB              Database name. Default: huff_hexaquot_staging
  POSTGRES_USER            Database user. Default: huff_staging
EOF
}

DRY_RUN="false"
ALLOW_NON_STAGING="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN="true"
      ;;
    --allow-non-staging)
      ALLOW_NON_STAGING="true"
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

POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres-staging}"
POSTGRES_DB="${POSTGRES_DB:-huff_hexaquot_staging}"
POSTGRES_USER="${POSTGRES_USER:-huff_staging}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff-staging}"

if [[ "${ALLOW_NON_STAGING}" != "true" ]]; then
  if [[ "${POSTGRES_CONTAINER_NAME}" != *staging* && "${POSTGRES_DB}" != *staging* ]]; then
    echo "Refusing to update a target that does not look like staging." >&2
    echo "POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME}" >&2
    echo "POSTGRES_DB=${POSTGRES_DB}" >&2
    echo "Pass --allow-non-staging only if this is intentional." >&2
    exit 2
  fi
fi

if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  echo "PostgreSQL container is not running: ${POSTGRES_CONTAINER_NAME}" >&2
  echo "Run scripts/redeploy-huff-staging.sh first." >&2
  exit 1
fi

if ! docker exec -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
  pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
  echo "PostgreSQL is not ready in container: ${POSTGRES_CONTAINER_NAME}" >&2
  exit 1
fi

transaction_end="COMMIT"
if [[ "${DRY_RUN}" == "true" ]]; then
  transaction_end="ROLLBACK"
fi

echo "Target: ${POSTGRES_CONTAINER_NAME}/${POSTGRES_DB}"
if [[ "${DRY_RUN}" == "true" ]]; then
  echo "Mode: dry-run"
fi

docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
  psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -P pager=off <<SQL
BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS star_available boolean;
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_awarded_at varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_used_at varchar(255);

ALTER TABLE users ALTER COLUMN star_available SET DEFAULT false;

WITH users_before AS (
  SELECT
    COUNT(*)::integer AS total_users,
    COUNT(*) FILTER (WHERE star_available IS TRUE AND star_used_at IS NULL)::integer AS users_already_ready,
    COUNT(*) FILTER (WHERE star_available IS DISTINCT FROM TRUE OR star_used_at IS NOT NULL)::integer AS users_to_reset
  FROM users
),
updated AS (
  UPDATE users
  SET
    star_available = true,
    star_awarded_at = to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
    star_used_at = NULL
  RETURNING id
)
SELECT
  users_before.total_users,
  users_before.users_already_ready,
  users_before.users_to_reset,
  (SELECT COUNT(*)::integer FROM updated) AS users_ready_after
FROM users_before
;

ALTER TABLE users ALTER COLUMN star_available SET NOT NULL;

${transaction_end};
SQL
