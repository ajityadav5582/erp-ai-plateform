# Integration Architecture

## Overview

The ERP AI Platform uses an event-driven integration architecture with API-first design principles.

## API Gateway

### Spring Cloud Gateway

- Single entry point for all client requests
- Protocol translation (HTTP/gRPC/WebSocket)
- Authentication and authorization
- Rate limiting and circuit breaking
- Request/response transformation

### Routing

```
Client Request ──▶ Gateway ──▶ Route Determination
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              Finance Service  HR Service    Inventory Service
```

## Event-Driven Architecture

### Event Bus: Apache Kafka

**Topic Organization:**
```
erpai.<context>.<aggregate>.<event>
```

Examples:
- `erpai.finance.invoice.created`
- `erpai.hr.employee.hired`
- `erpai.inventory.stock.adjusted`

### Event Schema

```json
{
  "eventId": "uuid",
  "eventType": "InvoiceCreated",
  "eventVersion": "1.0",
  "timestamp": "2024-01-01T00:00:00Z",
  "tenantId": "uuid",
  "correlationId": "uuid",
  "causationId": "uuid",
  "payload": {
    "invoiceId": "uuid",
    "invoiceNumber": "INV-001",
    "amount": 1000.00,
    "currency": "USD"
  },
  "metadata": {
    "userId": "uuid",
    "source": "finance-service"
  }
}
```

### Event Patterns

#### Event Sourcing

```
Command ──▶ Aggregate ──▶ Event ──▶ Event Store
                                    │
                                    ▼
                              Projection ──▶ Read Model
```

#### Pub/Sub

```
Publisher ──▶ Topic ──▶ Subscribers
                    │
                    ├──▶ Service A
                    ├──▶ Service B
                    └──▶ Service C
```

#### Saga Pattern

```
Orchestrator ──▶ Step 1 ──▶ Step 2 ──▶ Step 3
       │              │          │          │
       │              ▼          ▼          ▼
       │           Event      Event      Event
       │              │          │          │
       └──────────────┴──────────┴──────────┘
                    Compensation on failure
```

## Service Communication

### Synchronous

- REST APIs (JSON)
- gRPC (Protocol Buffers)
- GraphQL (for complex queries)

### Asynchronous

- Kafka events
- Redis Pub/Sub (for real-time features)

### Service Discovery

- Kubernetes DNS
- Spring Cloud LoadBalancer
- Health checks for availability

## External Integrations

### Integration Patterns

- **API Integration**: REST/GraphQL clients
- **Message Queue**: Kafka producers/consumers
- **File Transfer**: SFTP/S3 for bulk data
- **Database Link**: Read-only replicas for reporting

### Integration Layer

```
┌─────────────────────────────────────────────────────────────┐
│                    External Integrations                      │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│   Payment   │   Email     │   SMS       │   EDI/ERP         │
│  Gateways   │  Providers  │ Providers   │   Connectors      │
└─────────────┴─────────────┴─────────────┴───────────────────┘
         │             │            │              │
         └─────────────┼────────────┼──────────────┘
                       ▼            ▼
                 ┌─────────────────────┐
                 │  Integration Layer  │
                 │  (Anti-Corruption)  │
                 └─────────────────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │   Internal Services │
                 └─────────────────────┘
```

## API Design

### RESTful Conventions

```
GET    /api/v1/finance/invoices          # List invoices
POST   /api/v1/finance/invoices          # Create invoice
GET    /api/v1/finance/invoices/{id}     # Get invoice
PUT    /api/v1/finance/invoices/{id}     # Update invoice
DELETE /api/v1/finance/invoices/{id}     # Delete invoice
```

### Versioning

- URL versioning: `/api/v1/`, `/api/v2/`
- Header versioning: `X-API-Version: 1`

### Response Format

```json
{
  "data": { ... },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z",
    "version": "1.0"
  },
  "links": {
    "self": "/api/v1/finance/invoices/123"
  }
}
```

### Error Format

```json
{
  "error": {
    "code": "INVOICE_NOT_FOUND",
    "message": "Invoice with ID 123 not found",
    "details": [],
    "requestId": "uuid",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```
