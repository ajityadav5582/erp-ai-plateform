#!/bin/bash
# =============================================================================
# ERP AI Platform - Stop Script
# =============================================================================
# Description:
#   Stops all running ERP AI Platform infrastructure services.
#   This script gracefully stops all containers without removing volumes,
#   allowing you to resume work later with the same data.
#
# Use Cases:
#   - Stopping the platform at the end of the workday
#   - Freeing up system resources when not actively developing
#   - Preparing for system updates or maintenance
#   - Stopping specific services for debugging
#
# Real Examples:
#   # Stop all infrastructure services
#   ./scripts/docker/stop.sh
#
#   # Stop specific services only
#   ./scripts/docker/stop.sh --services postgres redis kafka
#
#   # Stop and remove containers (but keep volumes)
#   ./scripts/docker/stop.sh --remove
#
#   # Stop with timeout (default: 30s)
#   ./scripts/docker/stop.sh --timeout 60
#
#   # Force stop (SIGKILL after timeout)
#   ./scripts/docker/stop.sh --force
#
#   # Stop and show container logs before stopping
#   ./scripts/docker/stop.sh --with-logs
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - Running ERP AI Platform containers
#
# Environment Variables:
#   COMPOSE_FILES     - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   COMPOSE_PROFILES  - Override profiles (default: infrastructure)
#   STOP_TIMEOUT      - Shutdown timeout in seconds (default: 30)
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Docker Compose error
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
TIMEOUT="${STOP_TIMEOUT:-30}"
REMOVE_CONTAINERS=false
FORCE_STOP=false
WITH_LOGS=false
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
    echo "  ERP AI Platform - Stopping Services"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --services SERVICE_LIST  Stop only specific services (space-separated)
    --remove                 Remove containers after stopping (keeps volumes)
    --force                  Force stop (SIGKILL after timeout)
    --timeout SECONDS        Shutdown timeout (default: 30)
    --with-logs              Show container logs before stopping
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Stop all services
    $(basename "$0") --services postgres redis kafka    # Stop specific services
    $(basename "$0") --remove                           # Stop and remove containers
    $(basename "$0") --timeout 60 --force               # Force stop with 60s timeout
    $(basename "$0") --with-logs                        # Show logs before stopping
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

    # Check if any containers are running
    local running_containers=$(docker compose $COMPOSE_FILES $PROFILES ps --format "{{.Name}}" 2>/dev/null | grep -v "NAME" || true)

    if [ -z "$running_containers" ]; then
        log_warning "No running containers found"
        exit 0
    fi

    log_success "Prerequisites check passed"
}

show_container_logs() {
    if [ "$WITH_LOGS" = false ]; then
        return
    fi

    log_info "Showing container logs (last 50 lines each)..."

    local containers=$(docker compose $COMPOSE_FILES $PROFILES ps --format "{{.Name}}" 2>/dev/null | grep -v "NAME" || true)

    for container in $containers; do
        echo ""
        echo "--- $container ---"
        docker logs --tail 50 "$container" 2>&1 || true
    done

    echo ""
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
        --remove)
            REMOVE_CONTAINERS=true
            shift
            ;;
        --force)
            FORCE_STOP=true
            shift
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --with-logs)
            WITH_LOGS=true
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

# Show logs if requested
show_container_logs

# Build compose command
COMPOSE_CMD="docker compose $COMPOSE_FILES $PROFILES $VERBOSE stop --timeout $TIMEOUT"

# Add services if specified
if [ -n "$SERVICES" ]; then
    COMPOSE_CMD="$COMPOSE_CMD $SERVICES"
fi

log_info "Stopping services with command:"
log_info "$COMPOSE_CMD"
echo ""

# Execute docker compose stop
if [ "$VERBOSE" = "--verbose" ]; then
    set -x
fi

if ! eval "$COMPOSE_CMD"; then
    log_error "Failed to stop services gracefully"

    if [ "$FORCE_STOP" = true ]; then
        log_warning "Attempting force stop..."
        docker compose $COMPOSE_FILES $PROFILES kill 2>/dev/null || true
    fi

    exit 3
fi

set +x

echo ""
log_success "Services stopped successfully!"

# Remove containers if requested
if [ "$REMOVE_CONTAINERS" = true ]; then
    log_info "Removing containers..."

    COMPOSE_RM_CMD="docker compose $COMPOSE_FILES $PROFILES rm -f"

    if [ -n "$SERVICES" ]; then
        COMPOSE_RM_CMD="$COMPOSE_RM_CMD $SERVICES"
    fi

    if eval "$COMPOSE_RM_CMD"; then
        log_success "Containers removed successfully!"
    else
        log_warning "Some containers could not be removed"
    fi
fi

echo ""
log_info "To start services again: ./scripts/docker/start.sh"
log_info "To view stopped containers: docker compose $COMPOSE_FILES $PROFILES ps -a"

exit 0
