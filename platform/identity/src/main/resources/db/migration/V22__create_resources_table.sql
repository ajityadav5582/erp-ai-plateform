-- Create resources table
-- Migration: V22
-- Description: Creates the resources table for resource (module) master data.
-- This table stores business module/domain definitions used to categorize
-- permissions and organize the platform's functional areas.
-- Resources are master data shared across tenants and are therefore
-- not tenant-scoped.

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    resource_code VARCHAR(50) NOT NULL UNIQUE,
    resource_name VARCHAR(100) NOT NULL,
    module VARCHAR(50),
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_resources_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_resources_resource_code_length CHECK (length(resource_code) <= 50),
    CONSTRAINT chk_resources_resource_name_length CHECK (length(resource_name) <= 100),
    CONSTRAINT chk_resources_module_length CHECK (module IS NULL OR length(module) <= 50),
    CONSTRAINT chk_resources_description_length CHECK (description IS NULL OR length(description) <= 500)
);

-- Create indexes for resources table
CREATE INDEX idx_resources_resource_code ON resources(resource_code);
CREATE INDEX idx_resources_resource_name ON resources(resource_name);
CREATE INDEX idx_resources_module ON resources(module);
CREATE INDEX idx_resources_status ON resources(status);
