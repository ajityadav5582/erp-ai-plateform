# Architecture Decisions — POS → Sales and Sales/POS → IRD

**Status:** Approved architectural decisions (documentation only).  
**Scope:** POS-to-Sales integration and Sales/POS-to-IRD (CBMS) integration.  
**Constraint:** No application code changes recorded here; unresolved implementation details listed separately.

## Approved Decisions

1. **POS and Sales remain separate Spring Boot applications.**  
   Evidence: `business/sales/application/.../SalesApplication.java`, `business/pos/application/.../PosApplication.java`, `docker-compose.yml` services `sales-service` (port 8085) and `pos-service` (port 8086).

2. **All ERP services use one shared PostgreSQL database.**  
   Evidence: `docker-compose.yml` env `POSTGRES_HOST: postgres`, `POSTGRES_PORT: 5432`, DB `erpai_platform` for both services.

3. **POS → Sales integration uses the existing database-backed `PosSalesIntegrationJob`.**  
   Evidence: `PosTransactionServiceImpl.checkout(...)` creates `PosSalesIntegrationJob` (table `pos_sales_integration_jobs`); `PosSalesIntegrationJobRepository` provides `findPendingForUpdate(...)`.

4. **Kafka will not be used for POS → Sales integration.**  
   The existing `PosSalesEventPublisher` / `PosTransactionEventConsumer` Kafka path is to be retired in favor of the DB-backed job.

5. **Sales service processes POS integration jobs and creates Sales invoices.**  
   Evidence: `SalesInvoiceServiceImpl.createPosInvoice(...)` (`business/sales/application/.../invoice/SalesInvoiceServiceImpl.java`).

6. **POS payments must be recorded against the corresponding Sales invoice using existing payment functionality, with idempotency protection.**  
   Reuse existing `SalesInvoice` payment recording (`recordPayment`) and idempotency guards.

7. **Each company uses a default Walk-in Customer for POS transactions without a selected customer.**  
   Resolves the `SalesInvoice.customerId` non-null constraint for walk-in POS sales.

8. **All eligible Sales invoices, including POS-originated invoices, use the central IRD outbox for CBMS submission.**  
   Evidence: `IrdOutboxService.enqueueSalesBill(...)` (`integration/.../outbox/service/IrdOutboxService.java`).

9. **Central IRD integration is the single authoritative CBMS submission mechanism.**  
   Evidence: `IrdFiscalSyncPort`, `IrdSyncOutboxEntity`, `IrdImmediateOutboxListener`, `IrdOutboxRetryScheduler`.

10. **Direct POS/Sales CBMS workers must not independently submit invoices once central integration is active.**  
    Direct clients `CbmsClient`/`PosCbmsClient` and workers `CbmsSyncWorker`/`PosCbmsSyncWorker` are to be retired after parity.

11. **Development testing must distinguish simulated responses from actual IRD acknowledgements.**  
    `IrdSyncOutboxStatus` and `cbms_synced` flags must not conflate simulated success with real IRD acknowledgement.

12. **Do not introduce Kafka, another messaging framework, or unnecessary infrastructure.**  
    Reuse existing DB-backed outbox and scheduled workers.

## Unresolved Implementation Details (to be resolved before coding)

- Service-to-service authentication mechanism for any HTTP fallback (if DB-backed polling is insufficient).
- Whether POS-originated Sales invoices should be created with payment recorded (balance 0) or left unpaid.
- Walk-in Customer provisioning: per-company default customer creation vs. relaxation of `SalesInvoice.customerId` non-null constraint.
- Official IRD sandbox/test environment URL and credentials (not present in code; requires IRD documentation/ops config).
- Whether `pos_transactions` CBMS tracking columns remain authoritative or are superseded by `sales_invoices`/outbox status.
- Component scanning/auto-configuration of `com.erpai.integration.ird` in sales/pos apps (runtime verification needed).
- Credit note handling in `IrdImmediateOutboxListener` and `IrdOutboxRetryScheduler` (currently sales-bill only).
- `IrdOutboxService` hardcodes `sourceModule = IrdSourceModule.POS`; needs parameterization for SALES vs POS.
- `IrdFiscalSyncAdapter` is a no-op; requires real CBMS HTTP implementation.
- `createPosInvoice` passes null `soNumber`/`customerCode`/`branchId`/`salesOrderId`; needs alignment with entity constraints.
- `createPosInvoice` lacks `@RequiresPermission` annotation.

## Preserved Architecture

- Separate Spring Boot apps (sales-service, pos-service) sharing one Postgres DB.
- DB-backed `PosSalesIntegrationJob` outbox for POS→Sales.
- Central IRD outbox (`ird_sync_outbox`) as single CBMS submission mechanism.
- Existing `SalesInvoiceService.createPosInvoice(...)` as the Sales-side entry point.
- Existing payment and idempotency mechanisms.
- Existing `CompanyIrdConfig` + `AesIrdCryptoAdapter` for IRD credentials.
