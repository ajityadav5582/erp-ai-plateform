#!/usr/bin/env bash
# Local development helper: start Postgres, reset DB, build, test.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5434}"
DB_NAME="${POSTGRES_DB:-erpai_platform}"
DB_USER="${POSTGRES_USER:-erpai}"
DB_PASS="${POSTGRES_PASSWORD:-erpai_dev_password}"

echo "=== Starting PostgreSQL ==="
if ! docker ps --format '{{.Names}}' | grep -q '^erpai-postgres$'; then
    if docker ps -a --format '{{.Names}}' | grep -q '^erpai-postgres$'; then
        docker rm -f erpai-postgres >/dev/null 2>&1 || true
    fi
    docker compose -f compose.base.yml -f compose.infrastructure.yml --profile infrastructure up -d postgres
fi

echo "Waiting for PostgreSQL to be ready..."
for i in $(seq 1 30); do
    if docker exec erpai-postgres pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1 || \
       docker exec erpai-postgres pg_isready -U "$DB_USER" >/dev/null 2>&1; then
        echo "PostgreSQL is ready."
        break
    fi
    sleep 2
done

echo "=== Resetting database ==="
docker exec erpai-postgres psql -U "$DB_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB_NAME' AND pid <> pg_backend_pid();" 2>/dev/null || true
docker exec erpai-postgres psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec erpai-postgres psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"

echo "=== Running Flyway migrations ==="
docker run --rm --network host \
    -v "$ROOT/infrastructure/postgres/flyway:/flyway/sql:ro" \
    flyway/flyway:10.12-alpine \
    -url="jdbc:postgresql://localhost:$DB_PORT/$DB_NAME" \
    -user="$DB_USER" \
    -password="$DB_PASS" \
    -locations="filesystem:/flyway/sql/platform" \
    migrate

echo "=== Building all modules ==="
./gradlew clean build -x test

echo "=== Running tests ==="
./gradlew test

echo "=== Done. Start services with: ./scripts/start-services.sh ==="
