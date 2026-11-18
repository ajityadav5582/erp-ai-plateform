-- Make tenant_id nullable in role_permissions for system roles.
ALTER TABLE IF EXISTS role_permissions
    ALTER COLUMN tenant_id DROP NOT NULL;
