-- Seed initial global resources
-- Migration: V25
-- Description: Seeds the platform-level resources used to organize permissions.
-- Existing resources with the same resource_code are left unchanged.

INSERT INTO resources (
    resource_code,
    resource_name,
    status,
    is_system,
    created_at,
    version
) VALUES
    ('DASHBOARD', 'Dashboard', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PRODUCT', 'Product', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CATEGORY', 'Category', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('BRAND', 'Brand', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('UNIT', 'Unit', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CUSTOMER', 'Customer', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SUPPLIER', 'Supplier', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALE', 'Sale', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALE_RETURN', 'Sale Return', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE', 'Purchase', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE_RETURN', 'Purchase Return', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('INVENTORY', 'Inventory', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('STOCK_TRANSFER', 'Stock Transfer', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('EXPENSE', 'Expense', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PAYMENT', 'Payment', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('USER', 'User', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ROLE', 'Role', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('BRANCH', 'Branch', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('REPORT', 'Report', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('TAX', 'Tax', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SETTINGS', 'Settings', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('AUDIT_LOG', 'Audit Log', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (resource_code) DO NOTHING;
