-- Profile photos replace the emoji avatar. The image itself lives on disk
-- (app.avatars.dir); avatar_version is a content hash used to bust caches and
-- is NULL when the user has no photo (the UI then shows the name initials).
ALTER TABLE users ADD COLUMN avatar_version varchar(32);
ALTER TABLE users DROP COLUMN profile_emoji;
