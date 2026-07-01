#!/bin/sh
# =============================================================================
# ERP AI Platform - Docker Entrypoint Script
# Handles signal forwarding for graceful shutdown
# =============================================================================

set -e

# Function to handle SIGTERM (graceful shutdown)
_term() {
    echo "Received SIGTERM signal, shutting down gracefully..."
    kill -TERM "$child" 2>/dev/null
}

# Function to handle SIGINT (Ctrl+C)
_int() {
    echo "Received SIGINT signal, shutting down gracefully..."
    kill -INT "$child" 2>/dev/null
}

# Trap signals
trap _term SIGTERM
trap _int SIGINT

# Execute the Java application with all JVM options
# Using exec ensures the Java process replaces the shell, receiving signals directly
exec java \
    ${JAVA_OPTS} \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/tmp/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Dmanagement.endpoints.web.exposure.include=${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE} \
    -Dmanagement.endpoint.health.probes.enabled=${MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED} \
    -Dmanagement.health.livenessState.enabled=${MANAGEMENT_HEALTH_LIVENESS_STATE_ENABLED} \
    -Dmanagement.health.readinessState.enabled=${MANAGEMENT_HEALTH_READINESS_STATE_ENABLED} \
    -jar app.jar
