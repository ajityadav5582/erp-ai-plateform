# MinIO Backup and Recovery

## Overview

This document describes backup and recovery procedures for MinIO in the ERP AI Platform.

## Backup Strategy

### 1. Data Directory Backup

MinIO stores data in the `/data` directory. Backup the entire volume:

```bash
# Backup MinIO data directory
docker compose exec minio tar czf - /data > minio-backup-$(date +%Y%m%d_%H%M%S).tar.gz
```

### 2. Bucket Export

Export specific buckets using `mc` (MinIO Client):

```bash
# Install MinIO client
brew install minio/stable/mc

# Configure client
mc alias set local http://localhost:9000 minioadmin minioadmin

# Export bucket
mc mirror local/dev.tenant.documents ./backups/documents-$(date +%Y%m%d_%H%M%S)
```

### 3. Configuration Backup

Backup MinIO configuration:

```bash
# Backup configuration
docker compose exec minio cat /etc/minio/minio.env > minio-config-$(date +%Y%m%d_%H%M%S).env
```

## Recovery Procedures

### 1. Restore Data Directory

```bash
# Stop MinIO
docker compose stop minio

# Restore data
tar xzf minio-backup-20240101_120000.tar.gz -C /var/lib/docker/volumes/erpai-platform_minio_data/_data

# Start MinIO
docker compose start minio
```

### 2. Restore Bucket

```bash
# Restore bucket from backup
mc mirror ./backups/documents-20240101_120000 local/dev.tenant.documents
```

## Automated Backup Script

```bash
#!/bin/bash
# backup.sh - MinIO backup script

BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p ${BACKUP_DIR}/${DATE}

# Backup data directory
docker compose exec minio tar czf - /data > ${BACKUP_DIR}/${DATE}/minio-data.tar.gz

# Export critical buckets
mc mirror local/dev.tenant.documents ${BACKUP_DIR}/${DATE}/documents
mc mirror local/dev.tenant.attachments ${BACKUP_DIR}/${DATE}/attachments

echo "Backup completed: ${BACKUP_DIR}/${DATE}"
```

## Best Practices

- [ ] Schedule daily backups
- [ ] Test restore procedures regularly
- [ ] Store backups in multiple locations
- [ ] Encrypt backup files
- [ ] Document recovery procedures
- [ ] Practice disaster recovery drills
- [ ] Monitor backup job success
- [ ] Retain backups for at least 30 days
