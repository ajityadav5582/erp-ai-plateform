-- Add UI enhancement columns to pos_quick_items
ALTER TABLE pos_quick_items
    ADD COLUMN IF NOT EXISTS tab_name VARCHAR(100) NOT NULL DEFAULT 'All Items',
    ADD COLUMN IF NOT EXISTS page_number INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS item_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT',
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_tax_inclusive BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS hotkey VARCHAR(16);

-- Make item snapshot columns nullable for folder tiles
ALTER TABLE pos_quick_items
    ALTER COLUMN item_id DROP NOT NULL,
    ALTER COLUMN item_code DROP NOT NULL,
    ALTER COLUMN item_name DROP NOT NULL,
    ALTER COLUMN uom_code DROP NOT NULL;

-- Index for tab/page switching
CREATE INDEX IF NOT EXISTS idx_pos_quick_items_tab_page
    ON pos_quick_items (tenant_id, company_id, tab_name, page_number, is_active);
