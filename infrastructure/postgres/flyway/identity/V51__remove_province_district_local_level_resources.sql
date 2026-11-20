-- Migration: V51
-- Description: Remove PROVINCE, DISTRICT, LOCAL_LEVEL resources and their permissions.
-- These geographic master data resources are no longer managed via permissions/roles.

-- ============================================================================
-- 1. Remove role-template permissions referencing PROVINCE/DISTRICT/LOCAL_LEVEL
-- ============================================================================
DELETE FROM role_template_permissions
WHERE permission_id IN (
    SELECT p.id FROM permissions p
    JOIN resources r ON p.resource_id = r.id
    WHERE r.resource_code IN ('PROVINCE', 'DISTRICT', 'LOCAL_LEVEL')
);

-- ============================================================================
-- 2. Remove tenant-scoped role permissions referencing PROVINCE/DISTRICT/LOCAL_LEVEL
-- ============================================================================
DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT p.id FROM permissions p
    JOIN resources r ON p.resource_id = r.id
    WHERE r.resource_code IN ('PROVINCE', 'DISTRICT', 'LOCAL_LEVEL')
);

-- ============================================================================
-- 3. Remove permissions for PROVINCE/DISTRICT/LOCAL_LEVEL
-- ============================================================================
DELETE FROM permissions
WHERE resource_id IN (
    SELECT r.id FROM resources r
    WHERE r.resource_code IN ('PROVINCE', 'DISTRICT', 'LOCAL_LEVEL')
);

-- ============================================================================
-- 4. Remove PROVINCE/DISTRICT/LOCAL_LEVEL resources
-- ============================================================================
DELETE FROM resources
WHERE resource_code IN ('PROVINCE', 'DISTRICT', 'LOCAL_LEVEL');
