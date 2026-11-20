# Flyway Gap Analysis & Improvement Recommendations

> Senior Java / React / DBA review of database migration (Flyway) usage in the ERP AI Platform.
> Scope: `infrastructure/postgres/flyway/**`, service `application.yml` profiles, build scripts, CI checks.

## 1. Executive Summary

Flyway is present and heavily used for the `platform/identity` module, but it is **not consistently wired as the single source of truth** for schema management:

- Flyway is **disabled in every Spring Boot profile** (`spring.flyway.enabled: false`) for all services, so services never self-migrate; migrations run only via an external CLI/container step that is easy to skip.
- `start-services.sh` forces `SPRING_JPA_HIBERNATE_DDL_AUTO=update` for every service, so Hibernate silently creates/alters tables at runtime, defeating Flyway in dev and masking missing migrations.
- There are **two competing migration sources** (`infrastructure/postgres/flyway/identity` vs `platform/identity/src/main/resources/db/migration`), the latter stale (stops at `V50`).
- The identity baseline is configured at version `1`, which causes Flyway to **skip `V0` and `V1`** (real table creation) on a fresh database.
- Version/tooling is inconsistent (Flyway 10.12 CLI image vs 11.7.2 Java libs; Spring Boot 3.3.4 vs 3.4.5).
- Several platform migrations are **duplicated** (repair/align scripts repeated as `V027`/`V033`, etc.), re-running heavy 674 KB repairs.
- CI coverage check (`scripts/check-migration-coverage.sh`) is broken (regex splits `@Table(name = "...")`), so it never actually validates coverage.
- No Flyway Gradle plugin is configured, although frontend docs reference `./gradlew :platform:identity:flywayInfo`.

## 2. Current State

| Area | State |
|------|-------|
| Migration locations | `infrastructure/postgres/flyway/{identity,inventory,platform}` |
| History tables | `identity_flyway_schema_history`, `inventory_flyway_schema_history`, default `flyway_schema_history` (platform) |
| Spring Boot Flyway | Disabled in all profiles (`enabled: false`) |
| Hibernate DDL | `validate` in prod/staging, `update` in local, forced `update` by `start-services.sh` |
| Baseline | `baselineOnMigrate=true`, `baselineVersion=1` |
| Out of order | `false` |
| Clean | disabled |
| Validate on migrate | `true` |
| Docker image | `flyway/flyway:10.12-alpine` |
| Java libs | `flyway-core` / `flyway-database-postgresql` 11.7.2 (gradle.properties) / 10.15.0 (root ext) |

## 3. Gaps & Risks

### G1. Flyway disabled in application runtime
- All `application*.yml` set `spring.flyway.enabled: false`.
- Services depend on an external Flyway step; if it is skipped, services start against an incomplete schema and fail at runtime (or Hibernate `update` masks it).
- **Suggestion:** Either enable Spring Boot Flyway in each service (`spring.flyway.enabled: true`, `locations: classpath:db/migration`) and ship migrations inside the service jar, or keep external Flyway but add a startup gate/health check and document the required order. Prefer in-jar migrations for ownership and reproducibility.

### G2. Hibernate `ddl-auto=update` overrides Flyway
- `start-services.sh` exports `SPRING_JPA_HIBERNATE_DDL_AUTO=update` and `SPRING_JPA_GENERATE_DDL=true` for all services.
- This lets Hibernate mutate schema in dev, hiding missing migrations and causing drift vs Flyway.
- **Suggestion:** Remove the override; use `validate` everywhere (or `none` in prod). Keep `update` only behind an explicit, opt-in dev flag.

### G3. Stale duplicate migration source
- `platform/identity/src/main/resources/db/migration` contains an older copy (up to `V50`) of the identity migrations.
- `infrastructure/postgres/flyway/identity` is the current source (up to `V60`).
- **Suggestion:** Delete the stale `db/migration` tree (or regenerate it from the canonical location) to avoid confusion and accidental use if Flyway is ever enabled in Spring.

### G4. Identity baseline skips real tables
- `baselineVersion=1` with `baselineOnMigrate=true` means on a fresh DB Flyway baselines at version 1 and applies only migrations `> 1`, skipping `V0__create_tenants_table.sql` and `V1__create_identity_tables.sql`.
- **Suggestion:** Set `baselineVersion=0` for identity (or disable `baselineOnMigrate` for identity), so `V0`/`V1` actually run. Keep `baselineVersion=1` for platform where `V001` is a baseline marker.

### G5. Version/tooling mismatch
- Docker image `flyway/flyway:10.12-alpine` vs Java libs 11.7.2 (gradle.properties) / 10.15.0 (root ext).
- Spring Boot 3.3.4 (plugin) vs 3.4.5 (gradle.properties BOM).
- **Suggestion:** Align to Flyway 11.7.2 everywhere (image `flyway/flyway:11.7.2-alpine` or newer) and Spring Boot 3.4.x. Remove the unused `flywayVersion` from root `ext`.

### G6. Duplicate repair migrations
- Platform has repeated pairs: `V027`/`V033` (repair BS calendar, 674 KB each), `V028`/`V034`, `V029`/`V035`, `V030`/`V036`, `V031`/`V037`.
- These re-run expensive repairs and slow down fresh migrations.
- **Suggestion:** Keep only the latest of each repair; convert earlier ones to no-ops or remove from new baselines. Guard repairs with idempotent checks.

### G7. Heavy repeatable migration
- `R__repair_master_bs_calendar.sql` is ~674 KB and runs whenever its checksum changes.
- **Suggestion:** Split into smaller, versioned migrations or generate from `common-nepali-date` module at build time; keep the repeatable only as a safety net.

### G8. Inconsistent history tables / ownership
- Three different history tables; sales/procurement/pos share the platform history table.
- `reset-database.sh` runs identity, platform, inventory separately but not sales/procurement/pos explicitly (they are folded into platform).
- **Suggestion:** Define one history table per deployable service (or one shared table with clear ownership) and document which migration folder maps to which service. Add sales/procurement/pos to reset scripts explicitly if they ever get their own folders.

### G9. Broken CI coverage check
- `scripts/check-migration-coverage.sh` regex `@Table\([^)]*name\s*=\s*"[^"]+"` fails on multi-line/whitespace variants, producing false positives (`@Table(name`, `=`, `"branches"`).
- **Suggestion:** Fix the regex (e.g. `@Table\(\s*name\s*=\s*"([^"]+)"`) or replace with a small Java/Gradle task that parses JPA entities. Also extend coverage to all migration folders, not only `platform`.

### G10. Missing Gradle Flyway plugin
- Frontend docs reference `./gradlew :platform:identity:flywayInfo` / `flywayClean`, but no Flyway Gradle plugin is applied.
- **Suggestion:** Either add the Flyway Gradle plugin to the relevant modules or update docs to use the CLI/docker commands actually supported.

### G11. No migration tests / rollback strategy
- No Testcontainers-based migration smoke tests; no undo migrations (Flyway Teams) or documented rollback procedure.
- **Suggestion:** Add a CI job that runs all migrations against a fresh Postgres (Testcontainers) and validates `ddl-auto=validate`. Document rollback via new forward-fix migrations (avoid `flyway clean` in prod).

### G12. Out-of-order disabled
- `outOfOrder=false` blocks hotfix migrations applied out of sequence.
- **Suggestion:** Keep `false` in prod for safety, but allow `true` in dev/staging via profile override when needed.

### G13. Baseline-on-migrate risk
- `baselineOnMigrate=true` can silently baseline a non-empty DB and skip migrations.
- **Suggestion:** Disable in prod (`baselineOnMigrate=false`) and require an explicit baseline step during initial cutover; keep enabled only for dev bootstrap.

### G14. Migration naming / ordering
- Mixed schemes (`V0`, `V001`, `V47`) and gaps; `V42`/`V42_1` ordering relies on Flyway version parsing.
- **Suggestion:** Standardize on zero-padded 4-digit versions (`V0001`) for new migrations and avoid fractional versions; document the convention.

### G15. Seed data coupling
- Large seed (BS calendar, fiscal years, Nepal divisions) lives in migrations; `DataInitializer` also seeds at runtime. Risk of double seeding / drift.
- **Suggestion:** Keep reference data in Flyway (idempotent `INSERT ... ON CONFLICT DO NOTHING`) and make runtime seeders no-ops or version-gated; track via `db_bootstrap_state`.

## 4. Prioritized Action Plan

### P0 (production blockers)
1. Fix identity baseline (`baselineVersion=0`) so `V0`/`V1` run.
2. Remove `ddl-auto=update` override in `start-services.sh`; set `validate`.
3. Align Flyway/ Spring Boot versions and Docker image.
4. Remove stale `platform/identity/src/main/resources/db/migration`.

### P1 (quality)
5. Fix `check-migration-coverage.sh` regex and extend to all folders.
6. Remove duplicate repair migrations (V027/V033, etc.).
7. Enable Flyway in Spring Boot (or add startup gate) and ship migrations in jars.
8. Disable `baselineOnMigrate` in prod.

### P2 (robustness)
9. Add Testcontainers migration smoke test in CI.
10. Split heavy repeatable BS calendar repair.
11. Standardize migration versioning convention.
12. Add Flyway Gradle plugin or fix docs.

### P3 (polish)
13. Document rollback procedure.
14. Add migration dry-run (`flyway validate` + `info`) to CI.
15. Reconcile runtime seeders with Flyway seeds.

## 5. Suggested Config Snippets

### application.yml (prod)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
    clean-disabled: true
```

### flyway.conf (prod overrides)
```properties
flyway.baselineOnMigrate=false
flyway.baselineVersion=0
flyway.outOfOrder=false
```

### check-migration-coverage.sh (fixed regex)
```bash
ENTITY_TABLES=$(grep -rhoE '@Table\(\s*name\s*=\s*"[^"]+"' --include='*.java' . \
  | sed -E 's/.*name\s*=\s*"([^"]+)".*/\1/' | sort -u)
```

## 6. Notes for React / Frontend
- Update `frontend/src/docs/GETTING_STARTED.md` Flyway gradle commands to the actually supported CLI/docker commands, or add the Flyway Gradle plugin.
- Keep frontend typed-client generation tied to a validated OpenAPI spec produced from a schema that Flyway has migrated (CI order: migrate -> start service -> export spec).

---
*Generated as part of Flyway gap analysis. No code changes were made; this document is a recommendation set.*
