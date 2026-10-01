-- Migration: V37
-- Description: Adds explicit tenant_id scoping to role_permissions table
-- and establishes robust deduplication constraints for SaaS multi-tenancy.
--
-- This migration:
--   1. Adds tenant_id column to role_permissions (backfilled from roles)
--   2. Makes tenant_id NOT NULL with a default of the default tenant
--   3. Drops the old (role_id, permission_id) unique constraint
--   4. Adds composite unique constraint on (tenant_id, role_id, permission_id)
--   5. Adds foreign key constraint on tenant_id
--   6. Creates optimized indexes for tenant-scoped queries
--   7. Cleans up any existing duplicate rows

-- ============================================================
-- Step 1: Clean up any existing duplicate role_permissions rows
-- ============================================================
DELETE FROM role_permissions rp1
WHERE id > (
    SELECT MIN(id)
    FROM role_permissions rp2
    WHERE rp2.role_id = rp1.role_id
      AND rp2.permission_id = rp1.permission_id
);

-- ============================================================
-- Step 2: Add tenant_id column (nullable initially for backfill)
-- ============================================================
ALTER TABLE role_permissions
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ============================================================
-- Step 3: Backfill tenant_id from the roles table
-- ============================================================
UPDATE role_permissions rp
SET tenant_id = r.tenant_id
FROM roles r
WHERE rp.role_id = r.id
  AND rp.tenant_id IS NULL;

-- ============================================================
-- Step 4: Set default for any remaining NULL tenant_ids (safety)
-- ============================================================
UPDATE role_permissions
SET tenant_id = (SELECT id FROM tenants WHERE tenant_code = 'default' LIMIT 1)
WHERE tenant_id IS NULL;

-- ============================================================
-- Step 5: Make tenant_id NOT NULL
-- ============================================================
ALTER TABLE role_permissions
    ALTER COLUMN tenant_id SET NOT NULL;

-- ============================================================
-- Step 6: Drop old unique constraint on (role_id, permission_id)
-- ============================================================
ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS uk_role_permissions_role_permission;

DROP INDEX IF EXISTS idx_role_permissions_role_permission;

-- ============================================================
-- Step 7: Add composite unique constraint on (tenant_id, role_id, permission_id)
-- ============================================================
ALTER TABLE role_permissions
    ADD CONSTRAINT uk_role_permissions_tenant_role_permission
    UNIQUE (tenant_id, role_id, permission_id);

-- ============================================================
-- Step 8: Add foreign key constraint on tenant_id
-- ============================================================
ALTER TABLE role_permissions
    ADD CONSTRAINT fk_role_permissions_tenant_id_tenants
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
    ON DELETE RESTRICT;

-- ============================================================
-- Step 9: Create optimized indexes for tenant-scoped queries
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_role_permissions_tenant_id
    ON role_permissions(tenant_id);

CREATE INDEX IF NOT EXISTS idx_role_permissions_tenant_role
    ON role_permissions(tenant_id, role_id);

CREATE INDEX IF NOT EXISTS idx_role_permissions_tenant_permission
    ON role_permissions(tenant_id, permission_id);

-- ============================================================
-- Step 10: Verify the unique constraint covers all existing rows
-- (This is implicitly verified by the DELETE in Step 1)
