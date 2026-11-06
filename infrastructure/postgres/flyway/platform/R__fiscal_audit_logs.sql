-- Repeatable migration: fiscal audit logs table + immutability trigger
-- (moved from business/pos/audit/fiscal_audit_logs.sql)

CREATE TABLE IF NOT EXISTS fiscal_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    user_name VARCHAR(255),
    details_json TEXT,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION prevent_bill_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF current_setting('app.allow_test_purge', true) = 'on' THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'COMPLETED' THEN
            RAISE EXCEPTION 'Nepal IRD Compliance Violation: Committed invoices are immutable.' USING ERRCODE = 'P0001';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.status = 'COMPLETED' AND (
            NEW.total_amount IS DISTINCT FROM OLD.total_amount OR
            NEW.taxable_amount IS DISTINCT FROM OLD.taxable_amount OR
            NEW.vat_amount IS DISTINCT FROM OLD.vat_amount OR
            NEW.transaction_number IS DISTINCT FROM OLD.transaction_number
        ) THEN
            RAISE EXCEPTION 'Nepal IRD Compliance Violation: Committed invoices are immutable.' USING ERRCODE = 'P0001';
        END IF;
        RETURN NEW;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_bill_mutation ON pos_transactions;
CREATE TRIGGER trg_prevent_bill_mutation
BEFORE UPDATE OR DELETE ON pos_transactions
FOR EACH ROW
EXECUTE FUNCTION prevent_bill_mutation();
