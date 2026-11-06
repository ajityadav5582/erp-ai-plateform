#!/usr/bin/env bash
# Fresh setup: reset DB, run Flyway migrations, build & test all modules.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== 1. Reset database + run Flyway migrations ==="
./scripts/docker/database-reset.sh --no-confirm

echo "=== 2. Build all modules ==="
./gradlew clean build -x test

echo "=== 3. Run tests ==="
./gradlew test

echo "=== 4. Start infrastructure (optional) ==="
echo "Run: ./scripts/docker/docker.sh start --profile development"

echo "=== Setup complete ==="
