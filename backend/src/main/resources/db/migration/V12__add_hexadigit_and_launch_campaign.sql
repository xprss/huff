ALTER TABLE games RENAME TO hexaword_games;

ALTER INDEX IF EXISTS games_won_puzzle_date_user_idx
  RENAME TO hexaword_games_won_puzzle_date_user_idx;

-- Transitional, automatically updatable alias for older operational tools.
CREATE VIEW games AS SELECT * FROM hexaword_games;

CREATE TABLE hexadigit_games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL,
  puzzle_date varchar(10) NOT NULL,
  solution varchar(6) NOT NULL,
  guesses_json text NOT NULL,
  status varchar(255) NOT NULL,
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT hexadigit_games_user_puzzle_date_unique UNIQUE (user_id, puzzle_date),
  CONSTRAINT hexadigit_games_solution_check CHECK (solution ~ '^[0-9]{6}$')
);

CREATE INDEX hexadigit_games_won_puzzle_date_user_idx
  ON hexadigit_games (puzzle_date, user_id)
  WHERE status = 'WON';

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
  CONSTRAINT push_campaign_deliveries_subscription_campaign_unique UNIQUE (subscription_id, campaign)
);

INSERT INTO user_announcements (id, user_id, campaign, created_at)
SELECT md5('HEXADIGIT_LAUNCH:user:' || id), id, 'HEXADIGIT_LAUNCH', now()::text
FROM users
ON CONFLICT (user_id, campaign) DO NOTHING;

INSERT INTO push_campaign_deliveries (id, subscription_id, campaign, created_at)
SELECT md5('HEXADIGIT_LAUNCH:subscription:' || id), id, 'HEXADIGIT_LAUNCH', now()::text
FROM push_subscriptions
ON CONFLICT (subscription_id, campaign) DO NOTHING;
