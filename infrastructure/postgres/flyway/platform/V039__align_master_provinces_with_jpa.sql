-- Align master_provinces with the JPA Province entity columns.
ALTER TABLE IF EXISTS master_provinces
    ADD COLUMN IF NOT EXISTS province_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS province_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Backfill from legacy columns where present.
UPDATE master_provinces
   SET province_code = COALESCE(province_code, 'P' || id::text),
       province_name = COALESCE(province_name, name),
       status = COALESCE(status, 'ACTIVE'),
       version = COALESCE(version, 0)
 WHERE province_code IS NULL OR province_name IS NULL OR status IS NULL;

-- Enforce NOT NULL for required fields.
ALTER TABLE master_provinces
    ALTER COLUMN province_code SET NOT NULL,
    ALTER COLUMN province_name SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- Unique constraint on province_code (if not exists).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_master_provinces_code'
    ) THEN
        ALTER TABLE master_provinces ADD CONSTRAINT uk_master_provinces_code UNIQUE (province_code);
    END IF;
END $$;
