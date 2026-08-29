INSERT INTO user_announcements (id, user_id, campaign, created_at)
SELECT md5('HEXAFLOW_LAUNCH:user:' || id), id, 'HEXAFLOW_LAUNCH', now()::text
FROM users
ON CONFLICT (user_id, campaign) DO NOTHING;
