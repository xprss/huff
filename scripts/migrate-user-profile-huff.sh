#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

"${PROJECT_ROOT}/scripts/db-huff.sh" query "
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname varchar(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_emoji varchar(16);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio varchar(200);

UPDATE users
SET display_name = COALESCE(NULLIF(trim(display_name), ''), 'Giocatore');

UPDATE users
SET profile_emoji = '😀'
WHERE profile_emoji IS NULL
  OR profile_emoji NOT IN ('😀', '😄', '😎', '🤓', '🥳', '😇', '🤠', '😴', '😤', '😍', '🙃', '😌');

WITH normalized AS (
  SELECT
    id,
    COALESCE(
      NULLIF(regexp_replace(regexp_replace(lower(display_name), '[^a-z0-9._-]+', '-', 'g'), '^[._-]+|[._-]+\$', '', 'g'), ''),
      'giocatore'
    ) AS slug
  FROM users
),
generated AS (
  SELECT
    id,
    '@' || left(slug, 20) || '-' || substr(md5(id), 1, 8) AS nickname
  FROM normalized
)
UPDATE users u
SET nickname = generated.nickname
FROM generated
WHERE u.id = generated.id
  AND (
    u.nickname IS NULL
    OR u.nickname !~ '^@[a-z0-9._-]+$'
    OR length(u.nickname) > 30
  );

DO \$\$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM users
    GROUP BY nickname
    HAVING COUNT(*) > 1
  ) THEN
    RAISE EXCEPTION 'users.nickname contains duplicates; aborting unique index creation';
  END IF;
END \$\$;

CREATE UNIQUE INDEX IF NOT EXISTS users_nickname_unique ON users (nickname);

ALTER TABLE users ALTER COLUMN nickname SET NOT NULL;
ALTER TABLE users ALTER COLUMN profile_emoji SET NOT NULL;
"
