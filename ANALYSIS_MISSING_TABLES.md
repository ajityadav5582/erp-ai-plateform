# Analysis: Tables Not Created for Entities / Domain Classes

## Summary

A cross-check of every JPA `@Entity` class against every `CREATE TABLE` statement found in the
repository (Flyway migrations under `infrastructure/postgres/flyway/**` and
`platform/identity/src/main/resources/db/migration/**`, plus standalone SQL scripts) shows that
**most business-domain entities have no corresponding table-creation migration**. Only the
`platform/identity` module (and a handful of POS tables) are covered by Flyway. All other
modules rely on Hibernate `ddl-auto: update`, which creates tables at runtime in dev but is
not a reliable production schema-management strategy.

- Total `@Entity` classes found: **78**
- Unique entity table names: **73**
- Tables created by any SQL migration: **29**
- Entity tables with **no** `CREATE TABLE` anywhere: **44** (listed below)

## Issues Found

### 1. No Flyway migrations for whole business modules
The central Flyway location (`infrastructure/postgres/flyway/platform`) only contains migrations
for identity/platform tables plus a few POS tables. There are **no** migration folders/files for:
- `business/inventory` (7 entities)
- `business/sales` (13 entities)
- `business/procurement` (15 entities)
- `business/pos` core (11 entities, excluding the ones already migrated)
- `integration` (2 entities)
- `platform/tenant` (1 entity)

In production (where Flyway is the source of truth and `ddl-auto` should be `validate`/`none`),
these tables will not exist.

### 2. `V006__add_pos_quick_item_ui_fields.sql` alters a non-existent table
`V006` runs `ALTER TABLE pos_quick_items ...` and creates an index on `pos_quick_items`, but
**no migration ever creates `pos_quick_items`**. On a clean database this migration fails.

### 3. Standalone SQL scripts are not part of Flyway
- `business/pos/audit/fiscal_audit_logs.sql`
- `business/pos/audit/inspector/gap_detection.sql`
- `business/audit/inspector/gap_detection.sql`

These are not under a Flyway location, so they are not applied automatically.

### 4. Missing identity tables
`CompanyFiscalYear` (`company_fiscal_years`) and `MasterFiscalYear` (`master_fiscal_years`) have
entities but no `CREATE TABLE` migration (only repair/seed scripts reference them).

### 5. Index-name mismatches between entities and `V009`
`V009__create_pos_restaurant_tables.sql` creates indexes with names like
`idx_pos_table_sessions_tenant_company`, while the JPA `@Index` declarations use
`idx_pos_ts_tenant_company`, `idx_pos_kitchen_orders_tenant_company`, etc. Not a runtime failure,
but the declared index names will never match the migrated ones.

### 6. Missing foreign keys in `V009`
The restaurant tables (`pos_tables`, `pos_table_sessions`, `pos_kitchen_orders`,
`pos_kitchen_order_lines`) declare no FK constraints to `tenants`, `companies`, or parent rows,
unlike `pos_company_configs` (V007). Inconsistent and allows orphaned rows.

### 7. Duplicate table names across modules
The same table name is declared by entities in different modules:
- `document_sequences` — platform/shared, business/sales, business/pos, business/procurement
- `units` — platform/tenant, business/inventory

These modules share one database, so the duplicate mappings will collide.

### 8. Application-module entities not covered
`FiscalAuditLog` (`fiscal_audit_logs`) and `FiscalBillSequenceEntity` (`fiscal_bill_sequences`)
live in the `application` module rather than `domain`, and only `fiscal_audit_logs` is created
by a standalone script; `fiscal_bill_sequences` has no migration at all.

## Missing Tables by Module

### business/inventory
- categories (Category)
- items (Item)
- stock_balances (StockBalance)
- stock_movements (StockMovement)
- suppliers (Supplier)
- units (Unit)
- warehouses (Warehouse)

### business/pos
- document_sequences (DocumentSequence)
- fiscal_bill_sequences (FiscalBillSequenceEntity)
- pos_checkout_idempotency (PosCheckoutIdempotency)
- pos_invoice_sequence (PosInvoiceSequence)
- pos_invoices (PosInvoice)
- pos_quick_items (PosQuickItem)
- pos_registers (PosRegister)
- pos_sales_integration_jobs (PosSalesIntegrationJob)
- pos_sessions (PosSession)
- pos_transaction_lines (PosTransactionLine)
- pos_transactions (PosTransaction)

### business/procurement
- company_workflow_configurations (CompanyWorkflowConfiguration)
- document_sequences (DocumentSequence)
- goods_receipt_lines (GoodsReceiptLine)
- goods_receipts (GoodsReceipt)
- purchase_invoice_lines (PurchaseInvoiceLine)
- purchase_invoices (PurchaseInvoice)
- purchase_order_lines (PurchaseOrderLine)
- purchase_orders (PurchaseOrder)
- purchase_requisition_approval_policies (PurchaseRequisitionApprovalPolicy)
- purchase_requisition_approvals (PurchaseRequisitionApproval)
- purchase_requisition_lines (PurchaseRequisitionLine)
- purchase_requisition_order_links (PurchaseRequisitionOrderLink)
- purchase_requisition_status_history (PurchaseRequisitionStatusHistory)
- purchase_requisitions (PurchaseRequisition)
- supplier_payments (SupplierPayment)

### business/sales
- customer_receipts (CustomerReceipt)
- customers (Customer)
- document_sequences (DocumentSequence)
- sales_deliveries (SalesDelivery)
- sales_delivery_lines (SalesDeliveryLine)
- sales_invoice_lines (SalesInvoiceLine)
- sales_invoices (SalesInvoice)
- sales_order_lines (SalesOrderLine)
- sales_orders (SalesOrder)
- sales_quotation_lines (SalesQuotationLine)
- sales_quotations (SalesQuotation)
- sales_return_lines (SalesReturnLine)
- sales_returns (SalesReturn)

### integration
- company_ird_config (CompanyIrdConfig / CompanyIrdConfigJpaEntity)
- ird_sync_outbox (IrdSyncOutboxEntity)

### platform/identity
- company_fiscal_years (CompanyFiscalYear)
- master_fiscal_years (MasterFiscalYear)

### platform/shared
- document_sequences (DocumentSequence)

### platform/tenant
- units (Unit)

## Recommended Actions

1. Create per-service Flyway migration folders (`infrastructure/postgres/flyway/{inventory,sales,procurement,pos}`) and add `CREATE TABLE` migrations for every entity listed above.
2. Add a `CREATE TABLE pos_quick_items` migration (or fold it into an existing POS migration) so `V006` can run on a clean database.
3. Move standalone scripts (`fiscal_audit_logs.sql`, `gap_detection.sql`) into the appropriate Flyway location.
4. Add `CREATE TABLE` migrations for `company_fiscal_years` and `master_fiscal_years`.
5. Align index names in `V009` with the JPA `@Index` declarations (or vice-versa).
6. Add FK constraints in `V009` for consistency with `V007`.
7. Resolve duplicate table names (`document_sequences`, `units`) by namespacing per module or sharing a single entity.
8. Move application-module entities (`FiscalAuditLog`, `FiscalBillSequenceEntity`) into the domain module or add their migrations.
