#!/usr/bin/env bash
# Drops and recreates the configured local PostgreSQL database, then applies platform migrations.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DB_NAME="${POSTGRES_DB:-erpai_platform}"
DB_USER="${POSTGRES_USER:-erpai}"
DB_PASS="${POSTGRES_PASSWORD:-erpai_dev_password}"
DB_PORT="${POSTGRES_PORT:-5434}"
CONTAINER="${POSTGRES_CONTAINER:-erpai-postgres}"

if ! command -v docker >/dev/null 2>&1; then
    echo "Error: Docker is required to reset the local database." >&2
    exit 1
fi
if ! docker ps --format '{{.Names}}' | grep -Fxq "$CONTAINER"; then
    echo "Error: PostgreSQL container '$CONTAINER' is not running." >&2
    exit 1
fi

echo "This will permanently delete all data in database '$DB_NAME' and recreate its tables."
read -r -p "Type '$DB_NAME' to continue: " CONFIRM
if [[ "$CONFIRM" != "$DB_NAME" ]]; then
    echo "Database reset cancelled."
    exit 1
fi

echo "Terminating connections to '$DB_NAME'..."
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d postgres \
    -v db_name="$DB_NAME" <<'SQL'
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = :'db_name' AND pid <> pg_backend_pid();
SQL

echo "Dropping and recreating '$DB_NAME'..."
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d postgres \
    -v db_name="$DB_NAME" <<'SQL'
SELECT format('DROP DATABASE IF EXISTS %I', :'db_name') \gexec
SELECT format('CREATE DATABASE %I', :'db_name') \gexec
SQL

echo "Applying identity Flyway migrations..."
docker run --rm --network host \
    -v "$ROOT/infrastructure/postgres/flyway/identity:/flyway/sql/identity:ro" \
    flyway/flyway:11.7.2-alpine \
    -url="jdbc:postgresql://localhost:$DB_PORT/$DB_NAME" \
    -user="$DB_USER" \
    -password="$DB_PASS" \
    -table=identity_flyway_schema_history \
    -validateMigrationNaming=true \
    -locations="filesystem:/flyway/sql/identity" \
    migrate

echo "Applying platform Flyway migrations..."
docker run --rm --network host \
    -v "$ROOT/infrastructure/postgres/flyway:/flyway/sql:ro" \
    flyway/flyway:11.7.2-alpine \
    -url="jdbc:postgresql://localhost:$DB_PORT/$DB_NAME" \
    -user="$DB_USER" \
    -password="$DB_PASS" \
    -baselineOnMigrate=true \
    -baselineVersion=0 \
    -validateMigrationNaming=true \
    -locations="filesystem:/flyway/sql/platform" \
    migrate

echo "Applying inventory Flyway migrations..."
docker run --rm --network host \
    -v "$ROOT/infrastructure/postgres/flyway:/flyway/sql:ro" \
    flyway/flyway:11.7.2-alpine \
    -url="jdbc:postgresql://localhost:$DB_PORT/$DB_NAME" \
    -user="$DB_USER" \
    -password="$DB_PASS" \
    -baselineOnMigrate=true \
    -baselineVersion=0 \
    -table=inventory_flyway_schema_history \
    -validateMigrationNaming=true \
    -locations="filesystem:/flyway/sql/inventory" \
    migrate

echo "Database '$DB_NAME' has been reset and identity, platform, and inventory migrations were applied."
