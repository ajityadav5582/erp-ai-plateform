-- Create departments table
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    department_id UUID NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    department_code VARCHAR(50) NOT NULL,
    department_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    manager_id BIGINT,
    parent_department_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_departments_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_departments_department_code_length CHECK (length(department_code) <= 50),
    CONSTRAINT chk_departments_department_name_length CHECK (length(department_name) <= 200),
    CONSTRAINT chk_departments_parent_not_self CHECK (id != parent_department_id)
);

-- Create indexes for departments table
CREATE INDEX idx_departments_department_id ON departments(department_id);
CREATE INDEX idx_departments_tenant_id ON departments(tenant_id);
CREATE INDEX idx_departments_branch_id ON departments(branch_id);
CREATE INDEX idx_departments_department_code ON departments(department_code);
CREATE INDEX idx_departments_department_name ON departments(department_name);
CREATE INDEX idx_departments_status ON departments(status);
CREATE INDEX idx_departments_manager_id ON departments(manager_id);
CREATE INDEX idx_departments_parent_department_id ON departments(parent_department_id);

-- Composite unique constraint for department_code within branch
CREATE UNIQUE INDEX uk_departments_branch_department_code
ON departments(branch_id, department_code);

-- Composite unique constraint for department_name within branch
CREATE UNIQUE INDEX uk_departments_branch_department_name
ON departments(branch_id, department_name);

-- Composite index for common queries
CREATE INDEX idx_departments_tenant_branch
ON departments(tenant_id, branch_id);

CREATE INDEX idx_departments_branch_status
ON departments(branch_id, status);

-- Add foreign key constraints
ALTER TABLE departments
ADD CONSTRAINT fk_departments_tenant_id_tenants
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT;

ALTER TABLE departments
ADD CONSTRAINT fk_departments_branch_id_branches
FOREIGN KEY (branch_id) REFERENCES branches(id)
ON DELETE RESTRICT;

ALTER TABLE departments
ADD CONSTRAINT fk_departments_manager_id_users
FOREIGN KEY (manager_id) REFERENCES users(id)
ON DELETE SET NULL;

ALTER TABLE departments
ADD CONSTRAINT fk_departments_parent_department_id_departments
FOREIGN KEY (parent_department_id) REFERENCES departments(id)
ON DELETE RESTRICT;
