-- R005: realign the purchase requisition status CHECK constraints with the enum.
--
-- WHY
-- Converting an approved requisition to a purchase order failed with
--   new row for relation "purchase_requisition_status_history"
--   violates check constraint "purchase_requisition_status_history_to_status_check"
-- The service was correct: it wrote to_status = 'CONVERTED_TO_PO', a real value of
-- PurchaseRequisitionStatus. The database disagreed.
--
-- The constraint still described an earlier generation of the lifecycle:
--
--   stale constraint          current enum
--   ------------------------  ------------------------
--   DRAFT                     DRAFT
--   SUBMITTED                 SUBMITTED
--   PENDING_APPROVAL          APPROVED
--   RETURNED                  REJECTED
--   APPROVED                  CANCELLED
--   PARTIALLY_ORDERED         CONVERTED_TO_PO
--   ORDERED
--   REJECTED
--   CANCELLED
--   CLOSED
--
-- So it was stale in both directions: it rejected CONVERTED_TO_PO, and it still
-- admitted five states the code can no longer represent.
--
-- WHY IT DRIFTED
-- These constraints were not written by hand. Hibernate's ddl-auto=update emitted
-- them when it first created the tables, from whatever the enum looked like that
-- day, and update only ever ADDs things - it never re-derives or replaces an
-- existing CHECK. Renaming and reshaping the enum therefore changed the code
-- silently and left the database alone. A fresh database gets the correct
-- constraint straight from the current enum; only databases that predate the
-- rename were ever wrong, which is why this looks like an environment problem
-- rather than a code problem.
--
-- The constraint is not a second source of truth about transitions. SQL cannot
-- express "an approved requisition may never return to draft" without a trigger,
-- so PurchaseRequisitionStatus.canTransitionTo remains the only place the
-- permitted edges are written down. This constraint guards the value set only.
--
-- WHAT
-- purchase_requisitions.status
-- purchase_requisition_status_history.from_status
-- purchase_requisition_status_history.to_status
--
--   all three realigned to DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED,
--   CONVERTED_TO_PO.
--
-- IDEMPOTENCE
-- This is a repeatable migration, so it must converge rather than merely succeed.
-- Each constraint is replaced only when the values it currently permits differ
-- from the expected set, and the comparison is made against the values parsed out
-- of the live definition rather than against the definition text, because
-- Postgres reformats that text between versions and a text comparison would
-- rewrite the constraint forever without ever settling. Once aligned, reruns are
-- no-ops and take no lock.
--
-- NO DATA IS REWRITTEN
-- The states being removed were already unreachable: no row can hold a value the
-- enum no longer has. If a legacy row ever did, the ADD below would fail rather
-- than silently coerce a real history record, which is the correct outcome - a
-- status trail is an audit record and should never be edited to satisfy a schema.

DO $block$
DECLARE
    -- PurchaseRequisitionStatus, in the order the enum declares it.
    expected_values constant text[] := ARRAY[
        'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED', 'CONVERTED_TO_PO'
    ];

    target record;
    current_values text[];
    value_list text;
    expected_sorted text[];
BEGIN
    -- Quote each element on its own. Joining into one string first and quoting
    -- the result instead would emit ('DRAFT'', ''SUBMITTED'', ...), whose
    -- elements are the text DRAFT' and 'SUBMITTED - literals no row can ever
    -- equal, so the constraint would reject valid data while looking correct.
    SELECT string_agg(quote_literal(v), ', ' ORDER BY ord)
    INTO value_list
    FROM unnest(expected_values) WITH ORDINALITY AS t(v, ord);

    -- Compare as a SET, not as a positional list. The values parsed off the live
    -- definition come back sorted, while expected_values is in enum declaration
    -- order, and array equality is element-by-element - comparing the two
    -- directly would therefore always differ, and the constraint would be dropped
    -- and rebuilt on every single run instead of settling after the first.
    SELECT array_agg(v ORDER BY v)
    INTO expected_sorted
    FROM unnest(expected_values) AS t(v);

    FOR target IN
        SELECT *
        FROM (VALUES
            -- table, constraint, column, whether the column is nullable
            ('purchase_requisitions',
             'purchase_requisitions_status_check',
             'status',
             false),
            ('purchase_requisition_status_history',
             'purchase_requisition_status_history_from_status_check',
             'from_status',
             true),
            ('purchase_requisition_status_history',
             'purchase_requisition_status_history_to_status_check',
             'to_status',
             false)
        ) AS t(table_name, constraint_name, column_name, nullable)
    LOOP
        -- Parse the literals the live constraint actually permits. The pattern
        -- skips the '::character varying' casts that Postgres appends.
        SELECT array_agg(matched[1] ORDER BY matched[1])
        INTO current_values
        FROM pg_constraint c
        CROSS JOIN LATERAL regexp_matches(
            pg_get_constraintdef(c.oid), '''([A-Z_]+)''', 'g'
        ) AS matched
        WHERE c.conname = target.constraint_name;

        -- Missing constraint, or one permitting the wrong set: replace it.
        IF current_values IS DISTINCT FROM expected_sorted THEN
            EXECUTE format(
                'ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
                target.table_name, target.constraint_name
            );

            -- A CHECK rejects only when its expression is FALSE, so a bare
            -- IN (...) would already admit NULL. Stating it explicitly keeps
            -- the nullable from_status honest about intent, and keeps the
            -- "nothing to check" case from depending on three-valued logic.
            IF target.nullable THEN
                EXECUTE format(
                    'ALTER TABLE %I ADD CONSTRAINT %I CHECK (%I IS NULL OR %I IN (%s))',
                    target.table_name,
                    target.constraint_name,
                    target.column_name,
                    target.column_name,
                    value_list
                );
            ELSE
                EXECUTE format(
                    'ALTER TABLE %I ADD CONSTRAINT %I CHECK (%I IN (%s))',
                    target.table_name,
                    target.constraint_name,
                    target.column_name,
                    value_list
                );
            END IF;

            RAISE NOTICE 'R005: realigned %s.%s (was %s)',
                target.table_name, target.constraint_name,
                coalesce(array_to_string(current_values, ', '), '<absent>');
        END IF;
    END LOOP;
END
$block$;

COMMENT ON CONSTRAINT purchase_requisitions_status_check ON purchase_requisitions IS
    'Value set of PurchaseRequisitionStatus. Transitions are owned by the aggregate, '
    'not by this constraint: SQL cannot express a lifecycle edge without a trigger.';

COMMENT ON CONSTRAINT purchase_requisition_status_history_from_status_check
    ON purchase_requisition_status_history IS
    'Value set of PurchaseRequisitionStatus. Nullable: the row recording a '
    'requisition''s creation has no prior state.';

COMMENT ON CONSTRAINT purchase_requisition_status_history_to_status_check
    ON purchase_requisition_status_history IS
    'Value set of PurchaseRequisitionStatus. This is the constraint that rejected '
    'CONVERTED_TO_PO before R005.';
