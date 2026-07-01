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

## Usage

```bash
# Make scripts executable
chmod +x scripts/*/*.sh

# Run setup
./scripts/setup/setup-dev-environment.sh

# Run health check
./scripts/monitoring/health-check.sh

# Run backup
./scripts/backup/backup-database.sh
```
