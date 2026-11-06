-- Migration: V007
-- Description: Create pos_company_configs table holding per-company POS
-- operational configuration (enabled payment methods, cash rounding, service
-- charge, tip handling, default register/warehouse, price list, restaurant
-- settings). One row per (tenant, company).

CREATE TABLE IF NOT EXISTS pos_company_configs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    enabled_payment_methods VARCHAR(500),
    cash_rounding BOOLEAN NOT NULL DEFAULT TRUE,
    service_charge_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    tip_handling VARCHAR(20) NOT NULL DEFAULT 'NONE',
    default_register_id BIGINT,
    default_warehouse_id BIGINT,
    price_list_id BIGINT,
    restaurant_settings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_company_configs_tenant_company UNIQUE (tenant_id, company_id),
    CONSTRAINT chk_pos_company_configs_tip_handling CHECK (tip_handling IN ('NONE','ADD_TO_BILL','SEPARATE'))
);

CREATE INDEX IF NOT EXISTS idx_pos_company_configs_tenant_company
    ON pos_company_configs (tenant_id, company_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'pos_company_configs'
          AND constraint_name = 'fk_pos_company_configs_tenant'
    ) THEN
        ALTER TABLE pos_company_configs
            ADD CONSTRAINT fk_pos_company_configs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
    END IF;
EXCEPTION WHEN others THEN
    NULL;
END $$;

-- company_id references companies(id); guard for environments where companies
-- may not exist yet (idempotent).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'pos_company_configs'
          AND constraint_name = 'fk_pos_company_configs_company'
    ) THEN
        ALTER TABLE pos_company_configs
            ADD CONSTRAINT fk_pos_company_configs_company
            FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT;
    END IF;
EXCEPTION WHEN others THEN
    NULL;
END $$;
