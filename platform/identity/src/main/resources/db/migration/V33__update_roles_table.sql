-- Migration: V33
-- Description: Updates the roles table structure:
--   - Removes role_id (UUID) column
--   - Removes is_system_role column
--   - Makes tenant_id required (NOT NULL)
--   - Adds role_template_id column (nullable)
--   - Adds is_editable column (nullable)
--   - Adds is_deletable column (nullable)
--   - Adds status column (NOT NULL, default ACTIVE)
--   - Updates unique constraint to (tenant_id, role_code)
--   - Updates role_type CHECK constraint to include DEFAULT

-- Add new columns
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS role_template_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_editable BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_deletable BOOLEAN,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Make tenant_id required
-- Backfill NULL tenant_id (system roles) with the default tenant before enforcing NOT NULL
UPDATE roles
SET tenant_id = (
    SELECT id FROM tenants WHERE tenant_code = 'default' LIMIT 1
)
WHERE tenant_id IS NULL;

ALTER TABLE roles
    ALTER COLUMN tenant_id SET NOT NULL;

-- Drop old columns (UUID role ID and is_system_role)
ALTER TABLE roles
    DROP COLUMN IF EXISTS role_id,
    DROP COLUMN IF EXISTS is_system_role;

-- Drop old indexes
DROP INDEX IF EXISTS idx_roles_role_id;
DROP INDEX IF EXISTS uk_roles_tenant_role_code;
DROP INDEX IF EXISTS uk_roles_global_role_code;

-- Add new unique constraint on (tenant_id, role_code)
CREATE UNIQUE INDEX uk_roles_tenant_role_code
    ON roles(tenant_id, role_code);

-- Update role_type CHECK constraint to include DEFAULT
-- First drop the old constraint, then add the new one
ALTER TABLE roles
    DROP CONSTRAINT IF EXISTS chk_roles_role_type;

ALTER TABLE roles
    ADD CONSTRAINT chk_roles_role_type CHECK (role_type IN ('SYSTEM', 'DEFAULT', 'CUSTOM'));

-- Set defaults for existing rows
UPDATE roles SET is_editable = false WHERE is_editable IS NULL AND role_type = 'SYSTEM';
UPDATE roles SET is_editable = true WHERE is_editable IS NULL AND role_type IN ('DEFAULT', 'CUSTOM');
UPDATE roles SET is_deletable = false WHERE is_deletable IS NULL AND role_type = 'SYSTEM';
UPDATE roles SET is_deletable = true WHERE is_deletable IS NULL AND role_type IN ('DEFAULT', 'CUSTOM');

-- Set status for existing rows
UPDATE roles SET status = 'ACTIVE' WHERE status IS NULL;

-- Make new columns nullable (they already are, but being explicit)
-- role_template_id remains nullable (NULL for SYSTEM and CUSTOM)
-- is_editable remains nullable
-- is_deletable remains nullable
