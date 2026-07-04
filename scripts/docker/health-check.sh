#!/bin/bash
# =============================================================================
# ERP AI Platform - Health Check Script
# =============================================================================
# Description:
#   Performs comprehensive health checks on all ERP AI Platform services.
#   This script checks the health status of infrastructure services,
#   verifies connectivity, and reports any issues. It can be used for
#   monitoring, CI/CD pipelines, and troubleshooting.
#
# Use Cases:
#   - Verifying all services are running after startup
#   - Monitoring service health in CI/CD pipelines
#   - Troubleshooting connectivity issues
#   - Pre-deployment health verification
#   - Periodic health monitoring
#   - Validating service dependencies
#
# Real Examples:
#   # Run full health check
#   ./scripts/docker/health-check.sh
#
#   # Check specific services only
#   ./scripts/docker/health-check.sh --services postgres redis kafka
#
#   # Check with verbose output
#   ./scripts/docker/health-check.sh --verbose
#
#   # Check with JSON output
#   ./scripts/docker/health-check.sh --json
#
#   # Check with custom timeout
#   ./scripts/docker/health-check.sh --timeout 60
#
#   # Check and exit with code 0 even if unhealthy (for monitoring)
#   ./scripts/docker/health-check.sh --no-fail
#
#   # Check specific service endpoints
#   ./scripts/docker/health-check.sh --endpoint http://localhost:8080/actuator/health
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - curl (for HTTP health checks)
#   - Running ERP AI Platform containers
#
# Environment Variables:
#   COMPOSE_FILES      - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   COMPOSE_PROFILES   - Override profiles (default: infrastructure)
#   HEALTH_TIMEOUT     - HTTP request timeout in seconds (default: 10)
#   HEALTH_RETRIES     - Number of retries per service (default: 3)
#   HEALTH_RETRY_DELAY - Delay between retries in seconds (default: 2)
#
# Exit Codes:
#   0 - All services healthy
#   1 - One or more services unhealthy
#   2 - Prerequisites not met
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
SPECIFIC_SERVICES=()
CUSTOM_ENDPOINTS=()
TIMEOUT="${HEALTH_TIMEOUT:-10}"
RETRIES="${HEALTH_RETRIES:-3}"
RETRY_DELAY="${HEALTH_RETRY_DELAY:-2}"
JSON_OUTPUT=false
NO_FAIL=false
VERBOSE=""

# Service health check definitions
declare -A SERVICE_HEALTH_CHECKS=(
    ["postgres"]="host=localhost port=5432 type=tcp"
    ["redis"]="host=localhost port=6379 type=tcp"
    ["kafka"]="host=localhost port=29092 type=tcp"
    ["schema-registry"]="url=http://localhost:8081/health endpoint=200"
    ["keycloak"]="url=http://localhost:8080/health/ready endpoint=200"
    ["minio"]="url=http://localhost:9000/minio/health/live endpoint=200"
    ["prometheus"]="url=http://localhost:9090/-/healthy endpoint=200"
    ["grafana"]="url=http://localhost:3000/api/health endpoint=200"
    ["tempo"]="url=http://localhost:3200/ready endpoint=200"
    ["loki"]="url=http://localhost:3100/ready endpoint=200"
    ["jaeger"]="url=http://localhost:16686/ endpoint=200"
    ["mailhog"]="url=http://localhost:8025/ endpoint=200"
    ["pgadmin"]="url=http://localhost:5050/ endpoint=200"
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
    echo "  ERP AI Platform - Health Check"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --services SERVICE_LIST  Check specific services only (space-separated)
    --endpoint URL           Add custom endpoint to check
    --timeout SECONDS        HTTP request timeout (default: 10)
    --retries COUNT          Number of retries per service (default: 3)
    --retry-delay SECONDS    Delay between retries (default: 2)
    --json                   Output results in JSON format
    --no-fail                Don't exit with error code if unhealthy
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Check all services
    $(basename "$0") --services postgres redis kafka    # Check specific services
    $(basename "$0") --endpoint http://localhost:8080/actuator/health
    $(basename "$0") --json --no-fail                   # JSON output, don't fail
    $(basename "$0") --timeout 30 --retries 5           # Custom timeout and retries
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

    if ! command -v curl &> /dev/null; then
        log_error "curl is not installed or not in PATH"
        exit 2
    fi

    log_success "Prerequisites check passed"
}

check_tcp_port() {
    local host=$1
    local port=$2
    local timeout=$3

    if command -v nc &> /dev/null; then
        nc -z -w "$timeout" "$host" "$port" &> /dev/null
    elif command -v timeout &> /dev/null; then
        (echo > "/dev/tcp/$host/$port") &> /dev/null
    else
        # Fallback: try to connect with curl
        curl -s --connect-timeout "$timeout" "tcp://$host:$port" &> /dev/null
    fi
}

check_http_endpoint() {
    local url=$1
    local expected=$2
    local timeout=$3
    local retries=$4
    local retry_delay=$5

    local attempt=1
    local http_code=""

    while [ $attempt -le $retries ]; do
        http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout "$timeout" --max-time "$timeout" "$url" 2>/dev/null || echo "000")

        if [ "$http_code" = "$expected" ]; then
            echo "healthy"
            return
        fi

        if [ $attempt -lt $retries ]; then
            sleep "$retry_delay"
        fi

        attempt=$((attempt + 1))
    done

    echo "unhealthy (HTTP $http_code)"
}

check_service_health() {
    local service=$1
    local check_config=$2

    # Parse check configuration
    local host=$(echo "$check_config" | grep -o 'host=[^ ]*' | sed 's/host=//' || echo "localhost")
    local port=$(echo "$check_config" | grep -o 'port=[^ ]*' | sed 's/port=//' || echo "")
    local url=$(echo "$check_config" | grep -o 'url=[^ ]*' | sed 's/url=//' || echo "")
    local endpoint=$(echo "$check_config" | grep -o 'endpoint=[^ ]*' | sed 's/endpoint=//' || echo "200")
    local type=$(echo "$check_config" | grep -o 'type=[^ ]*' | sed 's/type=//' || echo "http")

    local status="unhealthy"
    local details=""

    if [ "$type" = "tcp" ] && [ -n "$port" ]; then
        if check_tcp_port "$host" "$port" "$TIMEOUT"; then
            status="healthy"
            details="Port $port is open"
        else
            details="Port $port is closed or unreachable"
        fi
    elif [ "$type" = "http" ] && [ -n "$url" ]; then
        local result=$(check_http_endpoint "$url" "$endpoint" "$TIMEOUT" "$RETRIES" "$RETRY_DELAY")
        status=$(echo "$result" | cut -d' ' -f1)
        details=$(echo "$result" | cut -d' ' -f2-)
    fi

    echo "$status|$details"
}

get_service_status_from_compose() {
    local service=$1
    docker compose $COMPOSE_FILES $PROFILES ps "$service" --format "{{.Status}}" 2>/dev/null || echo "not found"
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --services)
            # Read all remaining arguments until next option
            shift
            while [[ $# -gt 0 && $1 != --* ]]; do
                SPECIFIC_SERVICES+=("$1")
                shift
            done
            ;;
        --endpoint)
            CUSTOM_ENDPOINTS+=("$2")
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --retries)
            RETRIES="$2"
            shift 2
            ;;
        --retry-delay)
            RETRY_DELAY="$2"
            shift 2
            ;;
        --json)
            JSON_OUTPUT=true
            shift
            ;;
        --no-fail)
            NO_FAIL=true
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

# Determine services to check
SERVICES_TO_CHECK=()

if [ ${#SPECIFIC_SERVICES[@]} -gt 0 ]; then
    SERVICES_TO_CHECK=("${SPECIFIC_SERVICES[@]}")
else
    # Check all known services
    for service in "${!SERVICE_HEALTH_CHECKS[@]}"; do
        SERVICES_TO_CHECK+=("$service")
    done
fi

# Initialize results
declare -A RESULTS
TOTAL_SERVICES=${#SERVICES_TO_CHECK[@]}
HEALTHY_COUNT=0
UNHEALTHY_COUNT=0

log_info "Checking ${TOTAL_SERVICES} services..."
echo ""

# Check each service
for service in "${SERVICES_TO_CHECK[@]}"; do
    # Get container status
    local container_status=$(get_service_status_from_compose "$service")

    if [ "$container_status" = "not found" ]; then
        RESULTS["$service"]="not_running|Container not found"
        log_error "$service: Container not found"
        continue
    fi

    # Check if container is running
    if ! echo "$container_status" | grep -q "running\|healthy\|Up"; then
        RESULTS["$service"]="not_running|$container_status"
        log_error "$service: $container_status"
        UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
        continue
    fi

    # Perform health check
    if [ -n "${SERVICE_HEALTH_CHECKS[$service]+x}" ]; then
        local check_result=$(check_service_health "$service" "${SERVICE_HEALTH_CHECKS[$service]}")
        local status=$(echo "$check_result" | cut -d'|' -f1)
        local details=$(echo "$check_result" | cut -d'|' -f2-)

        RESULTS["$service"]="$status|$details"

        if [ "$status" = "healthy" ]; then
            log_success "$service: $details"
            HEALTHY_COUNT=$((HEALTHY_COUNT + 1))
        else
            log_error "$service: $details"
            UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
        fi
    else
        # No health check defined, just check if running
        RESULTS["$service"]="running|$container_status"
        log_success "$service: $container_status"
        HEALTHY_COUNT=$((HEALTHY_COUNT + 1))
    fi
done

# Check custom endpoints
for endpoint in "${CUSTOM_ENDPOINTS[@]}"; do
    log_info "Checking custom endpoint: $endpoint"

    local result=$(check_http_endpoint "$endpoint" "200" "$TIMEOUT" "$RETRIES" "$RETRY_DELAY")
    local status=$(echo "$result" | cut -d'|' -f1)
    local details=$(echo "$result" | cut -d'|' -f2-)

    if [ "$status" = "healthy" ]; then
        log_success "$endpoint: $details"
        HEALTHY_COUNT=$((HEALTHY_COUNT + 1))
    else
        log_error "$endpoint: $details"
        UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
    fi
done

# Output summary
echo ""
echo "=========================================="
echo "  Health Check Summary"
echo "=========================================="
echo ""
echo "Total services checked: $((TOTAL_SERVICES + ${#CUSTOM_ENDPOINTS[@]}))"
echo "Healthy: $HEALTHY_COUNT"
echo "Unhealthy: $UNHEALTHY_COUNT"
echo ""

# JSON output if requested
if [ "$JSON_OUTPUT" = true ]; then
    echo "{"
    echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
    echo "  \"total\": $((TOTAL_SERVICES + ${#CUSTOM_ENDPOINTS[@]})),"
    echo "  \"healthy\": $HEALTHY_COUNT,"
    echo "  \"unhealthy\": $UNHEALTHY_COUNT,"
    echo "  \"services\": {"

    local first=true
    for service in "${!RESULTS[@]}"; do
        local status=$(echo "${RESULTS[$service]}" | cut -d'|' -f1)
        local details=$(echo "${RESULTS[$service]}" | cut -d'|' -f2-)

        if [ "$first" = true ]; then
            first=false
        else
            echo ","
        fi

        # Escape quotes in details
        details=$(echo "$details" | sed 's/"/\\"/g')

        echo "    \"$service\": {"
        echo "      \"status\": \"$status\","
        echo "      \"details\": \"$details\""
        echo -n "    }"
    done

    echo ""
    echo "  }"
    echo "}"
fi

echo ""

# Exit with appropriate code
if [ "$UNHEALTHY_COUNT" -gt 0 ] && [ "$NO_FAIL" = false ]; then
    log_error "$UNHEALTHY_COUNT service(s) are unhealthy!"
    exit 1
elif [ "$UNHEALTHY_COUNT" -gt 0 ]; then
    log_warning "$UNHEALTHY_COUNT service(s) are unhealthy (--no-fail specified)"
    exit 0
else
    log_success "All services are healthy!"
    exit 0
fi
