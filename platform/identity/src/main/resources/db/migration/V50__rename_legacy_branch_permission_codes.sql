-- Migration: V50
-- Description: Rename the legacy BRANCH_READ permission code to BRANCH_VIEW in
-- the role template seed data.
--
-- The permission catalog is seeded at runtime by DataInitializer using the
-- canonical VIEW action (PermissionSeed("BRANCH", "VIEW") -> BRANCH_VIEW), but
-- the V32/V42 role-template seed data still references the old BRANCH_READ code.
-- The V32 join (role_template_permissions -> permissions on permission_code)
-- silently drops those rows because no permission with code BRANCH_READ exists,
-- so the ADMIN and TENANT_ADMIN templates ship without branch access.
--
-- This migration re-points the seed data at BRANCH_VIEW. It is idempotent:
-- running it twice leaves the table in the same state because the new rows
-- use ON CONFLICT DO NOTHING and the old rows are deleted by a code filter
-- rather than by a row count assumption.

-- 1. Drop any role-template permission rows that reference the legacy code.
--    These can exist when a database was seeded before this migration ran
--    against a permissions table that still held BRANCH_READ.
DELETE FROM role_template_permissions
WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code = 'BRANCH_READ'
);

-- 2. Re-insert the BRANCH_VIEW grant for every template that previously
--    referenced BRANCH_READ. The V32 seed data assigns BRANCH_READ to ADMIN
--    and TENANT_ADMIN only; OWNER already receives every permission via the
--    CROSS JOIN, so it is covered by the general insert below.
INSERT INTO role_template_permissions (
    role_template_id,
    permission_id,
    assigned_by,
    assigned_at,
    created_at,
    version
)
SELECT
    rt.id,
    p.id,
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM role_templates rt
CROSS JOIN permissions p
WHERE rt.template_code IN ('ADMIN', 'TENANT_ADMIN')
  AND rt.status = 'ACTIVE'
  AND p.permission_code = 'BRANCH_VIEW'
  AND p.status = 'ACTIVE'
ON CONFLICT (role_template_id, permission_id) DO NOTHING;