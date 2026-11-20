-- POS alignment
-- 1. pos_checkout_idempotency
DROP TABLE IF EXISTS pos_checkout_idempotency;
CREATE TABLE pos_checkout_idempotency (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    transaction_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_checkout_idempotency_scope_key UNIQUE (tenant_id, company_id, user_id, idempotency_key)
);

-- 2. pos_sessions
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS session_number VARCHAR(40);
UPDATE pos_sessions SET session_number = 'SES-' || id WHERE session_number IS NULL;
ALTER TABLE pos_sessions ALTER COLUMN session_number SET NOT NULL;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS register_code VARCHAR(40) NOT NULL DEFAULT 'REG-01';
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS register_name VARCHAR(255) NOT NULL DEFAULT 'Main Register';
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS fiscal_year_id BIGINT;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS z_report_number VARCHAR(40);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS opening_cash_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS closing_cash_amount NUMERIC(19,4);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS expected_cash NUMERIC(19,4);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS cash_variance NUMERIC(19,4);
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_transactions INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_voided INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_cash_sales NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_card_sales NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_wallet_sales NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_other_sales NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS total_vat_collected NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS gross_sales NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS close_notes TEXT;

-- 3. pos_transaction_lines
ALTER TABLE pos_transaction_lines ADD COLUMN IF NOT EXISTS barcode VARCHAR(100);

-- 4. pos_invoice_sequence
ALTER TABLE pos_invoice_sequence ADD COLUMN IF NOT EXISTS fiscal_year VARCHAR(16) NOT NULL DEFAULT '2081/82';
ALTER TABLE pos_invoice_sequence ADD COLUMN IF NOT EXISTS prefix VARCHAR(16) NOT NULL DEFAULT 'INV';
ALTER TABLE pos_invoice_sequence ADD COLUMN IF NOT EXISTS last_number BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pos_invoice_sequence ALTER COLUMN register_id DROP NOT NULL;

