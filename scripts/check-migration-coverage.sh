#!/usr/bin/env bash
# CI check: every JPA @Entity table name must have a CREATE TABLE in the
# central Flyway directory. Exits non-zero on any missing table.
set -euo pipefail

FLYWAY_DIR="infrastructure/postgres/flyway/platform"
ENTITY_TABLES=$(grep -rhoE '@Table\([^)]*name\s*=\s*"[^"]+"' --include='*.java' . \
  | sed -E 's/.*name\s*=\s*"([^"]+)".*/\1/' | sort -u)

MISSING=0
for t in $ENTITY_TABLES; do
  if ! grep -rqi "CREATE TABLE IF NOT EXISTS $t" "$FLYWAY_DIR"; then
    echo "MISSING MIGRATION: $t"
    MISSING=1
  fi
done

if [ "$MISSING" -ne 0 ]; then
  echo "Migration coverage check FAILED"
  exit 1
fi
echo "Migration coverage check PASSED"
