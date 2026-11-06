-- Migration: V012
-- Description: Sales module core tables (customers, sales_quotations, sales_quotation_lines,
-- sales_orders, sales_order_lines). Remaining sales tables (invoices, deliveries, returns,
-- receipts) follow the same pattern and can be added in a follow-up migration.

-- customers
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(32),
    address TEXT,
    pan_number VARCHAR(16),
    vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    credit_limit NUMERIC(19,4),
    payment_terms_days INTEGER,
    currency VARCHAR(8) NOT NULL DEFAULT 'NPR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_customers_company_code UNIQUE (tenant_id, company_id, code),
    CONSTRAINT uk_customers_company_name UNIQUE (tenant_id, company_id, name)
);
CREATE INDEX IF NOT EXISTS idx_customers_tenant_company ON customers (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_customers_type ON customers (tenant_id, company_id, type);
CREATE INDEX IF NOT EXISTS idx_customers_pan ON customers (tenant_id, company_id, pan_number);

-- sales_quotations
CREATE TABLE IF NOT EXISTS sales_quotations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    quotation_number VARCHAR(40) NOT NULL,
    quotation_date DATE NOT NULL,
    valid_until_date DATE,
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_pan_number VARCHAR(32),
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    warehouse_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    sales_order_id BIGINT,
    converted_at TIMESTAMP,
    reference VARCHAR(100),
    terms_and_conditions VARCHAR(2000),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_quotations_tenant_company_number UNIQUE (tenant_id, company_id, quotation_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_quotations_tenant_company ON sales_quotations (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_sales_quotations_customer ON sales_quotations (tenant_id, company_id, customer_id, quotation_date);
CREATE INDEX IF NOT EXISTS idx_sales_quotations_status ON sales_quotations (tenant_id, company_id, status, quotation_date);

-- sales_quotation_lines
CREATE TABLE IF NOT EXISTS sales_quotation_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    quotation_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00,
    line_amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_quotation_lines_quotation_line UNIQUE (quotation_id, line_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_quotation_lines_quotation ON sales_quotation_lines (quotation_id, line_number);
CREATE INDEX IF NOT EXISTS idx_sales_quotation_lines_item ON sales_quotation_lines (tenant_id, company_id, item_id);

-- sales_orders
CREATE TABLE IF NOT EXISTS sales_orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    so_number VARCHAR(40) NOT NULL,
    fiscal_year_id BIGINT,
    branch_id BIGINT,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    customer_id BIGINT NOT NULL,
    customer_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_pan_number VARCHAR(16),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(3) NOT NULL DEFAULT 'NPR',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    delivered_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    payment_terms_days INTEGER,
    warehouse_id BIGINT,
    warehouse_code VARCHAR(64),
    warehouse_name VARCHAR(255),
    reference VARCHAR(100),
    notes TEXT,
    confirmed_at TIMESTAMP,
    confirmed_by VARCHAR(100),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    closed_at TIMESTAMP,
    closed_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_orders_tenant_company_number UNIQUE (tenant_id, company_id, so_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_company ON sales_orders (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_customer ON sales_orders (tenant_id, company_id, customer_id, order_date);
CREATE INDEX IF NOT EXISTS idx_sales_orders_fiscal_year ON sales_orders (tenant_id, company_id, fiscal_year_id, order_date);
CREATE INDEX IF NOT EXISTS idx_sales_orders_status ON sales_orders (tenant_id, company_id, status, order_date);

-- sales_order_lines
CREATE TABLE IF NOT EXISTS sales_order_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    sales_order_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    uom_code VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    line_amount NUMERIC(19,4) NOT NULL,
    delivered_quantity NUMERIC(19,6) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_order_lines_order_line_number UNIQUE (sales_order_id, line_number)
);
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_order ON sales_order_lines (sales_order_id, line_number);
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_item ON sales_order_lines (tenant_id, company_id, item_id);
