-- Migration: V43
-- Description: Create master_bs_calendar table for Bikram Sambat (BS) to Gregorian (AD) date mapping.

CREATE TABLE IF NOT EXISTS master_bs_calendar (
    id BIGSERIAL PRIMARY KEY,
    ad_date DATE NOT NULL UNIQUE,
    bs_date VARCHAR(10) NOT NULL UNIQUE,
    bs_year INT NOT NULL,
    bs_month INT NOT NULL,
    bs_day INT NOT NULL,
    bs_month_name_np VARCHAR(50) NOT NULL,
    bs_month_name_en VARCHAR(50) NOT NULL,
    day_of_week INT NOT NULL,
    day_name_en VARCHAR(20) NOT NULL,
    day_name_np VARCHAR(50) NOT NULL,
    is_holiday BOOLEAN NOT NULL DEFAULT FALSE,
    master_fiscal_year_id SMALLINT REFERENCES master_fiscal_years(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bs_calendar_bs_month CHECK (bs_month >= 1 AND bs_month <= 12),
    CONSTRAINT chk_bs_calendar_bs_day CHECK (bs_day >= 1 AND bs_day <= 32),
    CONSTRAINT chk_bs_calendar_day_of_week CHECK (day_of_week >= 1 AND day_of_week <= 7)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_master_bs_cal_ad_date ON master_bs_calendar(ad_date);
CREATE INDEX IF NOT EXISTS idx_master_bs_cal_bs_date ON master_bs_calendar(bs_date);
CREATE INDEX IF NOT EXISTS idx_master_bs_cal_bs_year_month ON master_bs_calendar(bs_year, bs_month);
CREATE INDEX IF NOT EXISTS idx_master_bs_cal_fy_id ON master_bs_calendar(master_fiscal_year_id);
