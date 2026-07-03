#!/bin/bash
# =============================================================================
# Kafka Health Check Script
# =============================================================================

set -e

KAFKA_CONTAINER="${KAFKA_CONTAINER:-erpai-kafka}"
KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"
MAX_RETRIES=30
RETRY_INTERVAL=10

# Function to check if Kafka is healthy
check_kafka_health() {
    echo "Checking Kafka health on ${KAFKA_BOOTSTRAP_SERVER}..."

    # Check if we can connect to Kafka and get broker info
    if kafka-broker-api-versions.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" > /dev/null 2>&1; then
        echo "Kafka broker is responding"

        # Check if we can list topics (indicates controller is ready)
        if kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" --list > /dev/null 2>&1; then
            echo "Kafka controller is ready"
            return 0
        else
            echo "Kafka controller not ready yet"
            return 1
        fi
    else
        echo "Kafka broker not responding"
        return 1
    fi
}

# Function to check topic creation capability
check_topic_creation() {
    echo "Checking topic creation capability..."

    # Try to create a test topic
    if kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
        --create \
        --topic "__health-check-topic" \
        --partitions 1 \
        --replication-factor 1 \
        --if-not-exists > /dev/null 2>&1; then
        echo "Topic creation works"

        # Clean up test topic
        kafka-topics.sh --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
            --delete \
            --topic "__health-check-topic" > /dev/null 2>&1 || true

        return 0
    else
        echo "Topic creation failed"
        return 1
    fi
}

# Main health check loop
echo "Starting Kafka health check..."
for i in $(seq 1 ${MAX_RETRIES}); do
    if check_kafka_health; then
        if check_topic_creation; then
            echo "Kafka is healthy and ready"
            exit 0
        fi
    fi

    echo "Waiting for Kafka to be ready... (attempt ${i}/${MAX_RETRIES})"
    sleep ${RETRY_INTERVAL}
done

echo "Kafka health check failed after ${MAX_RETRIES} attempts"
exit 1
