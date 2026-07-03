# Keycloak Backup and Recovery

## Overview

This document describes backup and recovery procedures for Keycloak in the ERP AI Platform.

## Backup Strategy

### 1. Database Backup

For PostgreSQL-backed Keycloak, backup the database:

```bash
# Backup Keycloak database
pg_dump -U keycloak -h localhost -d erpai_platform > keycloak-backup-$(date +%Y%m%d_%H%M%S).sql
```

### 2. Realm Export

Export realm configuration:

```bash
# Export realm configuration
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export --realm erpai --file /tmp/realm-export.json

# Copy export to host
docker compose cp keycloak:/tmp/realm-export.json ./backups/realm-export-$(date +%Y%m%d_%H%M%S).json
```

### 3. Theme Backup

Backup custom themes:

```bash
# Backup themes directory
tar czf keycloak-themes-$(date +%Y%m%d_%H%M%S).tar.gz themes/
```

## Recovery Procedures

### 1. Restore Database

```bash
# Restore database from backup
psql -U keycloak -h localhost -d erpai_platform < keycloak-backup-20240101_120000.sql
```

### 2. Import Realm

```bash
# Import realm configuration
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh import --file /tmp/realm-export.json
```

## Automated Backup Script

```bash
#!/bin/bash
# backup.sh - Keycloak backup script

BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p ${BACKUP_DIR}/${DATE}

# Backup database
pg_dump -U keycloak -h localhost -d erpai_platform > ${BACKUP_DIR}/${DATE}/keycloak.sql

# Export realm
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export --realm erpai --file /tmp/realm-export.json
docker compose cp keycloak:/tmp/realm-export.json ${BACKUP_DIR}/${DATE}/realm-export.json

echo "Backup completed: ${BACKUP_DIR}/${DATE}"
```

## Best Practices

- [ ] Schedule daily backups
- [ ] Test restore procedures regularly
- [ ] Store backups in multiple locations
- [ ] Encrypt backup files
- [ ] Document recovery procedures
- [ ] Practice disaster recovery drills
