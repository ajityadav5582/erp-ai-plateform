-- Create districts table
-- Migration: V11
-- Description: Creates the districts table for geographic management.
-- Districts belong to provinces and are used to further organize
-- branches, departments, and users by geographic region.

CREATE TABLE districts (
    id BIGSERIAL PRIMARY KEY,
    district_code VARCHAR(20) NOT NULL UNIQUE,
    district_name VARCHAR(100) NOT NULL,
    nepali_name VARCHAR(100),
    province_id BIGINT NOT NULL,
    country_code VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_districts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_districts_district_code_length CHECK (length(district_code) <= 20),
    CONSTRAINT chk_districts_district_name_length CHECK (length(district_name) <= 100),
    CONSTRAINT chk_districts_nepali_name_length CHECK (nepali_name IS NULL OR length(nepali_name) <= 100),
    CONSTRAINT chk_districts_country_code_length CHECK (country_code IS NULL OR length(country_code) = 3),
    -- Foreign keys
    CONSTRAINT fk_districts_province_id_provinces
    FOREIGN KEY (province_id) REFERENCES provinces(id)
    ON DELETE RESTRICT
);

-- Create indexes for districts table
CREATE INDEX idx_districts_district_code ON districts(district_code);
CREATE INDEX idx_districts_district_name ON districts(district_name);
CREATE INDEX idx_districts_nepali_name ON districts(nepali_name);
CREATE INDEX idx_districts_province_id ON districts(province_id);
CREATE INDEX idx_districts_country_code ON districts(country_code);
CREATE INDEX idx_districts_status ON districts(status);

-- Composite index for common queries
CREATE INDEX idx_districts_province_status
ON districts(province_id, status);
