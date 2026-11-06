-- Migration: V009
-- Description: Restaurant-only POS tables: pos_tables, pos_table_sessions,
-- pos_kitchen_orders, pos_kitchen_order_lines. Retail companies never use these.

CREATE TABLE IF NOT EXISTS pos_tables (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    table_number VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 4,
    section VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    current_session_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pos_tables_status CHECK (status IN ('FREE','OCCUPIED','ORDERED','RESERVED','BILLING','CLEANING'))
);
CREATE INDEX IF NOT EXISTS idx_pos_tables_tenant_company ON pos_tables (tenant_id, company_id);

CREATE TABLE IF NOT EXISTS pos_table_sessions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    pos_transaction_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    merged_into_session_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pos_table_sessions_status CHECK (status IN ('OPEN','BILLED','CLOSED','MERGED'))
);
CREATE INDEX IF NOT EXISTS idx_pos_ts_tenant_company ON pos_table_sessions (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_ts_table ON pos_table_sessions (tenant_id, company_id, table_id);

CREATE TABLE IF NOT EXISTS pos_kitchen_orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    session_id BIGINT,
    pos_transaction_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    course INTEGER NOT NULL DEFAULT 1,
    fired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pos_kitchen_orders_status CHECK (status IN ('PENDING','PREPARING','READY','SERVED','CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_pos_kitchen_orders_tenant_company ON pos_kitchen_orders (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_kitchen_orders_table ON pos_kitchen_orders (tenant_id, company_id, table_id);

CREATE TABLE IF NOT EXISTS pos_kitchen_order_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    kitchen_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(100),
    item_name VARCHAR(255),
    quantity NUMERIC(19,4) NOT NULL,
    notes VARCHAR(255),
    course INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_kol_tenant_company ON pos_kitchen_order_lines (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_kol_order ON pos_kitchen_order_lines (tenant_id, company_id, kitchen_order_id);

-- FKs (idempotent)
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='pos_tables' AND constraint_name='fk_pos_tables_tenant') THEN
    ALTER TABLE pos_tables ADD CONSTRAINT fk_pos_tables_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
  END IF;
EXCEPTION WHEN others THEN NULL; END $$;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='pos_tables' AND constraint_name='fk_pos_tables_company') THEN
    ALTER TABLE pos_tables ADD CONSTRAINT fk_pos_tables_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT;
  END IF;
EXCEPTION WHEN others THEN NULL; END $$;
