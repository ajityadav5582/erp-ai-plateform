-- =============================================================================
-- ERP AI Platform - Cleanup Dummy Data Script
-- =============================================================================
-- This script removes all dummy data created by the seed script.
-- Use this if you need a clean slate before re-running seed.sql
--
-- Run with: psql -h localhost -p 5432 -U erpai -d erpai_platform -f infrastructure/postgres/seed/cleanup-dummy-data.sql
-- =============================================================================

-- Disable triggers temporarily for faster cleanup
SET session_replication_role = replica;

-- Delete in reverse order of dependencies
DELETE FROM items WHERE company_id IN (1, 2, 3, 4);
DELETE FROM categories WHERE company_id IN (1, 2, 3, 4);
DELETE FROM units WHERE company_id IN (1, 2, 3, 4);
DELETE FROM company_fiscal_years WHERE company_id IN (1, 2, 3, 4);
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE tenant_id IN (1, 2));
DELETE FROM users WHERE tenant_id IN (1, 2);
DELETE FROM roles WHERE tenant_id IN (1, 2);
DELETE FROM companies WHERE tenant_id IN (1, 2);
DELETE FROM tenants WHERE id IN (1, 2);

-- Re-enable triggers
SET session_replication_role = DEFAULT;

-- Show remaining counts
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
    RAISE NOTICE '  Cleanup Complete';
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
    RAISE NOTICE '  Master data (fiscal years, permissions, etc.) preserved';
    RAISE NOTICE '==========================================';
END $$;
