-- Migration: V38
-- Description: Adds ABAC DataScope, ActionCategory, and UI Matrix Metadata columns
-- to support enriched identity domain entities (Action, Permission, RolePermission).

-- ============================================================
-- Step 1: Update actions table with category and display_order
-- ============================================================
ALTER TABLE actions
    ADD COLUMN IF NOT EXISTS category VARCHAR(30) NOT NULL DEFAULT 'CRUD',
    ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;

-- Backfill Action Categories & Display Orders for compile-time Action enum mapping
UPDATE actions SET category = 'CRUD', display_order = 10 WHERE action_code = 'CREATE';
UPDATE actions SET category = 'CRUD', display_order = 20 WHERE action_code = 'VIEW';
UPDATE actions SET category = 'CRUD', display_order = 30 WHERE action_code = 'UPDATE';
UPDATE actions SET category = 'CRUD', display_order = 40 WHERE action_code = 'DELETE';
UPDATE actions SET category = 'WORKFLOW', display_order = 50 WHERE action_code = 'APPROVE';
UPDATE actions SET category = 'WORKFLOW', display_order = 60 WHERE action_code = 'REJECT';
UPDATE actions SET category = 'DATA_IO', display_order = 70 WHERE action_code = 'EXPORT';
UPDATE actions SET category = 'DATA_IO', display_order = 80 WHERE action_code = 'IMPORT';
UPDATE actions SET category = 'ADMIN', display_order = 90 WHERE action_code = 'MANAGE';
UPDATE actions SET category = 'ADMIN', display_order = 100 WHERE action_code = 'ASSIGN';
UPDATE actions SET category = 'DATA_IO', display_order = 110 WHERE action_code = 'REPORT';

CREATE INDEX IF NOT EXISTS idx_actions_category_order
    ON actions(category, display_order);

-- ============================================================
-- Step 2: Update permissions table with UI grouping metadata
-- ============================================================
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS module_group VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sub_module VARCHAR(100),
    ADD COLUMN IF NOT EXISTS display_order INT;

-- Backfill module_group from resources table or resource_code
UPDATE permissions p
SET module_group = COALESCE(r.module, r.resource_code, 'SYSTEM')
FROM resources r
WHERE p.resource_id = r.id
  AND (p.module_group IS NULL OR p.module_group = '');

UPDATE permissions
SET module_group = 'SYSTEM'
WHERE module_group IS NULL OR module_group = '';

ALTER TABLE permissions
    ALTER COLUMN module_group SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_permissions_module_group
    ON permissions(module_group, sub_module);

-- ============================================================
-- Step 3: Update role_permissions table with ABAC DataScope & deactivation audit
-- ============================================================
ALTER TABLE role_permissions
    ADD COLUMN IF NOT EXISTS data_scope VARCHAR(30) NOT NULL DEFAULT 'ALL',
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_role_perm_role
    ON role_permissions(role_id);

CREATE INDEX IF NOT EXISTS idx_role_perm_perm
    ON role_permissions(permission_id);

CREATE INDEX IF NOT EXISTS idx_role_perm_tenant_role
    ON role_permissions(tenant_id, role_id);
