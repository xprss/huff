CREATE TABLE hexatrap_games (
 id varchar(255) PRIMARY KEY,
 user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
 puzzle_date varchar(10) NOT NULL,
 rules_version integer NOT NULL CHECK (rules_version > 0),
 puzzle_json text NOT NULL,
 hypotheses_json text NOT NULL,
 evidence_json text NOT NULL DEFAULT '[]',
 status varchar(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'WON')),
 questions bigint NOT NULL DEFAULT 0 CHECK (questions >= 0),
 wrong_submissions bigint NOT NULL DEFAULT 0 CHECK (wrong_submissions >= 0),
 points bigint NOT NULL DEFAULT 1000,
 event_count bigint NOT NULL DEFAULT 0,
 number_requested boolean NOT NULL DEFAULT false,
 created_at varchar(255) NOT NULL,
 updated_at varchar(255) NOT NULL,
 completed_at varchar(255),
 CONSTRAINT hexatrap_games_user_date_unique UNIQUE (user_id, puzzle_date)
);
CREATE INDEX hexatrap_games_date_idx ON hexatrap_games(puzzle_date, user_id);
CREATE TABLE hexatrap_events (
 id varchar(255) PRIMARY KEY,
 game_id varchar(255) NOT NULL REFERENCES hexatrap_games(id) ON DELETE CASCADE,
 user_id varchar(255) NOT NULL REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
 sequence bigint NOT NULL CHECK (sequence > 0),
 request_id varchar(128) NOT NULL,
 fingerprint text NOT NULL,
 event_json text NOT NULL,
 response_json text NOT NULL,
 CONSTRAINT hexatrap_events_sequence_unique UNIQUE(game_id, sequence),
 CONSTRAINT hexatrap_events_request_unique UNIQUE(user_id, request_id)
);
