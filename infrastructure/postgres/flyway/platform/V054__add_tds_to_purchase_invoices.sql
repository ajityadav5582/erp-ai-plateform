-- V054: Add Nepal TDS (Withholding Tax) fields to purchase_invoices
ALTER TABLE purchase_invoices
    ADD COLUMN IF NOT EXISTS is_tds_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tds_rate NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS tds_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN IF NOT EXISTS net_payable_amount NUMERIC(19, 4);

-- Backfill net_payable_amount for existing invoices
UPDATE purchase_invoices
SET net_payable_amount = total_amount
WHERE net_payable_amount IS NULL;

ALTER TABLE purchase_invoices
    ALTER COLUMN net_payable_amount SET NOT NULL;

