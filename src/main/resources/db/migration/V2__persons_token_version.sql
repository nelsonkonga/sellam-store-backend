-- Hibernate ddl-auto=update emits:
--   alter table persons add column token_version integer not null
-- PostgreSQL rejects that on a non-empty table ("column contains null values")
-- because existing rows would be null. Add the column nullable, backfill, then
-- enforce NOT NULL with a default for future inserts.
ALTER TABLE persons ADD COLUMN IF NOT EXISTS token_version integer;

UPDATE persons SET token_version = 0 WHERE token_version IS NULL;

ALTER TABLE persons ALTER COLUMN token_version SET DEFAULT 0;
ALTER TABLE persons ALTER COLUMN token_version SET NOT NULL;
