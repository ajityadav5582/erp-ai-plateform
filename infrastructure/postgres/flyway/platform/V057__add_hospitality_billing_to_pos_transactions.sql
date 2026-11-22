-- V057: Add service charge, room charge and discount metadata to pos_transactions
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS service_charge_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS discount_reason VARCHAR(255);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS room_number VARCHAR(64);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS guest_name VARCHAR(255);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS split_payment_details TEXT;

