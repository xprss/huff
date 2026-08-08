ALTER TABLE games ADD COLUMN IF NOT EXISTS mode varchar(255);
ALTER TABLE games ADD COLUMN IF NOT EXISTS mouse_tile_index integer;
ALTER TABLE games ADD COLUMN IF NOT EXISTS mouse_revealed boolean;
ALTER TABLE games ADD COLUMN IF NOT EXISTS kitten_unlocked boolean;
ALTER TABLE games ADD COLUMN IF NOT EXISTS kitten_used_at varchar(255);

UPDATE games
SET
  mode = COALESCE(mode, 'CLASSIC'),
  mouse_revealed = COALESCE(mouse_revealed, true),
  kitten_unlocked = COALESCE(kitten_unlocked, false);

ALTER TABLE games ALTER COLUMN mode SET DEFAULT 'CLASSIC';
ALTER TABLE games ALTER COLUMN mouse_revealed SET DEFAULT true;
ALTER TABLE games ALTER COLUMN kitten_unlocked SET DEFAULT false;
ALTER TABLE games ALTER COLUMN mode SET NOT NULL;
ALTER TABLE games ALTER COLUMN mouse_revealed SET NOT NULL;
ALTER TABLE games ALTER COLUMN kitten_unlocked SET NOT NULL;
