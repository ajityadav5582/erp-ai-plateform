-- Migration: V44
-- Description: Scope branches to a company.
--
-- A tenant may own several companies, and every company runs its own branch
-- network. Without a company reference the branch list is scoped to the tenant
-- only, so switching the active company in the UI kept showing the previous
-- company's branches and branch codes/names were unique tenant-wide.
--
-- The company id is taken from the X-Company-Id header that the frontend sends
-- for the selected company. It is never accepted from the request body.

-- ============================================================
-- 1. Add company_id column (nullable first, for backfill)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'branches' AND column_name = 'company_id'
    ) THEN
        ALTER TABLE branches ADD COLUMN company_id BIGINT;
    END IF;
END $$;

-- ============================================================
-- 2. Backfill existing rows
-- ============================================================
-- Preference order per branch:
--   a. the tenant's first ACTIVE company (deterministic: lowest id);
--   b. the company of a user already assigned to that branch via user_branches;
--   c. the company of any user of the same tenant (last resort).
--
-- Branches whose tenant has no company at all stay NULL and are reported by
-- the final RAISE NOTICE so they can be fixed manually instead of being
-- silently attached to an unrelated company.
UPDATE branches b
SET company_id = c.company_id
FROM (
    SELECT tenant_id, MIN(id) AS company_id
    FROM companies
    GROUP BY tenant_id
) c
WHERE b.company_id IS NULL
  AND c.tenant_id = b.tenant_id;

UPDATE branches b
SET company_id = src.company_id
FROM (
    SELECT ub.branch_id, MIN(u.company_id) AS company_id
    FROM user_branches ub
    JOIN users u ON u.id = ub.user_id
    WHERE u.company_id IS NOT NULL
    GROUP BY ub.branch_id
) src
WHERE b.company_id IS NULL
  AND src.branch_id = b.id;

UPDATE branches b
SET company_id = src.company_id
FROM (
    SELECT tenant_id, MIN(company_id) AS company_id
    FROM users
    WHERE company_id IS NOT NULL
    GROUP BY tenant_id
) src
WHERE b.company_id IS NULL
  AND src.tenant_id = b.tenant_id;

DO $$
DECLARE
    unassigned BIGINT;
BEGIN
    SELECT count(*) INTO unassigned FROM branches WHERE company_id IS NULL;
    IF unassigned > 0 THEN
        RAISE NOTICE 'branches.company_id is NULL for % branch(es); their tenant has no company yet.', unassigned;
    END IF;
END $$;

-- ============================================================
-- 3. Enforce NOT NULL
-- ============================================================
-- Only applied when every row could be backfilled, otherwise the migration
-- would fail on a database whose tenants have no companies created yet.
DO $$
DECLARE
    unassigned BIGINT;
BEGIN
    SELECT count(*) INTO unassigned FROM branches WHERE company_id IS NULL;
    IF unassigned = 0 THEN
        ALTER TABLE branches ALTER COLUMN company_id SET NOT NULL;
    ELSE
        RAISE NOTICE 'Keeping branches.company_id nullable until the remaining % row(s) are assigned.', unassigned;
    END IF;
END $$;

-- ============================================================
-- 4. Foreign key to companies
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'branches' AND constraint_name = 'fk_branches_company_id_companies'
    ) THEN
        ALTER TABLE branches
            ADD CONSTRAINT fk_branches_company_id_companies
            FOREIGN KEY (company_id) REFERENCES companies(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;
END $$;

-- ============================================================
-- 5. Indexes for the company scoped queries
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_branches_company_id
    ON branches(company_id);

CREATE INDEX IF NOT EXISTS idx_branches_tenant_company
    ON branches(tenant_id, company_id);

CREATE INDEX IF NOT EXISTS idx_branches_tenant_company_status
    ON branches(tenant_id, company_id, status);

-- ============================================================
-- 6. Move uniqueness from tenant scope to company scope
-- ============================================================
-- Two companies of the same tenant must each be able to have a HEAD-OFFICE
-- branch, so the unique indexes now include company_id.
DROP INDEX IF EXISTS uk_branches_tenant_branch_code;
DROP INDEX IF EXISTS uk_branches_tenant_branch_name;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'branches' AND indexname = 'uk_branches_company_branch_code'
    ) THEN
        CREATE UNIQUE INDEX uk_branches_company_branch_code
            ON branches(company_id, branch_code);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'branches' AND indexname = 'uk_branches_company_branch_name'
    ) THEN
        CREATE UNIQUE INDEX uk_branches_company_branch_name
            ON branches(company_id, branch_name);
    END IF;
END $$;
