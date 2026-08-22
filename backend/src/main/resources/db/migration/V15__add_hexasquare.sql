CREATE TABLE hexasquare_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  rules_version integer NOT NULL,
  puzzle_json text NOT NULL,
  placements_json text,
  canonical_paths_json text,
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  simulations_count integer NOT NULL DEFAULT 0,
  used_cells integer,
  remaining_cells integer,
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexasquare_games_user_puzzle_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexasquare_games_puzzle_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexasquare_games_rules_version_check CHECK (rules_version > 0),
  CONSTRAINT hexasquare_games_puzzle_json_check CHECK (jsonb_typeof(puzzle_json::jsonb) = 'object'),
  CONSTRAINT hexasquare_games_placements_json_check CHECK (placements_json IS NULL OR jsonb_typeof(placements_json::jsonb) = 'array'),
  CONSTRAINT hexasquare_games_paths_json_check CHECK (canonical_paths_json IS NULL OR jsonb_typeof(canonical_paths_json::jsonb) = 'array'),
  CONSTRAINT hexasquare_games_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
  CONSTRAINT hexasquare_games_simulations_check CHECK (simulations_count >= 0),
  CONSTRAINT hexasquare_games_counts_check CHECK ((used_cells IS NULL OR used_cells >= 0) AND (remaining_cells IS NULL OR remaining_cells >= 0)),
  CONSTRAINT hexasquare_games_completion_check CHECK (
    (status = 'IN_PROGRESS' AND completed_at IS NULL AND canonical_paths_json IS NULL AND used_cells IS NULL AND remaining_cells IS NULL)
    OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND canonical_paths_json IS NOT NULL AND used_cells IS NOT NULL AND remaining_cells IS NOT NULL)
  )
);

CREATE TABLE hexasquare_simulations (
  id varchar(255) PRIMARY KEY,
  game_id varchar(255) NOT NULL REFERENCES hexasquare_games(id) ON UPDATE CASCADE ON DELETE CASCADE,
  request_id varchar(128) NOT NULL,
  placements_json text NOT NULL,
  outcome_json text NOT NULL,
  successful boolean NOT NULL,
  created_at varchar(255) NOT NULL,
  CONSTRAINT hexasquare_simulations_game_request_unique UNIQUE (game_id, request_id),
  CONSTRAINT hexasquare_simulations_request_id_check CHECK (length(trim(request_id)) > 0),
  CONSTRAINT hexasquare_simulations_placements_json_check CHECK (jsonb_typeof(placements_json::jsonb) = 'array'),
  CONSTRAINT hexasquare_simulations_outcome_json_check CHECK (jsonb_typeof(outcome_json::jsonb) = 'object')
);

CREATE INDEX hexasquare_games_completed_user_date_idx
  ON hexasquare_games (user_id, puzzle_date DESC) WHERE status = 'COMPLETED';
CREATE INDEX hexasquare_games_leaderboard_idx
  ON hexasquare_games (puzzle_date, user_id, completed_at) WHERE status = 'COMPLETED';
CREATE INDEX hexasquare_simulations_game_created_idx
  ON hexasquare_simulations (game_id, created_at);

INSERT INTO user_announcements (id, user_id, campaign, created_at)
SELECT md5('HEXASQUARE_LAUNCH:user:' || id), id, 'HEXASQUARE_LAUNCH', now()::text
FROM users
ON CONFLICT (user_id, campaign) DO NOTHING;

INSERT INTO push_campaign_deliveries (id, subscription_id, campaign, created_at)
SELECT md5('HEXASQUARE_LAUNCH:subscription:' || id), id, 'HEXASQUARE_LAUNCH', now()::text
FROM push_subscriptions
ON CONFLICT (subscription_id, campaign) DO NOTHING;
