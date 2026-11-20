-- Add ward_no column to branches for IRD address requirements.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'branches' AND column_name = 'ward_no'
    ) THEN
        ALTER TABLE branches ADD COLUMN ward_no VARCHAR(50);
    END IF;
END $$;
