-- V046__make_purchase_order_lines_line_amount_generated.sql
-- Change line_amount in purchase_order_lines to GENERATED ALWAYS AS ... STORED
-- to match the PurchaseOrderLine entity mapping (insertable = false, updatable = false).
ALTER TABLE IF EXISTS purchase_order_lines
    DROP COLUMN IF EXISTS line_amount;

ALTER TABLE IF EXISTS purchase_order_lines
    ADD COLUMN line_amount NUMERIC(19,4)
    GENERATED ALWAYS AS (ROUND(quantity * unit_price * (1 - discount_percentage / 100), 4)) STORED;

