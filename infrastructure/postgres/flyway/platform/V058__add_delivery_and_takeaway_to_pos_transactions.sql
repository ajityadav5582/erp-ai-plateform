-- V058: Add order type, token number, and delivery fields to pos_transactions and pos_kitchen_orders
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS order_type VARCHAR(32) NOT NULL DEFAULT 'DINE_IN';
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS token_number VARCHAR(32);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_partner VARCHAR(64);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(500);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(32);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_charge NUMERIC(19,4) NOT NULL DEFAULT 0;

ALTER TABLE pos_kitchen_orders ADD COLUMN IF NOT EXISTS token_number VARCHAR(32);
ALTER TABLE pos_kitchen_orders ADD COLUMN IF NOT EXISTS delivery_partner VARCHAR(64);
ALTER TABLE pos_kitchen_orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(32);

