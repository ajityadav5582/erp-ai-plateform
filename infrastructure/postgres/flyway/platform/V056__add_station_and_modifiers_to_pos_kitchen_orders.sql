-- V056: Add kitchen station routing, modifiers, and add-on pricing to pos_kitchen_orders and pos_kitchen_order_lines

ALTER TABLE pos_kitchen_orders
    ADD COLUMN IF NOT EXISTS kot_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) DEFAULT 'DINE_IN';

ALTER TABLE pos_kitchen_order_lines
    ADD COLUMN IF NOT EXISTS station VARCHAR(50) DEFAULT 'KITCHEN',
    ADD COLUMN IF NOT EXISTS modifiers VARCHAR(500),
    ADD COLUMN IF NOT EXISTS modifier_price NUMERIC(19,4) DEFAULT 0.0000;

CREATE INDEX IF NOT EXISTS idx_pos_kol_station ON pos_kitchen_order_lines (tenant_id, company_id, station);

