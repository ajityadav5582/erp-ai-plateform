#!/bin/bash
# =============================================================================
# ERP AI Platform - Start Script
# =============================================================================
# Description:
#   Starts the ERP AI Platform infrastructure services using Docker Compose.
#   This script brings up all core infrastructure services including databases,
#   cache, message broker, identity provider, and object storage.
#
# Use Cases:
#   - Starting the development environment for the first time
#   - Resuming work after stopping the platform
#   - Deploying infrastructure in CI/CD pipelines
#   - Starting specific service profiles (infrastructure, monitoring, development)
#
# Real Examples:
#   # Start all core infrastructure services
#   ./scripts/docker/start.sh
#
#   # Start with development profile (includes MailHog, pgAdmin)
#   ./scripts/docker/start.sh --profile development
#
#   # Start with monitoring profile (includes Prometheus, Grafana, etc.)
#   ./scripts/docker/start.sh --profile monitoring
#
#   # Start with both development and monitoring profiles
#   ./scripts/docker/start.sh --profile development --profile monitoring
#
#   # Start specific services only
#   ./scripts/docker/start.sh --services postgres redis kafka
#
#   # Start in detached mode (background)
#   ./scripts/docker/start.sh --detached
#
#   # Force rebuild of images before starting
#   ./scripts/docker/start.sh --build
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - At least 4GB of available RAM
#   - Ports 5432, 6379, 8080, 8081, 8082, 9000, 9001, 9092, 29092 available
#
# Environment Variables:
#   COMPOSE_FILES     - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   COMPOSE_PROFILES  - Override profiles (default: infrastructure)
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
DETACHED="-d"
BUILD=""
SERVICES=""
VERBOSE=""
WAIT_FOR_HEALTHY=true
HEALTH_WAIT_TIMEOUT=300  # 5 minutes

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
    echo "  ERP AI Platform - Starting Services"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --profile PROFILE        Add a compose profile (can be used multiple times)
                             Available: infrastructure, development, monitoring
    --services SERVICE_LIST  Start only specific services (space-separated)
    --detached, -d           Run in detached mode (background)
    --build                  Build images before starting
    --no-wait                Skip waiting for services to be healthy
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Start core infrastructure
    $(basename "$0") --profile development              # Start with dev tools
    $(basename "$0") --profile monitoring               # Start with monitoring
    $(basename "$0") --profile development --profile monitoring
    $(basename "$0") --services postgres redis kafka    # Start specific services
    $(basename "$0") --build --detached                 # Build and start in background
EOF
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 2
    fi

    # Check Docker Compose V2
    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose V2 is not available"
        exit 2
    fi

    # Check Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 2
    fi

    # Check available memory (at least 4GB)
    if [[ "$(uname)" == "Darwin" ]]; then
        # macOS
        TOTAL_MEM=$(sysctl -n hw.memsize | awk '{print $1/1024/1024/1024}')
    else
        # Linux
        TOTAL_MEM=$(free -g | awk '/^Mem:/{print $2}')
    fi

    if (( $(echo "$TOTAL_MEM < 4" | bc -l) )); then
        log_warning "Less than 4GB RAM available ($TOTAL_MEM GB). Performance may be degraded."
    fi

    log_success "Prerequisites check passed"
}

check_port_availability() {
    log_info "Checking port availability..."

    local ports=(
        "5432:PostgreSQL"
        "6379:Redis"
        "8080:Keycloak"
        "8081:Schema Registry"
        "8082:Kafka UI"
        "9000:MinIO API"
        "9001:MinIO Console"
        "9092:Kafka Broker"
        "29092:Kafka External"
    )

    local conflicts=()

    for port_info in "${ports[@]}"; do
        local port="${port_info%%:*}"
        local service="${port_info##*:}"

        if lsof -Pi ":$port" -sTCP:LISTEN -t &> /dev/null; then
            conflicts+=("$service (port $port)")
        fi
    done

    if [ ${#conflicts[@]} -gt 0 ]; then
        log_error "Port conflicts detected:"
        for conflict in "${conflicts[@]}"; do
            echo "  - $conflict"
        done
        log_error "Stop the conflicting services or modify port mappings in docker-compose files"
        exit 2
    fi

    log_success "All ports are available"
}

wait_for_services() {
    if [ "$WAIT_FOR_HEALTHY" = false ]; then
        return
    fi

    log_info "Waiting for services to become healthy (timeout: ${HEALTH_WAIT_TIMEOUT}s)..."

    local start_time=$(date +%s)
    local all_healthy=false

    while [ $all_healthy = false ]; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ $elapsed -gt $HEALTH_WAIT_TIMEOUT ]; then
            log_error "Timeout waiting for services to become healthy"
            log_error "Check service logs with: docker compose -f compose.base.yml -f compose.infrastructure.yml logs"
            exit 1
        fi

        # Check if all services are healthy
        local unhealthy=$(docker compose $COMPOSE_FILES $PROFILES ps --format "{{.Name}}\t{{.Status}}" 2>/dev/null | grep -v "healthy" | grep -v "running" | grep -v "NAME" || true)

        if [ -z "$unhealthy" ]; then
            all_healthy=true
            break
        fi

        log_info "Waiting for services... ($elapsed/${HEALTH_WAIT_TIMEOUT}s)"
        sleep 10
    done

    log_success "All services are healthy"
}

show_service_status() {
    log_info "Service status:"
    echo ""

    docker compose $COMPOSE_FILES $PROFILES ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

    echo ""
}

show_access_urls() {
    log_info "Access URLs:"
    echo ""
    echo "  Service          URL                           Credentials"
    echo "  ──────────────── ───────────────────────────── ──────────────────"
    echo "  Grafana          http://localhost:3000          admin/admin"
    echo "  Keycloak         http://localhost:8080          admin/admin"
    echo "  MinIO Console    http://localhost:9001          minioadmin/minioadmin"
    echo "  pgAdmin          http://localhost:5050          admin@erpai.local/admin"
    echo "  MailHog          http://localhost:8025          -"
    echo "  Prometheus       http://localhost:9090          -"
    echo "  Kafka UI         http://localhost:8082          -"
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
        --detached|-d)
            DETACHED="-d"
            shift
            ;;
        --build)
            BUILD="--build"
            shift
            ;;
        --no-wait)
            WAIT_FOR_HEALTHY=false
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
check_port_availability

# Build compose command
COMPOSE_CMD="docker compose $COMPOSE_FILES $PROFILES $VERBOSE up $DETACHED $BUILD"

# Add services if specified
if [ -n "$SERVICES" ]; then
    COMPOSE_CMD="$COMPOSE_CMD $SERVICES"
fi

log_info "Starting services with command:"
log_info "$COMPOSE_CMD"
echo ""

# Execute docker compose up
if [ "$VERBOSE" = "--verbose" ]; then
    set -x
fi

if ! eval "$COMPOSE_CMD"; then
    log_error "Failed to start services"
    exit 3
fi

set +x

echo ""
log_success "Services started successfully!"

# Wait for services to be healthy
wait_for_services

# Show status and access URLs
show_service_status
show_access_urls

log_info "To view logs: docker compose $COMPOSE_FILES $PROFILES logs -f"
log_info "To stop services: ./scripts/docker/stop.sh"

exit 0
