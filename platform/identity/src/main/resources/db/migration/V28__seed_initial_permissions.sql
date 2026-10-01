-- Migration: V28
-- Description: Seeds initial permissions for all business resources.
-- Resolves resource_id and action_id by their codes (resource_code, action_code).
-- Idempotent: uses ON CONFLICT DO NOTHING on unique constraints.

-- ============================================================
-- 1. Ensure all required actions exist
-- ============================================================
INSERT INTO actions (action_code, action_name, status, is_system, created_at, version) VALUES
    ('CANCEL', 'Cancel', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('REFUND', 'Refund', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PRINT', 'Print', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ADJUST', 'Adjust', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('TRANSFER', 'Transfer', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (action_code) DO NOTHING;

-- ============================================================
-- 2. Seed initial permissions
-- ============================================================
-- Permission code format: {RESOURCE}_{ACTION} (e.g., PRODUCT_CREATE)
-- Uses CTE to resolve resource_id and action_id by their codes.
-- ON CONFLICT on uk_permissions_resource_action (resource_id, action_id) DO NOTHING
-- ensures idempotency.

WITH perm_data(resource_code, action_code, permission_name, description) AS (
    VALUES
        -- PRODUCT: VIEW, CREATE, UPDATE, DELETE, EXPORT, IMPORT
        ('PRODUCT', 'VIEW',    'View Products',        'Permission to view product information'),
        ('PRODUCT', 'CREATE',  'Create Products',      'Permission to create new products'),
        ('PRODUCT', 'UPDATE',  'Update Products',      'Permission to update existing products'),
        ('PRODUCT', 'DELETE',  'Delete Products',      'Permission to delete products'),
        ('PRODUCT', 'EXPORT',  'Export Products',      'Permission to export product data'),
        ('PRODUCT', 'IMPORT',  'Import Products',      'Permission to import product data'),

        -- CATEGORY: VIEW, CREATE, UPDATE, DELETE
        ('CATEGORY', 'VIEW',   'View Categories',      'Permission to view category information'),
        ('CATEGORY', 'CREATE', 'Create Categories',    'Permission to create new categories'),
        ('CATEGORY', 'UPDATE', 'Update Categories',    'Permission to update existing categories'),
        ('CATEGORY', 'DELETE', 'Delete Categories',    'Permission to delete categories'),

        -- CUSTOMER: VIEW, CREATE, UPDATE, DELETE, EXPORT
        ('CUSTOMER', 'VIEW',   'View Customers',       'Permission to view customer information'),
        ('CUSTOMER', 'CREATE', 'Create Customers',     'Permission to create new customers'),
        ('CUSTOMER', 'UPDATE', 'Update Customers',     'Permission to update existing customers'),
        ('CUSTOMER', 'DELETE', 'Delete Customers',     'Permission to delete customers'),
        ('CUSTOMER', 'EXPORT', 'Export Customers',     'Permission to export customer data'),

        -- SUPPLIER: VIEW, CREATE, UPDATE, DELETE
        ('SUPPLIER', 'VIEW',   'View Suppliers',       'Permission to view supplier information'),
        ('SUPPLIER', 'CREATE', 'Create Suppliers',     'Permission to create new suppliers'),
        ('SUPPLIER', 'UPDATE', 'Update Suppliers',     'Permission to update existing suppliers'),
        ('SUPPLIER', 'DELETE', 'Delete Suppliers',     'Permission to delete suppliers'),

        -- SALE: VIEW, CREATE, CANCEL, REFUND, EXPORT, PRINT
        ('SALE', 'VIEW',    'View Sales',        'Permission to view sale information'),
        ('SALE', 'CREATE',  'Create Sales',      'Permission to create new sales'),
        ('SALE', 'CANCEL',  'Cancel Sales',      'Permission to cancel sales'),
        ('SALE', 'REFUND',  'Refund Sales',      'Permission to process sale refunds'),
        ('SALE', 'EXPORT',  'Export Sales',      'Permission to export sale data'),
        ('SALE', 'PRINT',   'Print Sales',       'Permission to print sale documents'),

        -- SALE_RETURN: VIEW, CREATE, APPROVE
        ('SALE_RETURN', 'VIEW',    'View Sale Returns',     'Permission to view sale return information'),
        ('SALE_RETURN', 'CREATE',  'Create Sale Returns',   'Permission to create new sale returns'),
        ('SALE_RETURN', 'APPROVE', 'Approve Sale Returns',  'Permission to approve sale returns'),

        -- PURCHASE: VIEW, CREATE, UPDATE, CANCEL, EXPORT, PRINT
        ('PURCHASE', 'VIEW',    'View Purchases',      'Permission to view purchase information'),
        ('PURCHASE', 'CREATE',  'Create Purchases',    'Permission to create new purchases'),
        ('PURCHASE', 'UPDATE',  'Update Purchases',    'Permission to update existing purchases'),
        ('PURCHASE', 'CANCEL',  'Cancel Purchases',    'Permission to cancel purchases'),
        ('PURCHASE', 'EXPORT',  'Export Purchases',    'Permission to export purchase data'),
        ('PURCHASE', 'PRINT',   'Print Purchases',     'Permission to print purchase documents'),

        -- PURCHASE_RETURN: VIEW, CREATE, APPROVE
        ('PURCHASE_RETURN', 'VIEW',    'View Purchase Returns',     'Permission to view purchase return information'),
        ('PURCHASE_RETURN', 'CREATE',  'Create Purchase Returns',   'Permission to create new purchase returns'),
        ('PURCHASE_RETURN', 'APPROVE', 'Approve Purchase Returns',  'Permission to approve purchase returns'),

        -- INVENTORY: VIEW, ADJUST, TRANSFER, EXPORT
        ('INVENTORY', 'VIEW',     'View Inventory',      'Permission to view inventory information'),
        ('INVENTORY', 'ADJUST',   'Adjust Inventory',    'Permission to adjust inventory levels'),
        ('INVENTORY', 'TRANSFER', 'Transfer Inventory',  'Permission to transfer inventory between locations'),
        ('INVENTORY', 'EXPORT',   'Export Inventory',    'Permission to export inventory data'),

        -- USER: VIEW, CREATE, UPDATE, DELETE, ASSIGN
        ('USER', 'VIEW',   'View Users',      'Permission to view user information'),
        ('USER', 'CREATE', 'Create Users',    'Permission to create new users'),
        ('USER', 'UPDATE', 'Update Users',    'Permission to update existing users'),
        ('USER', 'DELETE', 'Delete Users',    'Permission to delete users'),
        ('USER', 'ASSIGN', 'Assign Users',    'Permission to assign users to roles/branches'),

        -- ROLE: VIEW, CREATE, UPDATE, DELETE, ASSIGN
        ('ROLE', 'VIEW',   'View Roles',      'Permission to view role information'),
        ('ROLE', 'CREATE', 'Create Roles',    'Permission to create new roles'),
        ('ROLE', 'UPDATE', 'Update Roles',    'Permission to update existing roles'),
        ('ROLE', 'DELETE', 'Delete Roles',    'Permission to delete roles'),
        ('ROLE', 'ASSIGN', 'Assign Roles',    'Permission to assign roles to users'),

        -- BRANCH: VIEW, CREATE, UPDATE, DELETE
        ('BRANCH', 'VIEW',   'View Branches',     'Permission to view branch information'),
        ('BRANCH', 'CREATE', 'Create Branches',   'Permission to create new branches'),
        ('BRANCH', 'UPDATE', 'Update Branches',   'Permission to update existing branches'),
        ('BRANCH', 'DELETE', 'Delete Branches',   'Permission to delete branches'),

        -- REPORT: VIEW, EXPORT, PRINT
        ('REPORT', 'VIEW',   'View Reports',     'Permission to view reports'),
        ('REPORT', 'EXPORT', 'Export Reports',   'Permission to export report data'),
        ('REPORT', 'PRINT',  'Print Reports',    'Permission to print reports'),

        -- EXPENSE: VIEW, CREATE, UPDATE, DELETE, APPROVE
        ('EXPENSE', 'VIEW',    'View Expenses',      'Permission to view expense information'),
        ('EXPENSE', 'CREATE',  'Create Expenses',    'Permission to create new expenses'),
        ('EXPENSE', 'UPDATE',  'Update Expenses',    'Permission to update existing expenses'),
        ('EXPENSE', 'DELETE',  'Delete Expenses',    'Permission to delete expenses'),
        ('EXPENSE', 'APPROVE', 'Approve Expenses',   'Permission to approve expenses'),

        -- PAYMENT: VIEW, CREATE, UPDATE, REFUND
        ('PAYMENT', 'VIEW',   'View Payments',     'Permission to view payment information'),
        ('PAYMENT', 'CREATE', 'Create Payments',   'Permission to create new payments'),
        ('PAYMENT', 'UPDATE', 'Update Payments',   'Permission to update existing payments'),
        ('PAYMENT', 'REFUND', 'Refund Payments',   'Permission to process payment refunds'),

        -- SETTINGS: VIEW, UPDATE, MANAGE
        ('SETTINGS', 'VIEW',   'View Settings',    'Permission to view system settings'),
        ('SETTINGS', 'UPDATE', 'Update Settings',  'Permission to update system settings'),
        ('SETTINGS', 'MANAGE', 'Manage Settings',  'Permission to manage system settings'),

        -- AUDIT_LOG: VIEW, EXPORT
        ('AUDIT_LOG', 'VIEW',   'View Audit Logs',    'Permission to view audit logs'),
        ('AUDIT_LOG', 'EXPORT', 'Export Audit Logs',  'Permission to export audit logs'),

        -- DASHBOARD: VIEW
        ('DASHBOARD', 'VIEW', 'View Dashboard', 'Permission to view dashboard')
)
INSERT INTO permissions (
    permission_code,
    resource_id,
    action_id,
    permission_name,
    description,
    status,
    is_system,
    created_at,
    version
)
SELECT
    pd.resource_code || '_' || pd.action_code AS permission_code,
    r.id AS resource_id,
    a.id AS action_id,
    pd.permission_name,
    pd.description,
    'ACTIVE' AS status,
    TRUE AS is_system,
    CURRENT_TIMESTAMP AS created_at,
    0 AS version
FROM perm_data pd
JOIN resources r ON r.resource_code = pd.resource_code
JOIN actions a ON a.action_code = pd.action_code
WHERE r.status = 'ACTIVE'
  AND a.status = 'ACTIVE'
ON CONFLICT (resource_id, action_id) DO NOTHING;
