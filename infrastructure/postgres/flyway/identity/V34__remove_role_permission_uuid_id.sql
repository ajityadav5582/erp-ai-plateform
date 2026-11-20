ALTER TABLE role_permissions
    DROP COLUMN IF EXISTS role_permission_id;

ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS uk_role_permissions_role_permission;

DROP INDEX IF EXISTS uk_role_permissions_role_permission;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'role_permissions'::regclass
          AND conname = 'uk_role_permissions_role_permission'
    ) THEN
        ALTER TABLE role_permissions
            ADD CONSTRAINT uk_role_permissions_role_permission
            UNIQUE (role_id, permission_id);
    END IF;
END
$$;
