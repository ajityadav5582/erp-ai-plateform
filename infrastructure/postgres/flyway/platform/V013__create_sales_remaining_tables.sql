-- Migration: V013
-- Description: Remaining sales module tables (invoices, deliveries, returns, receipts).

-- sales_invoices
CREATE TABLE IF NOT EXISTS sales_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    sales_order_id BIGINT,
    pos_transaction_id BIGINT,
    so_number VARCHAR(40),
    delivery_id BIGINT,
    delivery_number VARCHAR(40),
    customer_id BIGINT,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_pan_number VARCHAR(16),
    invoice_date DATE NOT NULL,
    invoice_date_np VARCHAR(20) NOT NULL,
    due_date DATE,
    is_tax_invoice BOOLEAN NOT NULL DEFAULT TRUE,
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    exempt_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    zero_rated_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes TEXT,
    issued_at TIMESTAMP,
    issued_by VARCHAR(100),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    cbms_synced BOOLEAN NOT NULL DEFAULT FALSE,
    cbms_sync_time TIMESTAMP,
    cbms_response VARCHAR(500),
    cbms_retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_invoices_tenant_company_number UNIQUE (tenant_id, company_id, invoice_number),
    CONSTRAINT uk_sales_invoices_pos_transaction UNIQUE (pos_transaction_id)
);
CREATE INDEX IF NOT EXISTS idx_sales_invoices_tenant_company ON sales_invoices (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_sales_invoices_customer ON sales_invoices (tenant_id, company_id, customer_id, invoice_date);
CREATE INDEX IF NOT EXISTS idx_sales_invoices_order ON sales_invoices (tenant_id, company_id, sales_order_id, invoice_date);
CREATE INDEX IF NOT EXISTS idx_sales_invoices_status ON sales_invoices (tenant_id, company_id, status, invoice_date);

-- sales_invoice_lines
CREATE TABLE IF NOT EXISTS sales_invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    sales_invoice_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    vat_type VARCHAR(20) NOT NULL,
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    line_amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_invoice_lines_invoice_line_number UNIQUE (sales_invoice_id, line_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_invoice_lines_invoice ON sales_invoice_lines (sales_invoice_id, line_number);
CREATE INDEX IF NOT EXISTS idx_sales_invoice_lines_item ON sales_invoice_lines (tenant_id, company_id, item_id);

-- sales_deliveries
CREATE TABLE IF NOT EXISTS sales_deliveries (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    delivery_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    sales_order_id BIGINT NOT NULL,
    so_number VARCHAR(40) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64),
    warehouse_name VARCHAR(255),
    delivery_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    posted_at TIMESTAMP,
    posted_by VARCHAR(100),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_deliveries_tenant_company_number UNIQUE (tenant_id, company_id, delivery_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_deliveries_tenant_company ON sales_deliveries (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_sales_deliveries_order ON sales_deliveries (tenant_id, company_id, sales_order_id, delivery_date);
CREATE INDEX IF NOT EXISTS idx_sales_deliveries_status ON sales_deliveries (tenant_id, company_id, status, delivery_date);

-- sales_delivery_lines
CREATE TABLE IF NOT EXISTS sales_delivery_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    sales_delivery_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    sales_order_line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_delivery_lines_delivery_line_number UNIQUE (sales_delivery_id, line_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_delivery_lines_delivery ON sales_delivery_lines (sales_delivery_id, line_number);
CREATE INDEX IF NOT EXISTS idx_sales_delivery_lines_order_line ON sales_delivery_lines (tenant_id, company_id, sales_order_line_number);

-- sales_returns
CREATE TABLE IF NOT EXISTS sales_returns (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    credit_note_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    invoice_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    sales_order_id BIGINT,
    so_number VARCHAR(40),
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_pan_number VARCHAR(16),
    warehouse_id BIGINT NOT NULL,
    return_date DATE NOT NULL,
    return_date_np VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    posted_at TIMESTAMP,
    posted_by VARCHAR(100),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    cbms_synced BOOLEAN NOT NULL DEFAULT FALSE,
    cbms_sync_time TIMESTAMP,
    cbms_response VARCHAR(500),
    cbms_retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_returns_tenant_company_number UNIQUE (tenant_id, company_id, credit_note_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_returns_tenant_company ON sales_returns (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_sales_returns_invoice ON sales_returns (tenant_id, company_id, invoice_id, return_date);
CREATE INDEX IF NOT EXISTS idx_sales_returns_status ON sales_returns (tenant_id, company_id, status, return_date);

-- sales_return_lines
CREATE TABLE IF NOT EXISTS sales_return_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    sales_return_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    vat_type VARCHAR(20) NOT NULL,
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    line_amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_return_lines_return_line_number UNIQUE (sales_return_id, line_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_return_lines_return ON sales_return_lines (sales_return_id, line_number);
CREATE INDEX IF NOT EXISTS idx_sales_return_lines_item ON sales_return_lines (tenant_id, company_id, item_id);

-- customer_receipts
CREATE TABLE IF NOT EXISTS customer_receipts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    receipt_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    invoice_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    receipt_date DATE NOT NULL,
    reference VARCHAR(100),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    posted_at TIMESTAMP,
    posted_by VARCHAR(100),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_receipts_tenant_company_number UNIQUE (tenant_id, company_id, receipt_number)
);
CREATE INDEX IF NOT EXISTS idx_customer_receipts_tenant_company ON customer_receipts (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_customer_receipts_invoice ON customer_receipts (tenant_id, company_id, invoice_id, receipt_date);
CREATE INDEX IF NOT EXISTS idx_customer_receipts_customer ON customer_receipts (tenant_id, company_id, customer_id, receipt_date);
