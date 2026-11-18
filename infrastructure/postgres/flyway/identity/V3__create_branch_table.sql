-- Create branches table
CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    branch_id UUID NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    branch_code VARCHAR(50) NOT NULL,
    branch_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    timezone VARCHAR(50),
    currency VARCHAR(3),
    manager_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_branches_currency CHECK (currency IS NULL OR length(currency) = 3),
    CONSTRAINT chk_branches_branch_code_length CHECK (length(branch_code) <= 50),
    CONSTRAINT chk_branches_branch_name_length CHECK (length(branch_name) <= 200)
);

-- Create indexes for branches table
CREATE INDEX idx_branches_branch_id ON branches(branch_id);
CREATE INDEX idx_branches_tenant_id ON branches(tenant_id);
CREATE INDEX idx_branches_branch_code ON branches(branch_code);
CREATE INDEX idx_branches_branch_name ON branches(branch_name);
CREATE INDEX idx_branches_status ON branches(status);
CREATE INDEX idx_branches_manager_id ON branches(manager_id);
CREATE INDEX idx_branches_country ON branches(country);
CREATE INDEX idx_branches_city ON branches(city);
CREATE INDEX idx_branches_currency ON branches(currency);
CREATE INDEX idx_branches_timezone ON branches(timezone);

-- Composite unique constraint for branch_code within tenant
CREATE UNIQUE INDEX uk_branches_tenant_branch_code
ON branches(tenant_id, branch_code);

-- Composite unique constraint for branch_name within tenant
CREATE UNIQUE INDEX uk_branches_tenant_branch_name
ON branches(tenant_id, branch_name);

-- Composite index for common queries
CREATE INDEX idx_branches_tenant_status
ON branches(tenant_id, status);

-- Add foreign key constraints
ALTER TABLE branches
ADD CONSTRAINT fk_branches_tenant_id_tenants
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT;

ALTER TABLE branches
ADD CONSTRAINT fk_branches_manager_id_users
FOREIGN KEY (manager_id) REFERENCES users(id)
ON DELETE SET NULL;
