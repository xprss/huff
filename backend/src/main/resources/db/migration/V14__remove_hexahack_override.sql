UPDATE hexahack_games
SET event_log_json = COALESCE((
  SELECT jsonb_agg(event - 'override' ORDER BY ordinality)::text
  FROM jsonb_array_elements(event_log_json::jsonb) WITH ORDINALITY AS events(event, ordinality)
  WHERE event ->> 'kind' <> 'OVERRIDE'
), '[]');

ALTER TABLE hexahack_games DROP COLUMN override_count;
