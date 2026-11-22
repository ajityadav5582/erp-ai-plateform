-- V053: Add company bank details and payment QR code URL
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(100),
    ADD COLUMN IF NOT EXISTS qr_code_url VARCHAR(500);

