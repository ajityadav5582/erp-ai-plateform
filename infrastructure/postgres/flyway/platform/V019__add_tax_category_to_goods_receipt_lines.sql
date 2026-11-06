ALTER TABLE goods_receipt_lines
    ADD COLUMN IF NOT EXISTS tax_category VARCHAR(20);
