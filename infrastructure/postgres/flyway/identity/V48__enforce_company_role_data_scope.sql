-- Company-specific custom roles are confined to their company. Older or
-- manually-created ALL grants are narrowed, and future writes are rejected.
UPDATE role_permissions rp
SET data_scope = 'OWN_COMPANY'
FROM roles r
WHERE rp.role_id = r.id
  AND r.role_type = 'CUSTOM'
  AND r.company_id IS NOT NULL
  AND rp.data_scope = 'ALL';

CREATE OR REPLACE FUNCTION enforce_role_permission_scope() RETURNS trigger AS $$
DECLARE
    scoped_company BIGINT;
    scoped_type VARCHAR(30);
BEGIN
    SELECT company_id, role_type
      INTO scoped_company, scoped_type
      FROM roles
     WHERE id = NEW.role_id;

    IF scoped_type = 'CUSTOM' AND scoped_company IS NOT NULL AND NEW.data_scope = 'ALL' THEN
        RAISE EXCEPTION 'Company-specific roles cannot grant All Tenant Data';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_role_permission_scope ON role_permissions;
CREATE TRIGGER trg_role_permission_scope
    BEFORE INSERT OR UPDATE OF role_id, data_scope ON role_permissions
    FOR EACH ROW EXECUTE FUNCTION enforce_role_permission_scope();
