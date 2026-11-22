-- V052: Add Wholesale Price, Minimum Selling Price, Secondary UOM, and Conversion Factor to Items
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS wholesale_price NUMERIC(15, 4),
    ADD COLUMN IF NOT EXISTS min_selling_price NUMERIC(15, 4),
    ADD COLUMN IF NOT EXISTS secondary_uom_id BIGINT REFERENCES inventory_units(id),
    ADD COLUMN IF NOT EXISTS conversion_factor NUMERIC(15, 4);

CREATE INDEX IF NOT EXISTS idx_items_secondary_uom ON items(secondary_uom_id);

