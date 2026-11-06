ALTER TABLE purchase_invoices
    ADD COLUMN IF NOT EXISTS supplier_vat_registration_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS supplier_vat_registration_number VARCHAR(64),
    ADD COLUMN IF NOT EXISTS supplier_tax_id VARCHAR(64);
