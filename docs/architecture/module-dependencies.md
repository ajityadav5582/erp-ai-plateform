# Module Dependency Map

## 1. Purpose

This document defines the recommended implementation order for the ERP AI Platform modules. The dependency map ensures that modules are built in an order that minimizes dependencies and enables incremental value delivery.

## 2. Dependency Principles

- **Foundation First:** Platform services before business modules
- **Identity Before Authorization:** Users before permissions
- **Data Before Process:** Master data before transactions
- **Core Before Extended:** Essential modules before nice-to-have
- **Event-Driven:** Modules communicate via events, not direct calls
- **Database per Service:** Each module owns its data

## 3. Implementation Phases

### Phase 1: Platform Foundation (Weeks 1-4)

**Goal:** Establish the foundational platform services that all other modules depend on.

```
┌─────────────────────────────────────────────────────────────┐
│                    Phase 1: Platform Foundation              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ API Gateway │  │   Config    │  │     Discovery       │  │
│  │  Service    │  │  Service    │  │     Service         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Tenant    │  │    User     │  │   Authorization     │  │
│  │  Service    │  │  Service    │  │     Service         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Audit     │  │    File     │  │   Notification      │  │
│  │  Service    │  │  Service    │  │     Service         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- API Gateway is the entry point for all traffic
- Config and Discovery enable service communication
- Tenant is the foundation of multi-tenancy
- User and Authorization enable identity and access
- Audit, File, and Notification are cross-cutting utilities

**Deliverables:**
- All platform services operational
- Basic authentication and authorization working
- Tenant provisioning functional
- Basic audit logging in place

---

### Phase 2: Core Business Services (Weeks 5-8)

**Goal:** Implement core business services that don't depend on other business modules.

```
┌─────────────────────────────────────────────────────────────┐
│                  Phase 2: Core Business Services             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    CRM Service                       │   │
│  │  - Lead management                                  │   │
│  │  - Account management                               │   │
│  │  - Contact management                               │   │
│  │  - Opportunity tracking                             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 Localization Service                 │   │
│  │  - Translation management                           │   │
│  │  - Locale support                                   │   │
│  │  - Regional formats                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Country Configuration Service            │   │
│  │  - Country metadata                                 │   │
│  │  - Tax configuration                                │   │
│  │  - Nepal compliance pack                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- CRM is the foundation of customer relationships
- Localization enables multi-language support
- Country Configuration enables Nepal-specific features
- These services have minimal dependencies on other business modules

**Deliverables:**
- CRM fully functional
- Multi-language support working
- Nepal compliance pack integrated

---

### Phase 3: Sales and Procurement (Weeks 9-12)

**Goal:** Implement sales and procurement workflows.

```
┌─────────────────────────────────────────────────────────────┐
│                Phase 3: Sales and Procurement                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   Sales Service                     │   │
│  │  - Quotation management                             │   │
│  │  - Sales order management                           │   │
│  │  - Order fulfillment                                │   │
│  │  - Commission calculation                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                Procurement Service                  │   │
│  │  - Purchase requisition                             │   │
│  │  - Vendor management                                │   │
│  │  - Purchase order management                        │   │
│  │  - Goods receipt                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 Inventory Service                   │   │
│  │  - Product master data                              │   │
│  │  - Warehouse management                             │   │
│  │  - Stock level tracking                             │   │
│  │  - Inventory movements                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- Sales depends on CRM (customers)
- Procurement depends on Inventory (products)
- Inventory is the foundation for stock management
- These form the core operational ERP cycle

**Deliverables:**
- End-to-end sales cycle (quotation → order → fulfillment)
- Procurement cycle (requisition → PO → receipt)
- Inventory management functional

---

### Phase 4: Financial Core (Weeks 13-16)

**Goal:** Implement accounting and financial management.

```
┌─────────────────────────────────────────────────────────────┐
│                  Phase 4: Financial Core                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               Accounting Service                    │   │
│  │  - Chart of accounts                                │   │
│  │  - Journal entries                                  │   │
│  │  - General ledger                                   │   │
│  │  - Accounts payable                                 │   │
│  │  - Accounts receivable                              │   │
│  │  - Bank reconciliation                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   HR Service                        │   │
│  │  - Employee management                              │   │
│  │  - Attendance tracking                              │   │
│  │  - Leave management                                 │   │
│  │  - Payroll processing                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- Accounting depends on Sales (invoices) and Procurement (bills)
- HR depends on User Service (employees)
- Financial data is critical and needs careful implementation
- Payroll depends on attendance and leave data

**Deliverables:**
- Complete double-entry bookkeeping
- Accounts payable and receivable
- Payroll processing
- Financial reporting

---

### Phase 5: Extended ERP (Weeks 17-20)

**Goal:** Implement extended ERP modules.

```
┌─────────────────────────────────────────────────────────────┐
│                  Phase 5: Extended ERP                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Manufacturing Service                  │   │
│  │  - Bill of materials                                │   │
│  │  - Production planning                              │   │
│  │  - Work order management                            │   │
│  │  - Quality control                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Project Management Service                │   │
│  │  - Project planning                                 │   │
│  │  - Task management                                  │   │
│  │  - Resource allocation                              │   │
│  │  - Time tracking                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               Helpdesk Service                      │   │
│  │  - Ticket management                                │   │
│  │  - SLA tracking                                     │   │
│  │  - Knowledge base                                   │   │
│  │  - Customer support                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- Manufacturing depends on Inventory (materials) and Procurement (purchasing)
- Project Management depends on HR (resources) and Sales (projects)
- Helpdesk depends on CRM (customers)
- These modules extend core ERP functionality

**Deliverables:**
- Manufacturing operations
- Project management
- Customer support

---

### Phase 6: AI and Advanced Features (Weeks 21-24)

**Goal:** Implement AI platform and advanced features.

```
┌─────────────────────────────────────────────────────────────┐
│                Phase 6: AI and Advanced Features             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  AI Platform                        │   │
│  │  - AI Platform Service                              │   │
│  │  - AI Sales Agent                                   │   │
│  │  - AI Finance Assistant                             │   │
│  │  - AI HR Assistant                                  │   │
│  │  - AI Analytics                                     │   │
│  │  - AI Knowledge Base                                │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ enhances                          │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            Asset Management Service                 │   │
│  │  - Asset tracking                                   │   │
│  │  - Depreciation                                     │   │
│  │  - Maintenance scheduling                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ depends on                        │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          Quality Management Service                 │   │
│  │  - Quality plans                                    │   │
│  │  - Inspection management                            │   │
│  │  - Non-conformance tracking                         │   │
│  │  - CAPA management                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Why this order:**
- AI platform enhances all existing modules
- AI requires business data to be useful
- Asset Management depends on Accounting (depreciation)
- Quality Management depends on Manufacturing and Inventory

**Deliverables:**
- AI-powered features across all modules
- Asset management
- Quality management

---

## 4. Dependency Graph

```
                    ┌─────────────────┐
                    │  API Gateway    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   Tenant  │ │    User   │ │    Auth   │
        │  Service  │ │  Service  │ │  Service  │
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │     CRM         │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │     Sales       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │Procurement│ │Inventory  │ │Accounting│
        │  Service  │ │  Service  │ │  Service  │
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │       HR        │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Manufacturing  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   Project │ │ Helpdesk  │ │   Asset   │
        │Management │ │           │ │Management │
        └───────────┘ └───────────┘ └───────────┘
                             │
                    ┌────────▼────────┐
                    │     Quality     │
                    │   Management    │
                    └─────────────────┘
                             │
                    ┌────────▼────────┐
                    │   AI Platform   │
                    │  (enhances all) │
                    └─────────────────┘
```

## 5. Dependency Rules

### 5.1 Allowed Dependencies

| Module | Can Depend On |
|--------|--------------|
| API Gateway | All services |
| Platform Services | Other platform services |
| CRM | Platform services |
| Sales | Platform, CRM |
| Procurement | Platform, Inventory |
| Inventory | Platform |
| Accounting | Platform, Sales, Procurement, HR |
| HR | Platform, User |
| Manufacturing | Platform, Inventory, Procurement |
| Project Management | Platform, HR, Sales |
| Helpdesk | Platform, CRM |
| Asset Management | Platform, Accounting, HR |
| Quality Management | Platform, Manufacturing, Inventory |
| AI Platform | All services |
| Nepal Compliance | Platform, Sales, Accounting, HR |

### 5.2 Prohibited Dependencies

| Module | Cannot Depend On |
|--------|-----------------|
| Platform Services | Business modules |
| CRM | Sales, Procurement, etc. |
| Sales | Procurement, Inventory, etc. |
| Inventory | Sales, Procurement, etc. |
| Accounting | Manufacturing, etc. |
| Business modules | Other business modules (use events) |

## 6. Implementation Strategy

### 6.1 Vertical Slicing

Each phase delivers a vertical slice of functionality:
- Frontend (web/mobile)
- Backend API
- Database
- Integration

### 6.2 Incremental Delivery

- Each phase delivers production-ready features
- No big-bang releases
- Continuous deployment per module

### 6.3 Team Organization

| Phase | Teams | Focus |
|-------|-------|-------|
| 1 | Platform Team | Foundation services |
| 2 | Platform + CRM Team | Core business |
| 3 | Sales + Inventory Team | Operations |
| 4 | Finance + HR Team | Financial core |
| 5 | Manufacturing Team | Extended ERP |
| 6 | AI Team | Intelligence layer |

## 7. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Platform delays | Parallel development where possible |
| Dependency changes | Event-driven communication |
| Scope creep | Strict phase boundaries |
| Integration issues | Contract testing between modules |
| Performance bottlenecks | Early performance testing |
