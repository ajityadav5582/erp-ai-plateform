#!/bin/bash
# =============================================================================
# ERP AI Platform - Local Development Script
# =============================================================================
# Description:
#   Starts only the application and database services for fast local development.
#   This is optimized for speed — skips Kafka, Redis, Keycloak, MinIO,
#   monitoring, and other infrastructure services.
#
# Use Cases:
#   - Quick local development without full infrastructure
#   - Fast startup/shutdown cycles
#   - CI/CD pipelines that only need app + DB
#
# Real Examples:
#   # Start app and database
#   ./scripts/docker/local-dev.sh start
#
#   # Start in detached mode
#   ./scripts/docker/local-dev.sh start --detached
#
#   # Stop services
#   ./scripts/docker/local-dev.sh stop
#
#   # Restart services
#   ./scripts/docker/local-dev.sh restart
#
#   # Check health
#   ./scripts/docker/local-dev.sh health-check
#
#   # Full reset (destructive)
#   ./scripts/docker/local-dev.sh reset
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - bash 4.0+
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Script execution error
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILES="-f docker-compose.yml"
DETACHED="-d"
BUILD=""
VERBOSE=""
WAIT_FOR_HEALTHY=false
# A Spring Boot service on this stack takes ~4-6 minutes to become healthy on a
# cold restart (JIT warm-up plus Hibernate ddl-auto=update). 120s reported a
# spurious "Timeout waiting for services to become healthy" on every restart even
# though the services came up correctly a few minutes later. Override with
# HEALTH_WAIT_TIMEOUT=600 ./scripts/docker/local-dev.sh restart --wait
HEALTH_WAIT_TIMEOUT=${HEALTH_WAIT_TIMEOUT:-600}
SERVICES="postgres gateway identity-service inventory-service"
COMPOSE_SERVICES="postgres gateway identity-service inventory-service"

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
    echo "  ERP AI Platform - Local Dev"
    echo "  (App + Database only)"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") <COMMAND> [OPTIONS]

Commands:
    start           Start application and database services
    stop            Stop all local dev services
    restart         Restart all local dev services
    reset           Full reset (stop + remove volumes + start fresh)
    health-check    Check health of app and database services
    cleanup         Clean database and Docker infrastructure
    help            Show this help message

Options:
    --detached, -d      Run in background (default for start)
    --build             Build Docker images before starting
    --no-build          Skip Gradle JAR build (use existing JARs)
    --wait              Wait for services to become healthy
    --verbose, -v       Enable verbose output
    --no-confirm        Skip confirmation prompts (for reset/cleanup)
    --help, -h          Show this help message

Examples:
    $(basename "$0") start                          # Start app + DB (no wait)
    $(basename "$0") start --detached                # Start in background
    $(basename "$0") start --build                   # Build and start
    $(basename "$0") start --wait                    # Start and wait for healthy
    $(basename "$0") stop                           # Stop services
    $(basename "$0") restart                        # Restart services
    $(basename "$0") restart --wait                  # Restart and wait for healthy
    $(basename "$0") health-check                   # Check health
    $(basename "$0") reset --no-confirm             # Full reset no prompt
    $(basename "$0") cleanup --docker --no-confirm  # Clean everything

Quick Reference:
    Start:   ./scripts/docker/local-dev.sh start
    Stop:    ./scripts/docker/local-dev.sh stop
    Restart: ./scripts/docker/local-dev.sh restart
    Health:  ./scripts/docker/local-dev.sh health-check
EOF
}

check_prerequisites() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 2
    fi

    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose V2 is not available"
        exit 2
    fi
}

wait_for_services() {
    if [ "$WAIT_FOR_HEALTHY" = false ]; then
        return
    fi

    log_info "Waiting for services to become healthy (timeout: ${HEALTH_WAIT_TIMEOUT}s)..."

    local start_time=$(date +%s)
    local all_healthy=false

    while [ "$all_healthy" = false ]; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ $elapsed -gt $HEALTH_WAIT_TIMEOUT ]; then
            log_error "Timeout waiting for services to become healthy"
            log_error "Check service logs with: docker compose $COMPOSE_FILES logs"
            exit 1
        fi

        # Check if all services are healthy or started
        local unhealthy=$(docker compose $COMPOSE_FILES $VERBOSE ps --format "{{.Name}}\t{{.Status}}" 2>/dev/null | grep -v "healthy" | grep -v "running" | grep -v "Started" | grep -v "NAME" || true)

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

    docker compose $COMPOSE_FILES $VERBOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

    echo ""
}

show_access_urls() {
    log_info "Access URLs:"
    echo ""
    echo "  Database       http://localhost:5434 (PostgreSQL)"
    echo "  API Gateway    http://localhost:8080 (Single entry point)"
    echo "  Gateway Health http://localhost:8080/actuator/health"
    echo ""
    echo "  Identity (internal)  http://localhost:8082"
    echo "  Inventory (internal) http://localhost:8083"
    echo "  Tenant (internal)    http://localhost:8081"
    echo ""
}

# =============================================================================
# Command Functions
# =============================================================================

cmd_start() {
    print_banner
    check_prerequisites

    # Build JARs before Docker build (so Dockerfile.runtime can copy them)
    # Skip if --no-build flag is passed or if JARs already exist
    local SKIP_BUILD=false
    for arg in "$@"; do
        if [[ "$arg" == "--no-build" ]]; then
            SKIP_BUILD=true
            break
        fi
    done

    if [ "$SKIP_BUILD" = false ]; then
        log_info "Building application JARs..."
        echo ""
        if ! ./gradlew :platform:gateway:bootJar :platform:identity:bootJar :business:inventory:application:bootJar --no-daemon -x test; then
            log_error "Failed to build JARs"
            exit 3
        fi
        log_success "JARs built successfully"
        echo ""
    else
        log_info "Skipping JAR build (--no-build)"
        echo ""
    fi

    log_info "Starting application and database services..."
    echo ""

    # Build compose command - explicitly specify services for faster startup
    local COMPOSE_CMD="docker compose $COMPOSE_FILES $VERBOSE up $DETACHED $BUILD $COMPOSE_SERVICES"

    log_info "Running: $COMPOSE_CMD"
    echo ""

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

    log_info "To view logs: docker compose $COMPOSE_FILES logs -f"
    log_info "To stop services: ./scripts/docker/local-dev.sh stop"
    log_info "To rebuild JARs: ./gradlew bootJar"
}

cmd_stop() {
    print_banner
    check_prerequisites

    log_info "Stopping local dev services..."
    echo ""

    if ! docker compose $COMPOSE_FILES $VERBOSE down $COMPOSE_SERVICES; then
        log_error "Failed to stop services"
        exit 3
    fi

    log_success "Services stopped"
}

cmd_restart() {
    print_banner
    check_prerequisites

    log_info "Restarting local dev services..."
    echo ""

    # Rebuild the application JARs first, exactly like cmd_start does.
    #
    # This used to be skipped entirely, which made `restart` silently useless
    # after a code change: Dockerfile.runtime COPYs the boot JAR into the image,
    # so a container recreated from the cached image kept running the JAR baked
    # into it. The symptom was a fix that appeared to have no effect - the
    # service restarted cleanly and returned the exact same old error.
    local SKIP_BUILD=false
    for arg in "$@"; do
        if [[ "$arg" == "--no-build" ]]; then
            SKIP_BUILD=true
            break
        fi
    done

    if [ "$SKIP_BUILD" = false ]; then
        log_info "Building application JARs..."
        echo ""
        if ! ./gradlew :platform:gateway:bootJar :platform:identity:bootJar :business:inventory:application:bootJar --no-daemon -x test; then
            log_error "Failed to build JARs"
            exit 3
        fi
        log_success "JARs built successfully"
        echo ""
    fi

    # Always pass --build so the image picks up the freshly built JAR instead of
    # being recreated from the cached layer. --no-build opts out.
    local RESTART_BUILD="--build"
    if [ "$SKIP_BUILD" = true ]; then
        RESTART_BUILD=""
    fi

    # `if ! A && B` is a trap: it evaluates as "if (not A) then B", so a SUCCESSFUL
    # down silently skips up and leaves you with nothing running. Run the two
    # steps sequentially and check each one.
    if ! docker compose $COMPOSE_FILES $VERBOSE down $COMPOSE_SERVICES; then
        log_error "Failed to stop services"
        exit 3
    fi

    if ! docker compose $COMPOSE_FILES $VERBOSE up $DETACHED $RESTART_BUILD $COMPOSE_SERVICES; then
        log_error "Failed to start services"
        exit 3
    fi

    log_success "Services restarted"

    if [ "$WAIT_FOR_HEALTHY" = true ]; then
        wait_for_services
        show_service_status
        show_access_urls
    fi
}

cmd_reset() {
    print_banner
    check_prerequisites

    local NO_CONFIRM=""
    if [[ " $* " == *"--no-confirm"* ]]; then
        NO_CONFIRM="--no-confirm"
    fi

    if [ "$NO_CONFIRM" != "--no-confirm" ]; then
        log_warning "This will stop all services and remove volumes!"
        log_warning "All application data will be lost."
        echo ""
        read -p "Are you sure? [y/N] " confirm
        if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
            log_info "Reset cancelled"
            exit 0
        fi
    fi

    log_info "Performing full reset..."
    echo ""

    # Stop and remove volumes for local dev services only
    if ! docker compose $COMPOSE_FILES $VERBOSE down -v $COMPOSE_SERVICES; then
        log_error "Failed to stop and remove volumes"
        exit 3
    fi

    log_success "Reset complete"
    log_info "To start fresh: ./scripts/docker/local-dev.sh start"
}

cmd_health_check() {
    print_banner
    check_prerequisites

    log_info "Checking health of local dev services..."
    echo ""

    local all_healthy=true

    # Check PostgreSQL
    log_info "Checking PostgreSQL..."
    if docker compose $COMPOSE_FILES $VERBOSE ps postgres | grep -q "healthy\|running"; then
        log_success "PostgreSQL is healthy"
    else
        log_error "PostgreSQL is not healthy"
        all_healthy=false
    fi

    # Check Gateway
    log_info "Checking Gateway..."
    if docker compose $COMPOSE_FILES $VERBOSE ps gateway | grep -q "healthy\|running"; then
        log_success "Gateway is healthy"
    else
        log_error "Gateway is not healthy"
        all_healthy=false
    fi

    # Check Identity Service
    log_info "Checking Identity Service..."
    if docker compose $COMPOSE_FILES $VERBOSE ps identity-service | grep -q "healthy\|running"; then
        log_success "Identity Service is healthy"
    else
        log_error "Identity Service is not healthy"
        all_healthy=false
    fi

    # Check Inventory Service
    log_info "Checking Inventory Service..."
    if docker compose $COMPOSE_FILES $VERBOSE ps inventory-service | grep -q "healthy\|running"; then
        log_success "Inventory Service is healthy"
    else
        log_error "Inventory Service is not healthy"
        all_healthy=false
    fi

    # Check gateway endpoint
    log_info "Checking gateway endpoint..."
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "Gateway endpoint is responding"
    else
        log_warning "Gateway endpoint is not responding (may still be starting)"
    fi

    echo ""
    if [ "$all_healthy" = true ]; then
        log_success "All services are healthy"
    else
        log_error "Some services are not healthy"
        exit 1
    fi
}

cmd_cleanup() {
    print_banner
    check_prerequisites

    local DOCKER=""
    local NO_CONFIRM=""

    for arg in "$@"; do
        case $arg in
            --docker) DOCKER="--docker" ;;
            --no-confirm) NO_CONFIRM="--no-confirm" ;;
        esac
    done

    if [ "$NO_CONFIRM" != "--no-confirm" ]; then
        log_warning "This will clean database and Docker infrastructure!"
        echo ""
        read -p "Are you sure? [y/N] " confirm
        if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
            log_info "Cleanup cancelled"
            exit 0
        fi
    fi

    log_info "Running cleanup..."
    echo ""

    # Stop local dev services only
    docker compose $COMPOSE_FILES $VERBOSE down -v $COMPOSE_SERVICES ${DOCKER:+--remove-orphans} 2>/dev/null || true

    # Remove local data directories
    if [ "$DOCKER" = "--docker" ]; then
        log_info "Removing Docker volumes..."
        docker volume rm -f erpai-postgres_data erpai-postgres_wal 2>/dev/null || true
    fi

    log_success "Cleanup complete"
}

# =============================================================================
# Argument Parsing
# =============================================================================

cd "$PROJECT_ROOT"

# Check prerequisites early
check_prerequisites

# Handle no arguments
if [ $# -eq 0 ]; then
    print_banner
    print_usage
    exit 0
fi

# Parse command
COMMAND=$1
shift

# Parse global options
while [[ $# -gt 0 ]]; do
    case $1 in
        --detached|-d)
            DETACHED="-d"
            shift
            ;;
        --build)
            BUILD="--build"
            shift
            ;;
        --no-build)
            # Skip Gradle JAR build before Docker start
            shift
            ;;
        --wait)
                WAIT_FOR_HEALTHY=true
                shift
                ;;
        --verbose|-v)
            VERBOSE="--verbose"
            shift
            ;;
        --no-confirm)
            NO_CONFIRM="--no-confirm"
            shift
            ;;
        --help|-h)
            print_banner
            print_usage
            exit 0
            ;;
        *)
            break
            ;;
    esac
done

# Handle command
case $COMMAND in
    start)
        cmd_start "$@"
        ;;
    stop)
        cmd_stop "$@"
        ;;
    restart)
        cmd_restart "$@"
        ;;
    reset)
        cmd_reset "$@"
        ;;
    health-check)
        cmd_health_check "$@"
        ;;
    cleanup)
        cmd_cleanup "$@"
        ;;
    help|--help|-h)
        print_banner
        print_usage
        exit 0
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        echo ""
        print_usage
        exit 1
        ;;
esac
