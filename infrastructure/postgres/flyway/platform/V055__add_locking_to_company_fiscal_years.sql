-- V055: Add period locking to company_fiscal_years
ALTER TABLE company_fiscal_years
    ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(100);

