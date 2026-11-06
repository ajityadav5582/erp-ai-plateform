ALTER TABLE purchase_invoice_lines
    ADD COLUMN IF NOT EXISTS taxable_amount NUMERIC(19,4);
