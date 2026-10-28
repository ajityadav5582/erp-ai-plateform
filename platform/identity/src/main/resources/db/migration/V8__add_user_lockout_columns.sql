-- Add temporary lockout columns to the users table
-- Migration: V8
-- Description: Adds failed_login_attempts and locked_until columns to the users
-- table. These back the User entity's temporary lockout (brute-force protection)
-- fields, which were added to the entity after V1 was written.

-- failed_login_attempts: consecutive failed login counter (NOT NULL, defaults to 0)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'failed_login_attempts'
    ) THEN
        ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;

-- locked_until: instant until which the account is temporarily locked (nullable)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'locked_until'
    ) THEN
        ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
    END IF;
END $$;
