-- Migration: V015
-- Description: POS core tables.

-- pos_registers
CREATE TABLE IF NOT EXISTS pos_registers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    register_code VARCHAR(40) NOT NULL,
    register_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT,
    branch_id BIGINT,
    branch_name VARCHAR(255),
    warehouse_code VARCHAR(64),
    warehouse_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_session_id BIGINT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_registers_tenant_company_code UNIQUE (tenant_id, company_id, register_code)
);
CREATE INDEX IF NOT EXISTS idx_pos_registers_tenant_company ON pos_registers (tenant_id, company_id);

-- pos_sessions
CREATE TABLE IF NOT EXISTS pos_sessions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    register_id BIGINT NOT NULL,
    cashier_id VARCHAR(255) NOT NULL,
    cashier_name VARCHAR(255) NOT NULL,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    opening_cash NUMERIC(19,4),
    closing_cash NUMERIC(19,4),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_sessions_tenant_company ON pos_sessions (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_sessions_register ON pos_sessions (tenant_id, company_id, register_id);

-- pos_transactions
CREATE TABLE IF NOT EXISTS pos_transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    transaction_number VARCHAR(40),
    session_id BIGINT NOT NULL,
    register_id BIGINT NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    transaction_date DATE NOT NULL,
    transaction_date_np VARCHAR(20) NOT NULL,
    customer_id BIGINT,
    customer_code VARCHAR(64),
    customer_name VARCHAR(255) NOT NULL,
    customer_pan_number VARCHAR(16),
    cashier_id VARCHAR(255) NOT NULL,
    cashier_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    exempt_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    zero_rated_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    payment_method VARCHAR(32),
    tendered_amount NUMERIC(19,4),
    change_amount NUMERIC(19,4),
    notes TEXT,
    cbms_synced BOOLEAN NOT NULL DEFAULT FALSE,
    cbms_sync_time TIMESTAMP,
    cbms_response VARCHAR(500),
    cbms_retry_count INTEGER NOT NULL DEFAULT 0,
    voided_at TIMESTAMP,
    voided_by VARCHAR(100),
    void_reason VARCHAR(500),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_transactions_tenant_company_number UNIQUE (tenant_id, company_id, transaction_number)
);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_tenant_company ON pos_transactions (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_session ON pos_transactions (tenant_id, company_id, session_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_register ON pos_transactions (tenant_id, company_id, register_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_status ON pos_transactions (tenant_id, company_id, status, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_customer ON pos_transactions (tenant_id, company_id, customer_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_pos_transactions_cbms ON pos_transactions (tenant_id, company_id, cbms_synced, status);

-- pos_transaction_lines
CREATE TABLE IF NOT EXISTS pos_transaction_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    pos_transaction_id BIGINT NOT NULL,
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
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ptl_transaction ON pos_transaction_lines (pos_transaction_id, line_number);

-- pos_invoices
CREATE TABLE IF NOT EXISTS pos_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    register_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    customer_id BIGINT,
    customer_pan VARCHAR(16),
    payment_method VARCHAR(32) NOT NULL,
    tendered_amount NUMERIC(19,4),
    change_amount NUMERIC(19,4),
    gross_total NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4),
    taxable_amount NUMERIC(19,4),
    exempt_amount NUMERIC(19,4),
    zero_rated_amount NUMERIC(19,4),
    vat_amount NUMERIC(19,4),
    grand_total NUMERIC(19,4) NOT NULL,
    bs_date VARCHAR(32),
    lines_json TEXT NOT NULL,
    cashier_name VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_invoices_tenant_company ON pos_invoices (tenant_id, company_id);

-- pos_invoice_sequence
CREATE TABLE IF NOT EXISTS pos_invoice_sequence (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    register_id BIGINT NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    last_allocated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_invoice_sequence_scope UNIQUE (tenant_id, company_id, register_id)
);

-- fiscal_bill_sequences
CREATE TABLE IF NOT EXISTS fiscal_bill_sequences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    fiscal_year_id BIGINT NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    last_allocated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_fiscal_bill_sequences_scope UNIQUE (tenant_id, company_id, fiscal_year_id)
);

-- pos_checkout_idempotency
CREATE TABLE IF NOT EXISTS pos_checkout_idempotency (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    idempotency_key UUID NOT NULL,
    pos_transaction_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_checkout_idempotency_key UNIQUE (tenant_id, company_id, idempotency_key)
);

-- pos_sales_integration_jobs
CREATE TABLE IF NOT EXISTS pos_sales_integration_jobs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    pos_transaction_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_sales_integration_jobs_status ON pos_sales_integration_jobs (tenant_id, company_id, status);

-- document_sequences (pos)
CREATE TABLE IF NOT EXISTS pos_document_sequences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    period_key VARCHAR(20) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    last_allocated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_document_sequences_scope UNIQUE (tenant_id, company_id, document_type, period_key)
);
CREATE INDEX IF NOT EXISTS idx_pos_document_sequences_scope ON pos_document_sequences (tenant_id, company_id, document_type);
