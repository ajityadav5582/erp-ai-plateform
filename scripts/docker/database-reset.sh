#!/bin/bash
# =============================================================================
# ERP AI Platform - Database Reset Script
# =============================================================================
# Description:
#   Resets only the PostgreSQL database without affecting other services.
#   This script drops and recreates the database, runs Flyway migrations,
#   and optionally seeds initial data. Other services (Kafka, Redis, etc.)
#   remain running.
#
# Use Cases:
#   - Resetting database schema during development
#   - Cleaning test data without losing Kafka topics
#   - Re-running Flyway migrations from scratch
#   - Seeding fresh data for testing
#   - Recovering from database corruption
#
# Real Examples:
#   # Full database reset (drop, recreate, migrate)
#   ./scripts/docker/database-reset.sh
#
#   # Reset and seed with initial data
#   ./scripts/docker/database-reset.sh --seed
#
#   # Reset specific service database only
#   ./scripts/docker/database-reset.sh --service finance
#
#   # Reset without running migrations
#   ./scripts/docker/database-reset.sh --no-migrate
#
#   # Reset with custom database name
#   ./scripts/docker/database-reset.sh --db-name my_custom_db
#
#   # Reset and show SQL output
#   ./scripts/docker/database-reset.sh --verbose
#
#   # Force reset without confirmation
#   ./scripts/docker/database-reset.sh --no-confirm
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - Running PostgreSQL container
#   - Flyway migrations available in infrastructure/postgres/flyway/
#
# Environment Variables:
#   COMPOSE_FILES      - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   POSTGRES_DB        - Database name (default: erpai_platform)
#   POSTGRES_USER      - Database user (default: erpai)
#   POSTGRES_PASSWORD  - Database password (default: erpai_dev_password)
#   FLYWAY_LOCATIONS   - Flyway migration locations
#   AUTO_CONFIRM       - Skip confirmation prompt (default: false)
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Database operation error
#   4 - User cancelled
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
COMPOSE_FILES="-f compose.base.yml -f compose.infrastructure.yml"
DB_NAME="${POSTGRES_DB:-erpai_platform}"
DB_USER="${POSTGRES_USER:-erpai}"
DB_PASSWORD="${POSTGRES_PASSWORD:-erpai_dev_password}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
SERVICE=""
RUN_MIGRATIONS=true
SEED_DATA=false
AUTO_CONFIRM="${AUTO_CONFIRM:-false}"
VERBOSE=""

# Flyway configuration
FLYWAY_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
FLYWAY_LOCATIONS="${FLYWAY_LOCATIONS:-filesystem:infrastructure/postgres/flyway/{service}}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_banner() {
    echo ""
    echo "=========================================="
    echo "  ERP AI Platform - Database Reset"
    echo "=========================================="
    echo ""
}

print_warning() {
    echo ""
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║                            WARNING                             ║${NC}"
    echo -e "${YELLOW}╠════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║  This will DELETE all data in database: ${DB_NAME}${NC}"
    echo -e "${YELLOW}║  • All tables and data will be dropped                          ║${NC}"
    echo -e "${YELLOW}║  • Flyway migrations will be re-run                             ║${NC}"
    echo -e "${YELLOW}║  • Other services (Kafka, Redis) will NOT be affected           ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --service SERVICE        Reset specific service database (e.g., finance, hr)
    --db-name NAME           Custom database name (default: erpai_platform)
    --no-migrate             Skip Flyway migrations after reset
    --seed                   Seed database with initial data after reset
    --no-confirm             Skip confirmation prompt
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Reset main database
    $(basename "$0") --service finance                  # Reset finance database
    $(basename "$0") --seed                             # Reset and seed data
    $(basename "$0") --no-migrate --seed                # Reset without migrations
    $(basename "$0") --db-name test_db --no-confirm     # Reset test database
EOF
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if PostgreSQL is running
    if ! docker compose $COMPOSE_FILES ps postgres --format "{{.Status}}" 2>/dev/null | grep -q "running\|healthy"; then
        log_error "PostgreSQL container is not running"
        log_info "Start it with: ./scripts/docker/start.sh --services postgres"
        exit 2
    fi

    # Check if we can connect to PostgreSQL
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" &> /dev/null; then
        log_error "Cannot connect to PostgreSQL at ${DB_HOST}:${DB_PORT}"
        log_info "Check credentials and ensure PostgreSQL is ready"
        exit 2
    fi

    # Check if Flyway is available
    if ! command -v flyway &> /dev/null; then
        log_warning "Flyway CLI not found. Will use Docker to run migrations."
    fi

    log_success "Prerequisites check passed"
}

confirm_reset() {
    if [ "$AUTO_CONFIRM" = true ]; then
        return 0
    fi

    print_warning

    read -p "Are you sure you want to reset database '${DB_NAME}'? (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Reset cancelled by user"
        exit 4
    fi
}

drop_and_recreate_database() {
    log_info "Dropping database '${DB_NAME}'..."

    # Connect to postgres database and drop the target database
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "
        SELECT pg_terminate_backend(pid)
        FROM pg_stat_activity
        WHERE datname = '${DB_NAME}'
        AND pid <> pg_backend_pid();
    " 2>/dev/null || true

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "
        DROP DATABASE IF EXISTS ${DB_NAME};
    " 2>/dev/null || true

    log_info "Creating database '${DB_NAME}'..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "
        CREATE DATABASE ${DB_NAME}
        WITH ENCODING = 'UTF8'
        LC_COLLATE = 'en_US.UTF-8'
        LC_CTYPE = 'en_US.UTF-8'
        TEMPLATE = template0;
    " 2>/dev/null || true

    log_success "Database '${DB_NAME}' recreated"
}

run_flyway_migrations() {
    if [ "$RUN_MIGRATIONS" = false ]; then
        log_info "Skipping Flyway migrations (--no-migrate specified)"
        return
    fi

    log_info "Running Flyway migrations..."

    # Determine migration locations
    if [ -n "$SERVICE" ]; then
        MIGRATION_LOCATIONS="filesystem:infrastructure/postgres/flyway/${SERVICE}"
    else
        # Run migrations for all services
        MIGRATION_LOCATIONS=""
        for service_dir in "$PROJECT_ROOT"/infrastructure/postgres/flyway/*/; do
            if [ -d "$service_dir" ]; then
                service_name=$(basename "$service_dir")
                if [ -n "$MIGRATION_LOCATIONS" ]; then
                    MIGRATION_LOCATIONS="$MIGRATION_LOCATIONS,filesystem:infrastructure/postgres/flyway/${service_name}"
                else
                    MIGRATION_LOCATIONS="filesystem:infrastructure/postgres/flyway/${service_name}"
                fi
            fi
        done
    fi

    if command -v flyway &> /dev/null; then
        # Use Flyway CLI
        flyway \
            -url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
            -user="$DB_USER" \
            -password="$DB_PASSWORD" \
            -locations="$MIGRATION_LOCATIONS" \
            -baselineOnMigrate=true \
            -baselineVersion=1 \
            migrate
    else
        # Use Docker to run Flyway
        docker run --rm \
            -v "${PROJECT_ROOT}/infrastructure/postgres/flyway:/flyway/sql:ro" \
            flyway/flyway:10.12-alpine \
            -url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
            -user="$DB_USER" \
            -password="$DB_PASSWORD" \
            -locations="$MIGRATION_LOCATIONS" \
            -baselineOnMigrate=true \
            -baselineVersion=1 \
            migrate
    fi

    log_success "Flyway migrations completed"
}

seed_initial_data() {
    if [ "$SEED_DATA" = false ]; then
        return
    fi

    log_info "Seeding initial data..."

    # Check if seed script exists
    SEED_SCRIPT="$PROJECT_ROOT/infrastructure/postgres/seed/seed.sql"

    if [ ! -f "$SEED_SCRIPT" ]; then
        log_warning "No seed script found at $SEED_SCRIPT"
        return
    fi

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SEED_SCRIPT"

    log_success "Initial data seeded"
}

show_database_info() {
    log_info "Database information:"
    echo ""

    # Show database size
    DB_SIZE=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT pg_size_pretty(pg_database_size('${DB_NAME}'));" 2>/dev/null || echo "N/A")
    echo "  Database: $DB_NAME"
    echo "  Size: $DB_SIZE"
    echo ""

    # Show tables
    TABLE_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null || echo "N/A")
    echo "  Tables: $TABLE_COUNT"
    echo ""

    # Show Flyway schema history
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;" &> /dev/null; then
        echo "  Flyway migrations:"
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null || true
    fi

    echo ""
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --service)
            SERVICE="$2"
            shift 2
            ;;
        --db-name)
            DB_NAME="$2"
            shift 2
            ;;
        --no-migrate)
            RUN_MIGRATIONS=false
            shift
            ;;
        --seed)
            SEED_DATA=true
            shift
            ;;
        --no-confirm)
            AUTO_CONFIRM=true
            shift
            ;;
        --verbose|-v)
            VERBOSE="--verbose"
            shift
            ;;
        --help|-h)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# =============================================================================
# Main Execution
# =============================================================================

cd "$PROJECT_ROOT"

print_banner

# Check prerequisites
check_prerequisites

# Confirm reset
confirm_reset

# Drop and recreate database
drop_and_recreate_database

# Run Flyway migrations
run_flyway_migrations

# Seed initial data if requested
seed_initial_data

# Show database info
show_database_info

log_success "Database reset completed successfully!"
log_info "Database '${DB_NAME}' is ready for use"

exit 0
