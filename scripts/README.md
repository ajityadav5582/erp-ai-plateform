# ERP AI Platform - Scripts

Utility scripts for development, deployment, and operations.

## Scripts

### Setup
- `setup-dev-environment.sh` - Sets up local development environment

### Deployment
- `deploy-staging.sh` - Deploys to staging environment
- `deploy-production.sh` - Deploys to production environment

### Backup
- `backup-database.sh` - Creates database backups
- `backup-kafka.sh` - Creates Kafka topic backups

### Monitoring
- `health-check.sh` - Checks service health
- `metrics-collector.sh` - Collects custom metrics

### Migration
- `flyway-migrate.sh` - Runs database migrations
- `kafka-reassign.sh` - Rebalances Kafka partitions

### Docker
- `docker/docker.sh` - Main Docker automation entry point
- `docker/start.sh` - Starts Docker Compose services
- `docker/stop.sh` - Stops Docker Compose services
- `docker/restart.sh` - Restarts Docker Compose services
- `docker/reset.sh` - Full reset (stop + remove volumes + start)
- `docker/cleanup.sh` - Clean database and Docker infrastructure
- `docker/kill-ports.sh` - Kills processes on conflicting ports

### Cleanup
- `cleanup.sh` - Full cleanup (DB + Flyway + Docker + images)

## Usage

```bash
# Make scripts executable
chmod +x scripts/*/*.sh

# Run setup
./scripts/setup/setup-dev-environment.sh

# Run health check
./scripts/monitoring/health-check.sh

# Kill conflicting ports before starting
./scripts/docker/kill-ports.sh

# Run backup
./scripts/backup/backup-database.sh
```
