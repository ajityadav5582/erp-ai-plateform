# ERP AI Platform - Developer Guide

## Quick Start

### 1. Start All Services

```bash
# Start core infrastructure (PostgreSQL, Redis, Kafka, Keycloak, MinIO)
./scripts/docker/docker.sh start

# Start with development tools (MailHog, pgAdmin, Kafka UI)
./scripts/docker/docker.sh start --profile development

# Start with monitoring (Prometheus, Grafana, Tempo, Loki)
./scripts/docker/docker.sh start --profile monitoring

# Start with all profiles
./scripts/docker/docker.sh start --profile development --profile monitoring
```

### 2. Verify Services Are Healthy

```bash
./scripts/docker/docker.sh health-check
```

### 3. Stop All Services

```bash
./scripts/docker/docker.sh stop
```

### 4. Restart Services

```bash
# Restart without rebuilding
./scripts/docker/docker.sh restart

# Restart with rebuild (after code changes)
./scripts/docker/docker.sh restart --build
```

### 5. Full Reset (Destructive)

```bash
# Stop all services, remove all volumes, and start fresh
./scripts/docker/docker.sh reset --no-confirm
```

---

## Daily Development Workflow

### Starting Fresh

```bash
# 1. Stop any running services
./scripts/docker/docker.sh stop

# 2. Reset everything (optional, for a clean slate)
./scripts/docker/docker.sh reset --no-confirm

# 3. Start infrastructure
./scripts/docker/docker.sh start --profile development

# 4. Verify health
./scripts/docker/docker.sh health-check
```

### After Code Changes

```bash
# Rebuild and restart affected services
./scripts/docker/docker.sh restart --build

# Or restart specific services
./scripts/docker/docker.sh restart --services postgres redis
```

### Cleaning Database State

```bash
# Reset only the database (keeps Kafka, Redis, etc. running)
./scripts/docker/docker.sh database-reset --no-confirm

# Reset database and seed with initial data
./scripts/docker/docker.sh database-reset --seed --no-confirm
```

### Cleaning Up Between Test Runs (CI/CD)

```bash
# Clean database for next test run
./scripts/docker/docker.sh database-reset --no-confirm
```

### Full Cleanup (Start Fresh)

Use the cleanup script to remove everything — database tables, Flyway migration history, Docker containers, volumes, and images — so you can start completely fresh.

```bash
# Clean database + Flyway history only
./scripts/docker/cleanup.sh --no-confirm

# Also stop containers and remove volumes
./scripts/docker/cleanup.sh --docker --no-confirm

# Full cleanup including Docker images (complete fresh start)
./scripts/docker/cleanup.sh --docker --images --no-confirm
```

Or use the unified Docker script:

```bash
./scripts/docker/docker.sh cleanup --docker --images --no-confirm
```

---

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Keycloak | http://localhost:8080 | admin/admin |
| MinIO Console | http://localhost:9001 | minioadmin/minioadmin |
| pgAdmin | http://localhost:5050 | admin@erpai.local/admin |
| MailHog | http://localhost:8025 | - |
| Prometheus | http://localhost:9090 | - |
| Kafka UI | http://localhost:8082 | - |
| PostgreSQL | localhost:5434 | erpai / erpai_dev_password |
| Redis | localhost:6379 | erpai_dev_password |

---

## Service Profiles

### Infrastructure Profile (default)
Starts core infrastructure services:
- PostgreSQL (port 5434)
- Redis (port 6379)
- Kafka (port 29092)
- Schema Registry (port 8081)
- Keycloak (port 8080)
- MinIO (ports 9000, 9001)

### Development Profile
Adds development tools:
- MailHog (ports 1025, 8025)
- pgAdmin (port 5050)
- Kafka UI (port 8082)

### Monitoring Profile
Adds observability stack:
- Prometheus (port 9090)
- Grafana (port 3000)
- Alertmanager (port 9093)
- Tempo (port 3200)
- Loki (port 3100)
- Jaeger (port 16686)

---

## Troubleshooting

### Port Conflicts

If you see port conflict errors:

```bash
# Check what's using the ports
lsof -i :5434  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :8080  # Keycloak
lsof -i :9000  # MinIO
lsof -i :29092 # Kafka

# Stop conflicting services
brew services stop redis  # If Redis is running as Homebrew service
```

### Services Won't Start

```bash
# Check Docker daemon
docker info

# Check Docker Compose version
docker compose version

# Check service logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs

# Check specific service logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs postgres
```

### Health Check Failures

```bash
# Check specific service
docker compose -f compose.base.yml -f compose.infrastructure.yml ps <service>

# Restart specific service
./scripts/docker/docker.sh restart --services <service>
```

### Database Issues

```bash
# Reset database only
./scripts/docker/docker.sh database-reset --no-confirm

# Full database reset with migrations
./scripts/docker/docker.sh database-reset --migrate --no-confirm
```

---

## Environment Variables

All scripts respect these environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `COMPOSE_FILES` | Docker Compose files | `-f compose.base.yml -f compose.infrastructure.yml` |
| `COMPOSE_PROFILES` | Docker Compose profiles | `--profile infrastructure` |
| `POSTGRES_DB` | PostgreSQL database name | `erpai_platform` |
| `POSTGRES_USER` | PostgreSQL user | `erpai` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `erpai_dev_password` |
| `POSTGRES_HOST` | PostgreSQL host | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5434` |
| `AUTO_CONFIRM` | Skip confirmation prompts | `false` |
| `FLYWAY_SERVICE` | Flyway service name | `platform` |

---

## Project Structure

```
erp-ai-plateform/
├── docker-compose.yml          # Minimal local-dev default (PostgreSQL + application)
├── compose.base.yml            # Base networks and volumes
├── compose.infrastructure.yml  # Infrastructure services
├── compose.development.yml     # Development tools
├── compose.monitoring.yml      # Monitoring stack
├── scripts/
│   ├── docker/
│   │   ├── docker.sh           # Unified command interface
│   │   ├── start.sh            # Start services
│   │   ├── stop.sh             # Stop services
│   │   ├── restart.sh          # Restart services
│   │   ├── reset.sh            # Full reset (destructive)
│   │   ├── database-reset.sh   # Database reset only
│   │   ├── kafka-reset.sh      # Kafka reset
│   │   ├── keycloak-import.sh  # Keycloak realm import
│   │   ├── health-check.sh     # Health checks
│   │   └── cleanup.sh          # Full cleanup (DB + Flyway + Docker)
│   ├── README.md               # Scripts documentation
│   └── README.md               # Docker scripts guide
├── infrastructure/
│   ├── postgres/
│   │   ├── flyway/             # Flyway migrations
│   │   ├── init/               # Init SQL scripts
│   │   └── backups/            # Database backups
│   ├── redis/
│   │   └── redis.conf          # Redis configuration
│   ├── kafka/
│   │   └── config/             # Kafka configuration
│   └── docker/
│       ├── keycloak/           # Keycloak realm config
│       ├── prometheus/         # Prometheus config
│       ├── grafana/            # Grafana dashboards
│       └── alertmanager/       # Alertmanager config
└── .env                        # Environment variables
```

---

## Best Practices

1. **Always use `--no-confirm` in CI/CD** to avoid interactive prompts
2. **Use `--profile development`** for local development with MailHog and pgAdmin
3. **Use `--profile monitoring`** when you need observability
4. **Run `health-check`** after starting services to verify everything is working
5. **Use `database-reset`** instead of `reset` when you only need to clean the database
6. **Use `cleanup.sh`** for a complete fresh start — removes DB tables, Flyway history, and Docker infrastructure
7. **Stop services before restarting** to avoid port conflicts
8. **Use `brew services stop redis`** if Redis Homebrew service conflicts with Docker
9. **Keep `.env` file secure** - never commit it to version control
