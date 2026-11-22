-- V059: Add structured split payment amounts to pos_transactions
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS split_cash_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS split_digital_amount NUMERIC(19,4) NOT NULL DEFAULT 0;

