-- Migration: V45
-- Description: Add business_type column to companies to capture the operational
-- business category (RESTAURANT, CAFE, ...). Distinct from company_type, which
-- describes the legal structure. Existing rows are backfilled with 'OTHER'.

-- ============================================================
-- 1. Add business_type column (nullable first for safe backfill)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'companies' AND column_name = 'business_type'
    ) THEN
        ALTER TABLE companies ADD COLUMN business_type VARCHAR(40);
    END IF;
END $$;

-- ============================================================
-- 2. Backfill existing rows with a safe default
-- ============================================================
UPDATE companies
SET business_type = 'OTHER'
WHERE business_type IS NULL;

-- ============================================================
-- 3. Enforce NOT NULL + allowed values
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'companies' AND constraint_name = 'chk_companies_business_type'
    ) THEN
        ALTER TABLE companies
            ADD CONSTRAINT chk_companies_business_type
            CHECK (business_type IN (
                'RESTAURANT','CAFE','HOTEL','SWEETS_RETAIL','GENERAL_RETAIL',
                'GROCERY','HARDWARE','SANITARY','PHARMACY','CLOTHING','SERVICES','OTHER'
            ));
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE companies ALTER COLUMN business_type SET NOT NULL;
EXCEPTION WHEN others THEN
    -- ignore if already not null
    NULL;
END $$;

-- ============================================================
-- 4. Index for POS configuration lookups
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_companies_business_type
    ON companies(business_type);
