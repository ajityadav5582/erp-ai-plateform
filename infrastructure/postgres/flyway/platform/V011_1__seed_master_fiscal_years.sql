-- Seed the global master fiscal years so the table is populated immediately
-- after a database reset, independent of application-level seeding.
-- Idempotent: safe to re-run on upgrades (ON CONFLICT DO NOTHING).

INSERT INTO master_fiscal_years (name, code, start_date_bs, end_date_bs, start_date_ad, end_date_ad, display_order, is_active)
VALUES
    ('FY 2078/79', '2078-79', '2078-04-01', '2079-03-32', '2021-07-16', '2022-07-16', 1, true),
    ('FY 2079/80', '2079-80', '2079-04-01', '2080-03-31', '2022-07-17', '2023-07-16', 2, true),
    ('FY 2080/81', '2080-81', '2080-04-01', '2081-03-31', '2023-07-17', '2024-07-15', 3, true),
    ('FY 2081/82', '2081-82', '2081-04-01', '2082-03-31', '2024-07-16', '2025-07-16', 4, true),
    ('FY 2082/83', '2082-83', '2082-04-01', '2083-03-31', '2025-07-17', '2026-07-16', 5, true),
    ('FY 2083/84', '2083-84', '2083-04-01', '2084-03-31', '2026-07-17', '2027-07-16', 6, true)
ON CONFLICT (code) DO NOTHING;
