-- Hints were removed from Hexaflow. Retain only idempotent path events for
-- existing games before deleting the now-unused hint state.
UPDATE hexaflow_games
SET event_log_json = COALESCE((
  SELECT jsonb_agg(event - 'hint')::text
  FROM jsonb_array_elements(event_log_json::jsonb) AS event
  WHERE event->>'kind' = 'PATH'
), '[]');

ALTER TABLE hexaflow_games
  DROP CONSTRAINT IF EXISTS hexaflow_games_hints_check,
  DROP CONSTRAINT IF EXISTS hexaflow_games_hinted_json_check,
  DROP COLUMN IF EXISTS hinted_answers_json,
  DROP COLUMN IF EXISTS hints_used;
