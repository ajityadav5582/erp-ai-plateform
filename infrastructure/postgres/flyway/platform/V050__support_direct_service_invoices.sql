-- V050__support_direct_service_invoices.sql
-- Support direct service and expense invoices without requiring physical Goods Receipt Notes (GRN).

-- 1. Add invoice_type to distinguish 3-way match receipt billing vs direct expense/service billing
ALTER TABLE purchase_invoices
    ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(32) NOT NULL DEFAULT 'GOODS_RECEIPT';

-- 2. Ensure goods receipt and purchase order references are nullable for direct bills
ALTER TABLE purchase_invoices
    ALTER COLUMN goods_receipt_id DROP NOT NULL,
    ALTER COLUMN grn_number DROP NOT NULL,
    ALTER COLUMN purchase_order_id DROP NOT NULL,
    ALTER COLUMN po_number DROP NOT NULL;

ALTER TABLE purchase_invoice_lines
    ALTER COLUMN goods_receipt_line_id DROP NOT NULL;

-- 3. Add index on invoice_type for filtered querying
CREATE INDEX IF NOT EXISTS idx_purchase_invoices_type
    ON purchase_invoices (tenant_id, company_id, invoice_type);

