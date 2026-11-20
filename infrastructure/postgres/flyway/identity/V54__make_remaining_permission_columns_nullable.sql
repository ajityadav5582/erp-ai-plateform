-- Make all Flyway-only permission columns nullable so JPA inserts (code/name/resource/action) succeed.
ALTER TABLE IF EXISTS permissions
    ALTER COLUMN module_group DROP NOT NULL,
    ALTER COLUMN sub_module DROP NOT NULL,
    ALTER COLUMN display_order DROP NOT NULL,
    ALTER COLUMN is_system DROP NOT NULL,
    ALTER COLUMN resource_id DROP NOT NULL,
    ALTER COLUMN action_id DROP NOT NULL,
    ALTER COLUMN permission_code DROP NOT NULL,
    ALTER COLUMN permission_name DROP NOT NULL;
