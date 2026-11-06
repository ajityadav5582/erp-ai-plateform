-- Migration: V014
-- Description: Procurement module tables.

-- company_workflow_configurations
CREATE TABLE IF NOT EXISTS company_workflow_configurations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cwc_tenant_company ON company_workflow_configurations (tenant_id, company_id);

-- purchase_requisitions
CREATE TABLE IF NOT EXISTS purchase_requisitions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    pr_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    request_date DATE NOT NULL,
    request_date_np_year INTEGER,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    estimated_subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    estimated_tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    estimated_total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    approved_subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    approved_tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    approved_total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    ordered_subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    ordered_tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    ordered_total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_purchase_requisitions_tenant_company_number UNIQUE (tenant_id, company_id, pr_number)
);
CREATE INDEX IF NOT EXISTS idx_purchase_requisitions_scope ON purchase_requisitions (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_purchase_requisitions_branch ON purchase_requisitions (tenant_id, company_id, branch_id, request_date);
CREATE INDEX IF NOT EXISTS idx_purchase_requisitions_approval_queue ON purchase_requisitions (tenant_id, company_id, status, submitted_at);
CREATE INDEX IF NOT EXISTS idx_purchase_requisitions_fiscal_year ON purchase_requisitions (tenant_id, company_id, fiscal_year_id, request_date);

-- purchase_requisition_lines
CREATE TABLE IF NOT EXISTS purchase_requisition_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_requisition_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    requested_quantity NUMERIC(19,6) NOT NULL,
    approved_quantity NUMERIC(19,6) NOT NULL DEFAULT 0,
    ordered_quantity NUMERIC(19,6) NOT NULL DEFAULT 0,
    estimated_unit_price NUMERIC(19,4),
    estimated_line_amount NUMERIC(19,4),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_prl_requisition ON purchase_requisition_lines (purchase_requisition_id, line_number);

-- purchase_requisition_approvals
CREATE TABLE IF NOT EXISTS purchase_requisition_approvals (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_requisition_id BIGINT NOT NULL,
    approver_id BIGINT,
    approver_name VARCHAR(255),
    action VARCHAR(32) NOT NULL,
    comments TEXT,
    acted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pra_requisition ON purchase_requisition_approvals (purchase_requisition_id);

-- purchase_requisition_approval_policies
CREATE TABLE IF NOT EXISTS purchase_requisition_approval_policies (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    min_amount NUMERIC(19,4),
    max_amount NUMERIC(19,4),
    approver_role VARCHAR(64),
    sequence_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_prap_tenant_company ON purchase_requisition_approval_policies (tenant_id, company_id);

-- purchase_requisition_order_links
CREATE TABLE IF NOT EXISTS purchase_requisition_order_links (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_requisition_id BIGINT NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_prol_requisition ON purchase_requisition_order_links (purchase_requisition_id);
CREATE INDEX IF NOT EXISTS idx_prol_order ON purchase_requisition_order_links (purchase_order_id);

-- purchase_requisition_status_history
CREATE TABLE IF NOT EXISTS purchase_requisition_status_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_requisition_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_prsh_requisition ON purchase_requisition_status_history (purchase_requisition_id);

-- purchase_orders
CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    po_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    source_purchase_requisition_id BIGINT,
    branch_id BIGINT,
    order_date DATE NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    received_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_purchase_orders_tenant_company_number UNIQUE (tenant_id, company_id, po_number),
    CONSTRAINT uk_purchase_orders_source_requisition UNIQUE (tenant_id, company_id, source_purchase_requisition_id)
);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_tenant_company ON purchase_orders (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier ON purchase_orders (tenant_id, company_id, supplier_id, order_date);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_fiscal_year ON purchase_orders (tenant_id, company_id, fiscal_year_id, order_date);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_source_requisition ON purchase_orders (tenant_id, company_id, source_purchase_requisition_id);

-- purchase_order_lines
CREATE TABLE IF NOT EXISTS purchase_order_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    line_amount NUMERIC(19,4) NOT NULL,
    received_quantity NUMERIC(19,6) NOT NULL DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pol_order ON purchase_order_lines (purchase_order_id, line_number);

-- goods_receipts
CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    grn_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    purchase_order_id BIGINT NOT NULL,
    po_number VARCHAR(40) NOT NULL,
    receipt_date DATE NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_goods_receipts_tenant_company_number UNIQUE (tenant_id, company_id, grn_number)
);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_tenant_company ON goods_receipts (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_order ON goods_receipts (tenant_id, company_id, purchase_order_id, receipt_date);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_supplier ON goods_receipts (tenant_id, company_id, supplier_id, receipt_date);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_fiscal_year ON goods_receipts (tenant_id, company_id, fiscal_year_id, receipt_date);

-- goods_receipt_lines
CREATE TABLE IF NOT EXISTS goods_receipt_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    goods_receipt_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    purchase_order_line_id BIGINT,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    line_amount NUMERIC(19,4) NOT NULL,
    tax_category VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grl_receipt ON goods_receipt_lines (goods_receipt_id, line_number);

-- purchase_invoices
CREATE TABLE IF NOT EXISTS purchase_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_vat_number VARCHAR(64),
    invoice_date DATE NOT NULL,
    due_date DATE,
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_purchase_invoices_tenant_company_number UNIQUE (tenant_id, company_id, invoice_number)
);
CREATE INDEX IF NOT EXISTS idx_purchase_invoices_tenant_company ON purchase_invoices (tenant_id, company_id);

-- purchase_invoice_lines
CREATE TABLE IF NOT EXISTS purchase_invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purchase_invoice_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    line_amount NUMERIC(19,4) NOT NULL,
    tax_category VARCHAR(32),
    taxable_amount NUMERIC(19,4),
    tax_amount NUMERIC(19,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pil_invoice ON purchase_invoice_lines (purchase_invoice_id, line_number);

-- supplier_payments
CREATE TABLE IF NOT EXISTS supplier_payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    payment_number VARCHAR(40) NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    purchase_invoice_id BIGINT,
    amount NUMERIC(19,4) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_date DATE NOT NULL,
    reference VARCHAR(100),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_supplier_payments_tenant_company_number UNIQUE (tenant_id, company_id, payment_number)
);
CREATE INDEX IF NOT EXISTS idx_supplier_payments_tenant_company ON supplier_payments (tenant_id, company_id);

-- document_sequences (procurement)
CREATE TABLE IF NOT EXISTS procurement_document_sequences (
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
    CONSTRAINT uk_document_sequences_scope UNIQUE (tenant_id, company_id, document_type, period_key)
);
CREATE INDEX IF NOT EXISTS idx_procurement_document_sequences_scope ON procurement_document_sequences (tenant_id, company_id, document_type);
