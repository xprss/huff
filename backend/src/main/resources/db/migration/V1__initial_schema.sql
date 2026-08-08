CREATE TABLE IF NOT EXISTS users (
  id varchar(255) PRIMARY KEY,
  google_subject varchar(255),
  email varchar(255),
  display_name varchar(255),
  created_at varchar(255) NOT NULL,
  CONSTRAINT users_google_subject_unique UNIQUE (google_subject)
);

CREATE TABLE IF NOT EXISTS games (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL,
  puzzle_date varchar(255) NOT NULL,
  solution varchar(255) NOT NULL,
  guesses_json text NOT NULL,
  status varchar(255) NOT NULL,
  created_at varchar(255) NOT NULL,
  updated_at varchar(255) NOT NULL,
  completed_at varchar(255),
  CONSTRAINT games_user_puzzle_date_unique UNIQUE (user_id, puzzle_date)
);
