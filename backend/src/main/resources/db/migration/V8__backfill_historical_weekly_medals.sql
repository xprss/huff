WITH weekly_scores AS (
  SELECT
    g.user_id,
    date_trunc('week', g.puzzle_date::date)::date AS week_start,
    COUNT(*)::integer AS wins,
    MAX(COALESCE(g.completed_at, g.updated_at, g.created_at)) AS last_win_at
  FROM games g
  WHERE g.status = 'WON'
    AND g.puzzle_date::date < date_trunc('week', timezone('${game_timezone}', now()))::date
  GROUP BY g.user_id, date_trunc('week', g.puzzle_date::date)::date
), ranked AS (
  SELECT
    user_id,
    week_start,
    ROW_NUMBER() OVER (
      PARTITION BY week_start
      ORDER BY wins DESC, last_win_at ASC NULLS LAST, user_id ASC
    ) AS podium_rank
  FROM weekly_scores
)
INSERT INTO weekly_medals (id, user_id, week_start, medal, awarded_at)
SELECT
  md5('weekly-medal:' || user_id || ':' || week_start::text),
  user_id,
  week_start::text,
  CASE podium_rank
    WHEN 1 THEN 'GOLD'
    WHEN 2 THEN 'SILVER'
    WHEN 3 THEN 'BRONZE'
  END,
  now()::text
FROM ranked
WHERE podium_rank <= 3
ON CONFLICT (user_id, week_start) DO NOTHING;
