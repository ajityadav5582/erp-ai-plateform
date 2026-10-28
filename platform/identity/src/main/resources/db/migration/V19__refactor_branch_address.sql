-- Refactor branch address model
-- Migration: V19
-- Description:
--   Replaces the free-text branch address fields (city, country, postal_code,
--   timezone, currency, manager_id) with a structured geographic reference.
--   A branch now points to a local_level (municipality/rural municipality) via
--   local_level_id and an optional ward_no. Province/district are derived from
--   the referenced local_level.

-- Drop columns that are no longer part of the branch model.
ALTER TABLE branches DROP COLUMN IF EXISTS city;
ALTER TABLE branches DROP COLUMN IF EXISTS country;
ALTER TABLE branches DROP COLUMN IF EXISTS postal_code;
ALTER TABLE branches DROP COLUMN IF EXISTS timezone;
ALTER TABLE branches DROP COLUMN IF EXISTS currency;
ALTER TABLE branches DROP COLUMN IF EXISTS manager_id;

-- Drop indexes that referenced the removed columns.
DROP INDEX IF EXISTS idx_branches_country;
DROP INDEX IF EXISTS idx_branches_city;
DROP INDEX IF EXISTS idx_branches_currency;
DROP INDEX IF EXISTS idx_branches_timezone;
DROP INDEX IF EXISTS idx_branches_manager_id;

-- Add the new structured address columns.
ALTER TABLE branches ADD COLUMN local_level_id VARCHAR(50);
ALTER TABLE branches ADD COLUMN ward_no VARCHAR(50);

-- Foreign key to the local_levels table (local_level_id -> municipalities).
ALTER TABLE branches
ADD CONSTRAINT fk_branches_local_level_id_local_levels
FOREIGN KEY (local_level_id) REFERENCES local_levels(municipality_id)
ON DELETE RESTRICT;

-- Indexes for the new columns.
CREATE INDEX idx_branches_local_level_id ON branches(local_level_id);
