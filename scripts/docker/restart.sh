#!/bin/bash
# =============================================================================
# ERP AI Platform - Restart Script
# =============================================================================
# Description:
#   Restarts the ERP AI Platform infrastructure services.
#   This script stops and then starts all services, optionally rebuilding images.
#   Useful for applying configuration changes, code updates, or recovering from errors.
#
# Use Cases:
#   - Applying configuration changes to running services
#   - Recovering from service failures
#   - Rolling out new Docker image versions
#   - Refreshing service state after code changes
#   - Cycling services to clear temporary states
#
# Real Examples:
#   # Restart all services (stop and start)
#   ./scripts/docker/restart.sh
#
#   # Restart with rebuild (useful after code changes)
#   ./scripts/docker/restart.sh --build
#
#   # Restart specific services only
#   ./scripts/docker/restart.sh --services postgres redis kafka
#
#   # Restart with development profile
#   ./scripts/docker/restart.sh --profile development
#
#   # Force restart (kill and start)
#   ./scripts/docker/restart.sh --force
#
#   # Restart with extended timeout
#   ./scripts/docker/restart.sh --timeout 60
#
#   # Restart and show logs
#   ./scripts/docker/restart.sh --with-logs
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
#   DOCKER_BUILDKIT   - Enable BuildKit (default: 1)
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
BUILD=""
FORCE_RESTART=false
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
    echo "  ERP AI Platform - Restarting Services"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --profile PROFILE        Add a compose profile (can be used multiple times)
                             Available: infrastructure, development, monitoring
    --services SERVICE_LIST  Restart only specific services (space-separated)
    --build                  Rebuild images before restarting
    --force                  Force restart (kill and start)
    --timeout SECONDS        Shutdown timeout (default: 30)
    --with-logs              Show container logs before restarting
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Restart all services
    $(basename "$0") --build                            # Rebuild and restart
    $(basename "$0") --services postgres redis kafka    # Restart specific services
    $(basename "$0") --profile development --build      # Rebuild dev profile
    $(basename "$0") --force --timeout 60               # Force restart with timeout
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
        log_warning "No running containers found. Starting services instead..."
        exec "$SCRIPT_DIR/start.sh" "$@"
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
        --profile)
            PROFILES="$PROFILES --profile $2"
            shift 2
            ;;
        --services)
            SERVICES="$2"
            shift 2
            ;;
        --build)
            BUILD="--build"
            shift
            ;;
        --force)
            FORCE_RESTART=true
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

# Build compose commands
STOP_CMD="docker compose $COMPOSE_FILES $PROFILES stop --timeout $TIMEOUT"
START_CMD="docker compose $COMPOSE_FILES $PROFILES up -d $BUILD"

# Add services if specified
if [ -n "$SERVICES" ]; then
    STOP_CMD="$STOP_CMD $SERVICES"
    START_CMD="$START_CMD $SERVICES"
fi

# Stop services
log_info "Stopping services..."
echo ""

if [ "$VERBOSE" = "--verbose" ]; then
    set -x
fi

if [ "$FORCE_RESTART" = true ]; then
    log_warning "Force restarting services..."
    docker compose $COMPOSE_FILES $PROFILES kill 2>/dev/null || true
    sleep 2
else
    if ! eval "$STOP_CMD"; then
        log_error "Failed to stop services gracefully"
        exit 3
    fi
fi

set +x

echo ""
log_success "Services stopped"

# Start services
log_info "Starting services..."
echo ""

if [ "$VERBOSE" = "--verbose" ]; then
    set -x
fi

if ! eval "$START_CMD"; then
    log_error "Failed to start services"
    exit 3
fi

set +x

echo ""
log_success "Services restarted successfully!"

# Wait a moment for services to initialize
log_info "Waiting for services to initialize..."
sleep 10

# Show status
log_info "Service status:"
echo ""
docker compose $COMPOSE_FILES $PROFILES ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

echo ""
log_info "To view logs: docker compose $COMPOSE_FILES $PROFILES logs -f"
log_info "To stop services: ./scripts/docker/stop.sh"

exit 0
