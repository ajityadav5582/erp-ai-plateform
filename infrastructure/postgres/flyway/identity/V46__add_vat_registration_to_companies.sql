-- Migration: V46
-- Description: Add VAT registration number and effective dates to companies,
-- so PAN registration and VAT registration are tracked separately.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='companies' AND column_name='vat_registration_number') THEN
        ALTER TABLE companies ADD COLUMN vat_registration_number VARCHAR(30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='companies' AND column_name='vat_effective_from') THEN
        ALTER TABLE companies ADD COLUMN vat_effective_from DATE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='companies' AND column_name='vat_effective_to') THEN
        ALTER TABLE companies ADD COLUMN vat_effective_to DATE;
    END IF;
END $$;
