# =============================================================================
# ERP AI Platform - Environment Management Guide
# =============================================================================
# This document explains how to configure, use, and manage environment
# variables across development, testing, staging, and production environments.
# =============================================================================

## Table of Contents

1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Quick Start](#quick-start)
4. [Environment Files](#environment-files)
5. [Usage Patterns](#usage-patterns)
6. [Security Best Practices](#security-best-practices)
7. [CI/CD Integration](#cicd-integration)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The ERP AI Platform uses environment variables to configure all services without
modifying code or Docker Compose files. This approach follows the
[Twelve-Factor App](https://12factor.net/config) methodology.

### Key Principles

- **No secrets in code**: All sensitive values are injected via environment variables
- **Environment parity**: Same codebase runs in all environments
- **Explicit configuration**: Every setting is documented and intentional
- **Secure by default**: Production requires explicit security configuration

---

## File Structure

```
erp-ai-platform/
├── .env.example              # Template with ALL variables (committed to git)
├── .env.development          # Local development values (gitignored)
├── .env.testing              # CI/CD test values (gitignored)
├── .env.staging              # Staging environment values (gitignored)
├── .env.production           # Production values (gitignored)
├── .env                      # Local override (gitignored, highest priority)
├── .gitignore                # Excludes all .env.* files
└── docker-compose.yml        # Uses ${VARIABLE:-default} syntax
```

### File Purposes

| File | Purpose | Committed? |
|------|---------|------------|
| `.env.example` | Template with all variables and documentation | ✅ Yes |
| `.env.development` | Local development defaults | ❌ No |
| `.env.testing` | Automated testing / CI | ❌ No |
| `.env.staging` | Pre-production validation | ❌ No |
| `.env.production` | Production deployment | ❌ No |
| `.env` | Local overrides (highest priority) | ❌ No |

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/erp-ai-platform.git
cd erp-ai-platform
```

### 2. Choose Your Environment

```bash
# For local development (recommended for new developers)
cp .env.development .env

# For running tests
cp .env.testing .env

# For staging deployment
cp .env.staging .env

# For production deployment
cp .env.production .env
```

### 3. Customize Values

Edit `.env` and replace placeholder values with your actual configuration.

**Critical**: Change ALL passwords in production!

### 4. Start Services

```bash
# Start all infrastructure services
docker compose up

# Start with specific profile
docker compose --profile development up
docker compose --profile monitoring up
```

---

## Environment Files

### `.env.example` — The Template

This is the **only** environment file committed to version control. It contains:

- Every configurable variable in the platform
- Default values for local development
- Detailed comments explaining each variable
- Security warnings for sensitive values

**Never** put real secrets in `.env.example`.

### `.env.development` — Local Development

Optimized for developer productivity:

- Debug JVM options enabled (remote debugging on port 5005)
- Simple, memorable passwords
- Keycloak uses in-memory database (no PostgreSQL needed)
- Full SQL logging enabled
- Lower resource limits for local machines
- MailHog for email testing
- pgAdmin for database management

**Usage**:
```bash
cp .env.development .env
docker compose up
```

### `.env.testing` — Automated Testing

Optimized for CI/CD pipelines:

- Isolated test database (`erpai_platform_test`)
- Test-specific credentials
- Shorter Kafka retention (1 hour) for faster cleanup
- Image vulnerability scanning enabled
- Minimal resources for CI runners
- UTC timezone for consistent test results

**Usage**:
```bash
docker compose --env-file .env.testing up -d
./gradlew test
docker compose --env-file .env.testing down
```

### `.env.staging` — Pre-Production

Mirrors production configuration for final validation:

- Production-like resource limits
- Strong password placeholders (must be replaced)
- Image scanning enabled
- Production Spring profile
- Used for stakeholder demos and performance testing

**Usage**:
```bash
docker compose --env-file .env.staging up
```

### `.env.production` — Production

Security-hardened for live deployments:

- **All passwords must be strong, unique, and randomly generated** (min 32 chars)
- Higher resource limits for high availability
- Kafka replication factor of 3
- TLS-ready configurations
- Extended Prometheus retention (90 days)
- Comprehensive security warnings

**Usage**:
```bash
# NEVER commit this file!
docker compose --env-file .env.production up -d
```

---

## Usage Patterns

### Pattern 1: Local `.env` File (Recommended for Development)

```bash
# Copy environment file to .env
cp .env.development .env

# Edit with your values
vim .env

# Docker Compose automatically loads .env
docker compose up
```

**Priority order** (highest to lowest):
1. Shell environment variables
2. `.env` file
3. `--env-file` flag
4. `${VARIABLE:-default}` in Compose files

### Pattern 2: `--env-file` Flag (CI/CD)

```bash
# Use specific environment file without copying
docker compose --env-file .env.testing up
docker compose --env-file .env.staging up
docker compose --env-file .env.production up
```

### Pattern 3: Shell Environment Variables

```bash
# Override specific variables
POSTGRES_PASSWORD=my-secret-password docker compose up

# Multiple overrides
POSTGRES_PASSWORD=secret REDIS_PASSWORD=secret docker compose up
```

### Pattern 4: Docker Compose Profiles

```bash
# Base infrastructure only
docker compose -f compose.base.yml -f compose.infrastructure.yml up

# With monitoring
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up

# With development tools
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up

# Full stack
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml -f compose.development.yml --profile development --profile monitoring up
```

---

## Security Best Practices

### 1. Never Commit Secrets

```bash
# Verify .env files are ignored
git status --ignored | grep "\.env"

# Should show:
# .env.development
# .env.testing
# .env.staging
# .env.production
```

### 2. Use Strong Passwords

```bash
# Generate a strong password
openssl rand -base64 32

# Example output:
# K7fL9pQ2vN8xR5wZ1yA3bC6dE0gH4jM7nP2qS5tU8vW
```

### 3. Use a Secrets Manager (Production)

For production deployments, consider using:

- **HashiCorp Vault**: Centralized secrets management
- **AWS Secrets Manager**: Cloud-native secrets storage
- **Azure Key Vault**: Microsoft cloud secrets
- **Google Secret Manager**: GCP secrets storage

**Example with Docker Compose**:
```yaml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

```bash
# Export from secrets manager before starting
export POSTGRES_PASSWORD=$(vault kv get -field=password secret/postgres)
docker compose up
```

### 4. Rotate Credentials Regularly

- Rotate passwords every 90 days
- Use different passwords for each environment
- Never reuse passwords across environments
- Revoke credentials immediately when team members leave

### 5. Enable TLS in Production

```bash
# .env.production
MINIO_ENDPOINT=https://minio.example.com:9000
KEYCLOAK_HOSTNAME=auth.example.com
```

### 6. Restrict Network Access

```bash
# Use firewalls/security groups to restrict:
# - PostgreSQL: 5432 (internal only)
# - Redis: 6379 (internal only)
# - Kafka: 9092, 29092 (internal only)
# - Keycloak: 8080 (load balancer only)
# - MinIO: 9000, 9001 (internal only)
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: ERP AI Platform - Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    env:
      # Load from GitHub Secrets
      POSTGRES_PASSWORD: ${{ secrets.TEST_POSTGRES_PASSWORD }}
      REDIS_PASSWORD: ${{ secrets.TEST_REDIS_PASSWORD }}
      KEYCLOAK_ADMIN_PASSWORD: ${{ secrets.TEST_KEYCLOAK_PASSWORD }}
      MINIO_ROOT_PASSWORD: ${{ secrets.TEST_MINIO_PASSWORD }}
      GRAFANA_ADMIN_PASSWORD: ${{ secrets.TEST_GRAFANA_PASSWORD }}

    steps:
      - uses: actions/checkout@v4

      - name: Start infrastructure
        run: docker compose --env-file .env.testing up -d

      - name: Wait for services
        run: |
          sleep 30
          docker compose ps

      - name: Run tests
        run: ./gradlew test

      - name: Cleanup
        if: always()
        run: docker compose --env-file .env.testing down
```

### GitLab CI Example

```yaml
test:
  image: docker:24-git
  services:
    - docker:24-dind
  variables:
    POSTGRES_PASSWORD: $TEST_POSTGRES_PASSWORD
    REDIS_PASSWORD: $TEST_REDIS_PASSWORD
  script:
    - docker compose --env-file .env.testing up -d
    - ./gradlew test
    - docker compose --env-file .env.testing down
```

---

## Troubleshooting

### Issue: "Variable not found" error

**Solution**: Ensure the variable is defined in your `.env` file or `.env.example`.

```bash
# Check if variable exists
grep "VARIABLE_NAME" .env

# Check Docker Compose can see it
docker compose config | grep VARIABLE_NAME
```

### Issue: "Connection refused" to services

**Solution**: Services may not be ready yet. Increase health check start period.

```bash
# Check service health
docker compose ps

# View logs
docker compose logs postgres
docker compose logs redis
```

### Issue: "Password authentication failed"

**Solution**: Ensure passwords match across services.

```bash
# PostgreSQL and Flyway must use same password
# Redis exporter must use same password as Redis
# Postgres exporter must use same password as PostgreSQL
```

### Issue: Environment file not loading

**Solution**: Use correct flag and file path.

```bash
# Correct
docker compose --env-file .env.development up

# Incorrect (missing --env-file)
docker compose -f .env.development up
```

### Issue: Changes to `.env` not taking effect

**Solution**: Restart services after changing environment variables.

```bash
docker compose down
docker compose up
```

---

## Variable Reference

### Database (PostgreSQL)

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `erpai_platform` |
| `POSTGRES_USER` | Database username | `erpai` |
| `POSTGRES_PASSWORD` | Database password | `erpai_dev_password` |
| `POSTGRES_INITDB_ARGS` | Init arguments | `-E UTF8 --locale=en_US.UTF-8` |

### Cache (Redis)

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_PASSWORD` | Redis password | `erpai_dev_password` |

### Identity (Keycloak)

| Variable | Description | Default |
|----------|-------------|---------|
| `KEYCLOAK_ADMIN` | Admin username | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Admin password | `admin` |
| `KC_DB` | Database type | `dev-mem` |
| `KC_LOG_LEVEL` | Log level | `DEBUG` |

### Object Storage (MinIO)

| Variable | Description | Default |
|----------|-------------|---------|
| `MINIO_ROOT_USER` | Root username | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | Root password | `minioadmin` |
| `MINIO_REGION` | Storage region | `us-east-1` |

### Monitoring (Grafana)

| Variable | Description | Default |
|----------|-------------|---------|
| `GRAFANA_ADMIN_PASSWORD` | Admin password | `admin` |

### Message Broker (Kafka)

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_NODE_ID` | Node ID | `1` |
| `KAFKA_NUM_PARTITIONS` | Default partitions | `12` |
| `KAFKA_LOG_RETENTION_HOURS` | Log retention | `168` |

---

## Additional Resources

- [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Twelve-Factor App Config](https://12factor.net/config)
- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/)

---

## Support

For questions or issues with environment configuration:

1. Check this documentation
2. Review `.env.example` for variable descriptions
3. Check service logs: `docker compose logs <service-name>`
4. Open an issue on GitHub
