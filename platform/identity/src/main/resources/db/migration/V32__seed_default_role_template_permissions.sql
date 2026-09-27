-- Seed permissions for the default global role templates.
-- Migration: V32
-- Description: Assigns the default permission matrix to the six system role templates.
-- Template and permission lookups use stable business codes; no database IDs are embedded.
-- The existing permission catalog uses READ for the user-facing VIEW action.

-- OWNER receives every active tenant-level permission currently in the global catalog.
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
FROM role_templates rt
CROSS JOIN permissions p
WHERE rt.template_code = 'OWNER'
  AND rt.status = 'ACTIVE'
  AND p.status = 'ACTIVE'
ON CONFLICT (role_template_id, permission_id) DO NOTHING;

-- Explicit permission matrix for the remaining default templates.
WITH role_permission_data(template_code, permission_code) AS (
    VALUES
        -- ADMIN: tenant administration and core business operations.
        ('ADMIN', 'USER_READ'),
        ('ADMIN', 'USER_CREATE'),
        ('ADMIN', 'USER_UPDATE'),
        ('ADMIN', 'USER_DELETE'),
        ('ADMIN', 'USER_ASSIGN'),
        ('ADMIN', 'ROLE_READ'),
        ('ADMIN', 'ROLE_CREATE'),
        ('ADMIN', 'ROLE_UPDATE'),
        ('ADMIN', 'ROLE_DELETE'),
        ('ADMIN', 'ROLE_ASSIGN'),
        ('ADMIN', 'BRANCH_READ'),
        ('ADMIN', 'BRANCH_CREATE'),
        ('ADMIN', 'BRANCH_UPDATE'),
        ('ADMIN', 'BRANCH_DELETE'),
        ('ADMIN', 'PRODUCT_READ'),
        ('ADMIN', 'PRODUCT_CREATE'),
        ('ADMIN', 'PRODUCT_UPDATE'),
        ('ADMIN', 'PRODUCT_DELETE'),
        ('ADMIN', 'PRODUCT_EXPORT'),
        ('ADMIN', 'PRODUCT_IMPORT'),
        ('ADMIN', 'SALE_READ'),
        ('ADMIN', 'SALE_CREATE'),
        ('ADMIN', 'SALE_CANCEL'),
        ('ADMIN', 'SALE_REFUND'),
        ('ADMIN', 'SALE_EXPORT'),
        ('ADMIN', 'SALE_PRINT'),
        ('ADMIN', 'PURCHASE_READ'),
        ('ADMIN', 'PURCHASE_CREATE'),
        ('ADMIN', 'PURCHASE_UPDATE'),
        ('ADMIN', 'PURCHASE_CANCEL'),
        ('ADMIN', 'PURCHASE_EXPORT'),
        ('ADMIN', 'PURCHASE_PRINT'),
        ('ADMIN', 'INVENTORY_READ'),
        ('ADMIN', 'INVENTORY_ADJUST'),
        ('ADMIN', 'INVENTORY_TRANSFER'),
        ('ADMIN', 'INVENTORY_EXPORT'),
        ('ADMIN', 'REPORT_READ'),
        ('ADMIN', 'REPORT_EXPORT'),
        ('ADMIN', 'REPORT_PRINT'),
        ('ADMIN', 'SETTINGS_READ'),
        ('ADMIN', 'SETTINGS_UPDATE'),
        ('ADMIN', 'SETTINGS_MANAGE'),

        -- MANAGER: operational product, commerce, inventory, and reporting access.
        ('MANAGER', 'PRODUCT_READ'),
        ('MANAGER', 'PRODUCT_CREATE'),
        ('MANAGER', 'PRODUCT_UPDATE'),
        ('MANAGER', 'PRODUCT_DELETE'),
        ('MANAGER', 'PRODUCT_EXPORT'),
        ('MANAGER', 'PRODUCT_IMPORT'),
        ('MANAGER', 'CUSTOMER_READ'),
        ('MANAGER', 'CUSTOMER_CREATE'),
        ('MANAGER', 'CUSTOMER_UPDATE'),
        ('MANAGER', 'CUSTOMER_DELETE'),
        ('MANAGER', 'CUSTOMER_EXPORT'),
        ('MANAGER', 'SUPPLIER_READ'),
        ('MANAGER', 'SUPPLIER_CREATE'),
        ('MANAGER', 'SUPPLIER_UPDATE'),
        ('MANAGER', 'SUPPLIER_DELETE'),
        ('MANAGER', 'SALE_READ'),
        ('MANAGER', 'SALE_CREATE'),
        ('MANAGER', 'SALE_CANCEL'),
        ('MANAGER', 'SALE_REFUND'),
        ('MANAGER', 'SALE_EXPORT'),
        ('MANAGER', 'SALE_PRINT'),
        ('MANAGER', 'PURCHASE_READ'),
        ('MANAGER', 'PURCHASE_CREATE'),
        ('MANAGER', 'PURCHASE_UPDATE'),
        ('MANAGER', 'PURCHASE_CANCEL'),
        ('MANAGER', 'PURCHASE_EXPORT'),
        ('MANAGER', 'PURCHASE_PRINT'),
        ('MANAGER', 'INVENTORY_READ'),
        ('MANAGER', 'INVENTORY_ADJUST'),
        ('MANAGER', 'INVENTORY_TRANSFER'),
        ('MANAGER', 'INVENTORY_EXPORT'),
        ('MANAGER', 'REPORT_READ'),
        ('MANAGER', 'REPORT_EXPORT'),
        ('MANAGER', 'REPORT_PRINT'),

        -- CASHIER: point-of-sale access only.
        ('CASHIER', 'DASHBOARD_READ'),
        ('CASHIER', 'PRODUCT_READ'),
        ('CASHIER', 'CUSTOMER_READ'),
        ('CASHIER', 'CUSTOMER_CREATE'),
        ('CASHIER', 'CUSTOMER_UPDATE'),
        ('CASHIER', 'SALE_READ'),
        ('CASHIER', 'SALE_CREATE'),
        ('CASHIER', 'SALE_PRINT'),
        ('CASHIER', 'SALE_RETURN_READ'),
        ('CASHIER', 'SALE_RETURN_CREATE'),

        -- INVENTORY_MANAGER: stock, supplier, category, and purchasing access.
        ('INVENTORY_MANAGER', 'PRODUCT_READ'),
        ('INVENTORY_MANAGER', 'PRODUCT_CREATE'),
        ('INVENTORY_MANAGER', 'PRODUCT_UPDATE'),
        ('INVENTORY_MANAGER', 'CATEGORY_READ'),
        ('INVENTORY_MANAGER', 'CATEGORY_CREATE'),
        ('INVENTORY_MANAGER', 'CATEGORY_UPDATE'),
        ('INVENTORY_MANAGER', 'SUPPLIER_READ'),
        ('INVENTORY_MANAGER', 'SUPPLIER_CREATE'),
        ('INVENTORY_MANAGER', 'SUPPLIER_UPDATE'),
        ('INVENTORY_MANAGER', 'INVENTORY_READ'),
        ('INVENTORY_MANAGER', 'INVENTORY_ADJUST'),
        ('INVENTORY_MANAGER', 'INVENTORY_TRANSFER'),
        ('INVENTORY_MANAGER', 'PURCHASE_READ'),
        ('INVENTORY_MANAGER', 'PURCHASE_CREATE'),
        ('INVENTORY_MANAGER', 'PURCHASE_UPDATE'),
        ('INVENTORY_MANAGER', 'PURCHASE_RETURN_READ'),
        ('INVENTORY_MANAGER', 'PURCHASE_RETURN_CREATE'),

        -- ACCOUNTANT: financial visibility, expenses, payments, and reports.
        ('ACCOUNTANT', 'DASHBOARD_READ'),
        ('ACCOUNTANT', 'CUSTOMER_READ'),
        ('ACCOUNTANT', 'SUPPLIER_READ'),
        ('ACCOUNTANT', 'SALE_READ'),
        ('ACCOUNTANT', 'SALE_EXPORT'),
        ('ACCOUNTANT', 'PURCHASE_READ'),
        ('ACCOUNTANT', 'PURCHASE_EXPORT'),
        ('ACCOUNTANT', 'EXPENSE_READ'),
        ('ACCOUNTANT', 'EXPENSE_CREATE'),
        ('ACCOUNTANT', 'EXPENSE_UPDATE'),
        ('ACCOUNTANT', 'EXPENSE_APPROVE'),
        ('ACCOUNTANT', 'PAYMENT_READ'),
        ('ACCOUNTANT', 'PAYMENT_CREATE'),
        ('ACCOUNTANT', 'PAYMENT_UPDATE'),
        ('ACCOUNTANT', 'REPORT_READ'),
        ('ACCOUNTANT', 'REPORT_EXPORT'),
        ('ACCOUNTANT', 'REPORT_PRINT')
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
