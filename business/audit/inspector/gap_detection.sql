-- Detect gaps in invoice sequence numbers per company / fiscal year.
-- (Copy for IRD Auditor Inspection Mode path business/audit/inspector/)
WITH parsed AS (
    SELECT
        company_id,
        fiscal_year_id,
        transaction_number,
        (regexp_match(transaction_number, '(\d+)$'))[1]::bigint AS seq_no
    FROM pos_transactions
    WHERE company_id = :companyId
      AND (:fiscalYearId IS NULL OR fiscal_year_id = :fiscalYearId)
      AND status IN ('COMPLETED', 'CREDIT_NOTE', 'VOIDED')
),
bounds AS (
    SELECT company_id, fiscal_year_id, MIN(seq_no) AS min_seq, MAX(seq_no) AS max_seq, COUNT(*) AS issued_count
    FROM parsed
    GROUP BY company_id, fiscal_year_id
),
series AS (
    SELECT b.company_id, b.fiscal_year_id, g.seq AS expected_seq
    FROM bounds b
    CROSS JOIN LATERAL generate_series(b.min_seq, b.max_seq) AS g(seq)
)
SELECT s.company_id, s.fiscal_year_id, s.expected_seq AS missing_sequence_number
FROM series s
LEFT JOIN parsed p
       ON p.company_id = s.company_id
      AND p.fiscal_year_id IS NOT DISTINCT FROM s.fiscal_year_id
      AND p.seq_no = s.expected_seq
WHERE p.seq_no IS NULL
ORDER BY s.company_id, s.fiscal_year_id, s.expected_seq;
