-- V045__make_purchase_requisition_approvals_acted_at_nullable.sql
-- Allow acted_at in purchase_requisition_approvals to be null for pending approval steps
ALTER TABLE IF EXISTS purchase_requisition_approvals
    ALTER COLUMN acted_at DROP NOT NULL;

