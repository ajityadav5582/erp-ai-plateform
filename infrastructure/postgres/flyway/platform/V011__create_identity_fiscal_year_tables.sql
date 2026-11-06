-- Migration: V011
-- Description: Create company_fiscal_years and master_fiscal_years tables.

CREATE TABLE IF NOT EXISTS master_fiscal_years (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    start_date_bs VARCHAR(10) NOT NULL,
    end_date_bs VARCHAR(10) NOT NULL,
    start_date_ad DATE NOT NULL,
    end_date_ad DATE NOT NULL,
    display_order SMALLINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_master_fy_name UNIQUE (name),
    CONSTRAINT uk_master_fy_code UNIQUE (code),
    CONSTRAINT uk_master_fy_display_order UNIQUE (display_order),
    CONSTRAINT check_master_ad_dates CHECK (start_date_ad < end_date_ad)
);
CREATE INDEX IF NOT EXISTS idx_master_fy_code ON master_fiscal_years (code);
CREATE INDEX IF NOT EXISTS idx_master_fy_ad_range ON master_fiscal_years (start_date_ad, end_date_ad);

CREATE TABLE IF NOT EXISTS company_fiscal_years (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    master_fiscal_year_id SMALLINT,
    start_date_bs VARCHAR(10),
    end_date_bs VARCHAR(10),
    start_date_ad DATE,
    end_date_ad DATE,
    start_date VARCHAR(10) NOT NULL,
    end_date VARCHAR(10) NOT NULL,
    calendar_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_fiscal_year_code UNIQUE (company_id, code),
    CONSTRAINT fk_cfy_master FOREIGN KEY (master_fiscal_year_id) REFERENCES master_fiscal_years(id)
);
CREATE INDEX IF NOT EXISTS idx_company_fiscal_years_company ON company_fiscal_years (company_id);
