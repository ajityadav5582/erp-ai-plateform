#!/usr/bin/env bash
# =============================================================================
# ERP AI Platform - Redis Backup Script
# Automated backup for Redis persistence files (RDB and AOF).
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/infrastructure/redis/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_PREFIX="redis_backup"
RETENTION_DAYS=7

# Docker Compose files
COMPOSE_FILES="-f ${PROJECT_ROOT}/compose.base.yml -f ${PROJECT_ROOT}/compose.infrastructure.yml"

# Container and service names
CONTAINER_NAME="erpai-redis"
SERVICE_NAME="redis"

# Redis credentials (from environment or defaults)
REDIS_PASSWORD="${REDIS_PASSWORD:-erpai_dev_password}"

# =============================================================================
# Functions
# =============================================================================

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_success() {
    echo "[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not available"
        exit 1
    fi
}

check_container_running() {
    if ! docker compose ${COMPOSE_FILES} ps -q ${SERVICE_NAME} | grep -q .; then
        log_error "Redis container is not running"
        exit 1
    fi
}

create_backup_dir() {
    if [ ! -d "${BACKUP_DIR}" ]; then
        mkdir -p "${BACKUP_DIR}"
        log_info "Created backup directory: ${BACKUP_DIR}"
    fi
}

trigger_rdb_save() {
    log_info "Triggering RDB background save..."
    docker compose ${COMPOSE_FILES} exec -T ${SERVICE_NAME} \
        redis-cli -a "${REDIS_PASSWORD}" BGSAVE

    # Wait for save to complete
    local max_wait=60
    local waited=0
    while [ ${waited} -lt ${max_wait} ]; do
        local saving
        saving=$(docker compose ${COMPOSE_FILES} exec -T ${SERVICE_NAME} \
            redis-cli -a "${REDIS_PASSWORD}" info persistence | grep rdb_bgsave_in_progress | cut -d: -f2)
        if [ "${saving}" = "0" ]; then
            log_info "RDB save completed"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done

    log_error "RDB save did not complete within ${max_wait} seconds"
    return 1
}

backup_rdb() {
    local backup_file="${BACKUP_DIR}/${BACKUP_PREFIX}_rdb_${TIMESTAMP}.rdb"

    log_info "Backing up RDB file to: ${backup_file}"

    # Copy RDB file from container
    docker compose ${COMPOSE_FILES} cp \
        "${CONTAINER_NAME}:/data/dump.rdb" \
        "${backup_file}"

    if [ -f "${backup_file}" ]; then
        local size
        size=$(du -h "${backup_file}" | cut -f1)
        log_success "RDB backup completed (${size})"
    else
        log_error "RDB backup failed"
        return 1
    fi
}

backup_aof() {
    local backup_file="${BACKUP_DIR}/${BACKUP_PREFIX}_aof_${TIMESTAMP}.aof"

    log_info "Backing up AOF file to: ${backup_file}"

    # Copy AOF file from container
    docker compose ${COMPOSE_FILES} cp \
        "${CONTAINER_NAME}:/data/appendonly.aof" \
        "${backup_file}"

    if [ -f "${backup_file}" ]; then
        local size
        size=$(du -h "${backup_file}" | cut -f1)
        log_success "AOF backup completed (${size})"
    else
        log_error "AOF backup failed"
        return 1
    fi
}

backup_config() {
    local backup_file="${BACKUP_DIR}/${BACKUP_PREFIX}_config_${TIMESTAMP}.conf"

    log_info "Backing up Redis configuration to: ${backup_file}"

    # Export current Redis configuration
    docker compose ${COMPOSE_FILES} exec -T ${SERVICE_NAME} \
        redis-cli -a "${REDIS_PASSWORD}" CONFIG GET '*' > "${backup_file}" 2>/dev/null || true

    if [ -f "${backup_file}" ]; then
        local size
        size=$(du -h "${backup_file}" | cut -f1)
        log_success "Config backup completed (${size})"
    else
        log_error "Config backup failed"
        return 1
    fi
}

verify_backup() {
    local backup_file="$1"
    local file_type="$2"

    if [ ! -f "${backup_file}" ]; then
        log_error "Backup file not found: ${backup_file}"
        return 1
    fi

    local size
    size=$(stat -f%z "${backup_file}" 2>/dev/null || stat -c%s "${backup_file}" 2>/dev/null || echo "0")

    if [ "${size}" -eq 0 ]; then
        log_error "Backup file is empty: ${backup_file}"
        return 1
    fi

    log_info "Backup verified: ${backup_file} (${size} bytes)"
    return 0
}

cleanup_old_backups() {
    log_info "Cleaning up backups older than ${RETENTION_DAYS} days..."

    local deleted=0
    while IFS= read -r -d '' file; do
        rm -f "${file}"
        deleted=$((deleted + 1))
    done < <(find "${BACKUP_DIR}" -name "${BACKUP_PREFIX}_*" -type f -mtime +${RETENTION_DAYS} -print0)

    if [ ${deleted} -gt 0 ]; then
        log_info "Deleted ${deleted} old backup(s)"
    else
        log_info "No old backups to delete"
    fi
}

list_backups() {
    log_info "Existing backups in ${BACKUP_DIR}:"
    if [ -d "${BACKUP_DIR}" ]; then
        ls -lah "${BACKUP_DIR}"/${BACKUP_PREFIX}_* 2>/dev/null || log_info "No backups found"
    else
        log_info "Backup directory does not exist"
    fi
}

# =============================================================================
# Main
# =============================================================================

main() {
    log_info "Starting Redis backup process..."

    check_docker
    check_container_running
    create_backup_dir

    # Perform backups
    trigger_rdb_save
    backup_rdb
    backup_aof
    backup_config

    # Cleanup old backups
    cleanup_old_backups

    # List current backups
    list_backups

    log_success "Redis backup process completed successfully"
}

# =============================================================================
# Entry Point
# =============================================================================

main "$@"
