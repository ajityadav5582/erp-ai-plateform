#!/bin/bash
# =============================================================================
# ERP AI Platform - Docker Automation Script
# =============================================================================
# Description:
#   Main entry point for Docker automation scripts. This script provides
#   a unified interface to all Docker-related operations including start,
#   stop, restart, reset, and health checks.
#
# Use Cases:
#   - Single command interface for all Docker operations
#   - Consistent interface across development team
#   - Easy to remember commands for common operations
#   - Wrapper for complex multi-step operations
#
# Real Examples:
#   # Start all services
#   ./scripts/docker/docker.sh start
#
#   # Stop all services
#   ./scripts/docker/docker.sh stop
#
#   # Restart with rebuild
#   ./scripts/docker/docker.sh restart --build
#
#   # Full reset
#   ./scripts/docker/docker.sh reset
#
#   # Reset only database
#   ./scripts/docker/docker.sh database-reset
#
#   # Reset only Kafka
#   ./scripts/docker/docker.sh kafka-reset
#
#   # Import Keycloak realm
#   ./scripts/docker/docker.sh keycloak-import
#
#   # Run health check
#   ./scripts/docker/docker.sh health-check
#
#   # Start with development profile
#   ./scripts/docker/docker.sh start --profile development
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - bash 4.0+
#
# Environment Variables:
#   COMPOSE_FILES     - Override compose files
#   COMPOSE_PROFILES  - Override profiles
#   DOCKER_BUILDKIT   - Enable BuildKit (default: 1)
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
    echo "  ERP AI Platform - Docker Automation"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") <COMMAND> [OPTIONS]

Commands:
    start           Start all infrastructure services
    stop            Stop all infrastructure services
    restart         Restart all infrastructure services
    reset           Full reset (stop + remove volumes + start fresh)
    database-reset  Reset only the PostgreSQL database
    kafka-reset     Reset only Kafka (delete topics + recreate)
    keycloak-import Import Keycloak realm configuration
    health-check    Check health of all services
    help            Show this help message

Options:
    All options are passed through to the underlying script.
    Use --help with any command for command-specific options.

Examples:
    $(basename "$0") start                                    # Start all services
    $(basename "$0") start --profile development              # Start with dev tools
    $(basename "$0") stop --services postgres redis           # Stop specific services
    $(basename "$0") restart --build                          # Rebuild and restart
    $(basename "$0") reset --no-confirm                       # Full reset without prompt
    $(basename "$0") database-reset --seed                    # Reset and seed database
    $(basename "$0") kafka-reset --no-recreate                # Delete topics only
    $(basename "$0") keycloak-import --force                  # Force import realm
    $(basename "$0") health-check --services postgres kafka   # Check specific services

Quick Reference:
    Start:   ./scripts/docker/docker.sh start
    Stop:    ./scripts/docker/docker.sh stop
    Restart: ./scripts/docker/docker.sh restart
    Reset:   ./scripts/docker/docker.sh reset
    Health:  ./scripts/docker/docker.sh health-check
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

# =============================================================================
# Command Router
# =============================================================================

run_command() {
    local command=$1
    shift

    local script_path="$SCRIPT_DIR/${command}.sh"

    if [ ! -f "$script_path" ]; then
        log_error "Unknown command: $command"
        log_info "Run '$(basename "$0") help' for available commands"
        exit 1
    fi

    if [ ! -x "$script_path" ]; then
        chmod +x "$script_path"
    fi

    log_info "Running: $command"
    echo ""

    # Execute the script with all remaining arguments
    "$script_path" "$@"
}

# =============================================================================
# Main Execution
# =============================================================================

cd "$PROJECT_ROOT"

# Check prerequisites
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

case $COMMAND in
    start|stop|restart|reset|database-reset|kafka-reset|keycloak-import|health-check)
        print_banner
        run_command "$COMMAND" "$@"
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
