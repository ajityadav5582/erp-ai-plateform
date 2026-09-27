#!/bin/bash
# =============================================================================
# ERP AI Platform - Cleanup Script
# =============================================================================
# Description:
#   Cleans up Flyway migration history, database tables, and Docker
#   infrastructure (containers, volumes, images) so services can be
#   rebuilt and restarted from a completely clean state.
#
# Use Cases:
#   - Clearing database state before rebuilding services
#   - Removing Flyway migration history to re-run migrations from scratch
#   - Cleaning up everything including Docker images for a fresh start
#
# Real Examples:
#   ./scripts/docker/cleanup.sh                    # Clean DB + tables + Flyway
#   ./scripts/docker/cleanup.sh --docker           # Also stop containers + remove volumes
#   ./scripts/docker/cleanup.sh --docker --images  # Also remove Docker images
#   ./scripts/docker/cleanup.sh --no-confirm       # Skip confirmation prompt
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   4 - User cancelled
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DB_NAME="${POSTGRES_DB:-erpai_platform}"
DB_USER="${POSTGRES_USER:-erpai}"
DB_PASSWORD="${POSTGRES_PASSWORD:-erpai_dev_password}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5434}"
AUTO_CONFIRM="${AUTO_CONFIRM:-false}"
DOCKER_CLEANUP=false
REMOVE_IMAGES=false

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

print_warning() {
    echo ""
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║                            WARNING                             ║${NC}"
    echo -e "${YELLOW}╠════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║  This will CLEAN UP: ${DB_NAME}                                ║${NC}"
    echo -e "${YELLOW}║  • All tables and data will be dropped                          ║${NC}"
    echo -e "${YELLOW}║  • Flyway migration history will be cleared                     ║${NC}"
    if [ "$DOCKER_CLEANUP" = true ]; then
        echo -e "${YELLOW}║  • Docker containers will be stopped                             ║${NC}"
        echo -e "${YELLOW}║  • Docker volumes will be removed                                ║${NC}"
    fi
    if [ "$REMOVE_IMAGES" = true ]; then
        echo -e "${YELLOW}║  • Docker images will be removed                                 ║${NC}"
    fi
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 2
    fi

    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose V2 is not available"
        exit 2
    fi

    log_success "Prerequisites check passed"
}

confirm_cleanup() {
    if [ "$AUTO_CONFIRM" = true ]; then
        return 0
    fi

    print_warning
    read -p "Are you sure you want to clean up? (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Cleanup cancelled by user"
        exit 4
    fi
}

# ---------------------------------------------------------------------------
# Database Cleanup
# ---------------------------------------------------------------------------
drop_all_tables() {
    log_info "Dropping all tables in database '${DB_NAME}'..."

    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" &> /dev/null; then
        log_warning "Cannot connect to PostgreSQL — skipping table cleanup"
        return
    fi

    local tables
    tables=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
          AND table_name != 'flyway_schema_history'
        ORDER BY table_name;
    " 2>/dev/null | tr -d ' ' | grep -v '^$' || true)

    if [ -z "$tables" ]; then
        log_info "No user tables found to drop"
        return
    fi

    local table_count
    table_count=$(echo "$tables" | wc -l | tr -d ' ')
    log_info "Found ${table_count} table(s) to drop"

    local drop_sql="DROP TABLE IF EXISTS"
    local first=true
    while IFS= read -r table; do
        if [ -n "$table" ]; then
            if [ "$first" = true ]; then
                drop_sql="${drop_sql} public.${table}"
                first=false
            else
                drop_sql="${drop_sql}, public.${table}"
            fi
        fi
    done <<< "$tables"
    drop_sql="${drop_sql} CASCADE;"

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$drop_sql" 2>/dev/null || true
    log_success "All tables dropped"
}

cleanup_flyway_history() {
    log_info "Cleaning up Flyway schema history..."

    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &> /dev/null; then
        log_warning "Cannot connect to PostgreSQL — skipping Flyway cleanup"
        return
    fi

    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1 FROM flyway_schema_history LIMIT 1" &> /dev/null; then
        log_info "Flyway schema history table does not exist — nothing to clean"
        return
    fi

    local migration_count
    migration_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM flyway_schema_history;" 2>/dev/null | tr -d ' ' || echo "0")

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;" 2>/dev/null || true

    log_success "Flyway schema history cleaned (${migration_count} migration(s) removed)"
}

# ---------------------------------------------------------------------------
# Docker Cleanup
# ---------------------------------------------------------------------------
stop_docker_containers() {
    log_info "Stopping Docker containers..."

    local containers
    containers=$(docker compose -f compose.base.yml -f compose.infrastructure.yml ps --format "{{.Name}}" 2>/dev/null | grep -v "^NAME$" || true)

    if [ -z "$containers" ]; then
        log_info "No running containers found"
        return
    fi

    local count
    count=$(echo "$containers" | wc -l | tr -d ' ')
    log_info "Stopping ${count} container(s)..."

    docker compose -f compose.base.yml -f compose.infrastructure.yml down --timeout 30 2>/dev/null || true

    log_success "Containers stopped"
}

remove_docker_volumes() {
    log_info "Removing Docker volumes..."

    local volumes
    volumes=$(docker volume ls --format "{{.Name}}" 2>/dev/null | grep "^erpai_" || true)

    if [ -z "$volumes" ]; then
        log_info "No project volumes found"
        return
    fi

    local count
    count=$(echo "$volumes" | wc -l | tr -d ' ')
    log_info "Removing ${count} volume(s)..."

    echo "$volumes" | while IFS= read -r volume; do
        docker volume rm "$volume" 2>/dev/null || true
    done

    # Also remove dangling volumes
    docker volume prune -f 2>/dev/null || true

    log_success "Volumes removed"
}

remove_docker_images() {
    if [ "$REMOVE_IMAGES" = false ]; then
        return
    fi

    log_info "Removing all Docker images..."

    # First remove all containers (including stopped ones)
    local containers
    containers=$(docker ps -aq 2>/dev/null || true)
    if [ -n "$containers" ]; then
        log_info "Removing all containers..."
        docker rm -f $containers 2>/dev/null || true
    fi

    # Remove all images
    local images
    images=$(docker images --format "{{.Repository}}:{{.Tag}}" 2>/dev/null | grep -v "^<none>" || true)

    if [ -z "$images" ]; then
        log_info "No images found"
    else
        local count
        count=$(echo "$images" | wc -l | tr -d ' ')
        log_info "Removing ${count} image(s)..."

        # Use docker image prune -a to remove all unused images
        docker image prune -af 2>/dev/null || true

        # Force remove any remaining images
        echo "$images" | while IFS= read -r image; do
            docker rmi -f "$image" 2>/dev/null || true
        done
    fi

    # Final prune to clean up any dangling images
    docker image prune -af 2>/dev/null || true

    log_success "Images removed"
}

cleanup_docker() {
    if [ "$DOCKER_CLEANUP" = false ]; then
        return
    fi

    log_info "Cleaning up Docker infrastructure..."

    stop_docker_containers
    remove_docker_volumes
    remove_docker_images

    log_success "Docker infrastructure cleanup completed"
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --docker)
            DOCKER_CLEANUP=true
            shift
            ;;
        --images)
            REMOVE_IMAGES=true
            shift
            ;;
        --no-confirm)
            AUTO_CONFIRM=true
            shift
            ;;
        --help|-h)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --docker       Also stop containers and remove volumes
    --images       Also remove Docker images (with --docker)
    --no-confirm   Skip confirmation prompt
    --help, -h     Show this help message

Examples:
    $(basename "$0")                          # Clean DB tables + Flyway history
    $(basename "$0") --docker                # Also stop containers + remove volumes
    $(basename "$0") --docker --images       # Full cleanup including images
    $(basename "$0") --no-confirm            # Clean without prompt
EOF
}

# =============================================================================
# Main Execution
# =============================================================================

cd "$PROJECT_ROOT"

echo ""
echo "=========================================="
echo "  ERP AI Platform - Cleanup"
echo "=========================================="
echo ""

check_prerequisites
confirm_cleanup

# Database cleanup
drop_all_tables
cleanup_flyway_history

# Docker cleanup
cleanup_docker

echo ""
log_success "Cleanup completed!"
log_info "Run './scripts/docker/docker.sh start' to bring everything back up"

exit 0
