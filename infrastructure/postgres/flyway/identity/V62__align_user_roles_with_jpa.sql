-- Align user_roles table with JPA UserRole entity columns
ALTER TABLE IF EXISTS user_roles
    ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_roles' AND column_name = 'is_primary_role'
    ) THEN
        UPDATE user_roles SET is_primary = is_primary_role;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_roles' AND column_name = 'revoked_at'
    ) THEN
        UPDATE user_roles SET active = (revoked_at IS NULL);
    END IF;
END $$;

