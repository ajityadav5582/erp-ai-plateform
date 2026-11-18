-- Make Flyway-managed permission columns nullable so JPA (which uses code/name/resource/action) can insert rows.
ALTER TABLE IF EXISTS permissions
    ALTER COLUMN permission_code DROP NOT NULL,
    ALTER COLUMN permission_name DROP NOT NULL,
    ALTER COLUMN resource_id DROP NOT NULL,
    ALTER COLUMN action_id DROP NOT NULL;
