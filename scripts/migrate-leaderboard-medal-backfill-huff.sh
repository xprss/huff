#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=load-env-file.sh
  . "${SCRIPT_DIR}/load-env-file.sh"
  load_env_file "${ENV_FILE}"
fi

GAME_TIMEZONE="${GAME_TIMEZONE:-Europe/Rome}"

# Ensure the destination tables and indexes exist when this script is run manually.
ENV_FILE="${ENV_FILE}" "${PROJECT_ROOT}/scripts/migrate-leaderboards-huff.sh"

"${PROJECT_ROOT}/scripts/db-huff.sh" query "
WITH weekly_scores AS (
  SELECT
    g.user_id,
    date_trunc('week', g.puzzle_date::date)::date AS week_start,
    COUNT(*)::integer AS wins,
    MAX(COALESCE(g.completed_at, g.updated_at, g.created_at)) AS last_win_at
  FROM hexaword_games g
  WHERE g.status = 'WON'
    AND g.puzzle_date::date < date_trunc('week', timezone('${GAME_TIMEZONE}', now()))::date
  GROUP BY g.user_id, date_trunc('week', g.puzzle_date::date)::date
), ranked AS (
  SELECT
    user_id,
    week_start,
    ROW_NUMBER() OVER (
      PARTITION BY week_start
      ORDER BY wins DESC, last_win_at ASC NULLS LAST, user_id ASC
    ) AS podium_rank
  FROM weekly_scores
)
INSERT INTO weekly_medals (id, user_id, week_start, medal, awarded_at)
SELECT
  md5('weekly-medal:' || user_id || ':' || week_start::text),
  user_id,
  week_start::text,
  CASE podium_rank
    WHEN 1 THEN 'GOLD'
    WHEN 2 THEN 'SILVER'
    WHEN 3 THEN 'BRONZE'
  END,
  now()::text
FROM ranked
WHERE podium_rank <= 3
ON CONFLICT (user_id, week_start) DO NOTHING;
"
