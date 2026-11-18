-- Extend the permissions resource check constraint to include the
-- RESOURCE value introduced with the new Resource enum value.
-- This allows onboarding to seed RESOURCE_* permissions for new tenants.
ALTER TABLE permissions
DROP CONSTRAINT IF EXISTS chk_permissions_resource;

ALTER TABLE permissions
ADD CONSTRAINT chk_permissions_resource CHECK (resource IN (
    'PRODUCT', 'SALES', 'PURCHASE', 'INVENTORY', 'HR',
    'ACCOUNTING', 'CRM', 'AI', 'MANUFACTURING', 'PLATFORM',
    'REPORTING', 'INTEGRATION',
    'IDENTITY', 'BRANCH', 'DEPARTMENT', 'USER', 'ROLE',
    'PERMISSION', 'USER_ROLE', 'USER_BRANCH', 'USER_DEPARTMENT',
    'PROVINCE', 'DISTRICT', 'LOCAL_LEVEL', 'RESOURCE'
));
