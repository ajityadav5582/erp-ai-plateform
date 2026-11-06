# ERP AI Platform — Gap & Enhancement Analysis (Scoped: Identity, Inventory, Sales, Procurement, POS)

> Senior Java/React review. Backend: Spring Boot multi-module (hexagonal) with JPA entities. Frontend: Next.js (App Router) + TypeScript.

## 1. Executive Summary

- **Schema management fixed**: all 44 missing entity tables now have Flyway migrations (V005, V010–V020, R__). Per-module migration folders consolidated into `infrastructure/postgres/flyway/platform/`. `ddl-auto` should be set to `validate`.
- **Security consistency verified**: all four SecurityConfig files aligned (CORS/CSRF disabled, stateless, JWT filter, @Order).
- **OpenAPI/Swagger added**: springdoc webmvc-ui in common-core + shared OpenApiConfig; consolidated Swagger UI in gateway.
- **Remaining enhancements**: i18n, accessibility, E2E tests, general reports (P&L, balance sheet), restaurant terminal wiring, typed client generation.

---

## 2. Backend — Completed

- Flyway migrations for all missing tables (identity, inventory, sales, procurement, pos).
- V006 fixed (V005 creates pos_quick_items first).
- Standalone SQL moved to Flyway (R__fiscal_audit_logs.sql).
- V009 index names aligned with JPA @Index; FKs added.
- Duplicate table names resolved (inventory_units, module-prefixed document_sequences).
- App-module entities covered (fiscal_audit_logs, fiscal_bill_sequences).
- Security configs aligned across modules.
- OpenAPI/Swagger + consolidated gateway Swagger UI.

## 3. Backend — Remaining Enhancements

1. Set `spring.jpa.hibernate.ddl-auto=validate` in all services (currently `update`).
2. Add CI check that fails if an entity table lacks a migration.
3. Move `FiscalAuditLog`/`FiscalBillSequenceEntity` from application to domain module (code organization).
4. Remove or merge `platform:inventory` (duplicate of `business:inventory`).
5. Add integration/contract tests for controllers.
6. Implement general reports (P&L, balance sheet, trial balance) backend services.
7. Complete restaurant terminal backend wiring (table grid, KDS).

## 4. Frontend — Completed

- Stock report pages verified wired to backend StockReportController.
- Thin pages reviewed: sales-invoices is full implementation; sales-returns handled via dialog; sales-register functional; register redirects to login (intentional).

## 5. Frontend — Remaining Enhancements

1. Complete restaurant terminal UI (table grid, occupy/merge/split, KDS).
2. Add i18n framework (next-intl) with en/ne message catalogs.
3. Accessibility audit (ARIA, keyboard nav, focus management).
4. Add component/E2E tests (Playwright/Cypress).
5. Generate typed API client from OpenAPI spec.
6. Implement general reports screens (P&L, balance sheet, trial balance).
7. Add loading/error states to older pages (pos/restaurant scaffold).

## 8. Restaurant Terminal — Gap & Task Breakdown

### Backend (exists)
- Entities: PosTable, TableSession, KitchenOrder, KitchenOrderLine (V009 tables).
- Service: PosTableService (occupy, free, openSession) — partial; TableSessionRepository missing (noted in code).
- Capability guard: PosCapabilityGuard.requireRestaurantFamily.

### Backend (missing)
- REST controller for restaurant tables/sessions/kitchen orders (no PosRestaurantController found).
- TableSessionRepository (persistence) — referenced but not implemented.
- KitchenOrderService (fire, status transitions, billing).
- Merge/split table endpoints.

### Frontend (exists)
- `pos/restaurant/page.tsx` — scaffold with static 12-tile grid + KDS placeholder.

### Frontend (missing)
- Dynamic table grid from backend (status colors, capacity).
- Occupy/free/merge/split actions wired to API.
- KDS panel with real kitchen orders and status transitions.
- Session/billing flow (open session → order → bill → close).

### Task chunks
1. Backend: add TableSessionRepository + PosTableController (CRUD + occupy/free/openSession).
2. Backend: add KitchenOrderService + controller (fire, status transitions).
3. Backend: add merge/split endpoints.
4. Frontend: replace static grid with dynamic table tiles from API.
5. Frontend: wire occupy/free/merge/split actions.
6. Frontend: implement KDS panel with real orders.
7. Frontend: wire session → billing → close flow.
8. Tests: backend integration + frontend E2E.

## 6. Cross-Cutting

- CI test coverage enforcement.
- Observability (metrics/tracing) verification.
- OpenAPI typed client generation in frontend build pipeline.

## 7. Prioritized Task List

### P0 (production blockers) — DONE
- [x] Flyway migrations for all missing tables
- [x] Fix V006
- [x] Consolidate migrations into single directory
- [x] Align V009 index names + FKs

### P1 (quality) — DONE
- [x] Move standalone SQL into Flyway
- [x] Resolve duplicate table names
- [x] Security config consistency
- [x] OpenAPI/Swagger + consolidated UI

### P2 (frontend/enhancements) — TODO
- [ ] Restaurant terminal full implementation
- [ ] i18n framework
- [ ] Accessibility audit
- [ ] Component/E2E tests
- [ ] Typed API client generation
- [ ] General reports (P&L, balance sheet, trial balance)

### P3 (polish) — TODO
- [ ] PWA/offline support
- [ ] Performance budgets
- [ ] CI coverage enforcement
