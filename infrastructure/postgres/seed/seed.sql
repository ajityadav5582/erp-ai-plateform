-- =============================================================================
-- ERP AI Platform - Dummy Data Seed Script
-- =============================================================================
-- This script seeds sample dummy data for development and testing.
-- It is idempotent: running it multiple times will not create duplicate data.
--
-- Run with: psql -h localhost -p 5432 -U erpai -d erpai_platform -f infrastructure/postgres/seed/seed.sql
-- Or via: ./scripts/docker/database-reset.sh --seed
-- =============================================================================

-- =============================================================================
-- 1. PROVINCES (required by DataInitializer)
-- =============================================================================
INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P1', 'Koshi', 'कोशी', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P1');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P2', 'Madhesh', 'मधेश', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P2');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P3', 'Bagmati', 'बागमती', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P3');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P4', 'Gandaki', 'गण्डकी', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P4');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P5', 'Lumbini', 'लुम्बिनी', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P5');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P6', 'Karnali', 'कर्णाली', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P6');

INSERT INTO provinces (province_code, province_name, nepali_name, country_code, status, version, created_at, created_by, updated_at, updated_by)
SELECT 'P7', 'Sudurpashchim', 'सुदूरपश्चिम', 'NPL', 'ACTIVE', 1, NOW(), 'seed', NOW(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM provinces WHERE province_code = 'P7');

-- =============================================================================
-- 2. PERMISSIONS (required by DataInitializer)
-- =============================================================================
INSERT INTO permissions (code, name, resource, action, description, status, created_at, updated_at)
VALUES
    -- User permissions
    ('USER_VIEW', 'USER VIEW', 'USER', 'VIEW', 'user view permission', 'ACTIVE', NOW(), NOW()),
    ('USER_CREATE', 'USER CREATE', 'USER', 'CREATE', 'user create permission', 'ACTIVE', NOW(), NOW()),
    ('USER_UPDATE', 'USER UPDATE', 'USER', 'UPDATE', 'user update permission', 'ACTIVE', NOW(), NOW()),
    ('USER_DEACTIVATE', 'USER DEACTIVATE', 'USER', 'DEACTIVATE', 'user deactivate permission', 'ACTIVE', NOW(), NOW()),

    -- Role permissions
    ('ROLE_VIEW', 'ROLE VIEW', 'ROLE', 'VIEW', 'role view permission', 'ACTIVE', NOW(), NOW()),
    ('ROLE_CREATE', 'ROLE CREATE', 'ROLE', 'CREATE', 'role create permission', 'ACTIVE', NOW(), NOW()),
    ('ROLE_UPDATE', 'ROLE UPDATE', 'ROLE', 'UPDATE', 'role update permission', 'ACTIVE', NOW(), NOW()),
    ('ROLE_DEACTIVATE', 'ROLE DEACTIVATE', 'ROLE', 'DEACTIVATE', 'role deactivate permission', 'ACTIVE', NOW(), NOW()),

    -- Company permissions
    ('COMPANY_VIEW', 'COMPANY VIEW', 'COMPANY', 'VIEW', 'company view permission', 'ACTIVE', NOW(), NOW()),
    ('COMPANY_CREATE', 'COMPANY CREATE', 'COMPANY', 'CREATE', 'company create permission', 'ACTIVE', NOW(), NOW()),
    ('COMPANY_UPDATE', 'COMPANY UPDATE', 'COMPANY', 'UPDATE', 'company update permission', 'ACTIVE', NOW(), NOW()),

    -- Product permissions
    ('PRODUCT_VIEW', 'PRODUCT VIEW', 'PRODUCT', 'VIEW', 'product view permission', 'ACTIVE', NOW(), NOW()),
    ('PRODUCT_CREATE', 'PRODUCT CREATE', 'PRODUCT', 'CREATE', 'product create permission', 'ACTIVE', NOW(), NOW()),
    ('PRODUCT_UPDATE', 'PRODUCT UPDATE', 'PRODUCT', 'UPDATE', 'product update permission', 'ACTIVE', NOW(), NOW()),

    -- Purchase Requisition permissions
    ('PR_VIEW', 'PR VIEW', 'PR', 'VIEW', 'pr view permission', 'ACTIVE', NOW(), NOW()),
    ('PR_CREATE', 'PR CREATE', 'PR', 'CREATE', 'pr create permission', 'ACTIVE', NOW(), NOW()),
    ('PR_EDIT', 'PR EDIT', 'PR', 'EDIT', 'pr edit permission', 'ACTIVE', NOW(), NOW()),
    ('PR_SUBMIT', 'PR SUBMIT', 'PR', 'SUBMIT', 'pr submit permission', 'ACTIVE', NOW(), NOW()),
    ('PR_APPROVE', 'PR APPROVE', 'PR', 'APPROVE', 'pr approve permission', 'ACTIVE', NOW(), NOW()),
    ('PR_REJECT', 'PR REJECT', 'PR', 'REJECT', 'pr reject permission', 'ACTIVE', NOW(), NOW()),
    ('PR_CANCEL', 'PR CANCEL', 'PR', 'CANCEL', 'pr cancel permission', 'ACTIVE', NOW(), NOW()),
    ('PR_CONVERT_TO_PO', 'PR CONVERT_TO_PO', 'PR', 'CONVERT_TO_PO', 'pr convert to po permission', 'ACTIVE', NOW(), NOW()),
    ('PR_DELETE', 'PR DELETE', 'PR', 'DELETE', 'pr delete permission', 'ACTIVE', NOW(), NOW()),
    ('PR_ADMIN', 'PR ADMIN', 'PR', 'ADMIN', 'pr admin permission', 'ACTIVE', NOW(), NOW()),

    -- Purchase Order permissions
    ('PURCHASE_ORDER_VIEW', 'PURCHASE_ORDER VIEW', 'PURCHASE_ORDER', 'VIEW', 'purchase order view permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_ORDER_CREATE', 'PURCHASE_ORDER CREATE', 'PURCHASE_ORDER', 'CREATE', 'purchase order create permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_ORDER_APPROVE', 'PURCHASE_ORDER APPROVE', 'PURCHASE_ORDER', 'APPROVE', 'purchase order approve permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_ORDER_RECEIVE', 'PURCHASE_ORDER RECEIVE', 'PURCHASE_ORDER', 'RECEIVE', 'purchase order receive permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_ORDER_CANCEL', 'PURCHASE_ORDER CANCEL', 'PURCHASE_ORDER', 'CANCEL', 'purchase order cancel permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_ORDER_DELETE', 'PURCHASE_ORDER DELETE', 'PURCHASE_ORDER', 'DELETE', 'purchase order delete permission', 'ACTIVE', NOW(), NOW()),

    -- Goods Receipt permissions
    ('GOODS_RECEIPT_VIEW', 'GOODS_RECEIPT VIEW', 'GOODS_RECEIPT', 'VIEW', 'goods receipt view permission', 'ACTIVE', NOW(), NOW()),
    ('GOODS_RECEIPT_CREATE', 'GOODS_RECEIPT CREATE', 'GOODS_RECEIPT', 'CREATE', 'goods receipt create permission', 'ACTIVE', NOW(), NOW()),
    ('GOODS_RECEIPT_CANCEL', 'GOODS_RECEIPT CANCEL', 'GOODS_RECEIPT', 'CANCEL', 'goods receipt cancel permission', 'ACTIVE', NOW(), NOW()),

    -- Purchase Invoice permissions
    ('PURCHASE_INVOICE_VIEW', 'PURCHASE_INVOICE VIEW', 'PURCHASE_INVOICE', 'VIEW', 'purchase invoice view permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_INVOICE_CREATE', 'PURCHASE_INVOICE CREATE', 'PURCHASE_INVOICE', 'CREATE', 'purchase invoice create permission', 'ACTIVE', NOW(), NOW()),
    ('PURCHASE_INVOICE_CANCEL', 'PURCHASE_INVOICE CANCEL', 'PURCHASE_INVOICE', 'CANCEL', 'purchase invoice cancel permission', 'ACTIVE', NOW(), NOW()),

    -- Supplier Payment permissions
    ('SUPPLIER_PAYMENT_VIEW', 'SUPPLIER_PAYMENT VIEW', 'SUPPLIER_PAYMENT', 'VIEW', 'supplier payment view permission', 'ACTIVE', NOW(), NOW()),
    ('SUPPLIER_PAYMENT_CREATE', 'SUPPLIER_PAYMENT CREATE', 'SUPPLIER_PAYMENT', 'CREATE', 'supplier payment create permission', 'ACTIVE', NOW(), NOW()),
    ('SUPPLIER_PAYMENT_CANCEL', 'SUPPLIER_PAYMENT CANCEL', 'SUPPLIER_PAYMENT', 'CANCEL', 'supplier payment cancel permission', 'ACTIVE', NOW(), NOW()),

    -- Sales permissions
    ('SALES_VIEW', 'SALES VIEW', 'SALES', 'VIEW', 'sales view permission', 'ACTIVE', NOW(), NOW()),
    ('SALES_CREATE', 'SALES CREATE', 'SALES', 'CREATE', 'sales create permission', 'ACTIVE', NOW(), NOW()),
    ('SALES_APPROVE', 'SALES APPROVE', 'SALES', 'APPROVE', 'sales approve permission', 'ACTIVE', NOW(), NOW()),

    -- Inventory permissions
    ('INVENTORY_VIEW', 'INVENTORY VIEW', 'INVENTORY', 'VIEW', 'inventory view permission', 'ACTIVE', NOW(), NOW()),
    ('INVENTORY_ADJUST', 'INVENTORY ADJUST', 'INVENTORY', 'ADJUST', 'inventory adjust permission', 'ACTIVE', NOW(), NOW()),

    -- Product Category permissions
    ('PRODUCT_CATEGORY_VIEW', 'PRODUCT_CATEGORY VIEW', 'PRODUCT_CATEGORY', 'VIEW', 'product category view permission', 'ACTIVE', NOW(), NOW()),

    -- Warehouse permissions
    ('WAREHOUSE_VIEW', 'WAREHOUSE VIEW', 'WAREHOUSE', 'VIEW', 'warehouse view permission', 'ACTIVE', NOW(), NOW()),

    -- Vendor permissions
    ('VENDOR_VIEW', 'VENDOR VIEW', 'VENDOR', 'VIEW', 'vendor view permission', 'ACTIVE', NOW(), NOW()),

    -- Branch permissions
    ('BRANCH_VIEW', 'BRANCH VIEW', 'BRANCH', 'VIEW', 'branch view permission', 'ACTIVE', NOW(), NOW()),
    ('BRANCH_CREATE', 'BRANCH CREATE', 'BRANCH', 'CREATE', 'branch create permission', 'ACTIVE', NOW(), NOW()),
    ('BRANCH_UPDATE', 'BRANCH UPDATE', 'BRANCH', 'UPDATE', 'branch update permission', 'ACTIVE', NOW(), NOW()),
    ('BRANCH_DELETE', 'BRANCH DELETE', 'BRANCH', 'DELETE', 'branch delete permission', 'ACTIVE', NOW(), NOW()),

    -- Geography permissions
    ('PROVINCE_VIEW', 'PROVINCE VIEW', 'PROVINCE', 'VIEW', 'province view permission', 'ACTIVE', NOW(), NOW()),
    ('DISTRICT_VIEW', 'DISTRICT VIEW', 'DISTRICT', 'VIEW', 'district view permission', 'ACTIVE', NOW(), NOW()),
    ('LOCAL_LEVEL_VIEW', 'LOCAL_LEVEL VIEW', 'LOCAL_LEVEL', 'VIEW', 'local level view permission', 'ACTIVE', NOW(), NOW()),

    -- Department permissions
    ('DEPARTMENT_VIEW', 'DEPARTMENT VIEW', 'DEPARTMENT', 'VIEW', 'department view permission', 'ACTIVE', NOW(), NOW()),

    -- Permission permissions
    ('PERMISSION_VIEW', 'PERMISSION VIEW', 'PERMISSION', 'VIEW', 'permission view permission', 'ACTIVE', NOW(), NOW()),

    -- Accounting permissions
    ('ACCOUNTING_VIEW', 'ACCOUNTING VIEW', 'ACCOUNTING', 'VIEW', 'accounting view permission', 'ACTIVE', NOW(), NOW()),

    -- HR permissions
    ('HR_VIEW', 'HR VIEW', 'HR', 'VIEW', 'hr view permission', 'ACTIVE', NOW(), NOW()),

    -- Manufacturing permissions
    ('MANUFACTURING_VIEW', 'MANUFACTURING VIEW', 'MANUFACTURING', 'VIEW', 'manufacturing view permission', 'ACTIVE', NOW(), NOW()),

    -- Reporting permissions
    ('REPORTING_VIEW', 'REPORTING VIEW', 'REPORTING', 'VIEW', 'reporting view permission', 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 3. SYSTEM ROLES (required by DataInitializer)
-- =============================================================================
INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT null, 'SUPER_ADMIN', 'Super Administrator', 'System super administrator with full platform access across all tenants', 'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'SUPER_ADMIN');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT null, 'TENANT_ADMIN', 'Tenant Administrator', 'Platform-managed administrator role assigned to a tenant''s first user', 'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_ADMIN');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT null, 'STOREKEEPER', 'Storekeeper', 'Records goods receipts against purchase orders', 'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'STOREKEEPER');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT null, 'ACCOUNTS_CLERK', 'Accounts Clerk', 'Records supplier invoices against received goods', 'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'ACCOUNTS_CLERK');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT null, 'TREASURY', 'Treasury', 'Releases supplier payments against booked invoices', 'SYSTEM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TREASURY');

-- =============================================================================
-- 4. ROLE PERMISSIONS (required by DataInitializer)
-- =============================================================================
-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active, data_scope)
SELECT r.id, p.id, 'seed', NOW(), true, 'ALL'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TENANT_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active, data_scope)
SELECT r.id, p.id, 'seed', NOW(), true, 'ALL'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'TENANT_ADMIN' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- STOREKEEPER gets goods receipt permissions
INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active, data_scope)
SELECT r.id, p.id, 'seed', NOW(), true, 'ALL'
FROM roles r
JOIN permissions p ON p.code IN ('GOODS_RECEIPT_VIEW', 'GOODS_RECEIPT_CREATE', 'GOODS_RECEIPT_CANCEL')
WHERE r.code = 'STOREKEEPER' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ACCOUNTS_CLERK gets purchase invoice permissions
INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active, data_scope)
SELECT r.id, p.id, 'seed', NOW(), true, 'ALL'
FROM roles r
JOIN permissions p ON p.code IN ('PURCHASE_INVOICE_VIEW', 'PURCHASE_INVOICE_CREATE', 'PURCHASE_INVOICE_CANCEL')
WHERE r.code = 'ACCOUNTS_CLERK' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TREASURY gets supplier payment permissions
INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active, data_scope)
SELECT r.id, p.id, 'seed', NOW(), true, 'ALL'
FROM roles r
JOIN permissions p ON p.code IN ('SUPPLIER_PAYMENT_VIEW', 'SUPPLIER_PAYMENT_CREATE', 'SUPPLIER_PAYMENT_CANCEL')
WHERE r.code = 'TREASURY' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =============================================================================
-- 5. MASTER FISCAL YEARS (required by DataInitializer)
-- =============================================================================
INSERT INTO master_fiscal_years (name, code, start_date_bs, end_date_bs, start_date_ad, end_date_ad, display_order, is_active)
VALUES
    ('FY 2078/79', '2078-79', '2078-04-01', '2079-03-32', '2021-07-16', '2022-07-16', 1, true),
    ('FY 2079/80', '2079-80', '2079-04-01', '2080-03-31', '2022-07-17', '2023-07-16', 2, true),
    ('FY 2080/81', '2080-81', '2080-04-01', '2081-03-31', '2023-07-17', '2024-07-15', 3, true),
    ('FY 2081/82', '2081-82', '2081-04-01', '2082-03-31', '2024-07-16', '2025-07-16', 4, true),
    ('FY 2082/83', '2082-83', '2082-04-01', '2083-03-31', '2025-07-17', '2026-07-16', 5, true),
    ('FY 2083/84', '2083-84', '2083-04-01', '2084-03-31', '2026-07-17', '2027-07-16', 6, true)
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 6. TENANTS
-- =============================================================================
INSERT INTO tenants (tenant_code, legal_name, display_name, email, phone, website, status, isolation_strategy, activated_at, timezone, currency, language, version, deactivated_at, favicon_url, logo_url, subscription_plan_id, suspended_at)
SELECT 'TECH-SOL-001', 'Tech Solutions Pvt. Ltd.', 'Tech Solutions', 'admin@techsolutions.com', '+9779812345678', 'https://techsolutions.com', 'ACTIVE', 'SCHEMA_PER_TENANT', NOW(), 'UTC', 'USD', 'en', 1, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE tenant_code = 'TECH-SOL-001');

INSERT INTO tenants (tenant_code, legal_name, display_name, email, phone, website, status, isolation_strategy, activated_at, timezone, currency, language, version, deactivated_at, favicon_url, logo_url, subscription_plan_id, suspended_at)
SELECT 'GLOB-TRAD-002', 'Global Trading Company', 'Global Trading', 'admin@globaltrading.com', '+9779812345679', 'https://globaltrading.com', 'ACTIVE', 'SCHEMA_PER_TENANT', NOW(), 'UTC', 'USD', 'en', 1, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE tenant_code = 'GLOB-TRAD-002');

-- =============================================================================
-- 7. COMPANIES
-- =============================================================================
INSERT INTO companies (company_id, tenant_id, company_code, company_name, pan_number, city, phone, email, company_type, fiscal_year_type, vat_registered, status, legal_name, version, website, address)
SELECT UUID_GENERATE_V4(), 1, 'TECH-KTM', 'Tech Solutions Kathmandu', '123456789', 'Kathmandu', '+9779812345678', 'ktm@techsolutions.com', 'PRIVATE_LIMITED', 'NEPALI_BS', true, 'ACTIVE', 'Tech Solutions Pvt. Ltd.', 1, 'https://techsolutions.com', 'Kathmandu, Nepal'
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_id = 1 AND company_code = 'TECH-KTM');

INSERT INTO companies (company_id, tenant_id, company_code, company_name, pan_number, city, phone, email, company_type, fiscal_year_type, vat_registered, status, legal_name, version, website, address)
SELECT UUID_GENERATE_V4(), 1, 'TECH-PKH', 'Tech Solutions Pokhara', '987654321', 'Pokhara', '+9779812345670', 'pkh@techsolutions.com', 'PRIVATE_LIMITED', 'NEPALI_BS', true, 'ACTIVE', 'Tech Solutions Pvt. Ltd.', 1, 'https://techsolutions.com', 'Pokhara, Nepal'
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_id = 1 AND company_code = 'TECH-PKH');

INSERT INTO companies (company_id, tenant_id, company_code, company_name, pan_number, city, phone, email, company_type, fiscal_year_type, vat_registered, status, legal_name, version, website, address)
SELECT UUID_GENERATE_V4(), 2, 'GLOB-KTM', 'Global Trading Kathmandu', '456789123', 'Kathmandu', '+9779812345680', 'ktm@globaltrading.com', 'PRIVATE_LIMITED', 'ENGLISH_AD', true, 'ACTIVE', 'Global Trading Company', 1, 'https://globaltrading.com', 'Kathmandu, Nepal'
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_id = 2 AND company_code = 'GLOB-KTM');

INSERT INTO companies (company_id, tenant_id, company_code, company_name, pan_number, city, phone, email, company_type, fiscal_year_type, vat_registered, status, legal_name, version, website, address)
SELECT UUID_GENERATE_V4(), 2, 'GLOB-BIR', 'Global Trading Biratnagar', '789123456', 'Biratnagar', '+9779812345681', 'bir@globaltrading.com', 'PRIVATE_LIMITED', 'ENGLISH_AD', true, 'ACTIVE', 'Global Trading Company', 1, 'https://globaltrading.com', 'Biratnagar, Nepal'
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_id = 2 AND company_code = 'GLOB-BIR');

-- =============================================================================
-- 8. USERS
-- =============================================================================
-- Password for all users: Password@123 (BCrypt hash)
INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'admin.ktm@techsolutions.com', 'admin.ktm@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Admin', 'KTM', '+9779812345678', 'Administrator', 'ACTIVE', 0, 1, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'admin.ktm@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'manager.ktm@techsolutions.com', 'manager.ktm@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Manager', 'KTM', '+9779812345671', 'Manager', 'ACTIVE', 0, 1, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'manager.ktm@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'accountant.ktm@techsolutions.com', 'accountant.ktm@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Accountant', 'KTM', '+9779812345672', 'Accountant', 'ACTIVE', 0, 1, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'accountant.ktm@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'storekeeper.ktm@techsolutions.com', 'storekeeper.ktm@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Storekeeper', 'KTM', '+9779812345673', 'Storekeeper', 'ACTIVE', 0, 1, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'storekeeper.ktm@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'admin.pkh@techsolutions.com', 'admin.pkh@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Admin', 'PKH', '+9779812345674', 'Administrator', 'ACTIVE', 0, 2, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'admin.pkh@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'manager.pkh@techsolutions.com', 'manager.pkh@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Manager', 'PKH', '+9779812345675', 'Manager', 'ACTIVE', 0, 2, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'manager.pkh@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'accountant.pkh@techsolutions.com', 'accountant.pkh@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Accountant', 'PKH', '+9779812345676', 'Accountant', 'ACTIVE', 0, 2, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'accountant.pkh@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 1, 'storekeeper.pkh@techsolutions.com', 'storekeeper.pkh@techsolutions.com', crypt('Password@123', gen_salt('bf', 10)), 'Storekeeper', 'PKH', '+9779812345677', 'Storekeeper', 'ACTIVE', 0, 2, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 1 AND email = 'storekeeper.pkh@techsolutions.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'admin.ktm@globaltrading.com', 'admin.ktm@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Admin', 'KTM', '+9779812345682', 'Administrator', 'ACTIVE', 0, 3, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'admin.ktm@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'manager.ktm@globaltrading.com', 'manager.ktm@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Manager', 'KTM', '+9779812345683', 'Manager', 'ACTIVE', 0, 3, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'manager.ktm@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'accountant.ktm@globaltrading.com', 'accountant.ktm@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Accountant', 'KTM', '+9779812345684', 'Accountant', 'ACTIVE', 0, 3, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'accountant.ktm@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'storekeeper.ktm@globaltrading.com', 'storekeeper.ktm@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Storekeeper', 'KTM', '+9779812345685', 'Storekeeper', 'ACTIVE', 0, 3, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'storekeeper.ktm@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'admin.bir@globaltrading.com', 'admin.bir@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Admin', 'BIR', '+9779812345686', 'Administrator', 'ACTIVE', 0, 4, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'admin.bir@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'manager.bir@globaltrading.com', 'manager.bir@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Manager', 'BIR', '+9779812345687', 'Manager', 'ACTIVE', 0, 4, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'manager.bir@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'accountant.bir@globaltrading.com', 'accountant.bir@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Accountant', 'BIR', '+9779812345688', 'Accountant', 'ACTIVE', 0, 4, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'accountant.bir@globaltrading.com');

INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, phone_number, job_title, status, failed_login_attempts, company_id, version, activated_at, branch_id, deactivated_at, department_id, last_login_at, lock_reason, locked_at, locked_until, profile_image_url)
SELECT 2, 'storekeeper.bir@globaltrading.com', 'storekeeper.bir@globaltrading.com', crypt('Password@123', gen_salt('bf', 10)), 'Storekeeper', 'BIR', '+9779812345689', 'Storekeeper', 'ACTIVE', 0, 4, 1, NOW(), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE tenant_id = 2 AND email = 'storekeeper.bir@globaltrading.com');

-- =============================================================================
-- 9. CUSTOM ROLES (per tenant)
-- =============================================================================
INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT 1, 'MANAGER', 'Manager', 'Manages day-to-day operations', 'CUSTOM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id = 1 AND code = 'MANAGER');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT 1, 'ACCOUNTANT', 'Accountant', 'Handles accounting and finance', 'CUSTOM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id = 1 AND code = 'ACCOUNTANT');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT 2, 'MANAGER', 'Manager', 'Manages day-to-day operations', 'CUSTOM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id = 2 AND code = 'MANAGER');

INSERT INTO roles (tenant_id, code, name, description, role_type, status)
SELECT 2, 'ACCOUNTANT', 'Accountant', 'Handles accounting and finance', 'CUSTOM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id = 2 AND code = 'ACCOUNTANT');

-- =============================================================================
-- 10. USER ROLES
-- =============================================================================
-- Assign custom roles to users
INSERT INTO user_roles (user_id, role_id, is_primary, assigned_by, assigned_at, active)
SELECT u.id, r.id, true, 'seed', NOW(), true
FROM users u
JOIN roles r ON r.tenant_id = u.tenant_id AND r.code = 'MANAGER'
WHERE u.email IN (
    'manager.ktm@techsolutions.com', 'manager.pkh@techsolutions.com',
    'manager.ktm@globaltrading.com', 'manager.bir@globaltrading.com'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id, is_primary, assigned_by, assigned_at, active)
SELECT u.id, r.id, true, 'seed', NOW(), true
FROM users u
JOIN roles r ON r.tenant_id = u.tenant_id AND r.code = 'ACCOUNTANT'
WHERE u.email IN (
    'accountant.ktm@techsolutions.com', 'accountant.pkh@techsolutions.com',
    'accountant.ktm@globaltrading.com', 'accountant.bir@globaltrading.com'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Assign STOREKEEPER system role (tenant_id = null)
INSERT INTO user_roles (user_id, role_id, is_primary, assigned_by, assigned_at, active)
SELECT u.id, r.id, true, 'seed', NOW(), true
FROM users u
JOIN roles r ON r.tenant_id IS NULL AND r.code = 'STOREKEEPER'
WHERE u.email IN (
    'storekeeper.ktm@techsolutions.com', 'storekeeper.pkh@techsolutions.com',
    'storekeeper.ktm@globaltrading.com', 'storekeeper.bir@globaltrading.com'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Assign TENANT_ADMIN system role (tenant_id = null) to admin users
INSERT INTO user_roles (user_id, role_id, is_primary, assigned_by, assigned_at, active)
SELECT u.id, r.id, true, 'seed', NOW(), true
FROM users u
JOIN roles r ON r.tenant_id IS NULL AND r.code = 'TENANT_ADMIN'
WHERE u.email IN (
    'admin.ktm@techsolutions.com', 'admin.pkh@techsolutions.com',
    'admin.ktm@globaltrading.com', 'admin.bir@globaltrading.com'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =============================================================================
-- 11. COMPANY FISCAL YEARS
-- =============================================================================
-- Link companies to master fiscal years
INSERT INTO company_fiscal_years (tenant_id, company_id, name, code, master_fiscal_year_id, start_date_bs, end_date_bs, start_date_ad, end_date_ad, start_date, end_date, calendar_type, is_active, created_at)
SELECT
    c.tenant_id,
    c.id,
    mf.name,
    mf.code,
    mf.id,
    mf.start_date_bs,
    mf.end_date_bs,
    mf.start_date_ad,
    mf.end_date_ad,
    CASE WHEN c.fiscal_year_type = 'NEPALI_BS' THEN mf.start_date_bs ELSE mf.start_date_ad::text END,
    CASE WHEN c.fiscal_year_type = 'NEPALI_BS' THEN mf.end_date_bs ELSE mf.end_date_ad::text END,
    c.fiscal_year_type,
    true,
    NOW()
FROM companies c
CROSS JOIN master_fiscal_years mf
WHERE mf.code IN ('2080-81', '2081-82', '2082-83')
  AND c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, code) DO NOTHING;

-- =============================================================================
-- 12. UNITS (per company) - using dynamic company IDs
-- =============================================================================
INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'PCS', 'Pieces', 'COUNT', 'pcs', 0, true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'PCS');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'KG', 'Kilogram', 'MASS', 'kg', 2, true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'KG');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'L', 'Litre', 'VOLUME', 'L', 2, true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'L');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'BOX', 'Box', 'COUNT', 'box', 0, true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'BOX');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'PCS', 'Pieces', 'COUNT', 'pcs', 0, true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'PCS');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'KG', 'Kilogram', 'MASS', 'kg', 2, true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'KG');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'L', 'Litre', 'VOLUME', 'L', 2, true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'L');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'BOX', 'Box', 'COUNT', 'box', 0, true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'BOX');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'PCS', 'Pieces', 'COUNT', 'pcs', 0, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'PCS');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'KG', 'Kilogram', 'MASS', 'kg', 2, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'KG');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'L', 'Litre', 'VOLUME', 'L', 2, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'L');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'BOX', 'Box', 'COUNT', 'box', 0, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'BOX');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'PCS', 'Pieces', 'COUNT', 'pcs', 0, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'PCS');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'KG', 'Kilogram', 'MASS', 'kg', 2, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'KG');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'L', 'Litre', 'VOLUME', 'L', 2, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'L');

INSERT INTO units (tenant_id, company_id, code, name, dimension, symbol, decimal_scale, is_active, version)
SELECT c.tenant_id, c.id, 'BOX', 'Box', 'COUNT', 'box', 0, true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM units WHERE company_id = c.id AND code = 'BOX');

-- =============================================================================
-- 13. CATEGORIES (per company) - using dynamic company IDs
-- =============================================================================
INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Electronics', 'electronics', 'Electronic devices and accessories', true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'electronics');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Clothing', 'clothing', 'Apparel and fashion items', true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'clothing');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Food & Beverages', 'food-beverages', 'Food items and drinks', true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'food-beverages');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Home & Garden', 'home-garden', 'Home improvement and garden supplies', true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'home-garden');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Office Supplies', 'office-supplies', 'Office stationery and supplies', true, 1
FROM companies c
WHERE c.company_code = 'TECH-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'office-supplies');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Electronics', 'electronics', 'Electronic devices and accessories', true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'electronics');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Clothing', 'clothing', 'Apparel and fashion items', true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'clothing');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Food & Beverages', 'food-beverages', 'Food items and drinks', true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'food-beverages');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Home & Garden', 'home-garden', 'Home improvement and garden supplies', true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'home-garden');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Office Supplies', 'office-supplies', 'Office stationery and supplies', true, 1
FROM companies c
WHERE c.company_code = 'TECH-PKH'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'office-supplies');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Electronics', 'electronics', 'Electronic devices and accessories', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'electronics');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Clothing', 'clothing', 'Apparel and fashion items', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'clothing');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Food & Beverages', 'food-beverages', 'Food items and drinks', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'food-beverages');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Home & Garden', 'home-garden', 'Home improvement and garden supplies', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'home-garden');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Office Supplies', 'office-supplies', 'Office stationery and supplies', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-KTM'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'office-supplies');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Electronics', 'electronics', 'Electronic devices and accessories', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'electronics');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Clothing', 'clothing', 'Apparel and fashion items', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'clothing');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Food & Beverages', 'food-beverages', 'Food items and drinks', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'food-beverages');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Home & Garden', 'home-garden', 'Home improvement and garden supplies', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'home-garden');

INSERT INTO categories (tenant_id, company_id, name, slug, description, is_active, version)
SELECT c.tenant_id, c.id, 'Office Supplies', 'office-supplies', 'Office stationery and supplies', true, 1
FROM companies c
WHERE c.company_code = 'GLOB-BIR'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE company_id = c.id AND slug = 'office-supplies');

-- =============================================================================
-- 14. ITEMS (per company, per category) - using dynamic company IDs
-- =============================================================================
INSERT INTO items (tenant_id, company_id, sku, name_en, category_id, uom_id, item_type, taxability_type, vat_rate, excise_type, selling_price, purchase_price, mrp, is_vat_inclusive, is_inventory_tracked, is_batch_tracked, is_expiry_tracked, is_active, version, barcode, description_en, description_np, excise_amount, excise_rate, hs_code, min_stock_level, name_np, reorder_level, reorder_quantity)
SELECT
    c.tenant_id,
    c.id,
    'ELEC-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    cat.id,
    u.id,
    'GOODS',
    'TAXABLE',
    13.00,
    'NONE',
    item_data.selling_price,
    item_data.purchase_price,
    item_data.mrp,
    false,
    true,
    false,
    false,
    true,
    1,
    'ELEC-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    NULL,
    0,
    0,
    NULL,
    0,
    NULL,
    0,
    0
FROM companies c
JOIN categories cat ON cat.company_id = c.id AND cat.slug = 'electronics'
JOIN units u ON u.company_id = c.id AND u.code = 'PCS'
CROSS JOIN (
    VALUES
        ('Laptop', 1000.00, 800.00, 1200.00),
        ('Smartphone', 800.00, 600.00, 1000.00),
        ('Tablet', 500.00, 350.00, 700.00)
) AS item_data(name_en, selling_price, purchase_price, mrp)
WHERE c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, sku) DO NOTHING;

INSERT INTO items (tenant_id, company_id, sku, name_en, category_id, uom_id, item_type, taxability_type, vat_rate, excise_type, selling_price, purchase_price, mrp, is_vat_inclusive, is_inventory_tracked, is_batch_tracked, is_expiry_tracked, is_active, version, barcode, description_en, description_np, excise_amount, excise_rate, hs_code, min_stock_level, name_np, reorder_level, reorder_quantity)
SELECT
    c.tenant_id,
    c.id,
    'CLTH-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    cat.id,
    u.id,
    'GOODS',
    'TAXABLE',
    13.00,
    'NONE',
    item_data.selling_price,
    item_data.purchase_price,
    item_data.mrp,
    false,
    true,
    false,
    false,
    true,
    1,
    'CLTH-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    NULL,
    0,
    0,
    NULL,
    0,
    NULL,
    0,
    0
FROM companies c
JOIN categories cat ON cat.company_id = c.id AND cat.slug = 'clothing'
JOIN units u ON u.company_id = c.id AND u.code = 'PCS'
CROSS JOIN (
    VALUES
        ('T-Shirt', 25.00, 15.00, 40.00),
        ('Jeans', 60.00, 40.00, 90.00),
        ('Jacket', 120.00, 80.00, 180.00)
) AS item_data(name_en, selling_price, purchase_price, mrp)
WHERE c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, sku) DO NOTHING;

INSERT INTO items (tenant_id, company_id, sku, name_en, category_id, uom_id, item_type, taxability_type, vat_rate, excise_type, selling_price, purchase_price, mrp, is_vat_inclusive, is_inventory_tracked, is_batch_tracked, is_expiry_tracked, is_active, version, barcode, description_en, description_np, excise_amount, excise_rate, hs_code, min_stock_level, name_np, reorder_level, reorder_quantity)
SELECT
    c.tenant_id,
    c.id,
    'FOOD-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    cat.id,
    u.id,
    'GOODS',
    'TAXABLE',
    13.00,
    'NONE',
    item_data.selling_price,
    item_data.purchase_price,
    item_data.mrp,
    false,
    true,
    false,
    false,
    true,
    1,
    'FOOD-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    NULL,
    0,
    0,
    NULL,
    0,
    NULL,
    0,
    0
FROM companies c
JOIN categories cat ON cat.company_id = c.id AND cat.slug = 'food-beverages'
JOIN units u ON u.company_id = c.id AND u.code = 'PCS'
CROSS JOIN (
    VALUES
        ('Rice 5kg', 10.00, 7.00, 15.00),
        ('Cooking Oil 1L', 3.50, 2.50, 5.00),
        ('Sugar 1kg', 2.00, 1.50, 3.00)
) AS item_data(name_en, selling_price, purchase_price, mrp)
WHERE c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, sku) DO NOTHING;

INSERT INTO items (tenant_id, company_id, sku, name_en, category_id, uom_id, item_type, taxability_type, vat_rate, excise_type, selling_price, purchase_price, mrp, is_vat_inclusive, is_inventory_tracked, is_batch_tracked, is_expiry_tracked, is_active, version, barcode, description_en, description_np, excise_amount, excise_rate, hs_code, min_stock_level, name_np, reorder_level, reorder_quantity)
SELECT
    c.tenant_id,
    c.id,
    'HOME-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    cat.id,
    u.id,
    'GOODS',
    'TAXABLE',
    13.00,
    'NONE',
    item_data.selling_price,
    item_data.purchase_price,
    item_data.mrp,
    false,
    true,
    false,
    false,
    true,
    1,
    'HOME-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    NULL,
    0,
    0,
    NULL,
    0,
    NULL,
    0,
    0
FROM companies c
JOIN categories cat ON cat.company_id = c.id AND cat.slug = 'home-garden'
JOIN units u ON u.company_id = c.id AND u.code = 'PCS'
CROSS JOIN (
    VALUES
        ('Garden Hose', 15.00, 10.00, 25.00),
        ('LED Bulb', 5.00, 3.00, 8.00),
        ('Door Mat', 12.00, 8.00, 20.00)
) AS item_data(name_en, selling_price, purchase_price, mrp)
WHERE c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, sku) DO NOTHING;

INSERT INTO items (tenant_id, company_id, sku, name_en, category_id, uom_id, item_type, taxability_type, vat_rate, excise_type, selling_price, purchase_price, mrp, is_vat_inclusive, is_inventory_tracked, is_batch_tracked, is_expiry_tracked, is_active, version, barcode, description_en, description_np, excise_amount, excise_rate, hs_code, min_stock_level, name_np, reorder_level, reorder_quantity)
SELECT
    c.tenant_id,
    c.id,
    'OFFC-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    cat.id,
    u.id,
    'GOODS',
    'TAXABLE',
    13.00,
    'NONE',
    item_data.selling_price,
    item_data.purchase_price,
    item_data.mrp,
    false,
    true,
    false,
    false,
    true,
    1,
    'OFFC-' || LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY item_data.name_en) AS TEXT), 3, '0'),
    item_data.name_en,
    NULL,
    0,
    0,
    NULL,
    0,
    NULL,
    0,
    0
FROM companies c
JOIN categories cat ON cat.company_id = c.id AND cat.slug = 'office-supplies'
JOIN units u ON u.company_id = c.id AND u.code = 'PCS'
CROSS JOIN (
    VALUES
        ('A4 Paper Pack', 8.00, 5.00, 12.00),
        ('Ballpoint Pen Box', 6.00, 4.00, 10.00),
        ('Stapler', 4.00, 2.50, 7.00)
) AS item_data(name_en, selling_price, purchase_price, mrp)
WHERE c.company_code IN ('TECH-KTM', 'TECH-PKH', 'GLOB-KTM', 'GLOB-BIR')
ON CONFLICT (company_id, sku) DO NOTHING;

-- =============================================================================
-- 15. SUMMARY
-- =============================================================================
DO $$
DECLARE
    tenant_count INTEGER;
    company_count INTEGER;
    user_count INTEGER;
    role_count INTEGER;
    fiscal_year_count INTEGER;
    unit_count INTEGER;
    category_count INTEGER;
    item_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO tenant_count FROM tenants;
    SELECT COUNT(*) INTO company_count FROM companies;
    SELECT COUNT(*) INTO user_count FROM users;
    SELECT COUNT(*) INTO role_count FROM roles;
    SELECT COUNT(*) INTO fiscal_year_count FROM company_fiscal_years;
    SELECT COUNT(*) INTO unit_count FROM units;
    SELECT COUNT(*) INTO category_count FROM categories;
    SELECT COUNT(*) INTO item_count FROM items;

    RAISE NOTICE '==========================================';
    RAISE NOTICE '  Dummy Data Seeding Complete';
    RAISE NOTICE '==========================================';
    RAISE NOTICE '  Tenants:            %', tenant_count;
    RAISE NOTICE '  Companies:          %', company_count;
    RAISE NOTICE '  Users:              %', user_count;
    RAISE NOTICE '  Roles:              %', role_count;
    RAISE NOTICE '  Company Fiscal Yrs: %', fiscal_year_count;
    RAISE NOTICE '  Units:              %', unit_count;
    RAISE NOTICE '  Categories:         %', category_count;
    RAISE NOTICE '  Items:              %', item_count;
    RAISE NOTICE '==========================================';
END $$;
