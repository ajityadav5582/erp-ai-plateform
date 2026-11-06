#!/usr/bin/env bash
# Full local development setup: clean build + infra + migrations + all backend services.
# Frontend is started separately: cd frontend && npm run dev
# Usage: ./scripts/dev-full.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== 1. Clean build all modules ==="
./gradlew clean build -x test

echo "=== 2. Start infrastructure + migrations + backend services ==="
exec ./scripts/start-services.sh
