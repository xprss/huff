CREATE TABLE IF NOT EXISTS admin_users (
  user_id varchar(255) PRIMARY KEY,
  can_view_players boolean,
  can_view_player_details boolean,
  can_manage_players boolean,
  can_manage_admins boolean,
  created_at varchar(255),
  updated_at varchar(255)
);

ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_view_players boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_view_player_details boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_manage_players boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_manage_admins boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS created_at varchar(255);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS updated_at varchar(255);

UPDATE admin_users
SET
  can_view_players = COALESCE(can_view_players, false),
  can_view_player_details = COALESCE(can_view_player_details, false),
  can_manage_players = COALESCE(can_manage_players, false),
  can_manage_admins = COALESCE(can_manage_admins, false),
  created_at = COALESCE(created_at, now()::text),
  updated_at = COALESCE(updated_at, now()::text);

ALTER TABLE admin_users ALTER COLUMN can_view_players SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_view_player_details SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_manage_players SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_manage_admins SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_view_players SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_view_player_details SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_manage_players SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_manage_admins SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN updated_at SET NOT NULL;
