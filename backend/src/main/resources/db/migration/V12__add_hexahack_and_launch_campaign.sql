ALTER TABLE games RENAME TO hexaword_games;

ALTER INDEX IF EXISTS games_won_puzzle_date_user_idx
  RENAME TO hexaword_games_won_puzzle_date_user_idx;

CREATE TABLE hexahack_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  puzzle_date varchar(10) NOT NULL,
  rules_version integer NOT NULL,
  solution varchar(6) NOT NULL,
  event_log_json text NOT NULL DEFAULT '[]',
  total_cost integer NOT NULL DEFAULT 0,
  wrong_submissions integer NOT NULL DEFAULT 0,
  override_count integer NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  stealth integer,
  rank varchar(20),
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexahack_games_user_puzzle_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexahack_games_puzzle_date_check CHECK (puzzle_date ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'),
  CONSTRAINT hexahack_games_rules_version_check CHECK (rules_version > 0),
  CONSTRAINT hexahack_games_solution_check CHECK (solution ~ '^[0-9]{6}$'),
  CONSTRAINT hexahack_games_event_log_check CHECK (jsonb_typeof(event_log_json::jsonb) = 'array'),
  CONSTRAINT hexahack_games_total_cost_check CHECK (total_cost >= 0),
  CONSTRAINT hexahack_games_wrong_submissions_check CHECK (wrong_submissions >= 0),
  CONSTRAINT hexahack_games_override_count_check CHECK (override_count >= 0),
  CONSTRAINT hexahack_games_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
  CONSTRAINT hexahack_games_rank_check CHECK (rank IS NULL OR rank IN ('GHOST', 'SHADOW', 'BREACH', 'TRACED')),
  CONSTRAINT hexahack_games_stealth_check CHECK (stealth IS NULL OR stealth <= 100),
  CONSTRAINT hexahack_games_completion_check CHECK (
    (status = 'IN_PROGRESS' AND stealth IS NULL AND rank IS NULL AND completed_at IS NULL)
    OR (status = 'COMPLETED' AND stealth IS NOT NULL AND rank IS NOT NULL AND completed_at IS NOT NULL)
  )
);

CREATE INDEX hexahack_games_completed_user_date_idx
  ON hexahack_games (user_id, puzzle_date DESC)
  WHERE status = 'COMPLETED';

CREATE TABLE user_announcements (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
  campaign varchar(64) NOT NULL,
  created_at varchar(255) NOT NULL,
  seen_at varchar(255),
  CONSTRAINT user_announcements_user_campaign_unique UNIQUE (user_id, campaign)
);

CREATE TABLE push_campaign_deliveries (
  id varchar(255) PRIMARY KEY,
  subscription_id varchar(255) NOT NULL REFERENCES push_subscriptions(id) ON UPDATE CASCADE ON DELETE CASCADE,
  campaign varchar(64) NOT NULL,
  created_at varchar(255) NOT NULL,
  sent_at varchar(255),
  attempts integer NOT NULL DEFAULT 0,
  last_attempt_at varchar(255),
  CONSTRAINT push_campaign_deliveries_attempts_check CHECK (attempts >= 0),
  CONSTRAINT push_campaign_deliveries_subscription_campaign_unique UNIQUE (subscription_id, campaign)
);

INSERT INTO user_announcements (id, user_id, campaign, created_at)
SELECT md5('HEXAHACK_LAUNCH:user:' || id), id, 'HEXAHACK_LAUNCH', now()::text
FROM users
ON CONFLICT (user_id, campaign) DO NOTHING;

INSERT INTO push_campaign_deliveries (id, subscription_id, campaign, created_at)
SELECT md5('HEXAHACK_LAUNCH:subscription:' || id), id, 'HEXAHACK_LAUNCH', now()::text
FROM push_subscriptions
ON CONFLICT (subscription_id, campaign) DO NOTHING;
