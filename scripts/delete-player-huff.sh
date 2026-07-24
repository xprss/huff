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

POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres}"
POSTGRES_DB="${POSTGRES_DB:-huff_hexaquot}"
POSTGRES_USER="${POSTGRES_USER:-huff}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff}"

usage() {
  cat <<EOF
Usage:
  scripts/delete-player-huff.sh --id USER_ID [--yes]
  scripts/delete-player-huff.sh --email EMAIL [--yes]
  scripts/delete-player-huff.sh --google-subject SUBJECT [--yes]
  scripts/delete-player-huff.sh --display-name NAME [--yes] [--force-multiple]

By default this is a dry-run: it prints the matched player and game count.
Add --yes to delete matched games and users.

Options:
  --id USER_ID               Match users.id, for example google:123 or anon:...
  --email EMAIL              Match users.email exactly
  --google-subject SUBJECT   Match users.google_subject exactly
  --display-name NAME        Match users.display_name exactly
  --yes                      Actually delete; without it nothing is deleted
  --force-multiple           Allow deleting more than one matched user
  -h, --help                 Show this help

Environment:
  ENV_FILE                 Env file to load. Default: ${PROJECT_ROOT}/.env
  POSTGRES_CONTAINER_NAME  PostgreSQL container. Default: ${POSTGRES_CONTAINER_NAME}
  POSTGRES_DB              Database name. Default: ${POSTGRES_DB}
  POSTGRES_USER            Database user. Default: ${POSTGRES_USER}
EOF
}

fail() {
  echo "$1" >&2
  exit "${2:-2}"
}

set_lookup() {
  if [[ -n "${lookup_type}" ]]; then
    fail "Choose only one lookup option."
  fi

  lookup_type="$1"
  lookup_value="${2:-}"

  if [[ -z "${lookup_value}" ]]; then
    fail "Missing value for --${lookup_type//_/-}."
  fi
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
    psql -X -q -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -P pager=off "$@"
}

lookup_type=""
lookup_value=""
delete_mode="false"
force_multiple="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id)
      set_lookup "id" "${2:-}"
      shift 2
      ;;
    --email)
      set_lookup "email" "${2:-}"
      shift 2
      ;;
    --google-subject)
      set_lookup "google_subject" "${2:-}"
      shift 2
      ;;
    --display-name)
      set_lookup "display_name" "${2:-}"
      shift 2
      ;;
    --yes)
      delete_mode="true"
      shift
      ;;
    --force-multiple)
      force_multiple="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

if [[ -z "${lookup_type}" ]]; then
  usage >&2
  exit 2
fi

if [[ "${delete_mode}" != "true" ]]; then
  echo "Dry-run only. Re-run with --yes to delete." >&2
fi

require_postgres

psql_exec \
  -v lookup_type="${lookup_type}" \
  -v lookup_value="${lookup_value}" \
  -v delete_mode="${delete_mode}" \
  -v force_multiple="${force_multiple}" <<'SQL'
BEGIN;

SELECT set_config('huff_delete.lookup_type', :'lookup_type', true) AS ignored \gset
SELECT set_config('huff_delete.lookup_value', :'lookup_value', true) AS ignored \gset
SELECT set_config('huff_delete.delete_mode', :'delete_mode', true) AS ignored \gset
SELECT set_config('huff_delete.force_multiple', :'force_multiple', true) AS ignored \gset

CREATE TEMP TABLE selected_users ON COMMIT DROP AS
SELECT
  id,
  email,
  display_name,
  google_subject,
  created_at
FROM users
WHERE
  (current_setting('huff_delete.lookup_type') = 'id' AND id = current_setting('huff_delete.lookup_value'))
  OR (current_setting('huff_delete.lookup_type') = 'email' AND email = current_setting('huff_delete.lookup_value'))
  OR (
    current_setting('huff_delete.lookup_type') = 'google_subject'
    AND google_subject = current_setting('huff_delete.lookup_value')
  )
  OR (
    current_setting('huff_delete.lookup_type') = 'display_name'
    AND display_name = current_setting('huff_delete.lookup_value')
  );

\echo Matched users:
SELECT
  u.id,
  u.email,
  u.display_name,
  u.google_subject,
  u.created_at,
  COUNT(g.id)::integer AS games
FROM selected_users u
LEFT JOIN games g ON g.user_id = u.id
GROUP BY u.id, u.email, u.display_name, u.google_subject, u.created_at
ORDER BY u.display_name NULLS LAST, u.email NULLS LAST, u.id;

DO $$
DECLARE
  selected_count integer;
BEGIN
  SELECT COUNT(*) INTO selected_count FROM selected_users;

  IF selected_count = 0 THEN
    RAISE EXCEPTION 'No users matched % = %',
      current_setting('huff_delete.lookup_type'),
      current_setting('huff_delete.lookup_value');
  END IF;

  IF selected_count > 1 AND current_setting('huff_delete.force_multiple') <> 'true' THEN
    RAISE EXCEPTION 'Matched % users. Re-run with --force-multiple if you really want to delete all of them.',
      selected_count;
  END IF;
END $$;

WITH deleted_games AS (
  DELETE FROM games g
  USING selected_users u
  WHERE current_setting('huff_delete.delete_mode') = 'true'
    AND g.user_id = u.id
  RETURNING g.id
),
deleted_users AS (
  DELETE FROM users u
  USING selected_users selected
  WHERE current_setting('huff_delete.delete_mode') = 'true'
    AND u.id = selected.id
  RETURNING u.id
)
SELECT
  CASE
    WHEN current_setting('huff_delete.delete_mode') = 'true' THEN 'deleted'
    ELSE 'dry-run'
  END AS mode,
  (SELECT COUNT(*) FROM deleted_users)::integer AS users,
  (SELECT COUNT(*) FROM deleted_games)::integer AS games;

COMMIT;
SQL
