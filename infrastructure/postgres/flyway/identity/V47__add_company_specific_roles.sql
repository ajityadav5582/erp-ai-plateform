-- Add optional company scope to custom roles while preserving role IDs and assignments.
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS company_id BIGINT;

-- Roles are tenant-scoped except platform SYSTEM roles. Older migrations assigned
-- system roles to the default tenant and called tenant-provided roles DEFAULT.
UPDATE roles SET role_type = 'TENANT' WHERE role_type = 'DEFAULT';
UPDATE roles SET tenant_id = NULL WHERE role_type = 'SYSTEM';
UPDATE roles SET company_id = NULL WHERE role_type IN ('SYSTEM', 'TENANT');

ALTER TABLE roles ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE roles DROP CONSTRAINT IF EXISTS chk_roles_role_type;
ALTER TABLE roles ADD CONSTRAINT chk_roles_role_type
    CHECK (role_type IN ('SYSTEM', 'TENANT', 'CUSTOM'));
ALTER TABLE roles DROP CONSTRAINT IF EXISTS chk_roles_scope;
ALTER TABLE roles ADD CONSTRAINT chk_roles_scope CHECK (
    (role_type = 'SYSTEM' AND tenant_id IS NULL AND company_id IS NULL)
    OR (role_type = 'TENANT' AND tenant_id IS NOT NULL AND company_id IS NULL)
    OR (role_type = 'CUSTOM' AND tenant_id IS NOT NULL)
);

ALTER TABLE roles DROP CONSTRAINT IF EXISTS fk_roles_company_id_companies;
ALTER TABLE roles ADD CONSTRAINT fk_roles_company_id_companies
    FOREIGN KEY (company_id) REFERENCES companies(id);

-- Enforce same-tenant ownership for company-scoped custom roles at the database layer.
CREATE OR REPLACE FUNCTION enforce_role_company_tenant() RETURNS trigger AS $$
BEGIN
    IF NEW.company_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM companies c
        WHERE c.id = NEW.company_id AND c.tenant_id = NEW.tenant_id
    ) THEN
        RAISE EXCEPTION 'Role company must belong to the same tenant';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_roles_company_tenant ON roles;
CREATE TRIGGER trg_roles_company_tenant
    BEFORE INSERT OR UPDATE OF company_id, tenant_id ON roles
    FOR EACH ROW EXECUTE FUNCTION enforce_role_company_tenant();

DROP INDEX IF EXISTS uk_roles_tenant_role_code;
CREATE UNIQUE INDEX uk_roles_tenant_role_code
    ON roles(tenant_id, role_code)
    WHERE tenant_id IS NOT NULL AND company_id IS NULL;
CREATE UNIQUE INDEX uk_roles_company_role_code
    ON roles(tenant_id, company_id, role_code)
    WHERE company_id IS NOT NULL;
CREATE UNIQUE INDEX uk_roles_global_role_code
    ON roles(role_code)
    WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_roles_tenant_company ON roles(tenant_id, company_id);

-- Data scope is stored per role-permission; company scoped roles must never retain ALL.
UPDATE role_permissions rp
SET data_scope = 'OWN_COMPANY'
FROM roles r
WHERE rp.role_id = r.id AND r.company_id IS NOT NULL AND rp.data_scope = 'ALL';

CREATE OR REPLACE FUNCTION enforce_role_permission_scope() RETURNS trigger AS $$
DECLARE scoped_company BIGINT;
BEGIN
    SELECT company_id INTO scoped_company FROM roles WHERE id = NEW.role_id;
    IF scoped_company IS NOT NULL AND NEW.data_scope = 'ALL' THEN
        RAISE EXCEPTION 'Company-specific roles cannot grant All Tenant Data';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_role_permission_scope ON role_permissions;
CREATE TRIGGER trg_role_permission_scope
    BEFORE INSERT OR UPDATE OF role_id, data_scope ON role_permissions
    FOR EACH ROW EXECUTE FUNCTION enforce_role_permission_scope();
