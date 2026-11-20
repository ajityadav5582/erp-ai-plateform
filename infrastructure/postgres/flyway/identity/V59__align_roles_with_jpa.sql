-- Align the Flyway-managed roles table with the JPA Role entity columns.
-- The entity uses code/name; Flyway uses role_code/role_name.
-- Add the entity columns and backfill them so both schemas agree.
ALTER TABLE IF EXISTS roles
    ADD COLUMN IF NOT EXISTS code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS name VARCHAR(150);

UPDATE roles SET code = role_code WHERE code IS NULL;
UPDATE roles SET name = role_name WHERE name IS NULL;

-- Enforce NOT NULL for required fields.
ALTER TABLE roles
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN name SET NOT NULL;

-- No global unique constraint on code: role codes are tenant-scoped.
