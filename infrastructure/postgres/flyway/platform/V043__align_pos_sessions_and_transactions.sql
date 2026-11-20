-- POS Sessions and Transactions column alignment
-- 1. pos_sessions
ALTER TABLE pos_sessions ADD COLUMN IF NOT EXISTS notes TEXT;

-- 2. pos_transactions
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS amount_tendered NUMERIC(19,4);
UPDATE pos_transactions SET amount_tendered = tendered_amount WHERE amount_tendered IS NULL;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 1;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS last_printed_by BIGINT;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS is_tax_invoice BOOLEAN NOT NULL DEFAULT true;

