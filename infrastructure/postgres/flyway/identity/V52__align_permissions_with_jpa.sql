-- Align the Flyway-managed permissions table with the JPA Permission entity columns.
-- The entity uses code/name/resource/action; Flyway uses permission_code/permission_name/resource_id/action_id.
-- Add the entity columns and backfill them so both schemas agree.

ALTER TABLE IF EXISTS permissions
    ADD COLUMN IF NOT EXISTS code VARCHAR(150),
    ADD COLUMN IF NOT EXISTS name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS resource VARCHAR(100),
    ADD COLUMN IF NOT EXISTS action VARCHAR(50);

-- Backfill from the Flyway columns.
UPDATE permissions SET code = permission_code WHERE code IS NULL;
UPDATE permissions SET name = permission_name WHERE name IS NULL;

UPDATE permissions p
   SET resource = r.resource_code
  FROM resources r
 WHERE p.resource_id = r.id
   AND p.resource IS NULL;

UPDATE permissions p
   SET action = a.action_code
  FROM actions a
 WHERE p.action_id = a.id
   AND p.action IS NULL;

-- Enforce NOT NULL for required fields.
ALTER TABLE permissions
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN resource SET NOT NULL,
    ALTER COLUMN action SET NOT NULL;

-- Unique constraint on code (if not exists).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_permissions_code'
    ) THEN
        ALTER TABLE permissions ADD CONSTRAINT uk_permissions_code UNIQUE (code);
    END IF;
END $$;
