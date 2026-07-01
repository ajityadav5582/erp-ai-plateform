# Database Standards

## 1. Purpose

These standards ensure data integrity, performance, and maintainability across all database operations in the ERP SaaS platform. All database design and access must follow these guidelines.

## 2. General Principles

- **Data integrity first:** Constraints over application-level validation
- **Normalization:** Normalize to 3NF; denormalize only with justification
- **Immutability:** Prefer append-only patterns for audit trails
- **Tenant isolation:** Strict schema-level or row-level isolation
- **Performance:** Design for query patterns, not just storage

## 3. Naming Conventions

### 3.1 Tables

- **Format:** `snake_case`, plural nouns
- **Prefix:** No prefix (except tenant-specific if needed)
- **Examples:** `invoices`, `payment_methods`, `invoice_line_items`

### 3.2 Columns

- **Format:** `snake_case`
- **Primary key:** `id` (BIGSERIAL)
- **Foreign keys:** `{table_name}_id` (e.g., `customer_id`, `invoice_id`)
- **Timestamps:** `created_at`, `updated_at`, `deleted_at` (soft delete)
- **Boolean:** `is_` or `has_` prefix (e.g., `is_paid`, `has_attachments`)

### 3.3 Indexes

- **Format:** `idx_{table}_{column(s)}`
- **Unique indexes:** `uk_{table}_{column(s)}`
- **Examples:** `idx_invoices_tenant_status`, `uk_invoices_number`

### 3.4 Constraints

- **Foreign keys:** `fk_{table}_{column}_{referenced_table}`
- **Check constraints:** `chk_{table}_{column}`
- **Examples:** `fk_invoices_customer_id_customers`, `chk_invoices_total_positive`

### 3.5 Sequences

- **Format:** `seq_{table}_{column}`
- **Example:** `seq_invoices_id`

## 4. Data Types

### 4.1 Standard Types

| Data | Type | Notes |
|------|------|-------|
| Primary key | `BIGSERIAL` | Auto-incrementing 64-bit |
| Integer | `INTEGER` | 32-bit signed |
| Boolean | `BOOLEAN` | `NOT NULL DEFAULT false` |
| String (short) | `VARCHAR(255)` | Names, codes, identifiers |
| String (medium) | `VARCHAR(1000)` | Descriptions, addresses |
| String (long) | `TEXT` | Comments, JSON |
| Decimal (money) | `NUMERIC(19,4)` | Never use FLOAT for money |
| Decimal (percentage) | `NUMERIC(5,2)` | 0.00 to 100.00 |
| Date | `DATE` | No time component |
| Timestamp | `TIMESTAMP WITH TIME ZONE` | Always store in UTC |
| JSON | `JSONB` | For flexible schemas |
| UUID | `UUID` | For external identifiers |

### 4.2 Monetary Values

- **Always use:** `NUMERIC(19,4)` for monetary values
- **Never use:** `FLOAT`, `DOUBLE`, or `REAL`
- **Currency:** Store ISO 4217 code separately
- **Rounding:** Define rounding rules per currency

```sql
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL,
    -- Constraints
    CONSTRAINT chk_invoices_total_positive CHECK (total > 0),
    CONSTRAINT chk_invoices_currency_length CHECK (LENGTH(currency) = 3)
);
```

## 5. Table Design Standards

### 5.1 Every Table Must Have

```sql
id BIGSERIAL PRIMARY KEY,
tenant_id BIGINT NOT NULL,  -- For multi-tenancy
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
deleted_at TIMESTAMP WITH TIME ZONE  -- For soft deletes
```

### 5.2 Primary Keys

- **Type:** `BIGSERIAL` (auto-incrementing)
- **Never:** Use natural keys as primary keys
- **Surrogate keys:** Use for all tables
- **Composite keys:** Avoid; use unique constraints instead

### 5.3 Foreign Keys

- **Always:** Define foreign key constraints
- **Index:** Create index on all foreign key columns
- **Cascade:** Use `ON DELETE RESTRICT` by default; `CASCADE` only with justification
- **Naming:** Follow naming convention (see Section 3.4)

```sql
CREATE TABLE invoices (
    -- ...
    customer_id BIGINT NOT NULL,
    CONSTRAINT fk_invoices_customer_id_customers 
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
```

### 5.4 Unique Constraints

- **Business keys:** Define unique constraints for business identifiers
- **Composite keys:** Use when single column isn't unique
- **Partial indexes:** Use for filtered uniqueness

```sql
-- Unique invoice number per tenant
CREATE UNIQUE INDEX uk_invoices_tenant_number 
ON invoices(tenant_id, invoice_number) 
WHERE deleted_at IS NULL;
```

### 5.5 Check Constraints

- **Use for:** Simple validation rules
- **Avoid:** Complex business logic in constraints
- **Examples:** Positive amounts, valid status values, date ranges

```sql
ALTER TABLE invoices 
ADD CONSTRAINT chk_invoices_total_positive 
CHECK (total > 0);

ALTER TABLE invoices 
ADD CONSTRAINT chk_invoices_due_date 
CHECK (due_date >= created_at::DATE);
```

## 6. Indexing Strategy

### 6.1 Index Types

| Type | Usage | Example |
|------|-------|---------|
| B-tree | Default, equality/range | `idx_invoices_status` |
| Hash | Equality only, high cardinality | `idx_sessions_token` |
| GIN | JSONB, arrays, full-text | `idx_products_metadata` |
| GIST | Geometric, range types | `idx_events_time_range` |
| BRIN | Large, naturally ordered | `idx_logs_timestamp` |

### 6.2 Index Guidelines

- **Every foreign key:** Must have an index
- **Query patterns:** Index columns used in WHERE, JOIN, ORDER BY
- **Composite indexes:** Order by selectivity (most selective first)
- **Covering indexes:** Include frequently accessed columns
- **Avoid:** Over-indexing (impacts write performance)

```sql
-- Good: Composite index for common query
CREATE INDEX idx_invoices_tenant_status_created 
ON invoices(tenant_id, status, created_at DESC);

-- Good: Covering index
CREATE INDEX idx_invoices_tenant_covering 
ON invoices(tenant_id) 
INCLUDE (status, total, created_at);
```

### 6.3 Partial Indexes

Use for filtered queries to reduce index size:

```sql
-- Only index active invoices
CREATE INDEX idx_invoices_active 
ON invoices(tenant_id, created_at DESC) 
WHERE deleted_at IS NULL AND status != 'CANCELLED';
```

## 7. Multi-Tenant Data Isolation

### 7.1 Schema-per-Tenant (Recommended for Enterprise)

```sql
-- Shared catalog, separate schemas
CREATE SCHEMA tenant_123;
CREATE TABLE tenant_123.invoices (...);
CREATE TABLE tenant_123.customers (...);
```

### 7.2 Row-Level Security (Alternative)

```sql
-- Enable RLS
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;

-- Policy: Users see only their tenant's data
CREATE POLICY tenant_isolation ON invoices
    FOR ALL
    TO application_user
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);
```

### 7.3 Application-Level Filtering

- **Always:** Include `tenant_id` in WHERE clauses
- **Never:** Trust client-provided tenant IDs
- **Context:** Set tenant context at request boundary

## 8. Migrations

### 8.1 Tool: Flyway

- **Location:** `db/migration/`
- **Naming:** `V{version}__{description}.sql`
- **Versioning:** Sequential, timestamp-based
- **Repeatable:** `R__{description}.sql` for views, functions

### 8.2 Migration Standards

```sql
-- V001__create_invoices_table.sql
-- ============================================
-- Description: Create invoices table
-- Author: John Doe
-- Date: 2024-01-15
-- ============================================

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    -- ...
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_invoices_tenant_id ON invoices(tenant_id);
CREATE UNIQUE INDEX uk_invoices_tenant_number 
ON invoices(tenant_id, invoice_number) 
WHERE deleted_at IS NULL;

-- Comments
COMMENT ON TABLE invoices IS 'Stores invoice headers';
COMMENT ON COLUMN invoices.total IS 'Total amount including tax';
```

### 8.3 Migration Rules

- **Never:** Modify committed migrations
- **Always:** Test migrations on production-like data
- **Backwards compatible:** Additive changes only in same release
- **Rollback:** Provide rollback script for complex migrations

## 9. Query Standards

### 9.1 Prohibited Patterns

```sql
-- NEVER: SELECT *
SELECT * FROM invoices;

-- NEVER: Implicit joins
SELECT i.invoice_number, c.name 
FROM invoices i, customers c 
WHERE i.customer_id = c.id;

-- NEVER: Functions on indexed columns
WHERE LOWER(invoice_number) = 'inv-001';

-- NEVER: OFFSET for deep pagination
LIMIT 20 OFFSET 100000;
```

### 9.2 Required Patterns

```sql
-- Always: Explicit column list
SELECT id, invoice_number, total, created_at 
FROM invoices;

-- Always: Explicit JOINs
SELECT i.invoice_number, c.name 
FROM invoices i
INNER JOIN customers c ON i.customer_id = c.id;

-- Always: Tenant filter
WHERE tenant_id = :tenantId AND deleted_at IS NULL;

-- Prefer: Keyset pagination
WHERE tenant_id = :tenantId 
  AND id > :lastId 
ORDER BY id 
LIMIT 20;
```

### 9.3 Query Optimization

- **EXPLAIN ANALYZE:** Use for slow queries
- **Avoid:** N+1 queries (use JOINs or batch loading)
- **Batch:** Use `IN` clause or temporary tables for bulk operations
- **Connection pooling:** Use HikariCP with appropriate pool size

## 10. Data Retention

| Data Type | Retention | Archive Strategy |
|-----------|-----------|------------------|
| Active records | Indefinite | - |
| Completed transactions | 7 years | Archive to cold storage |
| Audit logs | 7 years | Partition by month, archive old |
| Session data | 30 days | Automatic cleanup |
| Temporary data | 24 hours | Automatic cleanup |

## 11. Backup & Recovery

- **Full backup:** Daily at 2 AM UTC
- **Incremental backup:** Every 4 hours
- **WAL archiving:** Continuous for point-in-time recovery
- **Retention:** 30 days of backups
- **Testing:** Monthly restore test
- **RPO:** 4 hours (from last incremental backup)
- **RTO:** 2 hours (restore to latest backup)
