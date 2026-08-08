ALTER TABLE users ADD COLUMN IF NOT EXISTS star_available boolean;
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_awarded_at varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_used_at varchar(255);

UPDATE users
SET star_available = COALESCE(star_available, false);

ALTER TABLE users ALTER COLUMN star_available SET DEFAULT false;
ALTER TABLE users ALTER COLUMN star_available SET NOT NULL;
