-- Migration: V032
-- Description: Add foreign key from branches.default_warehouse_id to warehouses(id)
-- (warehouses table is created in V010, so this migration runs after it).

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'branches' AND constraint_name = 'fk_branches_default_warehouse'
    ) THEN
        ALTER TABLE branches
            ADD CONSTRAINT fk_branches_default_warehouse
            FOREIGN KEY (default_warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL;
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
