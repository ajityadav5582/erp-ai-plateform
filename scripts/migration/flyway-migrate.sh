#!/bin/bash
# ERP AI Platform - Database Migration Script
# Runs Flyway migrations for all services

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "ERP AI Platform - Database Migration"
echo "=========================================="
echo ""

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-erpai}"
DB_PASSWORD="${DB_PASSWORD:-erpai_dev_password}"

# Find all migration directories
MIGRATION_DIRS=$(find "$PROJECT_ROOT" -path "*/infrastructure/flyway/migrations" -type d)

if [ -z "$MIGRATION_DIRS" ]; then
    echo "No migration directories found."
    exit 0
fi

# Run migrations for each service
for MIGRATION_DIR in $MIGRATION_DIRS; do
    SERVICE_NAME=$(basename "$(dirname "$(dirname "$MIGRATION_DIR")")")
    echo "Running migrations for $SERVICE_NAME..."
    
    # Extract database name from service configuration
    DB_NAME="${SERVICE_NAME}_db"
    
    flyway \
        -url="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
        -user="$DB_USER" \
        -password="$DB_PASSWORD" \
        -locations="filesystem:$MIGRATION_DIR" \
        migrate
    
    echo "Migrations completed for $SERVICE_NAME."
    echo ""
done

echo "=========================================="
echo "All migrations completed successfully!"
echo "=========================================="
