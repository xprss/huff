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
  scripts/admin-users-huff.sh list
  scripts/admin-users-huff.sh show --id USER_ID
  scripts/admin-users-huff.sh grant --id USER_ID [--all] [privilege flags] [--yes]
  scripts/admin-users-huff.sh update --id USER_ID [privilege flags] [--yes]
  scripts/admin-users-huff.sh revoke --id USER_ID [--yes]

Privilege flags:
  --view-players true|false
  --view-player-details true|false
  --manage-players true|false
  --manage-admins true|false
  --all                      Set every privilege to true

By default grant/update/revoke are dry-run. Add --yes to write changes.
EOF
}

fail() {
  echo "$1" >&2
  exit "${2:-2}"
}

require_postgres() {
  if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    echo "PostgreSQL container is not running: ${POSTGRES_CONTAINER_NAME}" >&2
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

parse_bool() {
  case "${1:-}" in
    true|TRUE|1|yes|YES|y|Y)
      printf 'true'
      ;;
    false|FALSE|0|no|NO|n|N)
      printf 'false'
      ;;
    *)
      fail "Expected boolean true|false, got: ${1:-}"
      ;;
  esac
}

command="${1:-help}"
shift || true

user_id=""
write_mode="false"
view_players=""
view_details=""
manage_players=""
manage_admins=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id)
      user_id="${2:-}"
      shift 2
      ;;
    --view-players)
      view_players="$(parse_bool "${2:-}")"
      shift 2
      ;;
    --view-player-details)
      view_details="$(parse_bool "${2:-}")"
      shift 2
      ;;
    --manage-players)
      manage_players="$(parse_bool "${2:-}")"
      shift 2
      ;;
    --manage-admins)
      manage_admins="$(parse_bool "${2:-}")"
      shift 2
      ;;
    --all)
      view_players="true"
      view_details="true"
      manage_players="true"
      manage_admins="true"
      shift
      ;;
    --yes)
      write_mode="true"
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

require_postgres

case "${command}" in
  list)
    psql_exec -c "
      SELECT user_id, can_view_players, can_view_player_details, can_manage_players, can_manage_admins, created_at, updated_at
      FROM admin_users
      ORDER BY user_id;
    "
    ;;
  show)
    [[ -n "${user_id}" ]] || fail "Missing --id."
    psql_exec -v user_id="${user_id}" -c "
      SELECT user_id, can_view_players, can_view_player_details, can_manage_players, can_manage_admins, created_at, updated_at
      FROM admin_users
      WHERE user_id = :'user_id';
    "
    ;;
  grant|update)
    [[ -n "${user_id}" ]] || fail "Missing --id."
    if [[ "${write_mode}" != "true" ]]; then
      echo "Dry-run only. Re-run with --yes to write." >&2
    fi
    psql_exec \
      -v user_id="${user_id}" \
      -v write_mode="${write_mode}" \
      -v view_players="${view_players}" \
      -v view_details="${view_details}" \
      -v manage_players="${manage_players}" \
      -v manage_admins="${manage_admins}" <<'SQL'
BEGIN;

SELECT set_config('huff_admin.user_id', :'user_id', true) AS ignored \gset
SELECT set_config('huff_admin.write_mode', :'write_mode', true) AS ignored \gset
SELECT set_config('huff_admin.view_players', :'view_players', true) AS ignored \gset
SELECT set_config('huff_admin.view_details', :'view_details', true) AS ignored \gset
SELECT set_config('huff_admin.manage_players', :'manage_players', true) AS ignored \gset
SELECT set_config('huff_admin.manage_admins', :'manage_admins', true) AS ignored \gset

WITH current_values AS (
  SELECT *
  FROM admin_users
  WHERE user_id = current_setting('huff_admin.user_id')
),
desired AS (
  SELECT
    current_setting('huff_admin.user_id') AS user_id,
    COALESCE(NULLIF(current_setting('huff_admin.view_players'), '')::boolean, (SELECT can_view_players FROM current_values), false) AS can_view_players,
    COALESCE(NULLIF(current_setting('huff_admin.view_details'), '')::boolean, (SELECT can_view_player_details FROM current_values), false) AS can_view_player_details,
    COALESCE(NULLIF(current_setting('huff_admin.manage_players'), '')::boolean, (SELECT can_manage_players FROM current_values), false) AS can_manage_players,
    COALESCE(NULLIF(current_setting('huff_admin.manage_admins'), '')::boolean, (SELECT can_manage_admins FROM current_values), false) AS can_manage_admins
),
written AS (
  INSERT INTO admin_users (
    user_id,
    can_view_players,
    can_view_player_details,
    can_manage_players,
    can_manage_admins,
    created_at,
    updated_at
  )
  SELECT
    user_id,
    can_view_players,
    can_view_player_details,
    can_manage_players,
    can_manage_admins,
    now()::text,
    now()::text
  FROM desired
  WHERE current_setting('huff_admin.write_mode') = 'true'
  ON CONFLICT (user_id) DO UPDATE
  SET
    can_view_players = EXCLUDED.can_view_players,
    can_view_player_details = EXCLUDED.can_view_player_details,
    can_manage_players = EXCLUDED.can_manage_players,
    can_manage_admins = EXCLUDED.can_manage_admins,
    updated_at = now()::text
  RETURNING *
)
SELECT
  CASE WHEN current_setting('huff_admin.write_mode') = 'true' THEN 'written' ELSE 'dry-run' END AS mode,
  desired.*
FROM desired;

COMMIT;
SQL
    ;;
  revoke)
    [[ -n "${user_id}" ]] || fail "Missing --id."
    if [[ "${write_mode}" != "true" ]]; then
      echo "Dry-run only. Re-run with --yes to write." >&2
    fi
    psql_exec -v user_id="${user_id}" -v write_mode="${write_mode}" <<'SQL'
BEGIN;

SELECT set_config('huff_admin.user_id', :'user_id', true) AS ignored \gset
SELECT set_config('huff_admin.write_mode', :'write_mode', true) AS ignored \gset

\echo Matched admin rows:
SELECT *
FROM admin_users
WHERE user_id = current_setting('huff_admin.user_id');

WITH deleted AS (
  DELETE FROM admin_users
  WHERE current_setting('huff_admin.write_mode') = 'true'
    AND user_id = current_setting('huff_admin.user_id')
  RETURNING user_id
)
SELECT
  CASE WHEN current_setting('huff_admin.write_mode') = 'true' THEN 'deleted' ELSE 'dry-run' END AS mode,
  COUNT(*)::integer AS admin_rows
FROM deleted;

COMMIT;
SQL
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
