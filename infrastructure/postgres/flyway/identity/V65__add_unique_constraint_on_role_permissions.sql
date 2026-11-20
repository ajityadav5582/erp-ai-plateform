-- Ensure unique constraint on (role_id, permission_id) to support ON CONFLICT (role_id, permission_id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_role_permissions_role_permission'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'uk_role_permissions_role_permission' AND c.relkind = 'i'
    ) THEN
        ALTER TABLE role_permissions ADD CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id);
    END IF;
END $$;

