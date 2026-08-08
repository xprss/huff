CREATE TABLE IF NOT EXISTS auth_sessions (
  id varchar(255) PRIMARY KEY,
  user_id varchar(255) NOT NULL,
  expires_at varchar(255) NOT NULL
);
