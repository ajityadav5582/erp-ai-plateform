#!/bin/bash
# =============================================================================
# ERP AI Platform - Reset Script
# =============================================================================
# Description:
#   Completely resets the ERP AI Platform by stopping all services and
#   removing all volumes (data). This is a destructive operation that
#   deletes all persistent data including databases, Kafka topics, and
#   Keycloak configurations.
#
# Use Cases:
#   - Starting fresh after corrupted data
#   - Cleaning up before a major version upgrade
#   - Resetting development environment to known state
#   - Removing all test data and configurations
#   - Troubleshooting persistent issues
#
# Real Examples:
#   # Full reset (stop + remove volumes + start fresh)
#   ./scripts/docker/reset.sh
#
#   # Reset without starting (just clean up)
#   ./scripts/docker/reset.sh --no-start
#
#   # Reset specific services only
#   ./scripts/docker/reset.sh --services postgres kafka keycloak
#
#   # Reset with confirmation prompt
#   ./scripts/docker/reset.sh --confirm
#
#   # Reset and rebuild images
#   ./scripts/docker/reset.sh --build
#
#   # Reset with verbose output
#   ./scripts/docker/reset.sh --verbose
#
# WARNING:
#   This script will PERMANENTLY DELETE all data including:
#   - PostgreSQL databases and all application data
#   - Kafka topics and messages
#   - Keycloak users, roles, and configurations
#   - MinIO buckets and objects
#   - Redis cache data
#   - All other persistent volumes
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - Confirmation that data can be deleted
#
# Environment Variables:
#   COMPOSE_FILES     - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   COMPOSE_PROFILES  - Override profiles (default: infrastructure)
#   AUTO_CONFIRM      - Skip confirmation prompt (default: false)
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Docker Compose error
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
PROFILES="--profile infrastructure"
SERVICES=""
BUILD=""
NO_START=false
AUTO_CONFIRM="${AUTO_CONFIRM:-false}"
VERBOSE=""

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
    echo "  ERP AI Platform - Full Reset"
    echo "=========================================="
    echo ""
}

print_warning() {
    echo ""
    echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                            WARNING                             ║${NC}"
    echo -e "${RED}╠════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${RED}║  This will PERMANENTLY DELETE all data including:              ║${NC}"
    echo -e "${RED}║  • PostgreSQL databases and all application data               ║${NC}"
    echo -e "${RED}║  • Kafka topics and messages                                   ║${NC}"
    echo -e "${RED}║  • Keycloak users, roles, and configurations                   ║${NC}"
    echo -e "${RED}║  • MinIO buckets and objects                                   ║${NC}"
    echo -e "${RED}║  • Redis cache data                                            ║${NC}"
    echo -e "${RED}║  • All other persistent volumes                                ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --services SERVICE_LIST  Reset only specific services (space-separated)
    --no-start               Reset without starting services
    --build                  Rebuild images after reset
    --confirm                Show confirmation prompt (default: true)
    --no-confirm             Skip confirmation prompt
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Full reset with confirmation
    $(basename "$0") --no-confirm                      # Full reset without prompt
    $(basename "$0") --no-start                        # Reset without starting
    $(basename "$0") --services postgres kafka         # Reset specific services
    $(basename "$0") --build --no-start                # Reset and rebuild
EOF
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

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 2
    fi

    log_success "Prerequisites check passed"
}

confirm_reset() {
    if [ "$AUTO_CONFIRM" = true ]; then
        return 0
    fi

    print_warning

    read -p "Are you sure you want to reset all data? This cannot be undone! (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Reset cancelled by user"
        exit 4
    fi
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --services)
            SERVICES="$2"
            shift 2
            ;;
        --no-start)
            NO_START=true
            shift
            ;;
        --build)
            BUILD="--build"
            shift
            ;;
        --confirm)
            AUTO_CONFIRM=false
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

# Stop all services
log_info "Stopping all services..."
echo ""

if [ "$VERBOSE" = "--verbose" ]; then
    set -x
fi

docker compose $COMPOSE_FILES $PROFILES down --timeout 30 2>/dev/null || true

set +x

echo ""
log_success "All services stopped"

# Remove volumes
log_info "Removing all volumes (this deletes all data)..."

if [ -n "$SERVICES" ]; then
    # Remove volumes for specific services
    for service in $SERVICES; do
        log_info "Removing volume for $service..."
        docker volume rm "${PROJECT_ROOT##*/}_${service}_data" 2>/dev/null || true
        docker volume rm "${PROJECT_ROOT##*/}_${service}_wal" 2>/dev/null || true
    done
else
    # Remove all volumes
    docker compose $COMPOSE_FILES $PROFILES down -v --timeout 30 2>/dev/null || true
fi

echo ""
log_success "All volumes removed"

# Remove any dangling volumes
log_info "Cleaning up dangling volumes..."
docker volume prune -f 2>/dev/null || true

echo ""
log_success "Reset complete!"

# Start services if requested
if [ "$NO_START" = false ]; then
    log_info "Starting fresh services..."

    # Build start command
    START_CMD="$SCRIPT_DIR/start.sh $BUILD"

    if [ -n "$SERVICES" ]; then
        START_CMD="$START_CMD --services $SERVICES"
    fi

    if [ "$VERBOSE" = "--verbose" ]; then
        START_CMD="$START_CMD --verbose"
    fi

    exec $START_CMD
else
    echo ""
    log_info "To start fresh services: ./scripts/docker/start.sh"
    exit 0
fi
