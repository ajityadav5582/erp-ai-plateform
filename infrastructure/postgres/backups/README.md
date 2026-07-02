# PostgreSQL Backup & Recovery

Comprehensive backup and recovery procedures for the ERP AI Platform PostgreSQL infrastructure.

## Table of Contents

1. [Backup Strategy](#backup-strategy)
2. [Backup Methods](#backup-methods)
3. [Recovery Procedures](#recovery-procedures)
4. [Automated Backups](#automated-backups)
5. [Monitoring](#monitoring)
6. [Disaster Recovery](#disaster-recovery)

## Backup Strategy

### RPO and RTO Targets

| Metric | Target | Description |
|--------|--------|-------------|
| RPO (Recovery Point Objective) | 4 hours | Maximum data loss acceptable |
| RTO (Recovery Time Objective) | 2 hours | Maximum downtime acceptable |

### Backup Types

| Type | Frequency | Retention | Purpose |
|------|-----------|-----------|---------|
| Full backup | Daily at 2 AM UTC | 30 days | Complete database snapshot |
| Incremental backup | Every 4 hours | 7 days | Changes since last backup |
| WAL archiving | Continuous | 30 days | Point-in-time recovery |
| Schema backup | On schema change | Indefinite | Schema version control |

## Backup Methods

### 1. pg_dump (Logical Backup)

Creates a SQL script file that can be used to reconstruct the database.

```bash
# Full database backup
pg_dump -U erpai -h localhost -p 5432 -F c -b -v -f \
  /backups/erpai_platform_$(date +%Y%m%d_%H%M%S).dump erpai_platform

# Specific database backup
pg_dump -U erpai -h localhost -p 5432 -F c -b -v -f \
  /backups/erpai_finance_$(date +%Y%m%d_%H%M%S).dump erpai_finance

# All databases backup
pg_dumpall -U erpai -h localhost -p 5432 -f \
  /backups/erpai_all_$(date +%Y%m%d_%H%M%S).sql
```

#### pg_dump Options

| Option | Description |
|--------|-------------|
| `-F c` | Custom format (compressed, allows selective restore) |
| `-F p` | Plain text format (SQL script) |
| `-F d` | Directory format (parallel dump) |
| `-b` | Include large objects (BLOBs) |
| `-v` | Verbose mode |
| `-n schema` | Dump specific schema |
| `-t table` | Dump specific table |
| `--exclude-table` | Exclude specific table |

### 2. pg_basebackup (Physical Backup)

Creates a binary copy of the database cluster for fast recovery.

```bash
# Full physical backup
pg_basebackup -U erpai -h localhost -p 5432 -D /backups/basebackup_$(date +%Y%m%d) \
  -Ft -z -P -X stream -C

# Options:
# -Ft: Tar format
# -z: Gzip compression
# -P: Progress reporting
# -X stream: Stream WAL files
# -C: Create replication slot
```

### 3. WAL Archiving

Enable continuous WAL archiving for point-in-time recovery.

```sql
-- postgresql.conf settings
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backups/wal/%f'
archive_timeout = 300  # Force archive every 5 minutes
max_wal_size = 4GB
min_wal_size = 1GB
```

## Recovery Procedures

### 1. Restore from pg_dump

```bash
# Restore a single database
pg_restore -U erpai -h localhost -p 5432 -d erpai_finance \
  -v /backups/erpai_finance_20240101_120000.dump

# Restore with cleanup (drop existing objects)
pg_restore -U erpai -h localhost -p 5432 -d erpai_finance \
  --clean --if-exists -v /backups/erpai_finance_20240101_120000.dump

# Restore plain SQL dump
psql -U erpai -h localhost -p 5432 -d erpai_finance \
  -f /backups/erpai_finance_20240101_120000.sql
```

### 2. Point-in-Time Recovery (PITR)

```bash
# 1. Stop PostgreSQL
docker compose stop postgres

# 2. Remove data directory
rm -rf /var/lib/postgresql/data/*

# 3. Restore base backup
tar -xzf /backups/basebackup_20240101.tar.gz -C /var/lib/postgresql/data

# 4. Configure recovery
cat > /var/lib/postgresql/data/postgresql.conf << EOF
restore_command = 'cp /backups/wal/%f %p'
recovery_target_time = '2024-01-15 14:30:00'
recovery_target_action = 'promote'
EOF

# 5. Create recovery signal
touch /var/lib/postgresql/data/recovery.signal

# 6. Start PostgreSQL
docker compose start postgres

# 7. Verify recovery
docker compose exec postgres psql -U erpai -c "SELECT pg_is_in_recovery();"
```

### 3. Restore Specific Tables

```bash
# List tables in dump
pg_restore -l /backups/erpai_finance_20240101_120000.dump

# Restore specific tables
pg_restore -U erpai -h localhost -p 5432 -d erpai_finance \
  -t invoices -t invoice_line_items \
  -v /backups/erpai_finance_20240101_120000.dump
```

## Automated Backups

### Docker Compose Backup Service

Add to `docker-compose.yml`:

```yaml
  postgres-backup:
    image: postgres:16-alpine
    container_name: erpai-postgres-backup
    restart: unless-stopped
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_DB: erpai_platform
      POSTGRES_USER: erpai
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-erpai_dev_password}
      BACKUP_DIR: /backups
      BACKUP_RETENTION_DAYS: 30
    volumes:
      - ./infrastructure/postgres/backups:/backups
      - ./infrastructure/postgres/backups/backup.sh:/backup.sh:ro
    entrypoint: ["/backup.sh"]
    networks:
      - erpai-network
    depends_on:
      postgres:
        condition: service_healthy
    profiles:
      - infrastructure
```

### Backup Script

Create `infrastructure/postgres/backups/backup.sh`:

```bash
#!/bin/bash
set -euo pipefail

# Configuration
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-erpai_platform}"
POSTGRES_USER="${POSTGRES_USER:-erpai}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-erpai_dev_password}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p "${BACKUP_DIR}"

# Backup all databases
echo "Starting backup at ${TIMESTAMP}"
pg_dumpall -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 | gzip > \
  "${BACKUP_DIR}/erpai_all_${TIMESTAMP}.sql.gz"

# Backup individual databases
for DB in erpai_platform erpai_finance erpai_hr erpai_inventory erpai_manufacturing \
          erpai_procurement erpai_sales erpai_ai erpai_integration erpai_gateway; do
  echo "Backing up ${DB}"
  pg_dump -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 -F c -b -v \
    "${DB}" | gzip > "${BACKUP_DIR}/${DB}_${TIMESTAMP}.dump.gz"
done

# Cleanup old backups
echo "Cleaning up backups older than ${BACKUP_RETENTION_DAYS} days"
find "${BACKUP_DIR}" -type f -mtime +${BACKUP_RETENTION_DAYS} -delete

echo "Backup completed at $(date +%Y%m%d_%H%M%S)"
```

Make it executable:

```bash
chmod +x infrastructure/postgres/backups/backup.sh
```

### Cron Schedule

```bash
# Add to crontab
0 2 * * * docker compose -f compose.base.yml -f compose.infrastructure.yml --profile infrastructure run --rm postgres-backup
0 */4 * * * docker compose -f compose.base.yml -f compose.infrastructure.yml --profile infrastructure run --rm postgres-backup
```

## Monitoring

### Backup Monitoring

```sql
-- Check backup status
SELECT * FROM pg_stat_backup;

-- Check WAL archiving status
SELECT * FROM pg_stat_archiver;

-- Check replication slots
SELECT * FROM pg_replication_slots;
```

### Backup Verification

```bash
# Verify backup integrity
pg_restore -l /backups/erpai_finance_20240101_120000.dump > /dev/null && echo "Backup OK"

# Test restore to temporary database
createdb -U erpai erpai_finance_test
pg_restore -U erpai -d erpai_finance_test /backups/erpai_finance_20240101_120000.dump
dropdb -U erpai erpai_finance_test
```

### Alerts

Set up alerts for:
- Backup failures
- Backup size anomalies
- WAL archiving failures
- Disk space < 20%
- Replication lag > 30 seconds

## Disaster Recovery

### Recovery Checklist

1. **Assess the situation**
   - Identify the failure type (hardware, software, data corruption)
   - Determine the recovery point (last known good backup)
   - Estimate RTO

2. **Prepare the recovery environment**
   - Provision new PostgreSQL instance
   - Restore base backup
   - Apply WAL files for PITR

3. **Execute recovery**
   - Start PostgreSQL in recovery mode
   - Monitor recovery progress
   - Verify data integrity

4. **Validate and resume**
   - Run application smoke tests
   - Verify data consistency
   - Resume normal operations

### Recovery Commands

```bash
# Quick recovery from latest backup
BACKUP=$(ls -t /backups/erpai_all_*.sql.gz | head -1)
gunzip -c "${BACKUP}" | psql -U erpai -h localhost

# Recovery with specific point in time
# (See Point-in-Time Recovery section above)
```

### Failover to Replica

```bash
# Promote replica to primary
docker compose exec postgres-replica pg_ctl promote -D /var/lib/postgresql/data

# Update application connection string
# Change from postgres:5432 to postgres-replica:5432
```

## AWS RDS Backups

For AWS RDS deployments:

### Automated Backups

```hcl
# terraform/modules/rds/main.tf
resource "aws_db_instance" "postgres" {
  identifier     = "erpai-postgres"
  engine         = "postgres"
  engine_version = "16.4"
  instance_class = "db.r6g.xlarge"
  
  allocated_storage     = 100
  storage_encrypted     = true
  backup_retention_period = 30
  backup_window          = "02:00-03:00"
  maintenance_window     = "sun:03:00-sun:04:00"
  
  enabled_cloudwatch_logs_exports = ["postgresql"]
  
  # Point-in-time recovery
  restore_to_point_in_time {
    source_db_instance_identifier = aws_db_instance.postgres.identifier
    restore_time                  = "2024-01-15 14:30:00"
  }
}
```

### Manual Snapshots

```bash
# Create snapshot
aws rds create-db-snapshot \
  --db-instance-identifier erpai-postgres \
  --db-snapshot-identifier erpai-postgres-$(date +%Y%m%d)

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier erpai-postgres-restored \
  --db-snapshot-identifier erpai-postgres-20240101
```

## Backup Retention Policy

| Backup Type | Retention | Storage Location |
|-------------|-----------|------------------|
| Daily full | 30 days | Local + S3 |
| Incremental | 7 days | Local |
| WAL archives | 30 days | Local + S3 |
| Snapshots | 90 days | AWS EBS/S3 |
| Schema backups | Indefinite | Git repository |

## Security

- All backups are encrypted at rest
- Backup files have restricted permissions (600)
- Backup access is logged and audited
- Sensitive data is masked in non-production backups

## Testing

Monthly recovery test procedure:

1. Create test database
2. Restore from latest backup
3. Run data integrity checks
4. Verify application functionality
5. Document results
6. Clean up test environment

```bash
# Monthly test script
#!/bin/bash
set -e

echo "Starting monthly recovery test..."
TEST_DB="erpai_recovery_test_$(date +%Y%m%d)"

# Create test database
psql -U erpai -c "CREATE DATABASE ${TEST_DB};"

# Restore backup
pg_restore -U erpai -d "${TEST_DB}" /backups/erpai_platform_latest.dump

# Run integrity checks
psql -U erpai -d "${TEST_DB}" -c "SELECT count(*) FROM pg_tables;"
psql -U erpai -d "${TEST_DB}" -c "SELECT * FROM pg_stat_user_tables;"

# Cleanup
psql -U erpai -c "DROP DATABASE ${TEST_DB};"

echo "Recovery test completed successfully"
```
