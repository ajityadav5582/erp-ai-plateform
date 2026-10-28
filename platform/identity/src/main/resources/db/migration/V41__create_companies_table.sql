-- Migration: V41
-- Description: Introduce the Company entity to support the new business model
-- where one Tenant can own multiple Companies. Companies are kept as separate
-- entities from Tenant. Registration no longer creates a Company; the Tenant
-- Admin creates Companies after login.

-- ============================================================
-- 1. Create companies table
-- ============================================================
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    company_code VARCHAR(50) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(200),
    address VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_companies_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_companies_company_code_length CHECK (length(company_code) <= 50),
    CONSTRAINT chk_companies_company_name_length CHECK (length(company_name) <= 200)
);

-- Indexes for companies table
CREATE INDEX IF NOT EXISTS idx_companies_company_id ON companies(company_id);
CREATE INDEX IF NOT EXISTS idx_companies_tenant_id ON companies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_companies_company_code ON companies(company_code);
CREATE INDEX IF NOT EXISTS idx_companies_company_name ON companies(company_name);
CREATE INDEX IF NOT EXISTS idx_companies_status ON companies(status);

-- Composite unique constraints for company_code / company_name within tenant
CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_tenant_company_code
    ON companies(tenant_id, company_code);

CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_tenant_company_name
    ON companies(tenant_id, company_name);

-- Composite index for common queries
CREATE INDEX IF NOT EXISTS idx_companies_tenant_status
    ON companies(tenant_id, status);

-- Foreign key constraint
ALTER TABLE companies
    ADD CONSTRAINT IF NOT EXISTS fk_companies_tenant_id_tenants
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
    ON DELETE RESTRICT;

-- ============================================================
-- 2. Add company_id reference to users table (nullable)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'company_id'
    ) THEN
        ALTER TABLE users ADD COLUMN company_id BIGINT;
    END IF;
END $$;

-- Foreign key constraint for company_id (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_company'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_company
            FOREIGN KEY (company_id) REFERENCES companies(id)
            ON DELETE SET NULL
            ON UPDATE CASCADE;
    END IF;
END $$;

-- Create index for company_id for efficient filtering (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);

-- Create composite index for tenant + company queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_tenant_company ON users(tenant_id, company_id);
