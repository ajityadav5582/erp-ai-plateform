-- Align pos_sales_integration_jobs table with JPA PosSalesIntegrationJob entity
ALTER TABLE pos_sales_integration_jobs ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pos_sales_integration_jobs ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pos_sales_integration_jobs ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMP WITH TIME ZONE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_pos_sales_integration_jobs_transaction'
    ) THEN
        ALTER TABLE pos_sales_integration_jobs
            ADD CONSTRAINT uk_pos_sales_integration_jobs_transaction
            UNIQUE (tenant_id, company_id, pos_transaction_id);
    END IF;
END $$;

