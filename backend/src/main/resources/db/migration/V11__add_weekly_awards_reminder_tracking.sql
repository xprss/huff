ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS last_weekly_awards_reminder_date varchar(255);
