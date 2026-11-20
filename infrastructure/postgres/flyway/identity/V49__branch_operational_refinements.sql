-- Migration: V49
-- Description: Add operational refinements to branches and user_branches tables.
-- - is_head_office, default_warehouse_id, manager_user_id on branches
-- - Single head office constraint per company
-- - company_id on user_branches with backfill and FK
-- - Company-aware primary branch unique index

-- ============================================================
-- 1. Drop redundant indexes and constraints on branches
-- ============================================================

-- Drop unique constraints scoped to company (from V44)
DROP INDEX IF EXISTS uk_branches_company_branch_code;
DROP INDEX IF EXISTS uk_branches_company_branch_name;

-- Drop tenant-scoped indexes (idempotent if already removed)
DROP INDEX IF EXISTS idx_branches_tenant_id;
DROP INDEX IF EXISTS idx_branches_tenant_company;
DROP INDEX IF EXISTS idx_branches_tenant_status;
DROP INDEX IF EXISTS idx_branches_branch_code;
DROP INDEX IF EXISTS idx_branches_branch_name;
DROP INDEX IF EXISTS idx_branches_status;

-- Drop redundant length validation constraints from initial schema
ALTER TABLE branches DROP CONSTRAINT IF EXISTS chk_branches_branch_code_length;
ALTER TABLE branches DROP CONSTRAINT IF EXISTS chk_branches_branch_name_length;

-- ============================================================
-- 2. Add new operational columns to branches
-- ============================================================

-- is_head_office: marks the head office branch per company
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'branches' AND column_name = 'is_head_office'
    ) THEN
        ALTER TABLE branches ADD COLUMN is_head_office BOOLEAN NOT NULL DEFAULT false;
    END IF;
END $$;

-- default_warehouse_id: references the default warehouse for this branch
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'branches' AND column_name = 'default_warehouse_id'
    ) THEN
        ALTER TABLE branches ADD COLUMN default_warehouse_id BIGINT;
    END IF;
END $$;

-- manager_user_id: references the branch manager user
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'branches' AND column_name = 'manager_user_id'
    ) THEN
        ALTER TABLE branches ADD COLUMN manager_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- ============================================================
-- 3. Create unique index for single head office per company
-- ============================================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_branches_single_head_office_per_company
    ON branches(company_id) WHERE is_head_office = true;

-- ============================================================
-- 4. Update user_branches table
-- ============================================================

-- Drop old unique constraint on user_branches (tenant_id, user_id, branch_id)
ALTER TABLE user_branches DROP CONSTRAINT IF EXISTS uq_user_branch_access;

-- Add company_id column (nullable first for safe backfill)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_branches' AND column_name = 'company_id'
    ) THEN
        ALTER TABLE user_branches ADD COLUMN company_id BIGINT;
    END IF;
END $$;

-- Backfill company_id from branches table
UPDATE user_branches ub
SET company_id = b.company_id
FROM branches b
WHERE ub.branch_id = b.id
  AND ub.company_id IS NULL;

-- Enforce NOT NULL after backfill (assumes all rows assigned)
DO $$
BEGIN
    PERFORM 1 FROM user_branches WHERE company_id IS NULL LIMIT 1;
    IF NOT FOUND THEN
        ALTER TABLE user_branches ALTER COLUMN company_id SET NOT NULL;
    END IF;
END $$;

-- Add FK to companies
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'user_branches' AND constraint_name = 'fk_user_branches_company_id_companies'
    ) THEN
        ALTER TABLE user_branches
            ADD CONSTRAINT fk_user_branches_company_id_companies
            FOREIGN KEY (company_id) REFERENCES companies(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;
END $$;

-- ============================================================
-- 5. Replace primary unique constraint with company-aware version
-- ============================================================

-- Drop old primary-per-user unique index
DROP INDEX IF EXISTS uk_user_branches_primary_per_user;

-- Create new unique index with company_id included and partial filter
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_branches_primary_per_user
    ON user_branches(tenant_id, company_id, user_id)
    WHERE is_primary = true;