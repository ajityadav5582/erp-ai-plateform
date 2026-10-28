-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    job_title VARCHAR(100),
    profile_image_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    last_login_at TIMESTAMP,
    locked_at TIMESTAMP,
    lock_reason VARCHAR(500),
    activated_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_users_status CHECK (status IN (
        'PENDING_ACTIVATION',
        'ACTIVE',
        'LOCKED',
        'DEACTIVATED',
        'ARCHIVED'
    ))
);

-- Create roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_id UUID NOT NULL UNIQUE,
    tenant_id BIGINT,
    role_code VARCHAR(100) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    role_type VARCHAR(20) NOT NULL,
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    deactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    -- Constraints
    CONSTRAINT chk_roles_role_type CHECK (role_type IN ('SYSTEM', 'CUSTOM'))
);

-- Create user_roles table (many-to-many join entity)
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_role_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    is_primary_role BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    revoke_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for users table
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);

-- Unique constraint for username per tenant
CREATE UNIQUE INDEX uk_users_tenant_username
ON users(tenant_id, username);

-- Unique constraint for email per tenant
CREATE UNIQUE INDEX uk_users_tenant_email
ON users(tenant_id, email);

-- Create indexes for roles table
CREATE INDEX idx_roles_role_id ON roles(role_id);
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX idx_roles_role_code ON roles(role_code);
CREATE INDEX idx_roles_role_type ON roles(role_type);

-- Unique constraint for role_code per tenant (custom roles)
CREATE UNIQUE INDEX uk_roles_tenant_role_code
ON roles(tenant_id, role_code)
WHERE tenant_id IS NOT NULL;

-- Unique constraint for global role_code (system roles)
CREATE UNIQUE INDEX uk_roles_global_role_code
ON roles(role_code)
WHERE tenant_id IS NULL;

-- Create indexes for user_roles table
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_tenant_id ON user_roles(tenant_id);
CREATE INDEX idx_user_roles_assigned_at ON user_roles(assigned_at);
CREATE INDEX idx_user_roles_expires_at ON user_roles(expires_at);
CREATE INDEX idx_user_roles_revoked_at ON user_roles(revoked_at);

-- Composite index for common queries
CREATE INDEX idx_user_roles_user_active
ON user_roles(user_id, assigned_at DESC)
WHERE revoked_at IS NULL;

CREATE INDEX idx_user_roles_role_active
ON user_roles(role_id, assigned_at DESC)
WHERE revoked_at IS NULL;

-- Unique constraint to prevent duplicate active assignments
CREATE UNIQUE INDEX uk_user_roles_user_role_active
ON user_roles(user_id, role_id)
WHERE revoked_at IS NULL;

-- Add foreign key constraints
ALTER TABLE users
ADD CONSTRAINT fk_users_tenant_id_tenants
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT;

ALTER TABLE roles
ADD CONSTRAINT fk_roles_tenant_id_tenants
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT;

ALTER TABLE user_roles
ADD CONSTRAINT fk_user_roles_user_id_users
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE;

ALTER TABLE user_roles
ADD CONSTRAINT fk_user_roles_role_id_roles
FOREIGN KEY (role_id) REFERENCES roles(id)
ON DELETE CASCADE;

ALTER TABLE user_roles
ADD CONSTRAINT fk_user_roles_tenant_id_tenants
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT;
