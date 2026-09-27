#!/bin/bash
# =============================================================================
# ERP AI Platform - PostgreSQL Backup Script
# =============================================================================
# This script performs automated backup of the single PostgreSQL database.
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
# Backup Single Database
# =============================================================================
log "Backing up database: ${POSTGRES_DB}..."
if pg_dump -U "${POSTGRES_USER}" -h "${POSTGRES_HOST}" -p 5432 -F c -b -v \
    "${POSTGRES_DB}" | gzip > "${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.dump.gz"; then
    log "SUCCESS: ${POSTGRES_DB} backed up"
else
    log "ERROR: Failed to backup ${POSTGRES_DB}"
    exit 1
fi

# =============================================================================
# Cleanup Old Backups
# =============================================================================
log "Cleaning up backups older than ${BACKUP_RETENTION_DAYS} days..."
DELETED_COUNT=$(find "${BACKUP_DIR}" -type f -name "*.dump.gz" \
    -mtime +${BACKUP_RETENTION_DAYS} -delete -print | wc -l)
log "Deleted ${DELETED_COUNT} old backup files"

# =============================================================================
# Backup Summary
# =============================================================================
log "=========================================="
log "Backup Summary"
log "=========================================="
log "Database: ${POSTGRES_DB}"
log "Backup directory: ${BACKUP_DIR}"
log "Backup timestamp: ${TIMESTAMP}"
log "Retention period: ${BACKUP_RETENTION_DAYS} days"
log ""

# List recent backups
log "Recent backups:"
ls -lh "${BACKUP_DIR}"/*.dump.gz 2>/dev/null | tail -5 || true

# Calculate total backup size
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" 2>/dev/null | cut -f1)
log "Total backup size: ${TOTAL_SIZE}"

log "=========================================="
log "Backup completed successfully"
log "=========================================="

exit 0
