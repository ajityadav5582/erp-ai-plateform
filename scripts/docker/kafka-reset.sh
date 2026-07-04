#!/bin/bash
# =============================================================================
# ERP AI Platform - Kafka Reset Script
# =============================================================================
# Description:
#   Resets Kafka by deleting all topics and recreating infrastructure topics.
#   This script preserves the Kafka container and its configuration but
#   removes all topic data. Useful for cleaning up test data or recovering
#   from topic corruption.
#
# Use Cases:
#   - Cleaning up test topics after integration tests
#   - Recovering from topic corruption or misconfiguration
#   - Resetting Kafka to a clean state for development
#   - Recreating topics with different configurations
#   - Clearing all messages without restarting Kafka
#
# Real Examples:
#   # Full Kafka reset (delete all topics + recreate infrastructure topics)
#   ./scripts/docker/kafka-reset.sh
#
#   # Reset without recreating topics
#   ./scripts/docker/kafka-reset.sh --no-recreate
#
#   # Reset specific topics only
#   ./scripts/docker/kafka-reset.sh --topics dev.system.health.service-up dev.dlq.events
#
#   # Reset and wait for topics to be ready
#   ./scripts/docker/kafka-reset.sh --wait
#
#   # Reset with verbose output
#   ./scripts/docker/kafka-reset.sh --verbose
#
#   # Force reset without confirmation
#   ./scripts/docker/kafka-reset.sh --no-confirm
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - Running Kafka container (erpai-kafka)
#   - Kafka CLI tools available in the container
#
# Environment Variables:
#   COMPOSE_FILES           - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   KAFKA_CONTAINER         - Kafka container name (default: erpai-kafka)
#   KAFKA_BOOTSTRAP_SERVER  - Kafka bootstrap server (default: localhost:9092)
#   AUTO_CONFIRM            - Skip confirmation prompt (default: false)
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Kafka operation error
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
KAFKA_CONTAINER="${KAFKA_CONTAINER:-erpai-kafka}"
KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"
RECREATE_TOPICS=true
WAIT_FOR_TOPICS=false
SPECIFIC_TOPICS=()
AUTO_CONFIRM="${AUTO_CONFIRM:-false}"
VERBOSE=""

# Infrastructure topics to recreate
INFRA_TOPICS=(
    "dev.system.health.service-up"
    "dev.system.health.service-down"
    "dev.system.metrics"
    "dev.dlq.events"
    "dev.dlq.commands"
    "dev.retry.events"
    "dev.retry.commands"
    "dev.audit.events"
)

# Topic configurations
declare -A TOPIC_CONFIGS=(
    ["dev.system.health.service-up"]="partitions=6 replication=1 retention=604800000"
    ["dev.system.health.service-down"]="partitions=6 replication=1 retention=604800000"
    ["dev.system.metrics"]="partitions=6 replication=1 retention=604800000"
    ["dev.dlq.events"]="partitions=6 replication=1 retention=2592000000 cleanup=delete"
    ["dev.dlq.commands"]="partitions=6 replication=1 retention=2592000000 cleanup=delete"
    ["dev.retry.events"]="partitions=6 replication=1 retention=86400000 cleanup=delete"
    ["dev.retry.commands"]="partitions=6 replication=1 retention=86400000 cleanup=delete"
    ["dev.audit.events"]="partitions=6 replication=1 retention=604800000"
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
    echo "  ERP AI Platform - Kafka Reset"
    echo "=========================================="
    echo ""
}

print_warning() {
    echo ""
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║                            WARNING                             ║${NC}"
    echo -e "${YELLOW}╠════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║  This will DELETE all Kafka topics and messages!               ║${NC}"
    echo -e "${YELLOW}║  • All topics will be deleted                                   ║${NC}"
    echo -e "${YELLOW}║  • All messages will be lost                                     ║${NC}"
    echo -e "${YELLOW}║  • Infrastructure topics will be recreated (if --recreate)      ║${NC}"
    echo -e "${YELLOW}║  • Kafka container will NOT be restarted                        ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --topics TOPIC_LIST      Reset specific topics only (space-separated)
    --no-recreate            Don't recreate infrastructure topics
    --wait                   Wait for topics to be ready after recreation
    --no-confirm             Skip confirmation prompt
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Full reset with recreation
    $(basename "$0") --no-recreate                      # Delete all topics only
    $(basename "$0") --topics dev.dlq.events dev.retry.events
    $(basename "$0") --wait --no-confirm                # Reset and wait for topics
EOF
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Kafka container is running
    if ! docker compose $COMPOSE_FILES ps kafka --format "{{.Status}}" 2>/dev/null | grep -q "running\|healthy"; then
        log_error "Kafka container is not running"
        log_info "Start it with: ./scripts/docker/start.sh --services kafka"
        exit 2
    fi

    # Check if we can connect to Kafka
    if ! docker exec "$KAFKA_CONTAINER" kafka-broker-api-versions.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" &> /dev/null; then
        log_error "Cannot connect to Kafka at $KAFKA_BOOTSTRAP_SERVER"
        log_info "Check if Kafka is ready: docker compose $COMPOSE_FILES ps kafka"
        exit 2
    fi

    log_success "Prerequisites check passed"
}

confirm_reset() {
    if [ "$AUTO_CONFIRM" = true ]; then
        return 0
    fi

    print_warning

    read -p "Are you sure you want to reset Kafka? All topics and messages will be deleted! (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Reset cancelled by user"
        exit 4
    fi
}

list_all_topics() {
    log_info "Current Kafka topics:"
    echo ""

    docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --list 2>/dev/null || echo "  (no topics found)"

    echo ""
}

delete_all_topics() {
    log_info "Deleting all Kafka topics..."

    local topics=$(docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --list 2>/dev/null | grep -v "^$" || true)

    if [ -z "$topics" ]; then
        log_info "No topics to delete"
        return
    fi

    for topic in $topics; do
        log_info "Deleting topic: $topic"
        docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --delete --topic "$topic" 2>/dev/null || true
    done

    # Wait for deletion to complete
    log_info "Waiting for topic deletion to complete..."
    sleep 5

    log_success "All topics deleted"
}

delete_specific_topics() {
    log_info "Deleting specific topics: ${SPECIFIC_TOPICS[*]}"

    for topic in "${SPECIFIC_TOPICS[@]}"; do
        log_info "Deleting topic: $topic"
        docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --delete --topic "$topic" 2>/dev/null || true
    done

    log_success "Specified topics deleted"
}

create_topic() {
    local topic_name=$1
    local partitions=$2
    local replication=$3
    local retention=$4
    local cleanup=${5:-}

    log_info "Creating topic: $topic_name"

    local cmd="docker exec $KAFKA_CONTAINER kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVER \
        --create \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --config retention.ms=$retention"

    if [ -n "$cleanup" ]; then
        cmd="$cmd --config cleanup.policy=$cleanup"
    fi

    eval "$cmd" 2>/dev/null || true

    log_success "Topic '$topic_name' created"
}

recreate_infrastructure_topics() {
    if [ "$RECREATE_TOPICS" = false ]; then
        log_info "Skipping topic recreation (--no-recreate specified)"
        return
    fi

    log_info "Recreating infrastructure topics..."

    # Create system health topics
    create_topic "dev.system.health.service-up" 6 1 604800000
    create_topic "dev.system.health.service-down" 6 1 604800000
    create_topic "dev.system.metrics" 6 1 604800000

    # Create DLQ topics
    create_topic "dev.dlq.events" 6 1 2592000000 delete
    create_topic "dev.dlq.commands" 6 1 2592000000 delete

    # Create retry topics
    create_topic "dev.retry.events" 6 1 86400000 delete
    create_topic "dev.retry.commands" 6 1 86400000 delete

    # Create audit topics
    create_topic "dev.audit.events" 6 1 604800000

    log_success "Infrastructure topics recreated"
}

wait_for_topics() {
    if [ "$WAIT_FOR_TOPICS" = false ]; then
        return
    fi

    log_info "Waiting for topics to be ready..."

    local max_retries=30
    local retry_count=0

    while [ $retry_count -lt $max_retries ]; do
        local topic_count=$(docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --list 2>/dev/null | wc -l)

        if [ "$topic_count" -gt 0 ]; then
            log_success "Topics are ready ($topic_count topics found)"
            return
        fi

        retry_count=$((retry_count + 1))
        log_info "Waiting for topics... (attempt $retry_count/$max_retries)"
        sleep 2
    done

    log_warning "Timeout waiting for topics to be ready"
}

show_topic_summary() {
    log_info "Kafka topic summary:"
    echo ""

    local topics=$(docker exec "$KAFKA_CONTAINER" kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" --list 2>/dev/null || true)

    if [ -z "$topics" ]; then
        echo "  No topics found"
    else
        echo "$topics" | while read -r topic; do
            echo "  - $topic"
        done
    fi

    echo ""
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --topics)
            # Read all remaining arguments until next option
            shift
            while [[ $# -gt 0 && $1 != --* ]]; do
                SPECIFIC_TOPICS+=("$1")
                shift
            done
            ;;
        --no-recreate)
            RECREATE_TOPICS=false
            shift
            ;;
        --wait)
            WAIT_FOR_TOPICS=true
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

# List current topics
list_all_topics

# Delete topics
if [ ${#SPECIFIC_TOPICS[@]} -gt 0 ]; then
    delete_specific_topics
else
    delete_all_topics
fi

# Recreate infrastructure topics
recreate_infrastructure_topics

# Wait for topics if requested
wait_for_topics

# Show summary
show_topic_summary

log_success "Kafka reset completed successfully!"
log_info "Kafka is ready for use"

exit 0
