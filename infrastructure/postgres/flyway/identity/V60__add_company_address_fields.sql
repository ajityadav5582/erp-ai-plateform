-- Migration: V60
-- Description: Add ward_no and tole_street columns to companies for IRD address requirements

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'companies' AND column_name = 'ward_no'
    ) THEN
        ALTER TABLE companies ADD COLUMN ward_no VARCHAR(20);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'companies' AND column_name = 'tole_street'
    ) THEN
        ALTER TABLE companies ADD COLUMN tole_street VARCHAR(200);
    END IF;
END $$;
