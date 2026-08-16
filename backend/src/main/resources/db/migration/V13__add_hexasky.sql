CREATE TABLE hexasky_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  rules_version integer NOT NULL,
  solution_json text NOT NULL,
  proposal_json text,
  event_log_json text NOT NULL DEFAULT '[]',
  checks_used integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexasky_games_user_puzzle_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexasky_games_puzzle_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexasky_games_checks_used_check CHECK (checks_used BETWEEN 0 AND 2),
  CONSTRAINT hexasky_games_status_check CHECK (status IN ('IN_PROGRESS', 'WON', 'LOST')),
  CONSTRAINT hexasky_games_solution_json_check CHECK (jsonb_array_length(solution_json::jsonb) = 16),
  CONSTRAINT hexasky_games_event_log_check CHECK (jsonb_typeof(event_log_json::jsonb) = 'array')
);

CREATE INDEX hexasky_games_completed_user_date_idx
  ON hexasky_games (user_id, puzzle_date DESC)
  WHERE status IN ('WON', 'LOST');
