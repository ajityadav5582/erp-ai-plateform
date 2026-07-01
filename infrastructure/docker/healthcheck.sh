#!/bin/sh
# =============================================================================
# ERP AI Platform - Docker Health Check Script
# Checks application readiness via Spring Boot Actuator
# =============================================================================

set -e

# Configuration
HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health/readiness}"
TIMEOUT="${HEALTH_TIMEOUT:-10}"
RETRIES="${HEALTH_RETRIES:-3}"
RETRY_DELAY="${HEALTH_RETRY_DELAY:-2}"

# Check if wget is available
if command -v wget >/dev/null 2>&1; then
    HTTP_CLIENT="wget --no-verbose --tries=1 --spider --timeout=${TIMEOUT}"
elif command -v curl >/dev/null 2>&1; then
    HTTP_CLIENT="curl --fail --silent --show-error --max-time ${TIMEOUT}"
else
    echo "ERROR: Neither wget nor curl found for health check"
    exit 1
fi

# Perform health check with retries
attempt=1
while [ $attempt -le $RETRIES ]; do
    echo "Health check attempt ${attempt}/${RETRIES}: ${HEALTH_URL}"

    if ${HTTP_CLIENT} "${HEALTH_URL}" 2>/dev/null; then
        echo "Health check passed"
        exit 0
    fi

    if [ $attempt -lt $RETRIES ]; then
        echo "Health check failed, retrying in ${RETRY_DELAY}s..."
        sleep ${RETRY_DELAY}
    fi

    attempt=$((attempt + 1))
done

echo "Health check failed after ${RETRIES} attempts"
exit 1
