#!/bin/bash
# ERP AI Platform - Development Environment Setup
# Sets up the complete local development environment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "ERP AI Platform - Development Setup"
echo "=========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

command -v java >/dev/null 2>&1 || { echo "ERROR: Java 21 is required but not installed."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "ERROR: Docker is required but not installed."; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "ERROR: Docker Compose is required but not installed."; exit 1; }

echo "All prerequisites satisfied."
echo ""

# Setup environment file
echo "Setting up environment configuration..."
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    cp "$PROJECT_ROOT/infrastructure/docker/.env.example" "$PROJECT_ROOT/.env"
    echo "Created .env file from template."
else
    echo ".env file already exists, skipping."
fi
echo ""

# Start infrastructure
echo "Starting infrastructure services..."
cd "$PROJECT_ROOT"
docker compose up -d postgres redis kafka schema-registry keycloak minio prometheus grafana tempo loki

echo "Waiting for services to be ready..."
sleep 30

# Check service health
echo "Checking service health..."
docker compose ps

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Services available at:"
echo "  - Grafana:    http://localhost:3000 (admin/admin)"
echo "  - Keycloak:   http://localhost:8080 (admin/admin)"
echo "  - MinIO:      http://localhost:9001 (minioadmin/minioadmin)"
echo "  - Prometheus: http://localhost:9090"
echo ""
echo "Next steps:"
echo "  1. Build the project: ./gradlew build"
echo "  2. Run tests: ./gradlew test"
echo "  3. Start a service: ./gradlew :platform:core:interfaces:bootRun"
echo ""
