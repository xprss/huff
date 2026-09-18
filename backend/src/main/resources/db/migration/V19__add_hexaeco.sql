CREATE TABLE hexaeco_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  rules_version integer NOT NULL CHECK (rules_version > 0),
  request_id varchar(128) NOT NULL,
  moves_json text NOT NULL,
  status varchar(20) NOT NULL CHECK (status = 'WON'),
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255) NOT NULL,
  CONSTRAINT hexaeco_games_user_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexaeco_games_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexaeco_games_moves_check CHECK (
    jsonb_typeof(moves_json::jsonb) = 'array'
    AND jsonb_array_length(moves_json::jsonb) BETWEEN 1 AND 20000
  )
);

CREATE INDEX hexaeco_games_date_idx ON hexaeco_games (puzzle_date, user_id);
