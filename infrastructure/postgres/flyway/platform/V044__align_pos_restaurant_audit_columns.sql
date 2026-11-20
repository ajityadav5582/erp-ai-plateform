-- POS Restaurant audit columns alignment
ALTER TABLE pos_tables ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pos_tables ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pos_table_sessions ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pos_table_sessions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pos_kitchen_orders ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pos_kitchen_orders ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE pos_kitchen_order_lines ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE pos_kitchen_order_lines ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

