-- Procurement alignment
-- 1. goods_receipt_lines
ALTER TABLE goods_receipt_lines ADD COLUMN IF NOT EXISTS received_quantity NUMERIC(19,6);
UPDATE goods_receipt_lines SET received_quantity = quantity WHERE received_quantity IS NULL;
ALTER TABLE goods_receipt_lines ALTER COLUMN received_quantity SET NOT NULL;
ALTER TABLE goods_receipt_lines ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE goods_receipt_lines ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE goods_receipt_lines ADD COLUMN IF NOT EXISTS remarks TEXT;

-- 2. goods_receipts
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS stock_post_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS stock_post_error VARCHAR(500);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS received_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS warehouse_code VARCHAR(64);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(255);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS delivery_challan_no VARCHAR(100);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS posted_by VARCHAR(100);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);
ALTER TABLE goods_receipts ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);

-- 3. purchase_orders
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS expected_delivery_date DATE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS shipping_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS payment_terms_days INTEGER;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS warehouse_code VARCHAR(64);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(255);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS buyer_id BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS reference VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);

-- 4. purchase_order_lines
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS expected_delivery_date DATE;
ALTER TABLE purchase_order_lines ADD COLUMN IF NOT EXISTS notes TEXT;

-- 5. purchase_invoices
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS purchase_order_id BIGINT;
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS po_number VARCHAR(40);
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS goods_receipt_id BIGINT;
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS grn_number VARCHAR(40);
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);

-- 6. purchase_invoice_lines
ALTER TABLE purchase_invoice_lines ADD COLUMN IF NOT EXISTS goods_receipt_line_id BIGINT;
ALTER TABLE purchase_invoice_lines ADD COLUMN IF NOT EXISTS discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE purchase_invoice_lines ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0;

-- 7. supplier_payments
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS fiscal_year_id BIGINT;
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS branch_id BIGINT;
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(60) NOT NULL DEFAULT '';
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS reference_no VARCHAR(100);
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS paid_from VARCHAR(255);
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS posted_by VARCHAR(100);
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);
ALTER TABLE supplier_payments ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);

-- 8. purchase_requisitions
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS requested_by BIGINT NOT NULL DEFAULT 1;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'NPR';
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS warehouse_code VARCHAR(64);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(255);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS justification TEXT;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(100);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(100);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS converted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS converted_by VARCHAR(100);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);
ALTER TABLE purchase_requisitions ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);

-- 9. purchase_requisition_lines
ALTER TABLE purchase_requisition_lines ADD COLUMN IF NOT EXISTS unit_id BIGINT;
ALTER TABLE purchase_requisition_lines ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE purchase_requisition_lines ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED';
ALTER TABLE purchase_requisition_lines ADD COLUMN IF NOT EXISTS remarks TEXT;

-- 10. purchase_requisition_approvals
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS sequence_no INTEGER NOT NULL DEFAULT 1;
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS approver_role_id BIGINT;
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS approver_role VARCHAR(100);
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS amount_threshold NUMERIC(19,4);
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS policy_branch_id BIGINT;
ALTER TABLE purchase_requisition_approvals ADD COLUMN IF NOT EXISTS acted_by VARCHAR(100);

-- 11. purchase_requisition_approval_policies
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS branch_id BIGINT;
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS sequence_no INTEGER NOT NULL DEFAULT 1;
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS threshold_min_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS threshold_max_amount NUMERIC(19,4);
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS approver_role_id BIGINT;
ALTER TABLE purchase_requisition_approval_policies ADD COLUMN IF NOT EXISTS approver_user_id BIGINT;

-- 12. purchase_requisition_status_history
ALTER TABLE purchase_requisition_status_history ADD COLUMN IF NOT EXISTS reason VARCHAR(500);

