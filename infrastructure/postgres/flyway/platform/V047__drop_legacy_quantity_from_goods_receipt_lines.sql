-- Drop legacy quantity column from goods_receipt_lines
-- V014 originally created `quantity NUMERIC(19,6) NOT NULL`.
-- V041 added `received_quantity NUMERIC(19,6) NOT NULL` to align with GoodsReceiptLine domain entity.
-- Dropping legacy column `quantity` removes the NOT NULL constraint conflict when GoodsReceiptLine is inserted.

ALTER TABLE goods_receipt_lines DROP COLUMN IF EXISTS quantity;

