-- AI-generated explanations of the daily words, keyed by the normalized word so
-- Hexaword and Hexastar share an entry when they land on the same solution.
-- Each word is generated once (by the first finished game) and then reused.
CREATE TABLE word_explanations (
  word varchar(6) PRIMARY KEY,
  explanation text NOT NULL,
  generated_at varchar(255) NOT NULL
);
