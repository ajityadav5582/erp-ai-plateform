-- Add branch and department references to users table
-- Migration: V5
-- Description: Adds branch_id and department_id columns to users table for organizational structure

-- Add branch_id column (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'branch_id'
    ) THEN
        ALTER TABLE users ADD COLUMN branch_id BIGINT;
    END IF;
END $$;

-- Add department_id column (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'department_id'
    ) THEN
        ALTER TABLE users ADD COLUMN department_id BIGINT;
    END IF;
END $$;

-- Add foreign key constraint for branch_id (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_branch'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_branch
            FOREIGN KEY (branch_id) REFERENCES branches(id)
            ON DELETE SET NULL
            ON UPDATE CASCADE;
    END IF;
END $$;

-- Add foreign key constraint for department_id (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_department'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_department
            FOREIGN KEY (department_id) REFERENCES departments(id)
            ON DELETE SET NULL
            ON UPDATE CASCADE;
    END IF;
END $$;

-- Create index for branch_id for efficient filtering (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_branch_id ON users(branch_id);

-- Create index for department_id for efficient filtering (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);

-- Create composite index for tenant + branch queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_tenant_branch ON users(tenant_id, branch_id);

-- Create composite index for tenant + department queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_tenant_department ON users(tenant_id, department_id);
