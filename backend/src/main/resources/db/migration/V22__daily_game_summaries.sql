CREATE TABLE daily_game_summaries (
  summary_date varchar(10) PRIMARY KEY,
  summary text NOT NULL,
  generated_at varchar(255) NOT NULL
);
