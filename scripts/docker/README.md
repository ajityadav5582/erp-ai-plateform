# ERP AI Platform - Docker Automation Scripts

Enterprise-grade automation scripts for managing the ERP AI Platform Docker infrastructure. These scripts provide a developer-friendly interface for common Docker operations.

## Quick Start

```bash
# Make scripts executable
chmod +x scripts/docker/*.sh

# Start all services
./scripts/docker/docker.sh start

# Stop all services
./scripts/docker/docker.sh stop

# Check health
./scripts/docker/docker.sh health-check
```

## Scripts Overview

| Script | Purpose | Use Case |
|--------|---------|----------|
| [`docker.sh`](docker.sh) | Main entry point | Unified interface for all commands |
| [`start.sh`](start.sh) | Start services | Starting the development environment |
| [`stop.sh`](stop.sh) | Stop services | Stopping services gracefully |
| [`restart.sh`](restart.sh) | Restart services | Applying configuration changes |
| [`reset.sh`](reset.sh) | Full reset | Starting fresh (destructive) |
| [`database-reset.sh`](database-reset.sh) | Reset database | Resetting only PostgreSQL |
| [`kafka-reset.sh`](kafka-reset.sh) | Reset Kafka | Cleaning up Kafka topics |
| [`keycloak-import.sh`](keycloak-import.sh) | Import realm | Setting up Keycloak |
| [`health-check.sh`](health-check.sh) | Health check | Monitoring service health |

## Detailed Documentation

### `docker.sh` - Main Entry Point

**Description:** Unified command interface for all Docker automation scripts.

**Usage:**
```bash
./scripts/docker/docker.sh <COMMAND> [OPTIONS]
```

**Examples:**
```bash
# Start all services
./scripts/docker/docker.sh start

# Start with development profile
./scripts/docker/docker.sh start --profile development

# Stop specific services
./scripts/docker/docker.sh stop --services postgres redis

# Restart with rebuild
./scripts/docker/docker.sh restart --build

# Full reset
./scripts/docker/docker.sh reset

# Reset database only
./scripts/docker/docker.sh database-reset

# Reset Kafka
./scripts/docker/docker.sh kafka-reset

# Import Keycloak realm
./scripts/docker/docker.sh keycloak-import

# Run health check
./scripts/docker/docker.sh health-check
```

---

### `start.sh` - Start Services

**Description:** Starts the ERP AI Platform infrastructure services using Docker Compose.

**Use Cases:**
- Starting the development environment for the first time
- Resuming work after stopping the platform
- Deploying infrastructure in CI/CD pipelines
- Starting specific service profiles

**Usage:**
```bash
./scripts/docker/start.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--profile PROFILE` | Add compose profile | `infrastructure` |
| `--services LIST` | Start specific services | All services |
| `--detached, -d` | Run in background | `true` |
| `--build` | Build images before starting | `false` |
| `--no-wait` | Skip health check wait | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Start all core infrastructure services
./scripts/docker/start.sh

# Start with development profile (includes MailHog, pgAdmin)
./scripts/docker/start.sh --profile development

# Start with monitoring profile
./scripts/docker/start.sh --profile monitoring

# Start specific services only
./scripts/docker/start.sh --services postgres redis kafka

# Start in detached mode with rebuild
./scripts/docker/start.sh --build --detached
```

**Access URLs after start:**
| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Keycloak | http://localhost:8080 | admin/admin |
| MinIO Console | http://localhost:9001 | minioadmin/minioadmin |
| pgAdmin | http://localhost:5050 | admin@erpai.local/admin |
| MailHog | http://localhost:8025 | - |
| Prometheus | http://localhost:9090 | - |
| Kafka UI | http://localhost:8082 | - |

---

### `stop.sh` - Stop Services

**Description:** Stops all running ERP AI Platform infrastructure services gracefully.

**Use Cases:**
- Stopping the platform at the end of the workday
- Freeing up system resources
- Preparing for system updates
- Stopping specific services for debugging

**Usage:**
```bash
./scripts/docker/stop.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--services LIST` | Stop specific services | All services |
| `--remove` | Remove containers after stopping | `false` |
| `--force` | Force stop (SIGKILL) | `false` |
| `--timeout SECONDS` | Shutdown timeout | `30` |
| `--with-logs` | Show logs before stopping | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Stop all services
./scripts/docker/stop.sh

# Stop specific services
./scripts/docker/stop.sh --services postgres redis kafka

# Stop and remove containers
./scripts/docker/stop.sh --remove

# Force stop with 60s timeout
./scripts/docker/stop.sh --force --timeout 60

# Show logs before stopping
./scripts/docker/stop.sh --with-logs
```

---

### `restart.sh` - Restart Services

**Description:** Restarts the ERP AI Platform infrastructure services. Stops and then starts all services, optionally rebuilding images.

**Use Cases:**
- Applying configuration changes
- Recovering from service failures
- Rolling out new Docker image versions
- Refreshing service state after code changes

**Usage:**
```bash
./scripts/docker/restart.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--profile PROFILE` | Add compose profile | `infrastructure` |
| `--services LIST` | Restart specific services | All services |
| `--build` | Rebuild images before restarting | `false` |
| `--force` | Force restart (kill and start) | `false` |
| `--timeout SECONDS` | Shutdown timeout | `30` |
| `--with-logs` | Show logs before restarting | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Restart all services
./scripts/docker/restart.sh

# Rebuild and restart
./scripts/docker/restart.sh --build

# Restart specific services
./scripts/docker/restart.sh --services postgres redis kafka

# Restart with development profile
./scripts/docker/restart.sh --profile development --build

# Force restart with extended timeout
./scripts/docker/restart.sh --force --timeout 60
```

---

### `reset.sh` - Full Reset

**Description:** Completely resets the ERP AI Platform by stopping all services and removing all volumes. **This is a destructive operation.**

**Use Cases:**
- Starting fresh after corrupted data
- Cleaning up before a major version upgrade
- Resetting development environment to known state
- Removing all test data and configurations

**WARNING:** This will permanently delete all data including:
- PostgreSQL databases and all application data
- Kafka topics and messages
- Keycloak users, roles, and configurations
- MinIO buckets and objects
- Redis cache data

**Usage:**
```bash
./scripts/docker/reset.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--services LIST` | Reset specific services only | All services |
| `--no-start` | Reset without starting services | `false` |
| `--build` | Rebuild images after reset | `false` |
| `--confirm` | Show confirmation prompt | `true` |
| `--no-confirm` | Skip confirmation prompt | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Full reset with confirmation
./scripts/docker/reset.sh

# Reset without starting
./scripts/docker/reset.sh --no-start

# Reset specific services
./scripts/docker/reset.sh --services postgres kafka keycloak

# Reset and rebuild
./scripts/docker/reset.sh --build --no-confirm
```

---

### `database-reset.sh` - Database Reset

**Description:** Resets only the PostgreSQL database without affecting other services. Drops and recreates the database, runs Flyway migrations.

**Use Cases:**
- Resetting database schema during development
- Cleaning test data without losing Kafka topics
- Re-running Flyway migrations from scratch
- Seeding fresh data for testing

**Usage:**
```bash
./scripts/docker/database-reset.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--service NAME` | Reset specific service database | All services |
| `--db-name NAME` | Custom database name | `erpai_platform` |
| `--no-migrate` | Skip Flyway migrations | `false` |
| `--seed` | Seed with initial data | `false` |
| `--no-confirm` | Skip confirmation prompt | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Full database reset
./scripts/docker/database-reset.sh

# Reset and seed with initial data
./scripts/docker/database-reset.sh --seed

# Reset specific service database
./scripts/docker/database-reset.sh --service finance

# Reset without running migrations
./scripts/docker/database-reset.sh --no-migrate --seed

# Reset with custom database name
./scripts/docker/database-reset.sh --db-name test_db --no-confirm
```

---

### `kafka-reset.sh` - Kafka Reset

**Description:** Resets Kafka by deleting all topics and recreating infrastructure topics. Preserves the Kafka container and configuration.

**Use Cases:**
- Cleaning up test topics after integration tests
- Recovering from topic corruption
- Resetting Kafka to a clean state for development
- Clearing all messages without restarting Kafka

**Usage:**
```bash
./scripts/docker/kafka-reset.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--topics LIST` | Reset specific topics only | All topics |
| `--no-recreate` | Don't recreate infrastructure topics | `false` |
| `--wait` | Wait for topics to be ready | `false` |
| `--no-confirm` | Skip confirmation prompt | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Full Kafka reset with recreation
./scripts/docker/kafka-reset.sh

# Reset without recreating topics
./scripts/docker/kafka-reset.sh --no-recreate

# Reset specific topics
./scripts/docker/kafka-reset.sh --topics dev.dlq.events dev.retry.events

# Reset and wait for topics
./scripts/docker/kafka-reset.sh --wait --no-confirm
```

**Infrastructure topics recreated:**
- `dev.system.health.service-up`
- `dev.system.health.service-down`
- `dev.system.metrics`
- `dev.dlq.events`
- `dev.dlq.commands`
- `dev.retry.events`
- `dev.retry.commands`
- `dev.audit.events`

---

### `keycloak-import.sh` - Keycloak Import

**Description:** Imports a Keycloak realm configuration from a JSON file. Supports both initial import and updating existing realms.

**Use Cases:**
- Setting up Keycloak for the first time
- Updating realm configuration after changes
- Importing realm from version control
- Deploying realm configuration in CI/CD
- Restoring realm from backup

**Usage:**
```bash
./scripts/docker/keycloak-import.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--realm-file PATH` | Path to realm JSON file | `infrastructure/docker/keycloak/realm-export.json` |
| `--realm-name NAME` | Name of realm to import | From JSON file |
| `--force` | Overwrite existing realm | `false` |
| `--skip-if-exists` | Skip if realm exists | `false` |
| `--no-confirm` | Skip confirmation prompt | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Import default realm
./scripts/docker/keycloak-import.sh

# Import custom realm file
./scripts/docker/keycloak-import.sh --realm-file /path/to/custom-realm.json

# Overwrite existing realm
./scripts/docker/keycloak-import.sh --force

# Skip if realm exists
./scripts/docker/keycloak-import.sh --skip-if-exists

# Import with specific realm name
./scripts/docker/keycloak-import.sh --realm-name my-realm --force
```

---

### `health-check.sh` - Health Check

**Description:** Performs comprehensive health checks on all ERP AI Platform services. Checks container status, TCP ports, and HTTP endpoints.

**Use Cases:**
- Verifying all services are running after startup
- Monitoring service health in CI/CD pipelines
- Troubleshooting connectivity issues
- Pre-deployment health verification
- Periodic health monitoring

**Usage:**
```bash
./scripts/docker/health-check.sh [OPTIONS]
```

**Options:**
| Option | Description | Default |
|--------|-------------|--------|
| `--services LIST` | Check specific services only | All services |
| `--endpoint URL` | Add custom endpoint to check | - |
| `--timeout SECONDS` | HTTP request timeout | `10` |
| `--retries COUNT` | Number of retries per service | `3` |
| `--retry-delay SECONDS` | Delay between retries | `2` |
| `--json` | Output results in JSON format | `false` |
| `--no-fail` | Don't exit with error if unhealthy | `false` |
| `--verbose, -v` | Enable verbose output | `false` |

**Examples:**
```bash
# Check all services
./scripts/docker/health-check.sh

# Check specific services
./scripts/docker/health-check.sh --services postgres redis kafka

# Check with custom endpoint
./scripts/docker/health-check.sh --endpoint http://localhost:8080/actuator/health

# JSON output for monitoring
./scripts/docker/health-check.sh --json --no-fail

# Custom timeout and retries
./scripts/docker/health-check.sh --timeout 30 --retries 5
```

**Services checked:**
| Service | Check Type | Port/Endpoint |
|---------|-----------|---------------|
| PostgreSQL | TCP | localhost:5432 |
| Redis | TCP | localhost:6379 |
| Kafka | TCP | localhost:29092 |
| Schema Registry | HTTP | http://localhost:8081/health |
| Keycloak | HTTP | http://localhost:8080/health/ready |
| MinIO | HTTP | http://localhost:9000/minio/health/live |
| Prometheus | HTTP | http://localhost:9090/-/healthy |
| Grafana | HTTP | http://localhost:3000/api/health |
| Tempo | HTTP | http://localhost:3200/ready |
| Loki | HTTP | http://localhost:3100/ready |
| Jaeger | HTTP | http://localhost:16686/ |
| MailHog | HTTP | http://localhost:8025/ |
| pgAdmin | HTTP | http://localhost:5050/ |

---

## Common Workflows

### Initial Setup

```bash
# 1. Start infrastructure
./scripts/docker/docker.sh start

# 2. Import Keycloak realm
./scripts/docker/docker.sh keycloak-import

# 3. Verify health
./scripts/docker/docker.sh health-check
```

### Daily Development

```bash
# Start services
./scripts/docker/docker.sh start --profile development

# Work on code...

# Stop services
./scripts/docker/docker.sh stop
```

### After Code Changes

```bash
# Rebuild and restart
./scripts/docker/docker.sh restart --build

# Verify health
./scripts/docker/docker.sh health-check
```

### Clean Slate

```bash
# Full reset
./scripts/docker/docker.sh reset --no-confirm

# Or selective reset
./scripts/docker/docker.sh database-reset --seed
./scripts/docker/docker.sh kafka-reset
```

### CI/CD Pipeline

```bash
# Start services
./scripts/docker/docker.sh start --detached

# Wait for health
./scripts/docker/docker.sh health-check --no-fail

# Run tests...

# Cleanup
./scripts/docker/docker.sh reset --no-start --no-confirm
```

## Environment Variables

All scripts respect the following environment variables:

| Variable | Description | Default |
|----------|-------------|--------|
| `COMPOSE_FILES` | Docker Compose files | `-f compose.base.yml -f compose.infrastructure.yml` |
| `COMPOSE_PROFILES` | Docker Compose profiles | `--profile infrastructure` |
| `POSTGRES_DB` | PostgreSQL database name | `erpai_platform` |
| `POSTGRES_USER` | PostgreSQL user | `erpai` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `erpai_dev_password` |
| `KEYCLOAK_ADMIN` | Keycloak admin user | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | `admin` |
| `KAFKA_CONTAINER` | Kafka container name | `erpai-kafka` |
| `AUTO_CONFIRM` | Skip confirmation prompts | `false` |

## Troubleshooting

### Services Won't Start

```bash
# Check Docker daemon
docker info

# Check port conflicts
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka

# Check logs
./scripts/docker/docker.sh start --verbose
```

### Health Check Failures

```bash
# Check specific service
docker compose -f compose.base.yml -f compose.infrastructure.yml ps <service>

# View logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs <service>

# Restart specific service
./scripts/docker/docker.sh restart --services <service>
```

### Reset Issues

```bash
# Force remove all volumes
docker volume prune -f

# Full reset
./scripts/docker/docker.sh reset --no-confirm
```

## Contributing

When adding new automation scripts:

1. Follow the existing script structure
2. Include comprehensive documentation in the script header
3. Add the script to `docker.sh` command router
4. Update this README with the new script documentation
5. Include real-world examples in the documentation
6. Add appropriate error handling and exit codes

## License

See [LICENSE](../LICENSE) in the project root.
