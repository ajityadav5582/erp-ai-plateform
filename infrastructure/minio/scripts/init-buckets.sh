#!/bin/bash
# =============================================================================
# MinIO Bucket Initialization Script
# Creates infrastructure buckets with appropriate policies
# =============================================================================

set -e

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ACCESS_KEY="${MINIO_ROOT_USER:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_ROOT_PASSWORD:-minioadmin}"
MINIO_ALIAS="local"

# Function to create a bucket
create_bucket() {
    local bucket_name=$1
    local policy=$2

    echo "Creating bucket: ${bucket_name}"

    if mc ls "${MINIO_ALIAS}/${bucket_name}" > /dev/null 2>&1; then
        echo "Bucket ${bucket_name} already exists"
    else
        mc mb "${MINIO_ALIAS}/${bucket_name}"
    fi

    # Apply policy
    if [ -n "$policy" ]; then
        echo "Applying policy: ${policy}"
        mc policy set "$policy" "${MINIO_ALIAS}/${bucket_name}"
    fi
}

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
for i in {1..30}; do
    if curl -f "${MINIO_ENDPOINT}/minio/health/ready" > /dev/null 2>&1; then
        echo "MinIO is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "MinIO not ready after 30 attempts"
        exit 1
    fi
    sleep 10
done

# Configure MinIO client
mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}"

# =============================================================================
# Infrastructure Buckets
# =============================================================================

echo "Creating infrastructure buckets..."

# Document storage (private)
create_bucket "dev.tenant.documents" "private"

# File attachments (private)
create_bucket "dev.tenant.attachments" "private"

# Data import files (private)
create_bucket "dev.tenant.imports" "private"

# Data export files (private)
create_bucket "dev.tenant.exports" "private"

# Document templates (public)
create_bucket "dev.tenant.templates" "public"

# Generated reports (private)
create_bucket "dev.tenant.reports" "private"

# System backups (private)
create_bucket "dev.tenant.backups" "private"

# User avatars (public)
create_bucket "dev.tenant.avatars" "public"

echo "Infrastructure buckets created successfully"

# =============================================================================
# Summary
# =============================================================================

echo ""
echo "=========================================="
echo "MinIO Buckets Summary"
echo "=========================================="
echo ""
echo "Private Buckets:"
echo "  - dev.tenant.documents"
echo "  - dev.tenant.attachments"
echo "  - dev.tenant.imports"
echo "  - dev.tenant.exports"
echo "  - dev.tenant.reports"
echo "  - dev.tenant.backups"
echo ""
echo "Public Buckets:"
echo "  - dev.tenant.templates"
echo "  - dev.tenant.avatars"
echo ""
echo "All infrastructure buckets are ready"
echo "=========================================="
