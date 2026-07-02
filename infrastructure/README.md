# ERP AI Platform - Infrastructure

Enterprise-grade infrastructure for the ERP AI Platform, including databases, messaging, identity, and deployment configurations.

## Directory Structure

```
infrastructure/
├── docker/                    # Docker configuration
│   ├── .env.example           # Build environment variables
│   ├── Dockerfile.service     # Service Dockerfile template
│   ├── build-service.sh       # Docker build script
│   ├── entrypoint.sh          # Container entrypoint
│   ├── healthcheck.sh         # Health check script
│   ├── jvm.config             # JVM configuration
│   ├── grafana/               # Grafana provisioning
│   ├── keycloak/              # Keycloak realm export
│   ├── loki/                  # Loki configuration
│   ├── prometheus/            # Prometheus configuration
│   └── tempo/                 # Tempo configuration
├── k8s/                       # Kubernetes manifests
│   ├── namespace.yaml         # Namespace definition
│   ├── configmap.yaml         # ConfigMap
│   └── secret.yaml            # Secrets template
├── postgres/                  # PostgreSQL infrastructure
│   ├── init/                  # Database initialization scripts
│   │   ├── 01-create-databases.sql
│   │   ├── 02-create-users.sql
│   │   └── 03-create-extensions.sql
│   ├── flyway/                # Flyway migrations
│   │   ├── flyway.conf
│   │   ├── platform/
│   │   ├── finance/
│   │   ├── hr/
│   │   ├── inventory/
│   │   ├── manufacturing/
│   │   ├── procurement/
│   │   ├── sales/
│   │   ├── ai/
│   │   ├── integration/
│   │   └── gateway/
│   ├── backups/               # Backup scripts and docs
│   │   ├── README.md
│   │   └── backup.sh
│   └── README.md              # PostgreSQL documentation
└── terraform/                 # Terraform IaC
    ├── main.tf                # Main configuration
    ├── README.md              # Terraform documentation
    └── modules/               # Terraform modules
        ├── vpc/
        ├── eks/
        ├── rds/
        ├── redis/
        ├── msk/
        ├── s3/
        └── security/
```

## Quick Start

### Docker Compose

```bash
# Start all infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up

# Start with development tools (pgAdmin, etc.)
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up

# Start with monitoring
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up
```

### PostgreSQL

```bash
# Start PostgreSQL only
docker compose -f compose.base.yml -f compose.infrastructure.yml up postgres

# Run Flyway migrations
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway

# Access PostgreSQL
docker compose exec postgres psql -U erpai -d erpai_platform
```

### Terraform (AWS)

```bash
# Initialize Terraform
cd infrastructure/terraform
terraform init

# Plan changes
terraform plan -var-file="environments/dev.tfvars"

# Apply changes
terraform apply -var-file="environments/dev.tfvars"
```

## Services

### Databases

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| PostgreSQL | postgres:16-alpine | 5432 | Primary database |
| Flyway | flyway/flyway:10.12-alpine | - | Database migrations |

### Cache

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Redis | redis:7-alpine | 6379 | Caching and sessions |

### Messaging

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Kafka | confluentinc/cp-kafka:7.7.0 | 29092 | Message broker |
| Schema Registry | confluentinc/cp-schema-registry:7.7.0 | 8081 | Schema management |

### Identity

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Keycloak | quay.io/keycloak/keycloak:26.2 | 8080 | Identity provider |

### Storage

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| MinIO | minio/minio:latest | 9000, 9001 | Object storage |

### Monitoring

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Prometheus | prom/prometheus:latest | 9090 | Metrics collection |
| Grafana | grafana/grafana:latest | 3000 | Visualization |
| Tempo | grafana/tempo:latest | 4317, 4318, 3200 | Distributed tracing |
| Loki | grafana/loki:latest | 3100 | Log aggregation |

## PostgreSQL Databases

The platform uses multiple PostgreSQL databases, one per microservice:

| Database | Service | Description |
|----------|---------|-------------|
| `erpai_platform` | Platform | Shared infrastructure |
| `erpai_finance` | Finance | Finance domain |
| `erpai_hr` | HR | Human resources |
| `erpai_inventory` | Inventory | Stock management |
| `erpai_manufacturing` | Manufacturing | Production |
| `erpai_procurement` | Procurement | Purchasing |
| `erpai_sales` | Sales | Sales orders |
| `erpai_ai` | AI/ML | AI models and predictions |
| `erpai_integration` | Integration | External integrations |
| `erpai_gateway` | Gateway | API gateway config |

## Flyway Migrations

Each microservice has its own Flyway migration directory under `infrastructure/postgres/flyway/`. Migrations follow the naming convention:

```
V{version}__{description}.sql
```

Example:
- `V001__create_invoices_table.sql`
- `V002__add_payment_methods.sql`

## Backup & Recovery

See [`postgres/backups/README.md`](postgres/backups/README.md) for detailed backup procedures.

Quick backup:
```bash
# Run backup script
bash infrastructure/postgres/backups/backup.sh
```

## Documentation

- [PostgreSQL Infrastructure](postgres/README.md)
- [Backup & Recovery](postgres/backups/README.md)
- [Docker Standards](../docs/standards/16-docker-standards.md)
- [Database Standards](../docs/standards/04-database-standards.md)
- [Terraform Documentation](terraform/README.md)
