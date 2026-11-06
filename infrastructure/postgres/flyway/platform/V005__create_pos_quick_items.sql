-- Migration: V005
-- Description: Create pos_quick_items base table so V006 can alter it on a clean DB.

CREATE TABLE IF NOT EXISTS pos_quick_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    register_id BIGINT,
    item_id BIGINT,
    item_code VARCHAR(100),
    item_name VARCHAR(255),
    uom_code VARCHAR(32),
    default_price NUMERIC(19,4),
    grid_row INTEGER NOT NULL DEFAULT 0,
    grid_col INTEGER NOT NULL DEFAULT 0,
    display_color VARCHAR(20),
    display_icon VARCHAR(100),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_quick_items_tenant_company ON pos_quick_items (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_pos_quick_items_register ON pos_quick_items (tenant_id, company_id, register_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_pos_quick_items_item ON pos_quick_items (tenant_id, company_id, item_id);
