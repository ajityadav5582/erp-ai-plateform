-- Seed platform SYSTEM roles (SUPER_ADMIN, TENANT_ADMIN) so master data exists
-- immediately after a database reset, even before the application boots.
INSERT INTO roles (tenant_id, role_code, role_name, description, role_type, status)
SELECT NULL, 'SUPER_ADMIN', 'Super Administrator',
       'System super administrator with full platform access across all tenants',
       'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND role_code = 'SUPER_ADMIN');

INSERT INTO roles (tenant_id, role_code, role_name, description, role_type, status)
SELECT NULL, 'TENANT_ADMIN', 'Tenant Administrator',
       'Platform-managed administrator role assigned to a tenant''s first user',
       'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND role_code = 'TENANT_ADMIN');
