-- ============================================================================
-- V051: Multi-Invoice Supplier Payments & Allocations
-- Allows a single payment (cheque, bank transfer, wire) to settle multiple
-- purchase invoices, or leave an unallocated advance balance.
-- ============================================================================

-- 1. Relax single-invoice constraints on supplier_payments and add unallocated_amount
ALTER TABLE supplier_payments 
    ALTER COLUMN invoice_number DROP NOT NULL;

ALTER TABLE supplier_payments 
    ADD COLUMN IF NOT EXISTS unallocated_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000;

-- 2. Create supplier_payment_allocations table
CREATE TABLE IF NOT EXISTS supplier_payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL REFERENCES supplier_payments(id) ON DELETE CASCADE,
    purchase_invoice_id BIGINT NOT NULL REFERENCES purchase_invoices(id),
    invoice_number VARCHAR(60) NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);

-- Index for searching allocations by payment
CREATE INDEX IF NOT EXISTS idx_spa_tenant_company_payment 
    ON supplier_payment_allocations (tenant_id, company_id, payment_id);

-- Index for searching allocations by purchase invoice
CREATE INDEX IF NOT EXISTS idx_spa_tenant_company_invoice 
    ON supplier_payment_allocations (tenant_id, company_id, purchase_invoice_id);

-- 3. Backfill allocations for existing single-invoice payments
INSERT INTO supplier_payment_allocations (
    tenant_id,
    company_id,
    payment_id,
    purchase_invoice_id,
    invoice_number,
    allocated_amount,
    discount_amount,
    created_at,
    created_by,
    version
)
SELECT 
    sp.tenant_id,
    sp.company_id,
    sp.id,
    sp.purchase_invoice_id,
    COALESCE(sp.invoice_number, pi.invoice_number, 'LEGACY'),
    sp.amount,
    0.0000,
    sp.created_at,
    sp.created_by,
    0
FROM supplier_payments sp
JOIN purchase_invoices pi ON pi.id = sp.purchase_invoice_id
WHERE sp.purchase_invoice_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM supplier_payment_allocations spa 
      WHERE spa.payment_id = sp.id AND spa.purchase_invoice_id = sp.purchase_invoice_id
  );

