-- R003: add Nepali (Bikram Sambat) date projections to purchase_requisitions.
--
-- WHY
-- A Nepali-issued purchase requisition has to show and print a BS date. Previously
-- the only column was request_date (Gregorian), so every report, export and printed
-- document either re-derived the BS date in application code or - worse - had it
-- typed in by hand. The hand-typed route is exactly how master_fiscal_years ended up
-- with mismatched AD/BS pairs and how master_bs_calendar drifted by up to 7 days.
--
-- WHAT
-- request_date_np        the BS date as yyyy-MM-dd, e.g. '2083-06-16'
-- request_date_np_year   the BS year alone, so "fiscal year 2081/82" is an indexed
--                        integer comparison instead of a string prefix match
-- required_by_date_np    the same projection for the optional needed-by date
--
-- DESIGN: AD STAYS CANONICAL
-- request_date remains the source of truth. These three columns are derived and are
-- never written independently: PurchaseRequisition.syncNepaliDate() recomputes them
-- on every @PrePersist and @PreUpdate. So no code path - API, seeder, console, import
-- job - can persist a row whose AD and BS dates disagree. The columns exist so reports
-- do not each re-implement the conversion.
--
-- NOT NULL is deliberately NOT set on the NP columns. A request date older than
-- 1943-04-14 or later than 2034-04-13 falls outside the published calendar range and
-- cannot be projected; refusing to record real business data because the calendar
-- table has not been extended would be worse than a null projection. Such rows keep
-- a valid AD date and are filtered out by any query that needs the BS value.

ALTER TABLE purchase_requisitions
    ADD COLUMN IF NOT EXISTS request_date_np     varchar(10),
    ADD COLUMN IF NOT EXISTS request_date_np_year integer,
    ADD COLUMN IF NOT EXISTS required_by_date_np varchar(10);

-- Backfill from master_bs_calendar, which was reseeded and validated in R001
-- (7670 rows, BS 2000-2090, roundtrip 0 errors). Joining on ad_date is exact:
-- uk_master_bs_cal_ad_date is unique, so each requisition matches at most one row.
--
-- Rows outside the calendar range are simply left null by the LEFT JOIN, which is the
-- documented behaviour above rather than an oversight.
UPDATE purchase_requisitions pr
SET request_date_np     = cal.bs_date,
    request_date_np_year = cal.bs_year
FROM master_bs_calendar cal
WHERE pr.request_date = cal.ad_date
  AND (pr.request_date_np IS DISTINCT FROM cal.bs_date
       OR pr.request_date_np_year IS DISTINCT FROM cal.bs_year);

UPDATE purchase_requisitions pr
SET required_by_date_np = cal.bs_date
FROM master_bs_calendar cal
WHERE pr.required_by_date = cal.ad_date
  AND pr.required_by_date_np IS DISTINCT FROM cal.bs_date;

-- Supports "show me everything in BS year 2081/82" without scanning, mirroring the
-- existing idx_purchase_requisitions_fiscal_year on the AD side.
CREATE INDEX IF NOT EXISTS idx_purchase_requisitions_np_year
    ON purchase_requisitions (tenant_id, company_id, request_date_np_year);

COMMENT ON COLUMN purchase_requisitions.request_date_np IS
    'Bikram Sambat projection of request_date (yyyy-MM-dd). Derived, never authoritative: '
    'recomputed from request_date by PurchaseRequisition.syncNepaliDate() on every write. '
    'Null when request_date falls outside the published BS calendar range.';

COMMENT ON COLUMN purchase_requisitions.request_date_np_year IS
    'BS year of request_date, materialised so fiscal-year filtering is an indexed '
    'integer comparison. Derived from request_date; see request_date_np.';

COMMENT ON COLUMN purchase_requisitions.required_by_date_np IS
    'Bikram Sambat projection of required_by_date (yyyy-MM-dd). Derived and nullable '
    'because required_by_date itself is optional.';
