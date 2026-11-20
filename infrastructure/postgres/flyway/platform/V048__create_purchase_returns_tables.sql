-- Migration: V048
-- Description: Create purchase_returns and purchase_return_lines tables for Purchase Return and Debit Note support.

CREATE TABLE IF NOT EXISTS purchase_returns (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    debit_note_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    purchase_invoice_id BIGINT NOT NULL,
    invoice_number VARCHAR(60) NOT NULL,
    goods_receipt_id BIGINT,
    grn_number VARCHAR(40),
    purchase_order_id BIGINT,
    po_number VARCHAR(40),
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_pan_number VARCHAR(64),
    warehouse_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64),
    warehouse_name VARCHAR(255),
    return_date DATE NOT NULL,
    return_date_np VARCHAR(20),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    stock_post_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    stock_post_error VARCHAR(500),
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes TEXT,
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(100),
    posted_at TIMESTAMP WITH TIME ZONE,
    posted_by VARCHAR(100),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_purchase_returns_tenant_company_number UNIQUE (tenant_id, company_id, debit_note_number)
);

CREATE INDEX IF NOT EXISTS idx_purchase_returns_tenant_company ON purchase_returns (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_invoice ON purchase_returns (tenant_id, company_id, purchase_invoice_id, return_date);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_supplier ON purchase_returns (tenant_id, company_id, supplier_id, return_date);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_status ON purchase_returns (tenant_id, company_id, status, return_date);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_fiscal_year ON purchase_returns (tenant_id, company_id, fiscal_year_id, return_date);

CREATE TABLE IF NOT EXISTS purchase_return_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_return_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    purchase_invoice_line_id BIGINT,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    return_quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    tax_category VARCHAR(32) NOT NULL DEFAULT 'TAXABLE',
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00,
    taxable_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_prl_return_line_number UNIQUE (purchase_return_id, line_number)
);

CREATE INDEX IF NOT EXISTS idx_prl_return ON purchase_return_lines (purchase_return_id, line_number);
CREATE INDEX IF NOT EXISTS idx_prl_invoice_line ON purchase_return_lines (tenant_id, company_id, purchase_invoice_line_id);

