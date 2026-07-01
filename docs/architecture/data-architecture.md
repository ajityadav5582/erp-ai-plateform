# Data Architecture

## Overview

The ERP AI Platform uses a polyglot persistence strategy, selecting the right database for each use case.

## Database Strategy

### Database per Service

Each microservice owns its database schema. Services never access other services' databases directly.

```
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│   Finance   │     HR      │  Inventory  │       Sales       │
│   Service   │   Service   │   Service   │     Service       │
├─────────────┼─────────────┼─────────────┼───────────────────┤
│  Finance    │    HR       │  Inventory  │      Sales        │
│   Database  │  Database   │  Database   │     Database      │
│ (PostgreSQL)│ (PostgreSQL)│ (PostgreSQL)│  (PostgreSQL)     │
└─────────────┴─────────────┴─────────────┴───────────────────┘
```

### Multi-Tenancy Data Model

Shared database, shared schema with tenant discriminator.

```sql
-- All tenant-specific tables include tenant_id
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    -- other columns
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_invoices_tenant_number UNIQUE (tenant_id, invoice_number)
);

-- Index for tenant isolation
CREATE INDEX idx_invoices_tenant_id ON invoices(tenant_id);
```

### Data Partitioning

- **Horizontal Partitioning**: By tenant for large tables
- **Time-based Partitioning**: For event and audit tables
- **Read Replicas**: For reporting and analytics

## Technology Stack

### Primary Database: PostgreSQL 16

**Use Cases:**
- Transactional data (OLTP)
- Relational data with complex queries
- ACID compliance required

**Configuration:**
- Connection pooling via HikariCP
- Read replicas for reporting
- Logical replication for CDC

### Cache: Redis 7

**Use Cases:**
- Session storage
- Rate limiting
- Distributed locking
- Hot data caching
- Pub/Sub for real-time features

**Configuration:**
- Cluster mode for scale
- Persistence enabled (AOF + RDB)
- Eviction policy: allkeys-lru

### Event Streaming: Kafka (KRaft)

**Use Cases:**
- Event sourcing
- Audit trails
- Inter-service communication
- Real-time data pipelines

**Topics:**
- `erpai.finance.invoice.created`
- `erpai.hr.employee.hired`
- `erpai.inventory.stock.adjusted`
- `erpai.sales.order.placed`

### Object Storage: MinIO / S3

**Use Cases:**
- Document storage
- Report exports
- AI model artifacts
- Backup storage

## Data Flow Patterns

### Command Query Responsibility Segregation (CQRS)

```
Write Path:                    Read Path:
Command ──▶ Aggregate ──▶     Query ──▶ Read Model ──▶
           │         │                  │
           ▼         ▼                  ▼
       Event Store    Projection    Materialized View
```

### Event Sourcing

```
Aggregate State ──▶ Event Store ──▶ Event Bus ──▶ Projections
       ▲                                                    │
       └────────────────────────────────────────────────────┘
```

### Change Data Capture (CDC)

```
PostgreSQL ──▶ Debezium ──▶ Kafka ──▶ Downstream Services
```

## Data Governance

### Data Classification

- **Public**: Marketing content, public APIs
- **Internal**: Internal documentation, non-sensitive metrics
- **Confidential**: Business data, financial records
- **Restricted**: PII, credentials, health data

### Data Retention

| Data Type | Retention | Reason |
|-----------|-----------|--------|
| Audit logs | 7 years | Compliance |
| Financial records | 7 years | Tax/legal |
| Employee records | 7 years post-termination | Legal |
| Event streams | 30 days | Operational |
| Metrics | 90 days | Operational |

### Backup Strategy

- **Full Backup**: Weekly (Sunday 02:00 UTC)
- **Incremental Backup**: Daily (02:00 UTC)
- **Continuous Archival**: WAL archiving to S3
- **Point-in-Time Recovery**: Up to 7 days
