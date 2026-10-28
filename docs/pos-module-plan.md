# POS Module — System Analysis & Implementation Plan (v2)

> Status: **Planning / suggestion only.** No implementation code is included.
> v2 adds **barcode-reader support** and **scan-to-price auto-calculation**, and
> restructures the plan around an **optimized, maintainable** architecture.

---

## 1. Executive summary

The POS **domain layer is already complete and high quality** — five aggregates
([`PosRegister`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosRegister.java:69),
[`PosSession`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosSession.java:78),
[`PosTransaction`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosTransaction.java:104),
[`PosTransactionLine`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosTransactionLine.java:61),
[`PosQuickItem`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosQuickItem.java:52))
plus five enums, with rich behaviour, tenant scoping, CBMS sync tracking and
cash-variance logic. The line already owns its amount formula
(`lineAmount = qty × unitPrice × (1 − disc%)`) and the header rolls up totals —
so **the domain is the single writer of every calculated figure**.

**What is missing is the entire application layer and all wiring**, and the build is
currently *broken*: [`settings.gradle`](settings.gradle:60) includes
`business:pos:application`, which does not exist yet.

The **Sales module is a near-perfect template** for structure and cross-cutting
concerns. v2 layers two POS-specific capabilities on top of it — a **scan resolver**
(barcode/SKU/code → item) and a **pricing service** (item → price → VAT → totals) —
designed so pricing/scan rules are isolated, testable, and extensible.

---

## 2. System design analysis (what exists today)

- **Modular monolith**, Gradle multi-module, Spring Boot 3.x / Java 21, hexagonal + DDD
  per module: pure `domain` + `application` (services, controllers, DTOs, mappers, repos).
- **Multi-tenant**: every entity extends `TenantScopedEntity`; tenant/company/user come
  from the JWT request context, never the payload
  ([`BusinessContextAccessor`](business/sales/application/src/main/java/com/erp/business/sales/shared/context/BusinessContextAccessor.java:23)).
- **API Gateway** (8080) routes by path prefix (`/api/v1/sales/**` → `sales-service:8085`).
- **Reusable patterns:** numbering ([`DocumentNumberService`](business/sales/application/src/main/java/com/erp/business/sales/shared/numbering/DocumentNumberService.java:21)),
  permissions + `@RequiresPermission` ([`SalesOrderPermissions`](business/sales/application/src/main/java/com/erp/business/sales/shared/security/SalesOrderPermissions.java:13)),
  global exception handler, CBMS/IRD sync ([`CbmsClient`](business/sales/application/src/main/java/com/erp/business/sales/cbms/CbmsClient.java:11)),
  HTTP adapter to the Inventory port ([`StockMovementService`](business/inventory/interfaces/src/main/java/com/erp/business/inventory/interfaces/stock/StockMovementService.java:17),
  idempotent `postIssue`/`reverseIssue`).
- **Inventory item lookup already exists:** [`ItemService`](business/inventory/interfaces/src/main/java/com/erp/business/inventory/interfaces/item/ItemService.java:42)
  exposes `findByBarcode`, `findBySku`, `findById`, and an `ItemResponse` with sku,
  barcode, name, UOM, `taxabilityType`, `isStockable`, `availableStock`.
- **Frontend:** Next.js 15 + React 19 + RTK Query over a shared axios base query
  ([`api.ts`](frontend/src/services/api.ts:80)); backend-driven, permission-gated menus
  ([`sales-menu.service.tsx`](frontend/src/services/sales-menu.service.tsx:20)).

---

## 3. New requirements decoded

**3.1 Barcode reader.** Hardware scanners emulate a keyboard (type the code + `Enter`).
Two sides:
- *Frontend:* an always-focused scan field on the terminal that captures the burst,
  calls the resolver, and adds the item to the cart with feedback.
- *Backend:* a fast **scan resolver** that maps a scanned string → item, trying
  barcode → SKU → product code in order, and returning everything the UI needs in one
  call (item snapshot + resolved price + VAT + stock).

**3.2 Product-code-by-price calculation ("scan-to-price").** Entering/scanning a code
must resolve the item **and** auto-compute its price, then roll up line VAT and header
totals. The arithmetic already lives in the domain; the new work is *resolving the
price* and *feeding the domain* — never duplicating the formulas.

> **Critical correctness rule:** the client cart is for UX only. At checkout the
> **server re-resolves price + VAT authoritatively** from `{itemId, quantity, discount}`
> and lets the domain compute amounts. Client-sent prices are never trusted (tamper-
> proof and drift-proof).

---

## 4. Current POS state & gap analysis

**Exists (domain — complete):** aggregates with behaviour
(`PosTransaction.open/addLine/removeLine/updateLineQuantity/applyLineDiscount/
recalculateTotals/complete/voidTransaction/markCbmsSynced/recordCbmsSyncFailure`;
`PosSession.open/close/recordCompletedTransaction/recordVoidedTransaction`;
`PosRegister.create/activate/deactivate/recordSessionOpened/recordSessionClosed/
hasOpenSession`) and enums `PosPaymentMethod`, `PosTransactionStatus`,
`PosSessionStatus`, `PosRegisterStatus`, `PosVatType`. DB indexes/constraints are
already modelled (barcode/item indexes, one-open-session partial unique index, CBMS
columns with defaults).

**Gaps:**
1. **`business/pos/application` missing** → build fails today. *#1 blocker.*
2. Empty domain `exception` / `shared` packages.
3. No services, controllers, DTOs, mappers, repositories.
4. No POS numbering (`DocumentSequence` + `DocumentNumberService`).
5. No permission constants; **no POS permissions seeded in Identity**.
6. No CBMS client/sync worker (domain javadoc already names `PosCbmsSyncWorker`).
7. No `SecurityConfig`, `InventoryClientConfiguration`, `application.yml`.
8. No gateway route, Docker `pos` target, or compose service.
9. No frontend `/pos` route, services, menu, RTK tags.
10. **No selling-price source:** Inventory's `ItemResponse` exposes no price, so
    scan-to-price has nothing to resolve against yet (see §5.2 / §7.2).

---

## 5. Target architecture (optimized + maintainable)

### 5.1 Package layout (feature-first, mirrors Sales)
```
business/pos/application/.../com/erp/business/pos/
  register/      (service+impl, controller, dto, mapper, infrastructure/persistence)
  session/       (...)
  transaction/   (... checkout, void, park/resume ...)
  quickitem/     (...)
  scan/          PosScanController, PosItemLookupService(Impl), BarcodeResolver, dto
  pricing/       PosPricingService(Impl), PosPriceCalculator (pure), PriceSource (port)
  cbms/          PosCbmsClient, PosCbmsSyncWorker, payloads
  shared/        context, numbering, security (*Permissions), web (GlobalExceptionHandler)
  config/        SecurityConfig, InventoryClientConfiguration, Http* adapters
```

### 5.2 Pricing — one source of truth, isolated rules
- **`PriceSource` port** → resolves a unit price for an `itemId` in context. Primary
  implementation calls Inventory; **recommend extending the Inventory
  `ItemService`/`ItemResponse` with a selling price (or a `getPrice(itemId)` /
  price-list lookup)** so price has a single owner. Fallback: `PosQuickItem.defaultPrice`
  override, then a POS-owned price list if product chooses POS-local pricing.
- **`PosPriceCalculator`** — pure, stateless functions (no Spring): given
  `{qty, unitPrice, discountPct, vatType, vatRate}` return line/VAT/header figures.
  Used for **client-side instant totals** and **unit tests**. At persistence time the
  **domain aggregate remains the single writer** (`PosTransactionLine.recalculateAmount()`,
  `PosTransaction.recalculateTotals()`); the calculator only *previews* and *validates*.
- **`PosPricingService`** — orchestrates: resolve price via `PriceSource`, map
  `taxabilityType → PosVatType + vatRate`, apply optional pricing **strategy**
  (retail / wholesale / promotional) — a small strategy interface so new rules plug in
  without touching checkout.

### 5.3 Scan resolver — one round-trip per scan
- **`PosItemLookupService`** with a **`BarcodeResolver` strategy chain**:
  `findByBarcode → findBySku → product-code`. Returns a `PosScannedItemResponse`
  (item snapshot + resolved price + VAT + available stock + uom). Strategy pattern keeps
  symbology-specific behaviour (e.g. **price-embedded / weighted EAN** where the code
  itself encodes weight or price) additive — new resolver, no change to checkout.
- **Optimized:** single call per scan; short-TTL cache for hot items; reuses existing
  Inventory lookups and the already-modelled barcode/item indexes.

### 5.4 Maintainability principles
- **Single writer of money:** domain computes; services feed inputs. No duplicated formulas.
- **Server re-prices at checkout** (§3) — security + correctness in one rule.
- **Feature-first packages** + thin controllers (context only) + `@RequiresPermission`
  on services — identical to Sales, so the team's mental model transfers.
- **Ports to Inventory** (`ItemService`, `StockMovementService`) via HTTP adapter — no
  coupling to Inventory internals.

---

## 6. Phased plan

> Sequencing: unblock build → vertical slices (Register → Session → Scan+Pricing →
> Checkout → Quick items → CBMS) → UI → wiring.

### Phase 0 — Unblock the build (skeleton)
- Create `business/pos/application/build.gradle` mirroring
  [`business/sales/application/build.gradle`](business/sales/application/build.gradle:1)
  (`api :business:pos:domain`, `implementation :business:inventory:interfaces`,
  common-core/data/security, common-nepali-date, web/data-jpa/validation/security/aop,
  postgres, jjwt). Add `PosApplication`, `application.yml` (port **8086**,
  `pos-service`), `SecurityConfig`. Put feature exceptions in the **application** module
  (as Sales does); keep domain `exception` for pure domain errors or drop it.

### Phase 1 — Register & Session
- `PosRegisterService(+Impl)`/`Controller` (`/api/v1/pos/registers`): create (mandatory
  warehouse), update, activate/deactivate, list/get.
- `PosSessionService(+Impl)`/`Controller` (`/api/v1/pos/sessions`): open (guard register
  ACTIVE + no open session; snapshot opening cash), close
  (`cashVariance = closingCash − (openingCash + totalCashSales)`), current/history.
- Repos + `PosRegisterPermissions`, `PosSessionPermissions`.

### Phase 2 — Scan resolver + Pricing (the new capabilities)
- `scan/`: `PosScanController` (`GET /api/v1/pos/scan?code=`), `PosItemLookupService`,
  `BarcodeResolver` chain, DTOs.
- `pricing/`: `PosPricingService`, `PosPriceCalculator`, `PriceSource` port + Inventory
  adapter, `taxabilityType → PosVatType` mapping, pricing strategy hook.
- **Extend Inventory `ItemService`/`ItemResponse` with price** (or add a price-list
  lookup) — the enabling change for scan-to-price.
- Permissions: `PosScanPermissions` (`POS_SCAN`), pricing is internal.

### Phase 3 — Core checkout
- `PosTransactionService(+Impl)`/`Controller` (`/api/v1/pos/transactions`):
  - **checkout** (single atomic call): accept `{sessionId, customerId?, lines:[{itemId,
    quantity, discountPct}]}` → **server re-prices** each line via `PosPricingService` →
    build `PosTransaction` + lines → `complete(...)` → allocate number → persist →
    `StockMovementService.postIssue(...)` → update session totals, all in one
    `@Transactional` method (mirrors [`SalesDeliveryServiceImpl`](business/sales/application/src/main/java/com/erp/business/sales/delivery/SalesDeliveryServiceImpl.java:41)).
  - **void**: `reverseIssue(...)` → `voidTransaction(actor, reason)` → session rollback.
  - **park / resume**: persist an `OPEN` transaction; rehydrate later.
  - list/get/by-number/by-session; receipt payload.
- Numbering: POS `DocumentSequence` + `DocumentNumberService` (prefix `POS-`, FY period key).
- Permissions: `PosTransactionPermissions` (`*_VIEW`, `CHECKOUT`, `VOID`, `REFUND`).

### Phase 4 — Quick items
- `PosQuickItemService(+Impl)`/`Controller` (`/api/v1/pos/quick-items`): CRUD + reorder
  (grid row/col, colour, icon), register- or company-scoped; batch load for the grid.
- Permissions: `PosQuickItemPermissions`.

### Phase 5 — CBMS (IRD real-time reporting)
- Port Sales CBMS to POS: `PosCbmsClient`, `PosCbmsSyncWorker` (scheduled, bounded
  retries via `cbmsRetryCount`), payloads; wire to `markCbmsSynced` /
  `recordCbmsSyncFailure`. **Keep CBMS off the synchronous checkout path.**

### Phase 6 — Frontend POS terminal
- Route group `frontend/src/app/(protected)/pos/`:
  - `terminal/page.tsx` — fast, keyboard-driven checkout: quick-item grid, **barcode
    scan field**, cart, live totals, payment pad, change due, park/resume, void.
  - `registers/`, `sessions/`, `transactions/`, `quick-items/` admin screens.
- `useBarcodeScanner` hook (detects scanner keystroke bursts vs manual typing; global
  focus; beep/visual feedback; graceful unknown-code handling).
- `frontend/src/services/pos-*.service.ts` (RTK Query) + add `Pos*` tags to
  [`api.ts`](frontend/src/services/api.ts:106); client-side pricing preview via a shared
  calculator mirroring `PosPriceCalculator`.
- `PosMenuController` + `pos-menu.service.tsx` + icon map (mirror Sales).

### Phase 7 — Platform wiring
- **Gateway:** `/api/v1/pos/**` → `${POS_SERVICE_URI:http://pos-service:8086}` in
  [`application.yml`](platform/gateway/src/main/resources/application.yml:79) and
  `application-local.yml` (before the identity catch-all).
- **Identity:** seed POS permissions + `POS_CASHIER` / `POS_SUPERVISOR` roles in
  [`DataInitializer`](platform/identity/src/main/java/com/erp/platform/identity/infrastructure/initializer/DataInitializer.java:35).
- **Docker:** `FROM runtime AS pos` in `infrastructure/docker/Dockerfile.runtime`; a
  `pos-service` block in [`docker-compose.yml`](docker-compose.yml:326) (port 8086,
  `INVENTORY_SERVICE_URI`, healthcheck) — copy the `sales-service` block.
- **Env:** document `POS_SERVICE_URI`, `POS_PORT`, `IRD_CBMS_*` in `.env.example`.

---

## 7. Key design decisions & recommendations

**7.1 Scan-to-price flow (the happy path).**
`scan code → PosItemLookupService resolves item (barcode→SKU→code) → PosPricingService
resolves price + VAT → add to client cart with instant totals → checkout re-prices
server-side → domain computes → stock issued → session updated`. One network call per
scan; one atomic call per checkout.

**7.2 Price source (decide first).** Inventory exposes no price today. **Recommend
extending the Inventory `ItemService`/`ItemResponse` with a selling price (or a
price-list lookup)** so price has one owner; `PosQuickItem.defaultPrice` and a pricing
strategy layer handle overrides/promotions. Avoid a second, divergent price store in POS.

**7.3 Server-authoritative pricing.** Never trust client prices. Checkout takes
`{itemId, quantity, discountPct}` and re-resolves price + VAT. This is both a security
control and the fix for price drift between add-to-cart and pay.

**7.4 Barcode extensibility.** `BarcodeResolver` strategy chain means standard codes
ship in v1 while **price-embedded / weighted barcodes** (code encodes weight or price)
and future symbologies are additive resolvers — no checkout changes.

**7.5 Split tender** is not in the domain
([`complete(...)`](business/pos/domain/src/main/java/com/erp/business/pos/domain/PosTransaction.java:529)
takes a single `PosPaymentMethod`). Recommend single-tender v1 with a future-proof
`payments[]` DTO; needs product sign-off.

**7.6 Cash movements** (pay-in/out/drop) are absent — recommend a follow-up
`PosCashMovement` aggregate; not a v1 blocker.

**7.7 Returns:** v1 = void only; route genuine post-shift returns through the existing
Sales Return / credit-note flow to avoid duplicating CBMS-return logic.

**7.8 Reuse vs copy:** copy the small shared infra classes (numbering, exception
handler) per module, consistent with the codebase; de-duplicate into a library later
only if a third module needs it.

---

## 8. Risks & open questions
- **Price source ownership** (§7.2) blocks Phase 2 — needs a decision + a small
  Inventory interface change.
- **Split tender** and **cash movements** need product sign-off before Phase 3.
- **Numbering prefix/format** and **fiscal-year period key** must match Finance/IRD
  (`POS-` vs `INV-`, BS FY label).
- **CBMS payload** for POS bills should be validated against Sales `CbmsBillPayload`.
- **Insufficient stock:** decide whether checkout blocks or warns (Inventory may allow
  negative) — confirm desired behaviour.
- **Weighted/price-embedded barcodes:** confirm whether v1 needs them or standard only.

## 9. Suggested delivery order & rough sizing
| Phase | Deliverable | Depends on | Size |
|---|---|---|---|
| 0 | Application module skeleton + build green | — | S |
| 1 | Register + Session | 0 | M |
| 2 | Scan resolver + Pricing (+ Inventory price) | 0, Inventory | M |
| 3 | Checkout + Void (+ numbering) | 1, 2 | L |
| 4 | Quick items | 0 | S |
| 5 | CBMS sync worker | 3 | M |
| 6 | POS terminal UI + barcode hook + menu | 2, 3, 4 | L |
| 7 | Gateway / Identity / Docker / env wiring | 0–6 | S |

*S ≈ ≤3 days, M ≈ 3–5 days, L ≈ 1–2 weeks (one engineer, following existing patterns).*

---

## 10. Recommendation
Proceed by **mirroring the Sales module** and adding two isolated POS capabilities —
a **scan resolver** and a **pricing service** — with the **domain kept as the single
writer of all money** and **checkout re-pricing server-side**. Resolve the price-source
decision (§7.2) and the split-tender/cash-movement questions (§8) before Phase 3.
First demoable slice: Phase 0 → 1 → 2 → 3 (open session → **scan a barcode → price
auto-calculates** → checkout → stock deducted → void). The domain is already
investment-grade; the rest is mechanical, pattern-driven application + wiring work.
