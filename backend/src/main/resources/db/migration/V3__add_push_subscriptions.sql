CREATE TABLE IF NOT EXISTS push_subscriptions (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL,
  endpoint varchar(2048) NOT NULL,
  p256dh varchar(512) NOT NULL,
  auth varchar(256) NOT NULL,
  last_notified_puzzle_date varchar(255),
  last_reminded_puzzle_date varchar(255),
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  CONSTRAINT push_subscriptions_endpoint_unique UNIQUE (endpoint)
);

-- `last_reminded_puzzle_date` was added while Hibernate still managed the
-- schema. Keep this transition safe for installations from that release.
ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS last_reminded_puzzle_date varchar(255);
