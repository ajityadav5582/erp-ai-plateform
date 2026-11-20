-- Migration: V67
-- Description: Add administrative geography fields (province_id, district_id, local_level_id) to companies and branches

ALTER TABLE IF EXISTS companies
    ADD COLUMN IF NOT EXISTS province_id SMALLINT,
    ADD COLUMN IF NOT EXISTS district_id SMALLINT,
    ADD COLUMN IF NOT EXISTS local_level_id SMALLINT;

CREATE INDEX IF NOT EXISTS idx_companies_province_id ON companies(province_id);
CREATE INDEX IF NOT EXISTS idx_companies_district_id ON companies(district_id);
CREATE INDEX IF NOT EXISTS idx_companies_local_level_id ON companies(local_level_id);

ALTER TABLE IF EXISTS branches
    ADD COLUMN IF NOT EXISTS province_id SMALLINT,
    ADD COLUMN IF NOT EXISTS district_id SMALLINT,
    ADD COLUMN IF NOT EXISTS local_level_id SMALLINT;

CREATE INDEX IF NOT EXISTS idx_branches_province_id ON branches(province_id);
CREATE INDEX IF NOT EXISTS idx_branches_district_id ON branches(district_id);
CREATE INDEX IF NOT EXISTS idx_branches_local_level_id ON branches(local_level_id);

-- Conditionally add foreign key constraints if reference tables exist in the database
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'master_provinces') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_companies_province_id') THEN
            ALTER TABLE companies ADD CONSTRAINT fk_companies_province_id FOREIGN KEY (province_id) REFERENCES master_provinces(id) ON DELETE SET NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branches_province_id') THEN
            ALTER TABLE branches ADD CONSTRAINT fk_branches_province_id FOREIGN KEY (province_id) REFERENCES master_provinces(id) ON DELETE SET NULL;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'master_districts') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_companies_district_id') THEN
            ALTER TABLE companies ADD CONSTRAINT fk_companies_district_id FOREIGN KEY (district_id) REFERENCES master_districts(id) ON DELETE SET NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branches_district_id') THEN
            ALTER TABLE branches ADD CONSTRAINT fk_branches_district_id FOREIGN KEY (district_id) REFERENCES master_districts(id) ON DELETE SET NULL;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'master_local_levels') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_companies_local_level_id') THEN
            ALTER TABLE companies ADD CONSTRAINT fk_companies_local_level_id FOREIGN KEY (local_level_id) REFERENCES master_local_levels(id) ON DELETE SET NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branches_local_level_id') THEN
            ALTER TABLE branches ADD CONSTRAINT fk_branches_local_level_id FOREIGN KEY (local_level_id) REFERENCES master_local_levels(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;
