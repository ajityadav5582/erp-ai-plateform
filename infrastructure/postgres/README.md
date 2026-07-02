# ERP AI Platform - PostgreSQL Infrastructure

Enterprise-grade PostgreSQL setup with multiple databases, Flyway migrations, and comprehensive backup support.

## Architecture

```
infrastructure/postgres/
├── init/                          # Database initialization scripts
│   ├── 01-create-databases.sql    # Creates all microservice databases
│   ├── 02-create-users.sql        # Creates dedicated users per service
│   └── 03-create-extensions.sql   # Enables common extensions
├── flyway/                        # Flyway migration directories
│   ├── flyway.conf                # Flyway configuration
│   ├── platform/                  # Platform database migrations
│   ├── finance/                   # Finance service migrations
│   ├── hr/                        # HR service migrations
│   ├── inventory/                 # Inventory service migrations
│   ├── manufacturing/             # Manufacturing service migrations
│   ├── procurement/               # Procurement service migrations
│   ├── sales/                     # Sales service migrations
│   ├── ai/                        # AI service migrations
│   ├── integration/               # Integration service migrations
│   └── gateway/                   # Gateway service migrations
└── backups/                       # Backup scripts and documentation
    └── README.md                  # Backup procedures
```

## Databases

| Database | Service | Purpose |
|----------|---------|---------|
| `erpai_platform` | Platform | Shared infrastructure (audit logs, event outbox) |
| `erpai_finance` | Finance | Finance domain (invoices, payments, accounting) |
| `erpai_hr` | HR | Human resources (employees, payroll, attendance) |
| `erpai_inventory` | Inventory | Stock management (products, warehouses, movements) |
| `erpai_manufacturing` | Manufacturing | Production (BOM, work orders, routing) |
| `erpai_procurement` | Procurement | Purchasing (POs, vendors, RFQs) |
| `erpai_sales` | Sales | Sales orders, customers, quotations |
| `erpai_ai` | AI/ML | AI models, embeddings, predictions |
| `erpai_integration` | Integration | External integrations, sync data |
| `erpai_gateway` | Gateway | API gateway configuration, rate limits |

## Users

Each microservice has a dedicated database user with least-privilege access:

| User | Database | Access |
|------|----------|--------|
| `erpai` | All | Superuser (platform admin) |
| `erpai_finance` | `erpai_finance` | Full access to finance schema |
| `erpai_hr` | `erpai_hr` | Full access to hr schema |
| `erpai_inventory` | `erpai_inventory` | Full access to inventory schema |
| `erpai_manufacturing` | `erpai_manufacturing` | Full access to manufacturing schema |
| `erpai_procurement` | `erpai_procurement` | Full access to procurement schema |
| `erpai_sales` | `erpai_sales` | Full access to sales schema |
| `erpai_ai` | `erpai_ai` | Full access to ai schema |
| `erpai_integration` | `erpai_integration` | Full access to integration schema |
| `erpai_gateway` | `erpai_gateway` | Full access to gateway schema |

## Extensions

The following PostgreSQL extensions are enabled by default:

| Extension | Purpose |
|-----------|---------|
| `uuid-ossp` | UUID generation |
| `pg_trgm` | Full-text search and similarity |
| `btree_gin` | GIN index support for btree types |
| `btree_gist` | GiST index support for btree types |
| `pg_stat_statements` | Query performance statistics |
| `pgcrypto` | Cryptographic functions |

## Flyway Migrations

### Directory Structure

Each microservice has its own Flyway migration directory:

```
infrastructure/postgres/flyway/
├── flyway.conf                    # Global Flyway configuration
├── platform/V001__baseline.sql    # Platform baseline
├── finance/V001__baseline.sql     # Finance baseline
├── hr/V001__baseline.sql          # HR baseline
├── ...                            # Other services
```

### Naming Convention

Follow the project's database standards:

```
V{version}__{description}.sql
```

Examples:
- `V001__create_invoices_table.sql`
- `V002__add_payment_methods.sql`
- `V003__create_indexes.sql`

### Running Migrations

#### Docker Compose

```bash
# Start PostgreSQL
docker compose -f compose.base.yml -f compose.infrastructure.yml up postgres

# Run Flyway migrations (with development profile)
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway
```

#### Manual Flyway

```bash
# Run migrations for a specific service
flyway -configFiles=infrastructure/postgres/flyway/flyway.conf -placeholders.service=finance migrate

# Check migration status
flyway -configFiles=infrastructure/postgres/flyway/flyway.conf -placeholders.service=finance info

# Validate migrations
flyway -configFiles=infrastructure/postgres/flyway/flyway.conf -placeholders.service=finance validate
```

## Configuration

### PostgreSQL Settings

The PostgreSQL configuration is optimized for the ERP workload:

| Setting | Value | Purpose |
|---------|-------|---------|
| `shared_buffers` | 256MB | Memory for caching data |
| `work_mem` | 16MB | Memory per sort/hash operation |
| `maintenance_work_mem` | 128MB | Memory for maintenance operations |
| `effective_cache_size` | 1GB | Estimated cache size |
| `random_page_cost` | 1.1 | SSD-optimized page cost |
| `effective_io_concurrency` | 200 | Concurrent I/O operations |
| `max_connections` | 200 | Maximum connections |
| `wal_buffers` | 16MB | WAL buffer size |
| `log_statement` | all | Log all statements (dev) |
| `log_duration` | on | Log query duration |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | `erpai_dev_password` | PostgreSQL superuser password |
| `POSTGRES_DB` | `erpai_platform` | Default database name |
| `POSTGRES_USER` | `erpai` | Default superuser name |

## Health Checks

PostgreSQL health check configuration:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U erpai -d erpai_platform"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

## Volumes

| Volume | Purpose |
|--------|---------|
| `postgres_data` | PostgreSQL data directory |
| `postgres_wal` | WAL (Write-Ahead Log) files |

## Backup & Recovery

See [`backups/README.md`](backups/README.md) for detailed backup procedures.

## Terraform (AWS RDS)

For AWS deployments, see [`infrastructure/terraform/main.tf`](../terraform/main.tf) for RDS configuration.

Key RDS settings:
- Engine: PostgreSQL 16.4
- Instance class: db.r6g.xlarge
- Storage: 100 GB (encrypted)
- Multi-AZ: Enabled (production)
- Backup retention: 30 days

## Kubernetes

For Kubernetes deployments, see [`infrastructure/k8s/`](../k8s/) for PostgreSQL StatefulSet configuration.

## Troubleshooting

### Connection Issues

```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check PostgreSQL logs
docker compose logs postgres

# Test connection
docker compose exec postgres pg_isready -U erpai -d erpai_platform
```

### Migration Issues

```bash
# Check Flyway status
docker compose exec flyway flyway info

# Repair failed migrations
docker compose exec flyway flyway repair

# Validate migrations
docker compose exec flyway flyway validate
```

### Performance Issues

```sql
-- Check active connections
SELECT count(*) FROM pg_stat_activity;

-- Check slow queries
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## References

- [Database Standards](../docs/standards/04-database-standards.md)
- [Docker Standards](../docs/standards/16-docker-standards.md)
- [Flyway Documentation](https://flywaydb.org/documentation/)
