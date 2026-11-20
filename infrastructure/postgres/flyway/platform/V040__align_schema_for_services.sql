-- V040__align_schema_for_services.sql
-- Align database schema with JPA entities for inventory, procurement, sales, and pos services.

-- 1. Inventory: TaxRate entity requires audit fields created_by and updated_by
ALTER TABLE IF EXISTS tax_rates
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100) DEFAULT 'system';

UPDATE tax_rates
   SET created_by = COALESCE(created_by, 'system'),
       updated_by = COALESCE(updated_by, 'system')
 WHERE created_by IS NULL OR updated_by IS NULL;

-- 2. Procurement: CompanyWorkflowConfiguration requires purchase_requisition_approval_required
ALTER TABLE IF EXISTS company_workflow_configurations
    ADD COLUMN IF NOT EXISTS purchase_requisition_approval_required BOOLEAN NOT NULL DEFAULT true;

-- 3. POS: FiscalBillSequenceEntity requires document_type and prefix, and compound unique constraint
ALTER TABLE IF EXISTS fiscal_bill_sequences
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(30) NOT NULL DEFAULT 'INVOICE',
    ADD COLUMN IF NOT EXISTS prefix VARCHAR(20) NOT NULL DEFAULT 'INV';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_fiscal_bill_sequences_scope'
    ) THEN
        ALTER TABLE fiscal_bill_sequences DROP CONSTRAINT uk_fiscal_bill_sequences_scope;
    END IF;
    ALTER TABLE fiscal_bill_sequences ADD CONSTRAINT uk_fiscal_bill_sequences_scope UNIQUE (tenant_id, company_id, fiscal_year_id, document_type);
EXCEPTION
    WHEN others THEN NULL;
END $$;

-- 4. Sales: DocumentSequence entity maps to sales_document_sequences table
CREATE TABLE IF NOT EXISTS sales_document_sequences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    period_key VARCHAR(20) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    last_allocated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_sales_document_sequences_scope UNIQUE (tenant_id, company_id, document_type, period_key)
);

CREATE INDEX IF NOT EXISTS idx_sales_document_sequences_scope
    ON sales_document_sequences (tenant_id, company_id, document_type);
