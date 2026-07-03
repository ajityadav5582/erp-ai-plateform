#!/bin/bash
# =============================================================================
# MinIO Health Check Script
# =============================================================================

set -e

MINIO_CONTAINER="${MINIO_CONTAINER:-erpai-minio}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MAX_RETRIES=30
RETRY_INTERVAL=10

# Function to check if MinIO is healthy
check_minio_health() {
    echo "Checking MinIO health on ${MINIO_ENDPOINT}..."
    
    # Check live endpoint
    if curl -f "${MINIO_ENDPOINT}/minio/health/live" > /dev/null 2>&1; then
        echo "MinIO live check passed"
    else
        echo "MinIO live check failed"
        return 1
    fi
    
    # Check ready endpoint
    if curl -f "${MINIO_ENDPOINT}/minio/health/ready" > /dev/null 2>&1; then
        echo "MinIO ready check passed"
    else
        echo "MinIO ready check failed"
        return 1
    fi
    
    return 0
}

# Function to check bucket access
check_bucket_access() {
    echo "Checking bucket access..."
    
    # Try to list buckets
    if mc ls local > /dev/null 2>&1; then
        echo "Bucket access works"
        return 0
    else
        echo "Bucket access failed"
        return 1
    fi
}

# Main health check loop
echo "Starting MinIO health check..."
for i in $(seq 1 ${MAX_RETRIES}); do
    if check_minio_health; then
        if check_bucket_access; then
            echo "MinIO is healthy and ready"
            exit 0
        fi
    fi
    
    echo "Waiting for MinIO to be ready... (attempt ${i}/${MAX_RETRIES})"
    sleep ${RETRY_INTERVAL}
done

echo "MinIO health check failed after ${MAX_RETRIES} attempts"
exit 1
