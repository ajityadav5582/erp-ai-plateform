-- Create provinces table
-- Migration: V9
-- Description: Creates the provinces table for geographic management.
-- This table stores province/state data used to organize branches,
-- departments, and users by geographic region.

CREATE TABLE provinces (
    id BIGSERIAL PRIMARY KEY,
    province_code VARCHAR(20) NOT NULL UNIQUE,
    province_name VARCHAR(100) NOT NULL,
    nepali_name VARCHAR(100),
    country_code VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_provinces_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_provinces_province_code_length CHECK (length(province_code) <= 20),
    CONSTRAINT chk_provinces_province_name_length CHECK (length(province_name) <= 100),
    CONSTRAINT chk_provinces_nepali_name_length CHECK (nepali_name IS NULL OR length(nepali_name) <= 100),
    CONSTRAINT chk_provinces_country_code_length CHECK (country_code IS NULL OR length(country_code) = 3)
);

-- Create indexes for provinces table
CREATE INDEX idx_provinces_province_code ON provinces(province_code);
CREATE INDEX idx_provinces_province_name ON provinces(province_name);
CREATE INDEX idx_provinces_nepali_name ON provinces(nepali_name);
CREATE INDEX idx_provinces_country_code ON provinces(country_code);
CREATE INDEX idx_provinces_status ON provinces(status);
