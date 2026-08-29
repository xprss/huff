ALTER TABLE admin_users
  ADD COLUMN IF NOT EXISTS can_manage_hexaflow_puzzles boolean NOT NULL DEFAULT false;

UPDATE admin_users
SET can_manage_hexaflow_puzzles = true
WHERE can_manage_players = true OR can_manage_admins = true;

CREATE TABLE hexaflow_puzzles (
  id varchar(255) PRIMARY KEY,
  puzzle_date varchar(10) NOT NULL UNIQUE,
  status varchar(20) NOT NULL DEFAULT 'DRAFT',
  theme_clue varchar(500) NOT NULL DEFAULT '',
  grid_json text NOT NULL DEFAULT '[]',
  answers_json text NOT NULL DEFAULT '[]',
  created_by varchar(255) NOT NULL,
  updated_by varchar(255) NOT NULL,
  published_by varchar(255),
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  published_at varchar(255),
  CONSTRAINT hexaflow_puzzles_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexaflow_puzzles_status_check CHECK (status IN ('DRAFT', 'PUBLISHED')),
  CONSTRAINT hexaflow_puzzles_grid_json_check CHECK (jsonb_typeof(grid_json::jsonb) = 'array'),
  CONSTRAINT hexaflow_puzzles_answers_json_check CHECK (jsonb_typeof(answers_json::jsonb) = 'array')
);

CREATE INDEX hexaflow_puzzles_month_idx ON hexaflow_puzzles (puzzle_date);

CREATE TABLE hexaflow_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_id varchar(255) NOT NULL REFERENCES hexaflow_puzzles(id) ON UPDATE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  found_answers_json text NOT NULL DEFAULT '[]',
  extra_sequences_json text NOT NULL DEFAULT '[]',
  hinted_answers_json text NOT NULL DEFAULT '[]',
  hints_used integer NOT NULL DEFAULT 0,
  event_log_json text NOT NULL DEFAULT '[]',
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexaflow_games_user_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexaflow_games_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexaflow_games_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
  CONSTRAINT hexaflow_games_hints_check CHECK (hints_used >= 0),
  CONSTRAINT hexaflow_games_found_json_check CHECK (jsonb_typeof(found_answers_json::jsonb) = 'array'),
  CONSTRAINT hexaflow_games_extra_json_check CHECK (jsonb_typeof(extra_sequences_json::jsonb) = 'array'),
  CONSTRAINT hexaflow_games_hinted_json_check CHECK (jsonb_typeof(hinted_answers_json::jsonb) = 'array'),
  CONSTRAINT hexaflow_games_event_json_check CHECK (jsonb_typeof(event_log_json::jsonb) = 'array')
);

CREATE INDEX hexaflow_games_completed_user_date_idx
  ON hexaflow_games (user_id, puzzle_date)
  WHERE status = 'COMPLETED';
