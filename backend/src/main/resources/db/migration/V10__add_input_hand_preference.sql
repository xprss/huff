ALTER TABLE users ADD COLUMN IF NOT EXISTS input_hand_preference varchar(5);

UPDATE users
SET input_hand_preference = 'RIGHT'
WHERE input_hand_preference IS NULL
   OR input_hand_preference NOT IN ('LEFT', 'RIGHT');

ALTER TABLE users ALTER COLUMN input_hand_preference SET DEFAULT 'RIGHT';
ALTER TABLE users ALTER COLUMN input_hand_preference SET NOT NULL;
