-- Create permissions table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_id UUID NOT NULL UNIQUE,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    deactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_permissions_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_permissions_resource CHECK (resource IN (
        'PRODUCT', 'SALES', 'PURCHASE', 'INVENTORY', 'HR',
        'ACCOUNTING', 'CRM', 'AI', 'MANUFACTURING', 'PLATFORM',
        'REPORTING', 'INTEGRATION'
    )),
    CONSTRAINT chk_permissions_action CHECK (action IN (
        'CREATE', 'READ', 'UPDATE', 'DELETE', 'APPROVE', 'EXPORT',
        'IMPORT', 'MANAGE', 'ASSIGN', 'REPORT'
    ))
);

-- Create role_permissions table (many-to-many join entity)
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_permission_id UUID NOT NULL UNIQUE,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for permissions table
CREATE INDEX idx_permissions_permission_id ON permissions(permission_id);
CREATE INDEX idx_permissions_permission_code ON permissions(permission_code);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE INDEX idx_permissions_status ON permissions(status);
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);

-- Create indexes for role_permissions table
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_role_permissions_assigned_at ON role_permissions(assigned_at);

-- Composite index for common queries
CREATE INDEX idx_role_permissions_role_permission
ON role_permissions(role_id, permission_id);

-- Unique constraint to prevent duplicate assignments
CREATE UNIQUE INDEX uk_role_permissions_role_permission
ON role_permissions(role_id, permission_id);

-- Add foreign key constraints
ALTER TABLE role_permissions
ADD CONSTRAINT fk_role_permissions_role_id_roles
FOREIGN KEY (role_id) REFERENCES roles(id)
ON DELETE CASCADE;

ALTER TABLE role_permissions
ADD CONSTRAINT fk_role_permissions_permission_id_permissions
FOREIGN KEY (permission_id) REFERENCES permissions(id)
ON DELETE CASCADE;
