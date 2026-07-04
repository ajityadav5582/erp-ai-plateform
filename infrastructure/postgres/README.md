# PostgreSQL Infrastructure

PostgreSQL is the primary relational database for the ERP AI Platform. Each microservice has its own dedicated database for data isolation and independent scaling.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `postgres:16-alpine` |
| **Port** | 5432 |
| **Container** | `erpai-postgres` |
| **Purpose** | Primary relational database |
| **Owner** | Platform Team |

## Databases

The platform uses multiple PostgreSQL databases, one per microservice:

| Database | Service | Description |
|----------|---------|-------------|
| `erpai_platform` | Platform | Shared infrastructure (users, tenants, settings) |
| `erpai_finance` | Finance | Finance domain (invoices, payments, accounts) |
| `erpai_hr` | HR | Human resources (employees, attendance, payroll) |
| `erpai_inventory` | Inventory | Stock management (products, warehouses, stock) |
| `erpai_manufacturing` | Manufacturing | Production (BOM, work orders, routing) |
| `erpai_procurement` | Procurement | Purchasing (PO, vendors, RFQ) |
| `erpai_sales` | Sales | Sales orders, customers, quotations |
| `erpai_ai` | AI/ML | AI models, predictions, training data |
| `erpai_integration` | Integration | External integrations, sync logs |
| `erpai_gateway` | Gateway | API gateway configuration, rate limits |

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_DB` | `erpai_platform` | Default database name |
| `POSTGRES_USER` | `erpai` | Database superuser |
| `POSTGRES_PASSWORD` | `erpai_dev_password` | Database password |

### PostgreSQL Settings

The container is configured with optimized settings for development:

```conf
shared_buffers = 256MB
work_mem = 16MB
maintenance_work_mem = 128MB
effective_cache_size = 1GB
random_page_cost = 1.1
effective_io_concurrency = 200
max_connections = 200
wal_buffers = 16MB
default_statistics_target = 100
log_statement = all
log_duration = on
log_min_messages = info
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
log_temp_files = 0
log_line_prefix = '%t [%p-%l] %q%u@%d '
```

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                        PostgreSQL                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ erpai_platform│  │ erpai_finance│  │ erpai_hr    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │erpai_inventory│ │erpai_manufact│ │erpai_procure│             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ erpai_sales │  │ erpai_ai    │  │erpai_integration│          │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  └──│ Flyway      │    │  Services   │    │  Exporters  │      │
│     │ (Migrations)│    │ (Hibernate) │    │ (Prometheus)│      │
│     └─────────────┘    └─────────────┘    └─────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **Flyway** | Runs database migrations on startup |
| **All Microservices** | Each service connects to its own database |
| **Postgres Exporter** | Scrapes metrics for Prometheus |
| **pgAdmin** | Web UI for database management (development) |
| **Keycloak** | Stores user identities and realm data (in dev-mem mode) |

## Quick Start

```bash
# Start PostgreSQL only
docker compose -f compose.base.yml -f compose.infrastructure.yml up postgres

# Run Flyway migrations
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway

# Access PostgreSQL
docker compose exec postgres psql -U erpai -d erpai_platform

# List all databases
docker compose exec postgres psql -U erpai -c "\l"
```

## Flyway Migrations

Each microservice has its own Flyway migration directory under `infrastructure/postgres/flyway/`. Migrations follow the naming convention:

```
V{version}__{description}.sql
```

Example:
- `V001__create_invoices_table.sql`
- `V002__add_payment_methods.sql`

### Running Migrations

```bash
# Run all migrations
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway

# Run migrations for a specific service
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway
```

## Extensions

The following PostgreSQL extensions are enabled:

| Extension | Purpose |
|-----------|---------|
| `uuid-ossp` | UUID generation |
| `pgcrypto` | Cryptographic functions |
| `pg_trgm` | Trigram text search |
| `btree_gist` | B-tree GiST index support |

## Backup & Recovery

See [`backups/README.md`](backups/README.md) for detailed backup procedures.

Quick backup:
```bash
# Run backup script
bash infrastructure/postgres/backups/backup.sh
```

## Monitoring

PostgreSQL metrics are exposed via the Postgres Exporter and collected by Prometheus:

| Metric | Description |
|--------|-------------|
| `pg_stat_activity_count` | Number of active connections |
| `pg_database_size` | Database size in bytes |
| `pg_stat_database_xact_commit` | Committed transactions |
| `pg_stat_database_xact_rollback` | Rolled back transactions |
| `pg_stat_user_tables_rows` | Row counts per table |

View in Grafana: **Dashboards → Database Metrics**

## Troubleshooting

### Connection Refused

```bash
# Check if PostgreSQL is running
docker compose -f compose.base.yml -f compose.infrastructure.yml ps postgres

# Check PostgreSQL logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs postgres
```

### Slow Queries

Enable slow query logging:
```sql
ALTER DATABASE erpai_platform SET log_min_duration_statement = 1000;  -- Log queries > 1s
```

### Connection Pool Exhausted

Check active connections:
```sql
SELECT count(*) FROM pg_stat_activity WHERE datname = 'erpai_platform';
```

Increase `max_connections` in `compose.infrastructure.yml` if needed.
