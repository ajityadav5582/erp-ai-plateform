-- Make code and name columns nullable in roles and sync with role_code and role_name via trigger
ALTER TABLE IF EXISTS roles ALTER COLUMN code DROP NOT NULL;
ALTER TABLE IF EXISTS roles ALTER COLUMN name DROP NOT NULL;

CREATE OR REPLACE FUNCTION sync_role_code_and_name() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.code IS NULL THEN
        NEW.code := NEW.role_code;
    END IF;
    IF NEW.role_code IS NULL THEN
        NEW.role_code := NEW.code;
    END IF;
    IF NEW.name IS NULL THEN
        NEW.name := NEW.role_name;
    END IF;
    IF NEW.role_name IS NULL THEN
        NEW.role_name := NEW.name;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_role_code_and_name ON roles;
CREATE TRIGGER trg_sync_role_code_and_name
    BEFORE INSERT OR UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION sync_role_code_and_name();

