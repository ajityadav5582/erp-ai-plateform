-- Migration: V010
-- Description: Create inventory module tables (categories, units, warehouses,
-- suppliers, items, stock_balances, stock_movements) that were previously
-- created only by Hibernate ddl-auto:update.

-- categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    parent_id BIGINT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_category_slug UNIQUE (company_id, slug),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_categories_tenant_company ON categories (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories (parent_id);

-- units
CREATE TABLE IF NOT EXISTS inventory_units (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    dimension VARCHAR(32) NOT NULL,
    symbol VARCHAR(16),
    decimal_scale INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_unit_code UNIQUE (company_id, code)
);
CREATE INDEX IF NOT EXISTS idx_inventory_units_tenant_company ON inventory_units (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_inventory_units_dimension ON inventory_units (tenant_id, company_id, dimension);

-- warehouses
CREATE TABLE IF NOT EXISTS warehouses (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    branch_id BIGINT,
    address VARCHAR(500),
    phone VARCHAR(32),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    default_marker INTEGER,
    allow_negative_stock BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_warehouses_tenant_company_code UNIQUE (tenant_id, company_id, code),
    CONSTRAINT uk_warehouses_tenant_company_default UNIQUE (tenant_id, company_id, default_marker)
);
CREATE INDEX IF NOT EXISTS idx_warehouses_tenant_company ON warehouses (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_warehouses_branch ON warehouses (tenant_id, company_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_warehouses_default ON warehouses (tenant_id, company_id, is_default);

-- suppliers
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(32),
    address TEXT,
    tax_id VARCHAR(64),
    vat_registration_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    vat_registration_number VARCHAR(64),
    vat_effective_from DATE,
    vat_effective_to DATE,
    payment_terms_days INTEGER,
    currency VARCHAR(8),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_supplier_code UNIQUE (company_id, code),
    CONSTRAINT uk_company_supplier_name UNIQUE (company_id, name)
);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_company ON suppliers (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_type ON suppliers (tenant_id, company_id, type);

-- items
CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    hs_code VARCHAR(20),
    name_en VARCHAR(255) NOT NULL,
    name_np VARCHAR(255),
    description_en TEXT,
    description_np TEXT,
    category_id BIGINT,
    uom_id BIGINT NOT NULL,
    item_type VARCHAR(20) NOT NULL DEFAULT 'GOODS',
    taxability_type VARCHAR(20) NOT NULL DEFAULT 'TAXABLE',
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00,
    excise_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    excise_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    excise_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    selling_price NUMERIC(15,4) NOT NULL,
    purchase_price NUMERIC(15,4),
    mrp NUMERIC(15,4),
    is_vat_inclusive BOOLEAN NOT NULL DEFAULT FALSE,
    is_inventory_tracked BOOLEAN NOT NULL DEFAULT TRUE,
    is_batch_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    is_expiry_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    min_stock_level NUMERIC(15,4) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(15,4) NOT NULL DEFAULT 0,
    reorder_quantity NUMERIC(15,4) NOT NULL DEFAULT 0,
    is_purchasable BOOLEAN NOT NULL DEFAULT FALSE,
    is_sellable BOOLEAN NOT NULL DEFAULT FALSE,
    is_component BOOLEAN NOT NULL DEFAULT FALSE,
    is_manufactured BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_item_sku UNIQUE (company_id, sku),
    CONSTRAINT uk_company_item_barcode UNIQUE (company_id, barcode),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_items_uom FOREIGN KEY (uom_id) REFERENCES inventory_units(id)
);
CREATE INDEX IF NOT EXISTS idx_items_tenant_company ON items (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items (category_id);
CREATE INDEX IF NOT EXISTS idx_items_uom ON items (uom_id);
CREATE INDEX IF NOT EXISTS idx_items_hs_code ON items (hs_code);
CREATE INDEX IF NOT EXISTS idx_items_taxability ON items (taxability_type);
CREATE INDEX IF NOT EXISTS idx_items_status ON items (company_id, is_active);

-- stock_balances
CREATE TABLE IF NOT EXISTS stock_balances (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    on_hand_qty NUMERIC(19,6) NOT NULL DEFAULT 0,
    reserved_qty NUMERIC(19,6) NOT NULL DEFAULT 0,
    last_movement_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_balances_warehouse_item_unit UNIQUE (tenant_id, company_id, warehouse_id, item_id, unit_id)
);
CREATE INDEX IF NOT EXISTS idx_stock_balances_tenant_company ON stock_balances (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_warehouse ON stock_balances (tenant_id, company_id, warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_item ON stock_balances (tenant_id, company_id, item_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_low_stock ON stock_balances (tenant_id, company_id, on_hand_qty);

-- stock_movements
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    reference_number VARCHAR(100),
    movement_date TIMESTAMP NOT NULL,
    posted_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_movements_reference_direction UNIQUE (tenant_id, company_id, reference_type, reference_id, movement_type, item_id, unit_id, warehouse_id)
);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_company ON stock_movements (tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_warehouse_item_unit ON stock_movements (tenant_id, company_id, warehouse_id, item_id, unit_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_reference ON stock_movements (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_date ON stock_movements (movement_date);

-- FKs to tenants/companies (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='categories' AND constraint_name='fk_categories_tenant') THEN
        ALTER TABLE categories ADD CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT;
    END IF;
EXCEPTION WHEN others THEN NULL;
END $$;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='categories' AND constraint_name='fk_categories_company') THEN
        ALTER TABLE categories ADD CONSTRAINT fk_categories_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT;
    END IF;
EXCEPTION WHEN others THEN NULL;
END $$;
