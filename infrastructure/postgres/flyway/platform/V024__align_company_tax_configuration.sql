-- Align the shared companies table with fields already mapped by Company.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS pan_number VARCHAR(30);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS company_type VARCHAR(40);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS business_type VARCHAR(40);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS fiscal_year_type VARCHAR(20);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS vat_registered BOOLEAN DEFAULT FALSE;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS vat_registration_number VARCHAR(30);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS vat_effective_from DATE;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS vat_effective_to DATE;

UPDATE companies SET vat_registered = FALSE WHERE vat_registered IS NULL;

CREATE INDEX IF NOT EXISTS idx_companies_tenant_pan ON companies (tenant_id, pan_number);
