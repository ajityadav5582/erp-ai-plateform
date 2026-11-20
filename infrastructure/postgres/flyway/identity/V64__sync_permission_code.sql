-- Make code column nullable and sync code with permission_code via trigger
ALTER TABLE IF EXISTS permissions ALTER COLUMN code DROP NOT NULL;

CREATE OR REPLACE FUNCTION sync_permission_code() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.code IS NULL THEN
        NEW.code := NEW.permission_code;
    END IF;
    IF NEW.permission_code IS NULL THEN
        NEW.permission_code := NEW.code;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_permission_code ON permissions;
CREATE TRIGGER trg_sync_permission_code
    BEFORE INSERT OR UPDATE ON permissions
    FOR EACH ROW
    EXECUTE FUNCTION sync_permission_code();

