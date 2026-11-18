ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS vat_registration_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS vat_registration_number VARCHAR(64),
    ADD COLUMN IF NOT EXISTS vat_effective_from DATE,
    ADD COLUMN IF NOT EXISTS vat_effective_to DATE;
