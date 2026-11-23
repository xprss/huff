-- Hexatrap v2: AI-authored daily cases, validated by the backend. Version 1 was never public.
DROP TABLE IF EXISTS hexatrap_events;
DROP TABLE IF EXISTS hexatrap_games;

ALTER TABLE admin_users
  ADD COLUMN IF NOT EXISTS can_manage_hexatrap_cases boolean NOT NULL DEFAULT false;

UPDATE admin_users
SET can_manage_hexatrap_cases = true
WHERE can_manage_players = true OR can_manage_admins = true;

CREATE TABLE hexatrap_cases (
  puzzle_date varchar(10) PRIMARY KEY,
  rules_version integer NOT NULL,
  status varchar(20) NOT NULL,
  lease_until_ms bigint,
  difficulty integer NOT NULL,
  format varchar(30) NOT NULL,
  title varchar(255),
  case_json text,
  metrics_json text,
  warnings_json text NOT NULL DEFAULT '[]',
  attempts integer NOT NULL DEFAULT 0,
  model varchar(80),
  last_error text,
  generated_at varchar(255),
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  CONSTRAINT hexatrap_cases_puzzle_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexatrap_cases_rules_version_check CHECK (rules_version > 0),
  CONSTRAINT hexatrap_cases_status_check CHECK (status IN ('GENERATING', 'READY', 'FAILED')),
  CONSTRAINT hexatrap_cases_difficulty_check CHECK (difficulty BETWEEN 1 AND 5),
  CONSTRAINT hexatrap_cases_attempts_check CHECK (attempts >= 0),
  CONSTRAINT hexatrap_cases_warnings_check CHECK (jsonb_typeof(warnings_json::jsonb) = 'array'),
  CONSTRAINT hexatrap_cases_case_check CHECK (case_json IS NULL OR jsonb_typeof(case_json::jsonb) = 'object'),
  CONSTRAINT hexatrap_cases_metrics_check CHECK (metrics_json IS NULL OR jsonb_typeof(metrics_json::jsonb) = 'object'),
  CONSTRAINT hexatrap_cases_ready_check CHECK (status <> 'READY' OR (case_json IS NOT NULL AND metrics_json IS NOT NULL AND title IS NOT NULL))
);

CREATE TABLE hexatrap_generation_attempts (
  id varchar(255) PRIMARY KEY,
  puzzle_date varchar(10) NOT NULL REFERENCES hexatrap_cases(puzzle_date) ON DELETE CASCADE,
  run_id varchar(255) NOT NULL,
  attempt integer NOT NULL,
  phase varchar(20) NOT NULL,
  ok boolean NOT NULL,
  issues_json text NOT NULL DEFAULT '[]',
  metrics_json text,
  draft_json text,
  duration_ms bigint NOT NULL DEFAULT 0,
  created_at varchar(255) NOT NULL,
  CONSTRAINT hexatrap_generation_attempts_phase_check CHECK (phase IN ('DRAFT', 'VALIDATE', 'AUDIT', 'ACCEPTED')),
  CONSTRAINT hexatrap_generation_attempts_attempt_check CHECK (attempt > 0),
  CONSTRAINT hexatrap_generation_attempts_issues_check CHECK (jsonb_typeof(issues_json::jsonb) = 'array')
);

CREATE INDEX hexatrap_generation_attempts_date_idx ON hexatrap_generation_attempts (puzzle_date, created_at);

CREATE TABLE hexatrap_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL REFERENCES hexatrap_cases(puzzle_date),
  rules_version integer NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  outcome varchar(20),
  minutes_used integer NOT NULL DEFAULT 0,
  actions integer NOT NULL DEFAULT 0,
  points bigint NOT NULL DEFAULT 0,
  verdict_json text,
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexatrap_games_user_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexatrap_games_rules_version_check CHECK (rules_version > 0),
  CONSTRAINT hexatrap_games_status_check CHECK (status IN ('IN_PROGRESS', 'WON', 'LOST')),
  CONSTRAINT hexatrap_games_outcome_check CHECK (outcome IS NULL OR outcome IN ('SOLVED', 'PARTIAL', 'WRONG')),
  CONSTRAINT hexatrap_games_minutes_check CHECK (minutes_used >= 0),
  CONSTRAINT hexatrap_games_actions_check CHECK (actions >= 0),
  CONSTRAINT hexatrap_games_points_check CHECK (points >= 0),
  CONSTRAINT hexatrap_games_verdict_check CHECK (verdict_json IS NULL OR jsonb_typeof(verdict_json::jsonb) = 'object'),
  CONSTRAINT hexatrap_games_completion_check CHECK (
    (status = 'IN_PROGRESS' AND outcome IS NULL AND completed_at IS NULL AND verdict_json IS NULL)
    OR (status = 'WON' AND outcome = 'SOLVED' AND completed_at IS NOT NULL AND verdict_json IS NOT NULL)
    OR (status = 'LOST' AND outcome IN ('PARTIAL', 'WRONG') AND completed_at IS NOT NULL AND verdict_json IS NOT NULL)
  )
);

CREATE INDEX hexatrap_games_date_idx ON hexatrap_games (puzzle_date, user_id);

CREATE TABLE hexatrap_actions (
  id varchar(255) PRIMARY KEY,
  game_id varchar(255) NOT NULL REFERENCES hexatrap_games(id) ON DELETE CASCADE,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  sequence integer NOT NULL,
  request_id varchar(128) NOT NULL,
  fingerprint text NOT NULL,
  kind varchar(10) NOT NULL,
  action_key varchar(64) NOT NULL,
  response_json text NOT NULL,
  created_at varchar(255) NOT NULL,
  CONSTRAINT hexatrap_actions_kind_check CHECK (kind IN ('QUESTION', 'CLUE', 'VERDICT')),
  CONSTRAINT hexatrap_actions_sequence_check CHECK (sequence > 0),
  CONSTRAINT hexatrap_actions_request_unique UNIQUE (user_id, request_id),
  CONSTRAINT hexatrap_actions_key_unique UNIQUE (game_id, action_key),
  CONSTRAINT hexatrap_actions_sequence_unique UNIQUE (game_id, sequence)
);
