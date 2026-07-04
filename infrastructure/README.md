# ERP AI Platform - Infrastructure

Enterprise-grade infrastructure for the ERP AI Platform, including databases, messaging, identity, storage, and observability.

## Directory Structure

```
infrastructure/
├── README.md                    # This file - overview of all components
├── docker/                      # Docker configuration and monitoring
│   ├── README.md               # Docker setup and monitoring guide
│   ├── DEVELOPER_GUIDE.md      # Developer guide for Java/React
│   ├── .env.example            # Build environment variables
│   ├── Dockerfile.service      # Service Dockerfile template
│   ├── build-service.sh        # Docker build script
│   ├── entrypoint.sh           # Container entrypoint
│   ├── healthcheck.sh          # Health check script
│   ├── jvm.config              # JVM configuration
│   ├── grafana/                # Grafana provisioning
│   │   ├── provisioning/
│   │   │   ├── datasources/
│   │   │   │   └── prometheus.yml
│   │   │   └── dashboards/
│   │   │       ├── dashboards.yml
│   │   │       ├── infrastructure-overview.json
│   │   │       ├── jvm-metrics.json
│   │   │       ├── application-metrics.json
│   │   │       ├── database-metrics.json
│   │   │       ├── kafka-metrics.json
│   │   │       ├── traces.json
│   │   │       └── logs.json
│   ├── keycloak/               # Keycloak realm export
│   ├── loki/                   # Loki configuration
│   │   └── local-config.yaml
│   ├── prometheus/             # Prometheus configuration
│   │   ├── prometheus.yml
│   │   └── alerting_rules.yml
│   └── tempo/                  # Tempo configuration
│       └── tempo.yml
├── k8s/                         # Kubernetes manifests
│   ├── namespace.yaml
│   ├── configmap.yaml
│   └── secret.yaml
├── postgres/                    # PostgreSQL database
│   ├── init/                    # Database initialization scripts
│   │   ├── 01-create-databases.sql
│   │   ├── 02-create-users.sql
│   │   └── 03-create-extensions.sql
│   ├── flyway/                  # Flyway migrations
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
│   ├── backups/                 # Backup scripts and docs
│   │   ├── README.md
│   │   └── backup.sh
│   └── README.md                # PostgreSQL documentation
├── redis/                       # Redis cache and session store
│   ├── redis.conf               # Redis server configuration
│   ├── backups/                 # Backup scripts and docs
│   │   ├── README.md
│   │   └── backup.sh
│   └── README.md                # Redis documentation
├── kafka/                       # Apache Kafka event streaming
│   ├── config/                  # Kafka configuration
│   │   ├── kraft/               # KRaft mode configuration
│   │   │   └── server.properties
│   │   ├── kafka-retry-dlq.yaml
│   │   └── KafkaRetryConfiguration.java
│   ├── scripts/                 # Management scripts
│   │   ├── healthcheck.sh
│   │   └── init-topics.sh
│   ├── backups/                 # Backup scripts and docs
│   │   └── README.md
│   ├── TOPIC_NAMING_STANDARDS.md
│   ├── RETRY_STRATEGY.md
│   ├── DLQ_STRATEGY.md
│   └── README.md                # Kafka documentation
├── keycloak/                    # Keycloak identity provider
│   ├── config/                  # Keycloak configuration
│   │   ├── realm.json           # Realm configuration
│   │   ├── keycloak-postgres.env
│   │   └── keycloak-dev.env
│   ├── themes/                  # Custom themes
│   ├── backups/                 # Backup scripts and docs
│   │   └── README.md
│   └── README.md                # Keycloak documentation
├── minio/                       # MinIO object storage
│   ├── config/                  # MinIO configuration
│   │   ├── minio.env            # Environment configuration
│   │   ├── minio-client.json    # MinIO client config
│   │   └── policy/              # Bucket policies
│   │       ├── public-read.json
│   │       └── private.json
│   ├── scripts/                 # Management scripts
│   │   ├── healthcheck.sh
│   │   └── init-buckets.sh
│   ├── backups/                 # Backup scripts and docs
│   │   └── README.md
│   └── README.md                # MinIO documentation
├── mailhog/                     # MailHog email testing
│   ├── README.md                # MailHog documentation
│   ├── application.yml.example  # SMTP configuration example
│   ├── EmailService.java.example # Email service example
│   ├── EmailProperties.java.example # Email config properties
│   └── welcome-email.html.example # Email template example
└── terraform/                   # Terraform IaC
    ├── main.tf                  # Main configuration
    ├── README.md                # Terraform documentation
    └── modules/                 # Terraform modules
        ├── vpc/
        ├── eks/
        ├── rds/
        ├── redis/
        ├── msk/
        ├── s3/
        └── security/
```

## Component Overview

### Core Services

| Component | Purpose | Ports | Documentation |
|-----------|---------|-------|---------------|
| **PostgreSQL** | Primary database | 5432 | [README](postgres/README.md) |
| **Redis** | Cache, sessions, rate limiting | 6379 | [README](redis/README.md) |
| **Kafka** | Event streaming, async communication | 9092 | [README](kafka/README.md) |
| **Keycloak** | Identity and access management | 8080 | [README](keycloak/README.md) |
| **MinIO** | Object storage (S3-compatible) | 9000, 9001 | [README](minio/README.md) |
| **MailHog** | Email testing (development) | 1025, 8025 | [README](mailhog/README.md) |

### Monitoring Stack

| Component | Purpose | Ports | Documentation |
|-----------|---------|-------|---------------|
| **Prometheus** | Metrics collection | 9090 | [README](docker/README.md) |
| **Grafana** | Metrics visualization | 3000 | [README](docker/README.md) |
| **Tempo** | Distributed tracing | 3200, 4317, 4318 | [README](docker/README.md) |
| **Jaeger** | Trace visualization | 16686 | [README](docker/README.md) |
| **Loki** | Log aggregation | 3100 | [README](docker/README.md) |
| **Alertmanager** | Alert routing | 9093 | [README](docker/README.md) |
| **Postgres Exporter** | PostgreSQL metrics | 9187 | [README](docker/README.md) |
| **Redis Exporter** | Redis metrics | 9121 | [README](docker/README.md) |

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ERP AI Platform                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  PostgreSQL  │  │    Redis    │  │    Kafka    │  │   Keycloak  │        │
│  │   (5432)    │  │   (6379)    │  │   (9092)    │  │   (8080)    │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                │               │
│         ▼                ▼                ▼                ▼               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Microservices                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                         Monitoring Stack                                    │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Prometheus  │  │   Grafana   │  │    Loki     │  │  Tempo/Jaeger│       │
│  │   (9090)    │  │   (3000)    │  │   (3100)    │  │  (3200/16686)│       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                │               │
│         ▼                ▼                ▼                ▼               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Observability Platform                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Start All Infrastructure

```bash
# Start core infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d

# Start with development profile (includes MailHog, pgAdmin)
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up -d

# Start with monitoring profile
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up -d
```

### Start Everything

```bash
# Start all services including application
docker compose up -d
```

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger** | http://localhost:16686 | - |
| **Keycloak** | http://localhost:8080 | admin/admin |
| **pgAdmin** | http://localhost:5050 | admin@erpai.com/admin |
| **MailHog** | http://localhost:8025 | - |
| **MinIO Console** | http://localhost:9001 | minioadmin/minioadmin |

## Configuration Files

### Docker Compose Files

| File | Purpose |
|------|---------|
| `compose.base.yml` | Base configuration shared by all profiles |
| `compose.infrastructure.yml` | Core infrastructure services |
| `compose.development.yml` | Development tools (MailHog, pgAdmin) |
| `compose.monitoring.yml` | Monitoring stack (Prometheus, Grafana, etc.) |
| `docker-compose.yml` | Main compose file (includes all) |

### Monitoring Configuration

| File | Purpose |
|------|---------|
| `docker/prometheus/prometheus.yml` | Prometheus scrape configuration |
| `docker/prometheus/alerting_rules.yml` | Alerting rules |
| `docker/grafana/provisioning/datasources/prometheus.yml` | Grafana datasources |
| `docker/grafana/provisioning/dashboards/dashboards.yml` | Dashboard provisioning |
| `docker/tempo/tempo.yml` | Tempo tracing configuration |
| `docker/loki/local-config.yaml` | Loki logging configuration |

## Environment Variables

All environment variables are defined in `infrastructure/docker/.env.example`. Copy to `.env` and customize:

```bash
cp infrastructure/docker/.env.example infrastructure/docker/.env
```

Key variables:
- `POSTGRES_PASSWORD` - PostgreSQL password
- `REDIS_PASSWORD` - Redis password
- `KAFKA_PASSWORD` - Kafka password
- `KEYCLOAK_ADMIN_PASSWORD` - Keycloak admin password
- `MINIO_ROOT_PASSWORD` - MinIO root password
- `GRAFANA_ADMIN_PASSWORD` - Grafana admin password

## Health Checks

All services include health checks. Check status with:

```bash
docker compose -f compose.base.yml -f compose.infrastructure.yml ps
```

## Backups

Backup scripts and documentation are in [`backups/`](backups/). Each component has its own backup strategy.

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs <service-name>

# Check port conflicts
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka
```

### Reset Everything

```bash
# Stop and remove all containers and volumes
docker compose -f compose.base.yml -f compose.infrastructure.yml down -v

# Start fresh
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d
```

## Contributing

When adding new infrastructure components:

1. Create a new directory under `infrastructure/`
2. Add a `README.md` with component documentation
3. Update `compose.base.yml` or `compose.infrastructure.yml`
4. Add health checks
5. Update this README with the new component

## License

See [LICENSE](../LICENSE) in the project root.
