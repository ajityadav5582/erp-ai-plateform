-- Create local_levels table
-- Migration: V15
-- Description: Creates the local_levels table for geographic management.
-- Local levels (municipalities, rural municipalities, etc.) are categorized by local_level_type.

CREATE TABLE local_levels (
    municipality_id VARCHAR(50) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    nepali_name VARCHAR(100),
    district_id BIGINT NOT NULL,
    local_level_type_id VARCHAR(50) NOT NULL,
    -- Constraints
    CONSTRAINT chk_local_levels_name_length CHECK (length(name) <= 100),
    CONSTRAINT chk_local_levels_nepali_name_length CHECK (nepali_name IS NULL OR length(nepali_name) <= 100),
    -- Foreign keys
    CONSTRAINT fk_local_levels_district_id_districts
    FOREIGN KEY (district_id) REFERENCES districts(id)
    ON DELETE RESTRICT,
    CONSTRAINT fk_local_levels_type_id_local_level_types
    FOREIGN KEY (local_level_type_id) REFERENCES local_level_types(local_level_type_id)
    ON DELETE RESTRICT
);

-- Create indexes for local_levels table
CREATE INDEX idx_local_levels_name ON local_levels(name);
CREATE INDEX idx_local_levels_nepali_name ON local_levels(nepali_name);
CREATE INDEX idx_local_levels_district_id ON local_levels(district_id);
CREATE INDEX idx_local_levels_local_level_type_id ON local_levels(local_level_type_id);
