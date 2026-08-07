#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

"${PROJECT_ROOT}/scripts/db-huff.sh" query "
CREATE TABLE IF NOT EXISTS weekly_medals (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL,
  week_start varchar(10) NOT NULL,
  medal varchar(10) NOT NULL,
  awarded_at varchar(255) NOT NULL,
  CONSTRAINT weekly_medals_user_week_unique UNIQUE (user_id, week_start),
  CONSTRAINT weekly_medals_medal_check CHECK (medal IN ('GOLD', 'SILVER', 'BRONZE'))
);

CREATE TABLE IF NOT EXISTS weekly_medal_award_state (
  id varchar(255) PRIMARY KEY,
  first_eligible_week_start varchar(10) NOT NULL,
  last_processed_week_start varchar(10) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS weekly_medals_user_week_unique_idx
  ON weekly_medals (user_id, week_start);
CREATE INDEX IF NOT EXISTS weekly_medals_user_medal_idx
  ON weekly_medals (user_id, medal);
CREATE INDEX IF NOT EXISTS games_won_puzzle_date_user_idx
  ON games (puzzle_date, user_id)
  WHERE status = 'WON';
"
