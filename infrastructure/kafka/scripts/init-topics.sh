#!/bin/bash
# =============================================================================
# Kafka Topic Initialization Script
# Creates infrastructure topics only (no business topics)
# =============================================================================

set -e

KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
KAFKA_OPTS="${KAFKA_OPTS:-}"

# Function to create a topic
create_topic() {
    local topic_name=$1
    local partitions=$2
    local replication=$3

    if kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" --list | grep -q "^${topic_name}$"; then
        echo "Topic ${topic_name} already exists"
    else
        echo "Creating topic: ${topic_name}"
        kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
            --create \
            --topic "${topic_name}" \
            --partitions "${partitions}" \
            --replication-factor "${replication}" \
            --config "retention.ms=604800000" \
            --config "segment.bytes=1073741824"
    fi
}

# Function to configure DLQ topics with longer retention
configure_dlq() {
    local topic_name=$1

    echo "Configuring DLQ topic: ${topic_name}"
    kafka-configs.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
        --alter \
        --entity-type topics \
        --entity-name "${topic_name}" \
        --add-config "retention.ms=2592000000" \
        --add-config "cleanup.policy=delete" \
        --add-config "delete.retention.ms=86400000"
}

# Function to configure retry topics with short retention
configure_retry() {
    local topic_name=$1

    echo "Configuring retry topic: ${topic_name}"
    kafka-configs.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
        --alter \
        --entity-type topics \
        --entity-name "${topic_name}" \
        --add-config "retention.ms=86400000" \
        --add-config "cleanup.policy=delete"
}

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
for i in {1..30}; do
    if kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" --list > /dev/null 2>&1; then
        echo "Kafka is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "Kafka not ready after 30 attempts"
        exit 1
    fi
    sleep 10
done

# =============================================================================
# Infrastructure Topics
# =============================================================================

echo "Creating infrastructure topics..."

# System health monitoring topics
create_topic "dev.system.health.service-up" 6 1
create_topic "dev.system.health.service-down" 6 1
create_topic "dev.system.metrics" 6 1

# Dead Letter Queue topics
create_topic "dev.dlq.events" 6 1
create_topic "dev.dlq.commands" 6 1

# Retry topics
create_topic "dev.retry.events" 6 1
create_topic "dev.retry.commands" 6 1

# Audit log topics
create_topic "dev.audit.events" 6 1

echo "Infrastructure topics created successfully"

# =============================================================================
# Topic Configuration
# =============================================================================

echo "Configuring topic settings..."

# Configure DLQ topics with longer retention
configure_dlq "dev.dlq.events"
configure_dlq "dev.dlq.commands"

echo "DLQ topics configured with extended retention"

# Configure retry topics with short retention
configure_retry "dev.retry.events"
configure_retry "dev.retry.commands"

echo "Retry topics configured with short retention"

# =============================================================================
# Summary
# =============================================================================

echo ""
echo "=========================================="
echo "Infrastructure Topics Summary"
echo "=========================================="
echo ""
echo "System Topics:"
echo "  - dev.system.health.service-up"
echo "  - dev.system.health.service-down"
echo "  - dev.system.metrics"
echo ""
echo "DLQ Topics:"
echo "  - dev.dlq.events"
echo "  - dev.dlq.commands"
echo ""
echo "Retry Topics:"
echo "  - dev.retry.events"
echo "  - dev.retry.commands"
echo ""
echo "Audit Topics:"
echo "  - dev.audit.events"
echo ""
echo "All infrastructure topics are ready"
echo "=========================================="
