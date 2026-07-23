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
OUTPUT_FILE="${1:-${PLAYER_STATS_JSON_FILE:-${PROJECT_ROOT}/data/player-stats.json}}"

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

psql_json() {
  docker exec -i -e "PGPASSWORD=${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER_NAME}" \
    psql -X -q -t -A -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -v ON_ERROR_STOP=1
}

require_postgres

mkdir -p "$(dirname "${OUTPUT_FILE}")"
TMP_OUTPUT="${OUTPUT_FILE}.tmp.$$"
cleanup() {
  rm -f "${TMP_OUTPUT}"
}
trap cleanup EXIT

psql_json >"${TMP_OUTPUT}" <<'SQL'
WITH player_stats AS (
  SELECT
    u.id AS player_id,
    u.email,
    u.display_name,
    u.created_at,
    COUNT(g.id)::integer AS games_played,
    COUNT(g.id) FILTER (WHERE g.status = 'WON')::integer AS games_won,
    COALESCE(
      jsonb_agg(
        jsonb_build_object(
          'puzzleDate', g.puzzle_date,
          'word', g.solution,
          'attempts', jsonb_array_length(g.guesses_json::jsonb)
        )
        ORDER BY g.puzzle_date
      ) FILTER (WHERE g.status = 'WON'),
      '[]'::jsonb
    ) AS wins
  FROM users u
  LEFT JOIN games g ON g.user_id = u.id
  GROUP BY u.id, u.email, u.display_name, u.created_at
)
SELECT jsonb_pretty(
  COALESCE(
    jsonb_agg(
      jsonb_build_object(
        'playerId', player_id,
        'email', email,
        'displayName', display_name,
        'createdAt', created_at,
        'gamesPlayed', games_played,
        'gamesWon', games_won,
        'wins', wins
      )
      ORDER BY display_name NULLS LAST, email NULLS LAST, player_id
    ),
    '[]'::jsonb
  )
)
FROM player_stats;
SQL

mv "${TMP_OUTPUT}" "${OUTPUT_FILE}"
trap - EXIT
echo "Wrote player stats JSON to ${OUTPUT_FILE}" >&2
