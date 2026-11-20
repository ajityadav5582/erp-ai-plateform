-- Seed global role templates
-- Migration: V30
-- Description: Seeds the platform-level role templates used when provisioning tenant roles.
-- Existing templates with the same template_code are left unchanged.

INSERT INTO role_templates (
    template_code,
    template_name,
    description,
    status,
    is_system,
    created_at,
    version
) VALUES
    ('OWNER', 'Owner', 'Platform owner with complete control over the ERP platform', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ADMIN', 'Admin', 'Administrator with full tenant management capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('MANAGER', 'Manager', 'Manager with operational oversight and approval capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CASHIER', 'Cashier', 'Cashier with point-of-sale transaction capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('INVENTORY_MANAGER', 'Inventory Manager', 'Inventory manager with stock and inventory management capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ACCOUNTANT', 'Accountant', 'Accountant with financial and accounting capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (template_code) DO NOTHING;
