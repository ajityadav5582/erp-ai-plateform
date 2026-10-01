-- Add audit columns to tenants table
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
