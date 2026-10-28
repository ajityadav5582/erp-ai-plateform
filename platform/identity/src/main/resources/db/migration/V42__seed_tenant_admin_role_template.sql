-- Migration: V42
-- Description: Seed the TENANT_ADMIN role template used during initial registration.
-- The TENANT_ADMIN is assigned to the user created during registration and is
-- responsible for creating and managing multiple Companies within the tenant.

-- ============================================================
-- 1. Seed the TENANT_ADMIN role template
-- ============================================================
INSERT INTO role_templates (
    template_code,
    template_name,
    description,
    status,
    is_system,
    created_at,
    version
) VALUES (
    'TENANT_ADMIN',
    'Tenant Administrator',
    'Tenant administrator responsible for creating and managing multiple Companies within the tenant, switching between companies, and managing users/roles according to the selected company',
    'ACTIVE',
    TRUE,
    CURRENT_TIMESTAMP,
    0)
ON CONFLICT (template_code) DO NOTHING;

-- ============================================================
-- 2. Seed permissions for the TENANT_ADMIN role template.
-- TENANT_ADMIN receives the same broad permission matrix as ADMIN (tenant
-- administration and core business operations) so the tenant admin can
-- manage users, roles, branches, products, sales, purchase, inventory,
-- reporting, and settings within the tenant.
-- ============================================================
WITH role_permission_data(template_code, permission_code) AS (
    VALUES
        ('TENANT_ADMIN', 'USER_READ'),
        ('TENANT_ADMIN', 'USER_CREATE'),
        ('TENANT_ADMIN', 'USER_UPDATE'),
        ('TENANT_ADMIN', 'USER_DELETE'),
        ('TENANT_ADMIN', 'USER_ASSIGN'),
        ('TENANT_ADMIN', 'ROLE_READ'),
        ('TENANT_ADMIN', 'ROLE_CREATE'),
        ('TENANT_ADMIN', 'ROLE_UPDATE'),
        ('TENANT_ADMIN', 'ROLE_DELETE'),
        ('TENANT_ADMIN', 'ROLE_ASSIGN'),
        ('TENANT_ADMIN', 'BRANCH_READ'),
        ('TENANT_ADMIN', 'BRANCH_CREATE'),
        ('TENANT_ADMIN', 'BRANCH_UPDATE'),
        ('TENANT_ADMIN', 'BRANCH_DELETE'),
        ('TENANT_ADMIN', 'PRODUCT_READ'),
        ('TENANT_ADMIN', 'PRODUCT_CREATE'),
        ('TENANT_ADMIN', 'PRODUCT_UPDATE'),
        ('TENANT_ADMIN', 'PRODUCT_DELETE'),
        ('TENANT_ADMIN', 'PRODUCT_EXPORT'),
        ('TENANT_ADMIN', 'PRODUCT_IMPORT'),
        ('TENANT_ADMIN', 'SALE_READ'),
        ('TENANT_ADMIN', 'SALE_CREATE'),
        ('TENANT_ADMIN', 'SALE_CANCEL'),
        ('TENANT_ADMIN', 'SALE_REFUND'),
        ('TENANT_ADMIN', 'SALE_EXPORT'),
        ('TENANT_ADMIN', 'SALE_PRINT'),
        ('TENANT_ADMIN', 'PURCHASE_READ'),
        ('TENANT_ADMIN', 'PURCHASE_CREATE'),
        ('TENANT_ADMIN', 'PURCHASE_UPDATE'),
        ('TENANT_ADMIN', 'PURCHASE_CANCEL'),
        ('TENANT_ADMIN', 'PURCHASE_EXPORT'),
        ('TENANT_ADMIN', 'PURCHASE_PRINT'),
        ('TENANT_ADMIN', 'INVENTORY_READ'),
        ('TENANT_ADMIN', 'INVENTORY_ADJUST'),
        ('TENANT_ADMIN', 'INVENTORY_TRANSFER'),
        ('TENANT_ADMIN', 'INVENTORY_EXPORT'),
        ('TENANT_ADMIN', 'REPORT_READ'),
        ('TENANT_ADMIN', 'REPORT_EXPORT'),
        ('TENANT_ADMIN', 'REPORT_PRINT'),
        ('TENANT_ADMIN', 'SETTINGS_READ'),
        ('TENANT_ADMIN', 'SETTINGS_UPDATE'),
        ('TENANT_ADMIN', 'SETTINGS_MANAGE')
)
INSERT INTO role_template_permissions (
    role_template_id,
    permission_id,
    assigned_by,
    assigned_at,
    created_at,
    version
)
SELECT
    rt.id,
    p.id,
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM role_permission_data data
JOIN role_templates rt
  ON rt.template_code = data.template_code
JOIN permissions p
  ON p.permission_code = data.permission_code
WHERE rt.status = 'ACTIVE'
  AND p.status = 'ACTIVE'
ON CONFLICT (role_template_id, permission_id) DO NOTHING;
