-- Make Flyway-managed role columns nullable so JPA inserts (code/name) succeed.
ALTER TABLE IF EXISTS roles
    ALTER COLUMN role_code DROP NOT NULL,
    ALTER COLUMN role_name DROP NOT NULL;
