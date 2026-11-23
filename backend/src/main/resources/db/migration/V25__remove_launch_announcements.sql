-- Notifications are sent only from the admin alerts panel: drop the hardcoded launch campaigns.
DROP TABLE IF EXISTS user_announcements;
DELETE FROM push_campaign_deliveries WHERE campaign IN ('HEXAHACK_LAUNCH', 'HEXASKY_LAUNCH');
