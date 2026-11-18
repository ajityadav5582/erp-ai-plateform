-- Allow SYSTEM roles to have a NULL tenant_id (required by chk_roles_scope).
ALTER TABLE IF EXISTS roles
    ALTER COLUMN tenant_id DROP NOT NULL;
