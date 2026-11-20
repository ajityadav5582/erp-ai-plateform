-- Extend the permissions resource check constraint to support the
-- identity-module resources (BRANCH, DEPARTMENT, USER, ROLE, PERMISSION,
-- USER_ROLE, USER_BRANCH, USER_DEPARTMENT, PROVINCE, DISTRICT, LOCAL_LEVEL)
-- introduced with the new Resource enum values. This allows onboarding to
-- seed the full set of identity permissions required by the REST controllers.
ALTER TABLE permissions
DROP CONSTRAINT IF EXISTS chk_permissions_resource;

ALTER TABLE permissions
ADD CONSTRAINT chk_permissions_resource CHECK (resource IN (
    'PRODUCT', 'SALES', 'PURCHASE', 'INVENTORY', 'HR',
    'ACCOUNTING', 'CRM', 'AI', 'MANUFACTURING', 'PLATFORM',
    'REPORTING', 'INTEGRATION',
    'IDENTITY', 'BRANCH', 'DEPARTMENT', 'USER', 'ROLE',
    'PERMISSION', 'USER_ROLE', 'USER_BRANCH', 'USER_DEPARTMENT',
    'PROVINCE', 'DISTRICT', 'LOCAL_LEVEL'
));
