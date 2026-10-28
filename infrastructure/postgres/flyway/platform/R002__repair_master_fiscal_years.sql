BEGIN;
UPDATE master_fiscal_years f
SET start_date_ad = c.ad_date
FROM master_bs_calendar c
WHERE c.bs_date = f.start_date_bs
  AND f.start_date_ad IS DISTINCT FROM c.ad_date;

UPDATE master_fiscal_years f
SET end_date_ad = c.ad_date
FROM master_bs_calendar c
WHERE c.bs_date = f.end_date_bs
  AND f.end_date_ad IS DISTINCT FROM c.ad_date;

UPDATE master_bs_calendar c
SET master_fiscal_year_id = f.id
FROM master_fiscal_years f
WHERE c.ad_date >= f.start_date_ad
  AND c.ad_date <= f.end_date_ad;
COMMIT;

-- =====================================================================
-- master_fiscal_years repair notes
--
-- FY 2081/82 and FY 2082/83 stored end_date_ad one day later than the
-- Gregorian date actually matching their end_date_bs. Any report that
-- filtered on end_date_ad would have included one extra day of the next
-- fiscal year. Both are realigned here from the repaired calendar.
--
-- The final statement re-links every calendar day to the fiscal year that
-- actually contains it, which the calendar reseed had nulled out.
-- =====================================================================
