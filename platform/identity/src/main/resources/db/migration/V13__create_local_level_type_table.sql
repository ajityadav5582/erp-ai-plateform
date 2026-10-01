-- Create local_level_types table
-- Migration: V13
-- Description: Creates the local_level_types reference table for
-- categorizing local bodies (e.g., Metropolitan City, Municipality,
-- Rural Municipality). This normalizes the local_body_type data.

CREATE TABLE local_level_types (
    id BIGSERIAL PRIMARY KEY,
    local_level_type_id VARCHAR(50) NOT NULL UNIQUE,
    local_level_type_code VARCHAR(20) NOT NULL UNIQUE,
    local_level_type_name VARCHAR(100) NOT NULL,
    nepali_name VARCHAR(100),
    country_code VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_local_level_types_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_local_level_types_code_length CHECK (length(local_level_type_code) <= 20),
    CONSTRAINT chk_local_level_types_name_length CHECK (length(local_level_type_name) <= 100),
    CONSTRAINT chk_local_level_types_nepali_name_length CHECK (nepali_name IS NULL OR length(nepali_name) <= 100),
    CONSTRAINT chk_local_level_types_country_code_length CHECK (country_code IS NULL OR length(country_code) = 3)
);

-- Create indexes for local_level_types table
CREATE INDEX idx_local_level_types_local_level_type_id ON local_level_types(local_level_type_id);
CREATE INDEX idx_local_level_types_local_level_type_code ON local_level_types(local_level_type_code);
CREATE INDEX idx_local_level_types_local_level_type_name ON local_level_types(local_level_type_name);
CREATE INDEX idx_local_level_types_nepali_name ON local_level_types(nepali_name);
CREATE INDEX idx_local_level_types_country_code ON local_level_types(country_code);
CREATE INDEX idx_local_level_types_status ON local_level_types(status);
