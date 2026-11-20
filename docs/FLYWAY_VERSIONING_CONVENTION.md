# Flyway Migration Versioning Convention

## Naming
- Versioned: `V####__short_description.sql` (4-digit zero-padded), e.g. `V0042__add_customer_vat_number.sql`.
- Repeatable: `R__short_description.sql` (double underscore), e.g. `R__fiscal_audit_logs.sql`.
- Repair (one-off): `R__repair_<subject>.sql` only when re-running is safe and idempotent.

## Rules
1. Never edit or delete a migration that has been applied to any shared environment (dev/staging/prod). Add a new forward migration instead.
2. One logical change per migration; keep files reviewable (< 200 KB). Split large seed data.
3. Use `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for idempotency where possible.
4. Foreign keys and indexes must be named consistently with JPA `@Index`/`@ForeignKey` names.
5. Each service owns its migration folder and history table:
   - identity -> `infrastructure/postgres/flyway/identity` (`identity_flyway_schema_history`)
   - platform -> `infrastructure/postgres/flyway/platform` (`flyway_schema_history`)
   - inventory -> `infrastructure/postgres/flyway/inventory` (`inventory_flyway_schema_history`)
6. Baseline version is `0`; `baselineOnMigrate` is disabled in prod.
7. `outOfOrder=false` in prod; hotfixes must be numbered after the latest applied version.
8. All migrations must pass `flyway validate` and a Testcontainers smoke run in CI before merge.
9. Reference/seed data must be idempotent (`INSERT ... ON CONFLICT DO NOTHING`).
10. No Hibernate `ddl-auto=update` in any profile; use `validate` (or `none` in prod).
