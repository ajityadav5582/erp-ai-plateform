-- Migration: V40
-- Description: Extends the actions_category_check constraint to include
-- FINANCIAL and EXECUTION categories added by the ActionCategory enum.
-- Also backfills category/display_order for any existing action rows so the
-- database stays in sync with the compile-time Action enum metadata.

-- ============================================================
-- 1. Drop old category check constraint
-- ============================================================
ALTER TABLE actions DROP CONSTRAINT IF EXISTS actions_category_check;

-- ============================================================
-- 2. Add extended category check constraint (idempotent)
-- ============================================================
ALTER TABLE actions
    ADD CONSTRAINT actions_category_check
    CHECK (category IN ('CRUD', 'WORKFLOW', 'FINANCIAL', 'DATA_IO', 'ADMIN', 'EXECUTION'));

-- ============================================================
-- 3. Backfill categories & display orders for existing action rows
-- ============================================================
UPDATE actions SET category = 'CRUD', display_order = 10 WHERE action_code = 'CREATE';
UPDATE actions SET category = 'CRUD', display_order = 20 WHERE action_code = 'VIEW';
UPDATE actions SET category = 'CRUD', display_order = 30 WHERE action_code = 'UPDATE';
UPDATE actions SET category = 'CRUD', display_order = 40 WHERE action_code = 'DELETE';
UPDATE actions SET category = 'CRUD', display_order = 45 WHERE action_code = 'LIST';
UPDATE actions SET category = 'WORKFLOW', display_order = 48 WHERE action_code = 'SUBMIT';
UPDATE actions SET category = 'WORKFLOW', display_order = 50 WHERE action_code = 'APPROVE';
UPDATE actions SET category = 'WORKFLOW', display_order = 60 WHERE action_code = 'REJECT';
UPDATE actions SET category = 'WORKFLOW', display_order = 65 WHERE action_code = 'CANCEL';
UPDATE actions SET category = 'WORKFLOW', display_order = 67 WHERE action_code = 'CLOSE';
UPDATE actions SET category = 'WORKFLOW', display_order = 68 WHERE action_code = 'REOPEN';
UPDATE actions SET category = 'WORKFLOW', display_order = 69 WHERE action_code = 'VOID';
UPDATE actions SET category = 'FINANCIAL', display_order = 70 WHERE action_code = 'POST';
UPDATE actions SET category = 'FINANCIAL', display_order = 75 WHERE action_code = 'RECONCILE';
UPDATE actions SET category = 'DATA_IO', display_order = 80 WHERE action_code = 'EXPORT';
UPDATE actions SET category = 'DATA_IO', display_order = 85 WHERE action_code = 'IMPORT';
UPDATE actions SET category = 'DATA_IO', display_order = 88 WHERE action_code = 'PRINT';
UPDATE actions SET category = 'DATA_IO', display_order = 90 WHERE action_code = 'REPORT';
UPDATE actions SET category = 'ADMIN', display_order = 95 WHERE action_code = 'MANAGE';
UPDATE actions SET category = 'ADMIN', display_order = 100 WHERE action_code = 'ASSIGN';
UPDATE actions SET category = 'ADMIN', display_order = 105 WHERE action_code = 'LOCK';
UPDATE actions SET category = 'ADMIN', display_order = 110 WHERE action_code = 'UNLOCK';
UPDATE actions SET category = 'ADMIN', display_order = 115 WHERE action_code = 'AUDIT';
UPDATE actions SET category = 'EXECUTION', display_order = 120 WHERE action_code = 'EXECUTE';
