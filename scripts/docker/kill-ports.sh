#!/bin/bash
# =============================================================================
# ERP AI Platform - Kill Ports Script
# =============================================================================
# Description:
#   Kills processes occupying ports required by the ERP AI Platform Docker
#   services. This resolves port conflicts that occur when starting the
#   application with `./scripts/docker/docker.sh start --profile development`.
#
# Use Cases:
#   - Resolving port conflicts before starting Docker services
#   - Cleaning up stale processes after stopping services
#   - Pre-flight check before running docker compose up
#   - CI/CD pipeline cleanup step
#
# Real Examples:
#   # Kill processes on all default ports
#   ./scripts/docker/kill-ports.sh
#
#   # Dry run - show what would be killed without actually killing
#   ./scripts/docker/kill-ports.sh --dry-run
#
#   # Kill processes on specific ports only
#   ./scripts/docker/kill-ports.sh 5432 8080 9000
#
#   # Force kill without confirmation prompt
#   ./scripts/docker/kill-ports.sh --force
#
#   # Kill and wait for ports to be freed
#   ./scripts/docker/kill-ports.sh --wait
#
# Prerequisites:
#   - bash 4.0+
#   - lsof (available on macOS and most Linux distributions)
#   - kill command (standard on Unix-like systems)
#
# Default Ports Killed:
#   Infrastructure:  5432 (PostgreSQL), 6379 (Redis), 8080 (Keycloak),
#                    8081 (Schema Registry), 9000 (MinIO API),
#                    9001 (MinIO Console), 9092 (Kafka), 29092 (Kafka External)
#   Monitoring:      3000 (Grafana), 9090 (Prometheus), 9093 (Alertmanager),
#                    3100 (Loki), 3200 (Tempo), 4317 (Tempo gRPC),
#                    4318 (Tempo HTTP), 9187 (Postgres Exporter),
#                    9121 (Redis Exporter)
#   Development:     1025 (MailHog SMTP), 8025 (MailHog UI), 5050 (pgAdmin),
#                    8082 (Kafka UI)
#
# Environment Variables:
#   KILL_PORTS_FORCE    - Set to "true" to skip confirmation prompt
#   KILL_PORTS_WAIT     - Set to "true" to wait for ports to be freed
#   KILL_PORTS_TIMEOUT  - Seconds to wait for port release (default: 30)
#
# Exit Codes:
#   0 - Success (all ports freed or no conflicts)
#   1 - General error
#   2 - Prerequisites not met
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default ports used by ERP AI Platform services
# Format: "port:service_name"
DEFAULT_PORTS=(
    "5432:PostgreSQL"
    "6379:Redis"
    "8080:Keycloak"
    "8081:Schema Registry"
    "8082:Kafka UI"
    "9000:MinIO API"
    "9001:MinIO Console"
    "9092:Kafka Broker"
    "29092:Kafka External"
    "3000:Grafana"
    "9090:Prometheus"
    "9093:Alertmanager"
    "3100:Loki"
    "3200:Tempo Query"
    "4317:Tempo OTLP gRPC"
    "4318:Tempo OTLP HTTP"
    "9187:Postgres Exporter"
    "9121:Redis Exporter"
    "1025:MailHog SMTP"
    "8025:MailHog UI"
    "5050:pgAdmin"
)

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
    echo "  ERP AI Platform - Kill Ports"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS] [PORTS...]

Kill processes occupying ports required by the ERP AI Platform.

Options:
    --dry-run, -n        Show what would be killed without actually killing
    --force, -f          Skip confirmation prompt
    --wait, -w           Wait for ports to be freed after killing
    --timeout SECONDS    Timeout for --wait (default: 30)
    --list, -l           List default ports and exit
    --help, -h           Show this help message

Arguments:
    PORTS                Specific ports to kill (space-separated)
                         If not provided, all default ports are checked

Examples:
    $(basename "$0")                                    # Kill all default ports
    $(basename "$0") --dry-run                         # Show what would be killed
    $(basename "$0") --force                           # Kill without prompt
    $(basename "$0") --wait --timeout 60               # Kill and wait up to 60s
    $(basename "$0") 5432 8080 9000                    # Kill specific ports
    $(basename "$0") --list                            # List default ports

Default Ports:
    Infrastructure:  5432, 6379, 8080, 8081, 8082, 9000, 9001, 9092, 29092
    Monitoring:      3000, 9090, 9093, 3100, 3200, 4317, 4318, 9187, 9121
    Development:     1025, 8025, 5050
EOF
}

list_default_ports() {
    echo "Default ports monitored by this script:"
    echo ""
    echo "  Infrastructure:"
    for port_info in "${DEFAULT_PORTS[@]}"; do
        local port="${port_info%%:*}"
        local service="${port_info##*:}"
        # Only show infrastructure ports (first 9)
        if [[ "$port" =~ ^(5432|6379|8080|8081|8082|9000|9001|9092|29092)$ ]]; then
            printf "    %-6s %s\n" "$port" "$service"
        fi
    done
    echo ""
    echo "  Monitoring:"
    for port_info in "${DEFAULT_PORTS[@]}"; do
        local port="${port_info%%:*}"
        local service="${port_info##*:}"
        if [[ "$port" =~ ^(3000|9090|9093|3100|3200|4317|4318|9187|9121)$ ]]; then
            printf "    %-6s %s\n" "$port" "$service"
        fi
    done
    echo ""
    echo "  Development:"
    for port_info in "${DEFAULT_PORTS[@]}"; do
        local port="${port_info%%:*}"
        local service="${port_info##*:}"
        if [[ "$port" =~ ^(1025|8025|5050)$ ]]; then
            printf "    %-6s %s\n" "$port" "$service"
        fi
    done
    echo ""
}

check_prerequisites() {
    # Check for lsof
    if ! command -v lsof &> /dev/null; then
        log_error "lsof is not installed or not in PATH"
        log_info "Install lsof:"
        log_info "  macOS:  brew install lsof (usually pre-installed)"
        log_info "  Ubuntu: sudo apt-get install lsof"
        log_info "  CentOS: sudo yum install lsof"
        exit 2
    fi

    # Check for kill
    if ! command -v kill &> /dev/null; then
        log_error "kill command is not available"
        exit 2
    fi
}

get_processes_on_port() {
    local port=$1
    # Get PIDs listening on the port, excluding the grep/lsof process itself
    lsof -Pi ":$port" -sTCP:LISTEN -t 2>/dev/null || true
}

kill_processes_on_port() {
    local port=$1
    local service=$2
    local force=$3
    local dry_run=$4

    local pids=$(get_processes_on_port "$port")

    if [ -z "$pids" ]; then
        log_info "Port $port ($service) is free"
        return 0
    fi

    log_warning "Port $port ($service) is in use by PID(s): $pids"

    if [ "$dry_run" = "true" ]; then
        log_info "[DRY RUN] Would kill PID(s): $pids"
        return 0
    fi

    if [ "$force" != "true" ]; then
        echo -n "Kill process(es) on port $port ($service)? [Y/n] "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]] && [ -n "$response" ]; then
            log_info "Skipped port $port"
            return 0
        fi
    fi

    # Try graceful termination first (SIGTERM)
    for pid in $pids; do
        if kill -0 "$pid" 2>/dev/null; then
            kill -TERM "$pid" 2>/dev/null || true
        fi
    done

    # Wait briefly for graceful shutdown
    sleep 2

    # Check if processes are still running
    local remaining_pids=$(get_processes_on_port "$port")
    if [ -n "$remaining_pids" ]; then
        log_warning "Process(es) $remaining_pids did not exit gracefully, force killing..."
        for pid in $remaining_pids; do
            if kill -0 "$pid" 2>/dev/null; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        done
        sleep 1
    fi

    # Verify port is now free
    local final_pids=$(get_processes_on_port "$port")
    if [ -z "$final_pids" ]; then
        log_success "Port $port ($service) is now free"
    else
        log_error "Failed to free port $port ($service). Remaining PID(s): $final_pids"
        return 1
    fi
}

wait_for_ports() {
    local ports=("$@")
    local timeout=${KILL_PORTS_TIMEOUT:-30}
    local start_time=$(date +%s)

    log_info "Waiting for ports to be freed (timeout: ${timeout}s)..."

    while true; do
        local all_free=true
        for port_info in "${ports[@]}"; do
            local port="${port_info%%:*}"
            local pids=$(get_processes_on_port "$port")
            if [ -n "$pids" ]; then
                all_free=false
                break
            fi
        done

        if [ "$all_free" = true ]; then
            log_success "All ports are now free"
            return 0
        fi

        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ "$elapsed" -gt "$timeout" ]; then
            log_error "Timeout waiting for ports to be freed after ${timeout}s"
            return 1
        fi

        log_info "Waiting for ports to be freed... (${elapsed}/${timeout}s)"
        sleep 2
    done
}

# =============================================================================
# Argument Parsing
# =============================================================================

DRY_RUN=false
FORCE=false
WAIT=false
CUSTOM_PORTS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run|-n)
            DRY_RUN=true
            shift
            ;;
        --force|-f)
            FORCE=true
            shift
            ;;
        --wait|-w)
            WAIT=true
            shift
            ;;
        --timeout)
            KILL_PORTS_TIMEOUT="$2"
            shift 2
            ;;
        --list|-l)
            print_banner
            list_default_ports
            exit 0
            ;;
        --help|-h)
            print_banner
            print_usage
            exit 0
            ;;
        -*)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
        *)
            # Treat as port number
            CUSTOM_PORTS+=("$1")
            shift
            ;;
    esac
done

# Check environment variables
if [ "${KILL_PORTS_FORCE:-false}" = "true" ]; then
    FORCE=true
fi

if [ "${KILL_PORTS_WAIT:-false}" = "true" ]; then
    WAIT=true
fi

# =============================================================================
# Main Execution
# =============================================================================

print_banner

# Check prerequisites
check_prerequisites

# Build port list
PORTS_TO_CHECK=()

if [ ${#CUSTOM_PORTS[@]} -gt 0 ]; then
    # Use custom ports
    for port in "${CUSTOM_PORTS[@]}"; do
        # Validate port is a number
        if ! [[ "$port" =~ ^[0-9]+$ ]]; then
            log_error "Invalid port number: $port"
            exit 1
        fi
        PORTS_TO_CHECK+=("$port:Custom")
    done
    log_info "Checking custom ports: ${CUSTOM_PORTS[*]}"
else
    # Use default ports
    PORTS_TO_CHECK=("${DEFAULT_PORTS[@]}")
    log_info "Checking default ERP AI Platform ports"
fi

echo ""

# Track if any ports were in use
ANY_CONFLICTS=false

# Kill processes on each port
for port_info in "${PORTS_TO_CHECK[@]}"; do
    port="${port_info%%:*}"
    service="${port_info##*:}"

    pids=$(get_processes_on_port "$port")
    if [ -n "$pids" ]; then
        ANY_CONFLICTS=true
    fi

    if ! kill_processes_on_port "$port" "$service" "$FORCE" "$DRY_RUN"; then
        log_error "Failed to free port $port"
        exit 1
    fi
done

echo ""

# Wait for ports if requested
if [ "$WAIT" = true ] && [ "$DRY_RUN" != true ]; then
    if [ "$ANY_CONFLICTS" = true ]; then
        wait_for_ports "${PORTS_TO_CHECK[@]}"
    else
        log_info "No ports needed waiting"
    fi
fi

echo ""
log_success "Port cleanup complete!"

if [ "$DRY_RUN" = true ]; then
    log_info "This was a dry run. No processes were actually killed."
    log_info "Run without --dry-run to actually free the ports."
fi

exit 0
