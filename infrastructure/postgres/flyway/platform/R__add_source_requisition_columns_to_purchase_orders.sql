-- R004: record the origin requisition on converted purchase orders and their lines.
--
-- WHY
-- Conversion created a purchase order that carried no trace of the requisition it
-- came from. purchase_requisition_order_links already relate the two documents, but
-- only once an order award has been recorded by hand - an order raised by converting
-- an approved requisition carried no source, so "why was this bought" was only
-- answerable by reading the requisition separately and guessing from the dates.
--
-- WHAT
-- purchase_orders.source_purchase_requisition_id        the requisition converted
-- purchase_orders.branch_id                              owning branch, copied across
-- purchase_order_lines.source_purchase_requisition_item_id  the requisition line each
--                                                         order line was copied from
--
-- DUPLICATE CONVERSION
-- uk_purchase_orders_source_requisition makes a second conversion of the same
-- requisition impossible at the database level, not only in the service. The service
-- check is a plain SELECT and therefore races: two concurrent requests both see no
-- order, both build one, and the loser would otherwise commit a second order. The
-- unique key makes the loser fail instead.
--
-- NULLs ARE EXEMPT
-- Postgres treats NULL as distinct in a unique constraint, so orders raised without a
-- requisition (source id null) are unaffected and any number of them may coexist.
-- That is why the column is nullable rather than pointing at a synthetic row.

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS source_purchase_requisition_id bigint,
    ADD COLUMN IF NOT EXISTS branch_id bigint;

ALTER TABLE purchase_order_lines
    ADD COLUMN IF NOT EXISTS source_purchase_requisition_item_id bigint;

-- Backfill branch_id from the source requisition so converted orders are already
-- branch-scoped without a rewrite. Orders with no requisition keep a null branch.
UPDATE purchase_orders po
SET branch_id = pr.branch_id
FROM purchase_requisitions pr
WHERE po.source_purchase_requisition_id = pr.id
  AND po.branch_id IS DISTINCT FROM pr.branch_id;

-- Backfill the line origins from the award links already on file. A requisition line
-- may have several links (a split award across suppliers), so the earliest wins and
-- any further ones are left null rather than guessing which one this line came from.
UPDATE purchase_order_lines pol
SET source_purchase_requisition_item_id = first_link.purchase_requisition_line_id
FROM (
    SELECT DISTINCT ON (purchase_order_line_id)
           purchase_order_line_id,
           purchase_requisition_line_id
    FROM purchase_requisition_order_links
    WHERE purchase_order_line_id IS NOT NULL
    ORDER BY purchase_order_line_id, linked_at, id
) first_link
WHERE pol.id = first_link.purchase_order_line_id
  AND pol.source_purchase_requisition_item_id IS DISTINCT FROM first_link.purchase_requisition_line_id;

-- Named constraint, matching the @UniqueConstraint on the entity, so that Hibernate's
-- ddl-auto=update and this migration converge on the same object rather than the
-- service trying to create a second, differently named one on every start.
DO $block$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_purchase_orders_source_requisition'
    ) THEN
        ALTER TABLE purchase_orders
            ADD CONSTRAINT uk_purchase_orders_source_requisition
            UNIQUE (tenant_id, company_id, source_purchase_requisition_id);
    END IF;
END
$block$;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_source_requisition
    ON purchase_orders (tenant_id, company_id, source_purchase_requisition_id);

-- Answers "which order line satisfied this requisition line?" without a join, which
-- is the trace query conversion exists to support.
CREATE INDEX IF NOT EXISTS idx_purchase_order_lines_source_item
    ON purchase_order_lines (tenant_id, company_id, source_purchase_requisition_item_id);

COMMENT ON COLUMN purchase_orders.source_purchase_requisition_id IS
    'The approved purchase requisition this order was converted from. Null for an order '
    'raised directly. Unique within (tenant_id, company_id), which is what makes a second '
    'conversion of the same requisition impossible.';

COMMENT ON COLUMN purchase_orders.branch_id IS
    'Owning branch, copied from the source requisition at conversion time.';

COMMENT ON COLUMN purchase_order_lines.source_purchase_requisition_item_id IS
    'The requisition line this order line was converted from. Null for a line raised '
    'directly on the order. Backfilled from purchase_requisition_order_links where an '
    'award already carried a purchase-order line reference.';
