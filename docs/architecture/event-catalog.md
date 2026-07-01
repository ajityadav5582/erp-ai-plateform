# High-Level Event Catalog

## 1. Purpose

This document defines the business events exchanged between services in the ERP AI Platform. Events enable loose coupling, scalability, and real-time integration between modules.

## 2. Event Design Principles

- **Past Tense:** Events represent something that already happened
- **Immutable:** Events cannot be modified after publication
- **Self-Describing:** Events contain all necessary context
- **Idempotent:** Consumers can safely process duplicates
- **Versioned:** Events support schema evolution
- **Tenant-Scoped:** All events include tenant context

## 3. Event Categories

### 3.1 Platform Events

Events related to platform infrastructure and tenant management.

### 3.2 Identity Events

Events related to users, authentication, and authorization.

### 3.3 CRM Events

Events related to customer relationship management.

### 3.4 Sales Events

Events related to sales operations.

### 3.5 Procurement Events

Events related to purchasing and vendors.

### 3.6 Inventory Events

Events related to stock and warehouse management.

### 3.7 Accounting Events

Events related to financial operations.

### 3.8 HR Events

Events related to human resources.

### 3.9 Manufacturing Events

Events related to production operations.

### 3.10 AI Events

Events related to AI operations.

### 3.11 Compliance Events

Events related to Nepal compliance.

## 4. Event Catalog

### 4.1 Platform Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `TenantCreated` | Tenant Service | All services | New tenant provisioned |
| `TenantProvisioned` | Tenant Service | Notification Service | Tenant provisioning complete |
| `TenantSuspended` | Tenant Service | All services | Tenant access suspended |
| `TenantReactivated` | Tenant Service | All services | Tenant access restored |
| `TenantDeactivated` | Tenant Service | All services | Tenant deactivated |
| `ServiceRegistered` | Discovery Service | API Gateway | New service registered |
| `ServiceHealthChanged` | Discovery Service | Monitoring | Service health status changed |

---

### 4.2 Identity Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `UserCreated` | User Service | Notification Service, CRM | New user account created |
| `UserUpdated` | User Service | Audit Service | User profile updated |
| `UserActivated` | User Service | Notification Service | User account activated |
| `UserSuspended` | User Service | Notification Service | User account suspended |
| `UserLocked` | User Service | Notification Service | User account locked |
| `UserPasswordChanged` | User Service | Audit Service | User password changed |
| `RoleCreated` | Authorization Service | All services | New role created |
| `RoleUpdated` | Authorization Service | All services | Role permissions updated |
| `UserRoleAssigned` | Authorization Service | Audit Service | Role assigned to user |
| `UserRoleRevoked` | Authorization Service | Audit Service | Role revoked from user |

---

### 4.3 CRM Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `LeadCreated` | CRM Service | Sales Service, Notification Service | New lead captured |
| `LeadQualified` | CRM Service | Sales Service | Lead converted to account |
| `LeadDisqualified` | CRM Service | Notification Service | Lead disqualified |
| `AccountCreated` | CRM Service | Sales Service, Accounting Service | New customer account |
| `AccountUpdated` | CRM Service | Audit Service | Account details updated |
| `ContactAdded` | CRM Service | Sales Service | New contact added to account |
| `ContactUpdated` | CRM Service | Audit Service | Contact details updated |
| `OpportunityCreated` | CRM Service | Sales Service | New sales opportunity |
| `OpportunityStageChanged` | CRM Service | Sales Service, AI Sales Agent | Opportunity progressed |
| `OpportunityWon` | CRM Service | Sales Service, Accounting Service | Opportunity won |
| `OpportunityLost` | CRM Service | Sales Service, AI Sales Agent | Opportunity lost |
| `InteractionRecorded` | CRM Service | AI Sales Agent | Customer interaction logged |

---

### 4.4 Sales Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `QuotationCreated` | Sales Service | CRM Service, Notification Service | New quotation issued |
| `QuotationSent` | Sales Service | CRM Service | Quotation sent to customer |
| `QuotationViewed` | Sales Service | CRM Service, AI Sales Agent | Quotation viewed by customer |
| `QuotationApproved` | Sales Service | Inventory Service, Accounting Service | Quotation accepted |
| `QuotationRejected` | Sales Service | CRM Service, AI Sales Agent | Quotation declined |
| `QuotationExpired` | Sales Service | CRM Service | Quotation expired |
| `SalesOrderCreated` | Sales Service | Inventory Service, Accounting Service | Sales order created |
| `SalesOrderConfirmed` | Sales Service | Inventory Service, Notification Service | Sales order confirmed |
| `SalesOrderShipped` | Sales Service | Accounting Service, Customer | Order shipped |
| `SalesOrderDelivered` | Sales Service | Accounting Service | Order delivered |
| `SalesOrderCancelled` | Sales Service | Inventory Service, Accounting Service | Order cancelled |
| `SalesOrderReturned` | Sales Service | Inventory Service, Accounting Service | Order returned |

---

### 4.5 Procurement Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `PurchaseRequisitionCreated` | Procurement Service | Workflow Service | New purchase requisition |
| `PurchaseRequisitionApproved` | Procurement Service | Procurement Service | Requisition approved |
| `PurchaseRequisitionRejected` | Procurement Service | Notification Service | Requisition rejected |
| `PurchaseOrderCreated` | Procurement Service | Vendor, Inventory Service | PO issued to vendor |
| `PurchaseOrderAcknowledged` | Procurement Service | Inventory Service | Vendor acknowledged PO |
| `PurchaseOrderAmended` | Procurement Service | Vendor, Inventory Service | PO amended |
| `PurchaseOrderCancelled` | Procurement Service | Vendor, Inventory Service | PO cancelled |
| `GoodsReceived` | Procurement Service | Inventory Service, Accounting Service | Goods received |
| `GoodsReceiptConfirmed` | Procurement Service | Procurement Service | Receipt confirmed |
| `VendorInvoiceReceived` | Procurement Service | Accounting Service | Vendor invoice received |
| `VendorInvoiceMatched` | Procurement Service | Accounting Service | Three-way match done |
| `VendorPaymentScheduled` | Accounting Service | Vendor, Notification Service | Payment scheduled |
| `VendorPaymentProcessed` | Accounting Service | Vendor, Procurement Service | Payment processed |

---

### 4.6 Inventory Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `ProductCreated` | Inventory Service | Procurement Service, Sales Service | New product created |
| `ProductUpdated` | Inventory Service | Audit Service | Product master data updated |
| `ProductDeactivated` | Inventory Service | Sales Service, Procurement Service | Product deactivated |
| `WarehouseCreated` | Inventory Service | Inventory Service | New warehouse created |
| `StockReceived` | Inventory Service | Accounting Service, AI Inventory Agent | Stock received |
| `StockIssued` | Inventory Service | Accounting Service, AI Inventory Agent | Stock issued |
| `StockTransferred` | Inventory Service | Accounting Service | Stock transferred |
| `StockAdjusted` | Inventory Service | Accounting Service, AI Inventory Agent | Stock adjusted |
| `StockCountStarted` | Inventory Service | Notification Service | Physical count started |
| `StockCountCompleted` | Inventory Service | Accounting Service | Physical count completed |
| `LowStockAlert` | Inventory Service | Notification Service, AI Inventory Agent | Stock below reorder point |
| `ProductExpired` | Inventory Service | Notification Service, AI Inventory Agent | Product expired |
| `BatchCreated` | Inventory Service | Quality Service | New batch created |
| `SerialNumberRegistered` | Inventory Service | Asset Service | Serial number registered |

---

### 4.7 Accounting Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `ChartOfAccountCreated` | Accounting Service | All services | New account created |
| `JournalEntryCreated` | Accounting Service | Audit Service | Journal entry created |
| `JournalEntryPosted` | Accounting Service | Report Service | Entry posted to ledger |
| `JournalEntryReversed` | Accounting Service | Audit Service | Entry reversed |
| `FiscalPeriodClosed` | Accounting Service | All services | Fiscal period closed |
| `FiscalPeriodOpened` | Accounting Service | All services | New fiscal period opened |
| `BankReconciliationStarted` | Accounting Service | Notification Service | Reconciliation started |
| `BankReconciliationCompleted` | Accounting Service | Report Service | Reconciliation completed |
| `AccountBalanceUpdated` | Accounting Service | Report Service | Account balance changed |
| `InvoiceIssued` | Sales Service → Accounting | Accounting Service, AI Finance Assistant | Invoice issued |
| `PaymentReceived` | Sales Service → Accounting | Accounting Service, AI Finance Assistant | Payment received |
| `VendorInvoiceRecorded` | Procurement Service → Accounting | Accounting Service | Vendor invoice recorded |
| `VendorPaymentMade` | Accounting Service | Procurement Service, Vendor | Payment made to vendor |
| `PayrollProcessed` | HR Service → Accounting | Accounting Service, AI Finance Assistant | Payroll processed |

---

### 4.8 HR Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `EmployeeCreated` | HR Service | User Service, Notification Service | New employee onboarded |
| `EmployeeUpdated` | HR Service | Audit Service | Employee data updated |
| `EmployeeActivated` | HR Service | User Service, Notification Service | Employee activated |
| `EmployeeSuspended` | HR Service | User Service, Notification Service | Employee suspended |
| `EmployeeTerminated` | HR Service | User Service, Accounting Service | Employee offboarded |
| `AttendanceRecorded` | HR Service | Payroll Service, AI HR Assistant | Daily attendance logged |
| `AttendanceCorrected` | HR Service | Payroll Service | Attendance corrected |
| `LeaveRequested` | HR Service | Workflow Service, Notification Service | Leave application submitted |
| `LeaveApproved` | HR Service | Payroll Service, Notification Service | Leave approved |
| `LeaveRejected` | HR Service | Notification Service | Leave rejected |
| `LeaveCancelled` | HR Service | Payroll Service | Leave cancelled |
| `PayrollRunStarted` | HR Service | Notification Service | Payroll processing started |
| `PayslipGenerated` | HR Service | File Service, Notification Service | Individual payslip ready |
| `PayrollProcessed` | HR Service | Accounting Service, AI Finance Assistant | Payroll run completed |

---

### 4.9 Manufacturing Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `BillOfMaterialCreated` | Manufacturing Service | Inventory Service, Procurement Service | New BOM created |
| `BillOfMaterialUpdated` | Manufacturing Service | Inventory Service | BOM updated |
| `ProductionPlanCreated` | Manufacturing Service | Inventory Service, HR Service | Production plan created |
| `WorkOrderCreated` | Manufacturing Service | Inventory Service, HR Service | Work order issued |
| `WorkOrderStarted` | Manufacturing Service | Inventory Service | Production started |
| `WorkOrderPaused` | Manufacturing Service | Notification Service | Production paused |
| `WorkOrderResumed` | Manufacturing Service | Notification Service | Production resumed |
| `WorkOrderCompleted` | Manufacturing Service | Inventory Service, Accounting Service | Production completed |
| `WorkOrderCancelled` | Manufacturing Service | Inventory Service | Production cancelled |
| `QualityInspectionCreated` | Manufacturing Service | Quality Service | Inspection scheduled |
| `QualityInspectionPassed` | Quality Service | Manufacturing Service, Inventory Service | QC passed |
| `QualityInspectionFailed` | Quality Service | Manufacturing Service, Quality Service | QC failed |
| `NonConformanceCreated` | Quality Service | Workflow Service, Notification Service | Quality issue identified |
| `ProductionOutputRecorded` | Manufacturing Service | Inventory Service, Accounting Service | Output recorded |

---

### 4.10 AI Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `AiChatStarted` | AI Platform Service | Audit Service | AI conversation started |
| `AiChatMessage` | AI Platform Service | Audit Service | AI message exchanged |
| `AiChatEnded` | AI Platform Service | Audit Service | AI conversation ended |
| `AiCreditConsumed` | AI Platform Service | License Service | AI credits consumed |
| `AiAgentInvoked` | AI Platform Service | Audit Service | AI agent invoked |
| `AiInsightGenerated` | AI Analytics | Notification Service, Report Service | AI insight generated |
| `AiAnomalyDetected` | AI Analytics | Notification Service, Workflow Service | AI anomaly detected |
| `AiRecommendationCreated` | AI Agents | Notification Service | AI recommendation available |
| `AiDocumentProcessed` | AI Platform Service | File Service | AI processed document |
| `AiModelInvoked` | AI Platform Service | Metrics Service | LLM model invoked |

---

### 4.11 Compliance Events

| Event | Publisher | Subscribers | Description |
|-------|-----------|-------------|-------------|
| `IrdInvoiceSubmitted` | IRD Service | Audit Service, Notification Service | Invoice submitted to IRD |
| `IrdSubmissionAcknowledged` | IRD Service | Sales Service | IRD acknowledged submission |
| `IrdSubmissionRejected` | IRD Service | Notification Service, Sales Service | IRD rejected submission |
| `TaxReturnPrepared` | Tax Services | Notification Service | Tax return ready for review |
| `TaxReturnSubmitted` | Tax Services | Audit Service, Notification Service | Tax return submitted |
| `TdsCertificateGenerated` | TDS Service | File Service, Notification Service | TDS certificate generated |
| `FiscalPeriodClosed` | Fiscal Year Service | All services | Fiscal period closed |
| `ComplianceAlert` | Compliance Services | Notification Service, Workflow Service | Compliance issue detected |

---

## 5. Event Flow Examples

### 5.1 Sales Order Flow

```
QuotationCreated
    ↓
QuotationApproved
    ↓
SalesOrderCreated
    ↓
SalesOrderConfirmed
    ↓
[Inventory: Stock reserved]
    ↓
SalesOrderShipped
    ↓
[Inventory: Stock issued]
    ↓
[Accounting: Invoice issued]
    ↓
InvoiceIssued
    ↓
[Accounting: Journal entry posted]
    ↓
PaymentReceived
    ↓
[Accounting: Payment applied]
```

### 5.2 Procurement Flow

```
PurchaseRequisitionCreated
    ↓
PurchaseRequisitionApproved
    ↓
PurchaseOrderCreated
    ↓
PurchaseOrderAcknowledged
    ↓
GoodsReceived
    ↓
[Inventory: Stock received]
    ↓
VendorInvoiceReceived
    ↓
VendorInvoiceMatched
    ↓
[Accounting: Liability recorded]
    ↓
VendorPaymentScheduled
    ↓
VendorPaymentProcessed
    ↓
[Accounting: Payment recorded]
```

### 5.3 Employee Onboarding Flow

```
EmployeeCreated
    ↓
[User Service: User account created]
    ↓
UserCreated
    ↓
[Notification Service: Welcome email]
    ↓
[HR Service: Onboarding tasks created]
    ↓
[Workflow Service: Onboarding workflow started]
    ↓
AttendanceRecorded (daily)
    ↓
[Payroll Service: Payroll calculated]
    ↓
PayrollProcessed
    ↓
[Accounting Service: Salary expense recorded]
```

## 6. Event Naming Conventions

### 6.1 Format

```
{Domain}{PastTenseVerb}Event

# Examples
TenantCreated
UserActivated
LeadQualified
SalesOrderConfirmed
StockReceived
JournalEntryPosted
EmployeeTerminated
```

### 6.2 Verb Guidelines

| Action | Verb | Example |
|--------|------|---------|
| Create | Created | `TenantCreated` |
| Update | Updated | `UserUpdated` |
| Delete/Remove | Deleted | `ProductDeleted` |
| Start | Started | `WorkOrderStarted` |
| Complete | Completed | `WorkOrderCompleted` |
| Cancel | Cancelled | `OrderCancelled` |
| Approve | Approved | `QuotationApproved` |
| Reject | Rejected | `QuotationRejected` |
| Submit | Submitted | `ReturnSubmitted` |
| Receive | Received | `PaymentReceived` |
| Send | Sent | `InvoiceSent` |
| Assign | Assigned | `RoleAssigned` |
| Revoke | Revoked | `RoleRevoked` |

## 7. Event Schema Standards

### 7.1 Required Fields

```java
public interface DomainEvent {
    UUID eventId();           // Unique event identifier
    String eventType();       // Event type name
    Long tenantId();          // Tenant context
    Instant occurredAt();     // When the event occurred
    Instant publishedAt();    // When the event was published
    Integer version();        // Schema version
}
```

### 7.2 Event Envelope

```java
public record DomainEventEnvelope(
    UUID eventId,
    String eventType,
    Long tenantId,
    Instant occurredAt,
    Instant publishedAt,
    Integer version,
    Map<String, Object> metadata,
    Object payload
) {}
```

### 7.3 Metadata

| Key | Description | Example |
|-----|-------------|---------|
| `correlationId` | Request correlation ID | `req-abc123` |
| `causationId` | Causation event ID | `event-xyz789` |
| `userId` | User who triggered | `user-123` |
| `sourceService` | Publishing service | `sales-service` |
| `environment` | Deployment environment | `production` |

## 8. Event Processing Guarantees

### 8.1 Delivery Guarantees

- **At-least-once:** Events delivered at least once
- **Idempotent processing:** Consumers handle duplicates
- **Ordering:** Events ordered within partition
- **Retention:** Events retained for 7 days

### 8.2 Error Handling

- **Dead Letter Queue:** Failed events after max retries
- **Retry policy:** Exponential backoff, max 3 retries
- **Alerting:** DLQ depth alerts
- **Manual reprocessing:** DLQ events can be reprocessed

## 9. Event Monitoring

### 9.1 Metrics

- Event throughput (events/second)
- Event latency (publish to consume)
- Error rate (failed events)
- DLQ depth
- Consumer lag

### 9.2 Alerts

- DLQ depth > 100
- Error rate > 1%
- Consumer lag > 1000
- Throughput drop > 50%
