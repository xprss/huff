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
  scripts/set-user-nickname-huff.sh --id USER_ID --nickname NICKNAME [--yes]

By default this is a dry-run: it validates the change and prints the final nickname.
Add --yes to update users.nickname.

Options:
  --id USER_ID          Match users.id exactly
  --nickname NICKNAME   New nickname. A missing @ is added automatically
  --yes                 Actually update; without it nothing is changed
  -h, --help            Show this help

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

normalize_nickname() {
  local value
  value="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  if [[ "${value}" != @* ]]; then
    value="@${value}"
  fi
  printf '%s' "${value}"
}

user_id=""
nickname=""
update_mode="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id)
      user_id="${2:-}"
      shift 2
      ;;
    --nickname)
      nickname="${2:-}"
      shift 2
      ;;
    --yes)
      update_mode="true"
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

if [[ -z "${user_id}" || -z "${nickname}" ]]; then
  usage >&2
  exit 2
fi

normalized_nickname="$(normalize_nickname "${nickname}")"

if [[ ${#normalized_nickname} -gt 30 ]]; then
  fail "Invalid nickname: max 30 characters including @."
fi
if [[ ! "${normalized_nickname}" =~ ^@[a-z0-9._-]+$ ]]; then
  fail "Invalid nickname: expected @[a-z0-9._-]+."
fi

if [[ "${update_mode}" != "true" ]]; then
  echo "Dry-run only. Re-run with --yes to update." >&2
fi

require_postgres

psql_exec \
  -v user_id="${user_id}" \
  -v nickname="${normalized_nickname}" \
  -v update_mode="${update_mode}" <<'SQL'
BEGIN;

SELECT set_config('huff_nickname.user_id', :'user_id', true) AS ignored \gset
SELECT set_config('huff_nickname.nickname', :'nickname', true) AS ignored \gset
SELECT set_config('huff_nickname.update_mode', :'update_mode', true) AS ignored \gset

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM users
    WHERE id = current_setting('huff_nickname.user_id')
  ) THEN
    RAISE EXCEPTION 'No user found with id %', current_setting('huff_nickname.user_id');
  END IF;

  IF EXISTS (
    SELECT 1
    FROM users
    WHERE nickname = current_setting('huff_nickname.nickname')
      AND id <> current_setting('huff_nickname.user_id')
  ) THEN
    RAISE EXCEPTION 'Nickname % already belongs to another user', current_setting('huff_nickname.nickname');
  END IF;
END $$;

UPDATE users
SET nickname = current_setting('huff_nickname.nickname')
WHERE current_setting('huff_nickname.update_mode') = 'true'
  AND id = current_setting('huff_nickname.user_id');

SELECT
  CASE
    WHEN current_setting('huff_nickname.update_mode') = 'true' THEN 'updated'
    ELSE 'dry-run'
  END AS mode,
  id,
  display_name,
  CASE
    WHEN current_setting('huff_nickname.update_mode') = 'true' THEN nickname
    ELSE current_setting('huff_nickname.nickname')
  END AS nickname
FROM users
WHERE id = current_setting('huff_nickname.user_id');

COMMIT;
SQL
