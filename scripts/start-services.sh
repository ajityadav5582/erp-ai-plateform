#!/usr/bin/env bash
# Start local infrastructure + all backend services + frontend.
# Usage: ./scripts/start-services.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# ---------------------------------------------------------------------------
# Environment defaults
# ---------------------------------------------------------------------------
export POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
export POSTGRES_PORT="${POSTGRES_PORT:-5434}"
export POSTGRES_DB="${POSTGRES_DB:-erpai_platform}"
export POSTGRES_USER="${POSTGRES_USER:-erpai}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-erpai_dev_password}"
export DB_USERNAME="${DB_USERNAME:-$POSTGRES_USER}"
export DB_PASSWORD="${DB_PASSWORD:-$POSTGRES_PASSWORD}"
export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:29092}"
export JWT_SECRET="${JWT_SECRET:-jwt-secret-key-change-me-in-production-32b}"
export JWT_ISSUER="${JWT_ISSUER:-erp-ai-platform-identity}"

export IDENTITY_SERVICE_URI="${IDENTITY_SERVICE_URI:-http://localhost:8081}"
export INVENTORY_SERVICE_URI="${INVENTORY_SERVICE_URI:-http://localhost:8083}"
export PROCUREMENT_SERVICE_URI="${PROCUREMENT_SERVICE_URI:-http://localhost:8084}"
export SALES_SERVICE_URI="${SALES_SERVICE_URI:-http://localhost:8085}"
export POS_SERVICE_URI="${POS_SERVICE_URI:-http://localhost:8086}"

mkdir -p "$ROOT/logs"
PIDS=()

cleanup() {
    echo ""
    echo "Stopping services..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    echo "Stopped."
}
trap cleanup INT TERM EXIT

# ---------------------------------------------------------------------------
# Infrastructure
# ---------------------------------------------------------------------------
echo "=== Starting infrastructure ==="

# Helper: start an infra container, reusing a running one or recreating a
# stopped/conflicting one instead of failing on a name conflict.
start_infra() {
    local name="$1"
    local service="$2"
    local optional="${3:-false}"

    if docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
        echo "$name is already running."
        return 0
    fi

    # Remove any stopped/exited container occupying the name so compose can
    # recreate it cleanly instead of erroring with "name already in use".
    if docker ps -a --format '{{.Names}}' | grep -q "^${name}$"; then
        echo "Removing stopped container $name..."
        docker rm -f "$name" >/dev/null 2>&1 || true
    fi

    if [ "$optional" = "true" ]; then
        docker compose -f compose.base.yml -f compose.infrastructure.yml --profile infrastructure up -d "$service" || true
    else
        docker compose -f compose.base.yml -f compose.infrastructure.yml --profile infrastructure up -d "$service"
    fi
}

start_infra erpai-postgres postgres false
start_infra erpai-redis redis true
start_infra erpai-kafka kafka true

echo "Waiting for PostgreSQL to be ready..."
for i in $(seq 1 30); do
    if docker exec erpai-postgres pg_isready -U "$POSTGRES_USER" >/dev/null 2>&1; then
        echo "PostgreSQL is ready."
        break
    fi
    sleep 2
done

echo "=== Running Flyway migrations ==="
docker run --rm --network host \
    -v "$ROOT/infrastructure/postgres/flyway:/flyway/sql:ro" \
    flyway/flyway:10.12-alpine \
    -url="jdbc:postgresql://localhost:$POSTGRES_PORT/$POSTGRES_DB" \
    -user="$POSTGRES_USER" \
    -password="$POSTGRES_PASSWORD" \
    -locations="filesystem:/flyway/sql/platform" \
    migrate

# ---------------------------------------------------------------------------
# Backend services
# ---------------------------------------------------------------------------
start_backend() {
    local name="$1"
    local module="$2"
    local port="$3"
    echo "Starting $name on port $port (logs: logs/$name.log)..."
    (
        cd "$ROOT"
        SERVER_PORT="$port" \
        POSTGRES_HOST="$POSTGRES_HOST" \
        POSTGRES_PORT="$POSTGRES_PORT" \
        POSTGRES_DB="$POSTGRES_DB" \
        POSTGRES_USER="$POSTGRES_USER" \
        POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
        DB_USERNAME="$DB_USERNAME" \
        DB_PASSWORD="$DB_PASSWORD" \
        KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
        SPRING_JPA_HIBERNATE_DDL_AUTO=update \
        SPRING_JPA_GENERATE_DDL=true \
        JWT_SECRET="$JWT_SECRET" \
        JWT_ISSUER="$JWT_ISSUER" \
        IDENTITY_SERVICE_URI="$IDENTITY_SERVICE_URI" \
        INVENTORY_SERVICE_URI="$INVENTORY_SERVICE_URI" \
        PROCUREMENT_SERVICE_URI="$PROCUREMENT_SERVICE_URI" \
        SALES_SERVICE_URI="$SALES_SERVICE_URI" \
        POS_SERVICE_URI="$POS_SERVICE_URI" \
        INVENTORY_SERVICE_URI=http://localhost:8083 \
        ./gradlew "$module:bootRun" > "$ROOT/logs/$name.log" 2>&1
    ) &
    PIDS+=("$!")
}

start_backend "identity"     "platform:identity"                8081
start_backend "inventory"    "business:inventory:application"  8083
start_backend "procurement"  "business:procurement:application" 8084
start_backend "sales"        "business:sales:application"      8085
start_backend "pos"          "business:pos:application"        8086
start_backend "reports"      "business:reports:application"    8090
start_backend "gateway"      "platform:gateway"                8080

echo "Waiting 45s for backend services to start..."
sleep 45

# ---------------------------------------------------------------------------
# Frontend is started separately: cd frontend && npm run dev
# ---------------------------------------------------------------------------

echo ""
echo "=========================================="
echo "Services starting. URLs:"
echo "  Frontend:     http://localhost:3000"
echo "  Gateway:      http://localhost:8080"
echo "  Identity:     http://localhost:8081"
echo "  Inventory:    http://localhost:8083"
echo "  Procurement:  http://localhost:8084"
echo "  Sales:        http://localhost:8085"
echo "  POS:          http://localhost:8086"
echo "  Reports:      http://localhost:8090"
echo "Logs: $ROOT/logs/*.log"
echo "Press Ctrl+C to stop all services."
echo "=========================================="

wait
