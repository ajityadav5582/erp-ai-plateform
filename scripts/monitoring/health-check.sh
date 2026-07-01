#!/bin/bash
# ERP AI Platform - Health Check Script
# Checks the health of all platform services

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "ERP AI Platform - Health Check"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Service health checks
check_service() {
    local name=$1
    local url=$2
    local expected=$3
    
    echo -n "Checking $name... "
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "$expected"; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}FAILED${NC}"
        return 1
    fi
}

# Check services
FAILED=0

check_service "PostgreSQL" "http://localhost:5432" "200" || FAILED=$((FAILED + 1))
check_service "Redis" "http://localhost:6379" "PONG" || FAILED=$((FAILED + 1))
check_service "Kafka" "http://localhost:29092" "200" || FAILED=$((FAILED + 1))
check_service "Keycloak" "http://localhost:8080/health/ready" "200" || FAILED=$((FAILED + 1))
check_service "MinIO" "http://localhost:9000/minio/health/live" "200" || FAILED=$((FAILED + 1))
check_service "Prometheus" "http://localhost:9090/-/healthy" "200" || FAILED=$((FAILED + 1))
check_service "Grafana" "http://localhost:3000/api/health" "ok" || FAILED=$((FAILED + 1))

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All services are healthy!${NC}"
    exit 0
else
    echo -e "${RED}$FAILED service(s) failed health check!${NC}"
    exit 1
fi
