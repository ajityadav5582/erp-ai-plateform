-- Align role_permissions table with JPA RolePermission entity columns
ALTER TABLE IF EXISTS role_permissions
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'role_permissions' AND column_name = 'deactivated_at'
    ) THEN
        UPDATE role_permissions SET active = (deactivated_at IS NULL);
    END IF;
END $$;

