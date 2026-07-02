# Redis Backup & Recovery

Automated backup procedures for Redis persistence files.

## Overview

The backup system creates snapshots of Redis data using both RDB and AOF persistence mechanisms. Backups include:

- **RDB snapshots** (`dump.rdb`) - Point-in-time snapshots
- **AOF logs** (`appendonly.aof`) - Append-only file for durability
- **Configuration** (`*.conf`) - Current Redis configuration export

## Quick Start

### Run a Backup

```bash
# From project root
bash infrastructure/redis/backups/backup.sh

# Or make executable and run directly
chmod +x infrastructure/redis/backups/backup.sh
./infrastructure/redis/backups/backup.sh
```

### List Existing Backups

```bash
ls -lah infrastructure/redis/backups/
```

### Restore from Backup

```bash
# Stop Redis
docker compose -f compose.base.yml -f compose.infrastructure.yml stop redis

# Remove existing data volume
docker compose -f compose.base.yml -f compose.infrastructure.yml rm -f redis
docker volume rm erpai-platform_redis_data

# Restore RDB backup
docker compose -f compose.base.yml -f compose.infrastructure.yml cp \
    ./infrastructure/redis/backups/redis_backup_rdb_YYYYMMDD_HHMMSS.rdb \
    erpai-redis:/data/dump.rdb

# Restore AOF backup (optional)
docker compose -f compose.base.yml -f compose.infrastructure.yml cp \
    ./infrastructure/redis/backups/redis_backup_aof_YYYYMMDD_HHMMSS.aof \
    erpai-redis:/data/appendonly.aof

# Start Redis
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d redis

# Verify restoration
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD ping
```

## Automated Backups

### Cron Job (Linux/macOS)

Add to crontab (`crontab -e`):

```bash
# Daily backup at 2:00 AM
0 2 * * * /path/to/erp-ai-platform/infrastructure/redis/backups/backup.sh >> /var/log/redis-backup.log 2>&1

# Weekly full backup on Sundays at 3:00 AM
0 3 * * 0 /path/to/erp-ai-platform/infrastructure/redis/backups/backup.sh >> /var/log/redis-backup-weekly.log 2>&1
```

### Systemd Timer (Linux)

Create `/etc/systemd/system/redis-backup.service`:

```ini
[Unit]
Description=Redis Backup Service
After=docker.service

[Service]
Type=oneshot
ExecStart=/path/to/erp-ai-platform/infrastructure/redis/backups/backup.sh
User=your-user
Group=docker
```

Create `/etc/systemd/system/redis-backup.timer`:

```ini
[Unit]
Description=Redis Backup Timer
Requires=redis-backup.service

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now redis-backup.timer
```

## Backup Retention

The backup script automatically removes backups older than 7 days. To change retention:

```bash
# Edit the backup script
RETENTION_DAYS=14  # Keep backups for 14 days
```

## Backup Verification

After each backup, the script verifies:

1. Backup files were created successfully
2. Files are not empty
3. File sizes are reported

### Manual Verification

```bash
# Check backup file integrity
ls -lah infrastructure/redis/backups/

# Verify RDB file (requires redis-cli)
redis-cli --rdb < infrastructure/redis/backups/redis_backup_rdb_*.rdb

# Check AOF file
tail -n 20 infrastructure/redis/backups/redis_backup_aof_*.aof
```

## Disaster Recovery

### Complete Data Loss Recovery

```bash
# 1. Stop Redis
docker compose -f compose.base.yml -f compose.infrastructure.yml stop redis

# 2. Remove container and volume
docker compose -f compose.base.yml -f compose.infrastructure.yml rm -f redis
docker volume rm erpai-platform_redis_data

# 3. Restore from latest backup
LATEST_RDB=$(ls -t infrastructure/redis/backups/redis_backup_rdb_*.rdb | head -1)
LATEST_AOF=$(ls -t infrastructure/redis/backups/redis_backup_aof_*.aof | head -1)

docker compose -f compose.base.yml -f compose.infrastructure.yml cp \
    "${LATEST_RDB}" erpai-redis:/data/dump.rdb

if [ -f "${LATEST_AOF}" ]; then
    docker compose -f compose.base.yml -f compose.infrastructure.yml cp \
        "${LATEST_AOF}" erpai-redis:/data/appendonly.aof
fi

# 4. Start Redis
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d redis

# 5. Verify data
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD DBSIZE
```

### Partial Data Recovery

```bash
# Export specific keys to JSON
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD --rdb /tmp/partial.rdb

# Or use redis-dump for JSON export
# docker compose exec redis redis-cli -a $REDIS_PASSWORD --scan | \
#     while read key; do
#         redis-cli -a $REDIS_PASSWORD DUMP "$key"
#     done
```

## Monitoring Backups

### Check Backup Status

```bash
# View backup logs
tail -f /var/log/redis-backup.log

# Check last backup time
ls -lt infrastructure/redis/backups/ | head -5
```

### Backup Health Check

```bash
# Verify Redis persistence is working
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD info persistence

# Check last save time
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD info persistence | grep rdb_last_save_time
```

## Troubleshooting

### Backup Script Fails

```bash
# Check Docker is running
docker ps

# Check Redis container is running
docker compose -f compose.base.yml -f compose.infrastructure.yml ps redis

# Check permissions
ls -la infrastructure/redis/backups/

# Run with debug output
bash -x infrastructure/redis/backups/backup.sh
```

### Restore Issues

```bash
# Check Redis logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs redis

# Verify backup file integrity
file infrastructure/redis/backups/redis_backup_rdb_*.rdb

# Check Redis configuration
docker compose -f compose.base.yml -f compose.infrastructure.yml exec redis \
    redis-cli -a $REDIS_PASSWORD CONFIG GET dir
```

## References

- [Redis Persistence](https://redis.io/topics/persistence)
- [Redis Backup and Restore](https://redis.io/docs/management/backup/)
- [PostgreSQL Backup Procedures](../postgres/backups/README.md)
