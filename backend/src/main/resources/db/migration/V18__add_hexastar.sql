CREATE TABLE hexastar_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  rules_version integer NOT NULL,
  solution varchar(6) NOT NULL,
  solution_syllables_json text NOT NULL,
  attempts_json text NOT NULL DEFAULT '[]',
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexastar_games_user_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexastar_games_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexastar_games_rules_version_check CHECK (rules_version > 0),
  CONSTRAINT hexastar_games_solution_check CHECK (solution ~ '^[a-z]{6}$'),
  CONSTRAINT hexastar_games_solution_syllables_check CHECK (
    jsonb_typeof(solution_syllables_json::jsonb) = 'array'
    AND jsonb_array_length(solution_syllables_json::jsonb) BETWEEN 2 AND 4
  ),
  CONSTRAINT hexastar_games_attempts_check CHECK (
    jsonb_typeof(attempts_json::jsonb) = 'array'
    AND jsonb_array_length(attempts_json::jsonb) BETWEEN 0 AND 6
  ),
  CONSTRAINT hexastar_games_status_check CHECK (status IN ('IN_PROGRESS', 'WON', 'LOST')),
  CONSTRAINT hexastar_games_completion_check CHECK (
    (status = 'IN_PROGRESS' AND completed_at IS NULL)
    OR (status IN ('WON', 'LOST') AND completed_at IS NOT NULL)
  )
);

CREATE INDEX hexastar_games_completed_user_date_idx
  ON hexastar_games (user_id, puzzle_date DESC)
  WHERE status IN ('WON', 'LOST');
