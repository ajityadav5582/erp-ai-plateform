-- Create tenants table in identity database if it doesn't exist
-- This table is referenced by the users table foreign key (V1) and must be
-- created BEFORE any migration that adds a foreign key to tenants(id).
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE,
    tenant_code VARCHAR(100) NOT NULL UNIQUE,
    legal_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    isolation_strategy VARCHAR(30) NOT NULL,
    timezone VARCHAR(50),
    currency VARCHAR(3),
    language VARCHAR(10),
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    subscription_plan_id UUID,
    activated_at TIMESTAMP,
    suspended_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id ON tenants(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_code ON tenants(tenant_code);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_isolation_strategy ON tenants(isolation_strategy);

-- Insert default tenant if not exists
INSERT INTO tenants (tenant_id, tenant_code, legal_name, display_name, email, status, isolation_strategy, timezone, currency, language, activated_at, version)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'default',
    'Default Tenant',
    'Default Tenant',
    'admin@erpai.com',
    'ACTIVE',
    'SCHEMA_PER_TENANT',
    'Asia/Kathmandu',
    'NPR',
    'en',
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (tenant_code) DO NOTHING;
