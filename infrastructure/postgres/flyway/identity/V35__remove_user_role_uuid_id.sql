-- Drop the legacy UUID business identifier column from user role assignments.
-- UserRole assignments are now identified by their numeric primary key (id).

ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS user_roles_user_role_id_key;

DROP INDEX IF EXISTS user_roles_user_role_id_key;

ALTER TABLE user_roles
    DROP COLUMN IF EXISTS user_role_id;

-- Keep the active-assignment uniqueness constraint tenant-scoped. Revoked
-- assignments remain available for audit and may be reassigned.
DROP INDEX IF EXISTS uk_user_roles_user_role_active;

CREATE UNIQUE INDEX uk_user_roles_user_role_active
    ON user_roles(tenant_id, user_id, role_id)
    WHERE revoked_at IS NULL;
