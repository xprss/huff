#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FIRST_ADMIN_ID="google:VincenzoSagristano:2399532d11393886cfb8d77c1207d666933aa31cedd6b10f92247b79e5d74e96"

"${PROJECT_ROOT}/scripts/db-huff.sh" query "
CREATE TABLE IF NOT EXISTS admin_users (
  user_id varchar(255) PRIMARY KEY,
  can_view_players boolean,
  can_view_player_details boolean,
  can_manage_players boolean,
  can_manage_admins boolean,
  can_manage_hexaflow_puzzles boolean,
  created_at varchar(255),
  updated_at varchar(255)
);

ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_view_players boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_view_player_details boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_manage_players boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_manage_admins boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS can_manage_hexaflow_puzzles boolean;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS created_at varchar(255);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS updated_at varchar(255);

UPDATE admin_users
SET
  can_view_players = COALESCE(can_view_players, false),
  can_view_player_details = COALESCE(can_view_player_details, false),
  can_manage_players = COALESCE(can_manage_players, false),
  can_manage_admins = COALESCE(can_manage_admins, false),
  can_manage_hexaflow_puzzles = COALESCE(can_manage_hexaflow_puzzles, can_manage_players OR can_manage_admins, false),
  created_at = COALESCE(created_at, now()::text),
  updated_at = COALESCE(updated_at, now()::text);

INSERT INTO admin_users (
  user_id,
  can_view_players,
  can_view_player_details,
  can_manage_players,
  can_manage_admins,
  can_manage_hexaflow_puzzles,
  created_at,
  updated_at
)
VALUES (
  '${FIRST_ADMIN_ID}',
  true,
  true,
  true,
  true,
  true,
  now()::text,
  now()::text
)
ON CONFLICT (user_id) DO UPDATE
SET
  can_view_players = true,
  can_view_player_details = true,
  can_manage_players = true,
  can_manage_admins = true,
  can_manage_hexaflow_puzzles = true,
  updated_at = now()::text;

ALTER TABLE admin_users ALTER COLUMN can_view_players SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_view_player_details SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_manage_players SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_manage_admins SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_manage_hexaflow_puzzles SET DEFAULT false;
ALTER TABLE admin_users ALTER COLUMN can_view_players SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_view_player_details SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_manage_players SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_manage_admins SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN can_manage_hexaflow_puzzles SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE admin_users ALTER COLUMN updated_at SET NOT NULL;
"
