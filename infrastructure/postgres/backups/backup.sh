#!/bin/bash
# =============================================================================
# ERP AI Platform - PostgreSQL Backup Script
# =============================================================================
# This script performs automated backups of all PostgreSQL databases.
# It should be run via cron or Docker Compose scheduled task.
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-erpai_platform}"
POSTGRES_USER="${POSTGRES_USER:-erpai}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-erpai_dev_password}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${BACKUP_DIR}/backup.log"

# =============================================================================
# Logging
# =============================================================================
log() {
    echo "[$(date +%Y-%m-%d\ %H:%M:%S)] $*" | tee -a "${LOG_FILE}"
}

log "=========================================="
log "Starting PostgreSQL backup"
log "=========================================="

# =============================================================================
# Pre-flight Checks
# =============================================================================
log "Running pre-flight checks..."

# Check if PostgreSQL is accessible
if ! pg_isready -h "${POSTGRES_HOST}" -p 5432 -U "${POSTGRES_USER}" -q; then
    log "ERROR: PostgreSQL is not accessible at ${POSTGRES_HOST}:5432"
    exit 1
fi

# Create backup directory
mkdir -p "${BACKUP_DIR}"

# =============================================================================
# Backup All Databases
# =============================================================================
log "Backing up all databases..."
if pg_dumpall -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 | gzip > \
    "${BACKUP_DIR}/erpai_all_${TIMESTAMP}.sql.gz"; then
    log "SUCCESS: All databases backed up to erpai_all_${TIMESTAMP}.sql.gz"
else
    log "ERROR: Failed to backup all databases"
    exit 1
fi

# =============================================================================
# Backup Individual Databases
# =============================================================================
DATABASES=(
    "erpai_platform"
    "erpai_finance"
    "erpai_hr"
    "erpai_inventory"
    "erpai_manufacturing"
    "erpai_procurement"
    "erpai_sales"
    "erpai_ai"
    "erpai_integration"
    "erpai_gateway"
)

for DB in "${DATABASES[@]}"; do
    log "Backing up ${DB}..."
    if pg_dump -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 -F c -b -v \
        "${DB}" | gzip > "${BACKUP_DIR}/${DB}_${TIMESTAMP}.dump.gz"; then
        log "SUCCESS: ${DB} backed up"
    else
        log "ERROR: Failed to backup ${DB}"
        exit 1
    fi
done

# =============================================================================
# Backup Global Objects
# =============================================================================
log "Backing up global objects (roles, tablespaces)..."
if pg_dumpall -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 --globals-only | \
    gzip > "${BACKUP_DIR}/erpai_globals_${TIMESTAMP}.sql.gz"; then
    log "SUCCESS: Global objects backed up"
else
    log "ERROR: Failed to backup global objects"
    exit 1
fi

# =============================================================================
# Cleanup Old Backups
# =============================================================================
log "Cleaning up backups older than ${BACKUP_RETENTION_DAYS} days..."
DELETED_COUNT=$(find "${BACKUP_DIR}" -type f \( -name "*.dump.gz" -o -name "*.sql.gz" \) \
    -mtime +${BACKUP_RETENTION_DAYS} -delete -print | wc -l)
log "Deleted ${DELETED_COUNT} old backup files"

# =============================================================================
# Backup Summary
# =============================================================================
log "=========================================="
log "Backup Summary"
log "=========================================="
log "Backup directory: ${BACKUP_DIR}"
log "Backup timestamp: ${TIMESTAMP}"
log "Retention period: ${BACKUP_RETENTION_DAYS} days"
log ""

# List recent backups
log "Recent backups:"
ls -lh "${BACKUP_DIR}"/*.dump.gz "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | tail -5 || true

# Calculate total backup size
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" 2>/dev/null | cut -f1)
log "Total backup size: ${TOTAL_SIZE}"

log "=========================================="
log "Backup completed successfully"
log "=========================================="

exit 0
