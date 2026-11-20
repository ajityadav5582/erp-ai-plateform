-- Create the shared fiscal-year reference table before V43 adds its foreign key.
-- Platform V011 uses CREATE TABLE IF NOT EXISTS and adds the company-specific
-- fiscal-year table later in the platform migration chain.
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
