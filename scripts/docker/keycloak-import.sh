#!/bin/bash
# =============================================================================
# ERP AI Platform - Keycloak Import Script
# =============================================================================
# Description:
#   Imports a Keycloak realm configuration from a JSON file.
#   This script can be used to import the default ERP AI Platform realm
#   or a custom realm configuration. It supports both initial import
#   and updating existing realms.
#
# Use Cases:
#   - Setting up Keycloak for the first time
#   - Updating realm configuration after changes
#   - Importing realm from version control
#   - Deploying realm configuration in CI/CD
#   - Restoring realm from backup
#   - Syncing realm across environments
#
# Real Examples:
#   # Import default realm
#   ./scripts/docker/keycloak-import.sh
#
#   # Import custom realm file
#   ./scripts/docker/keycloak-import.sh --realm-file /path/to/custom-realm.json
#
#   # Import and overwrite existing realm
#   ./scripts/docker/keycloak-import.sh --force
#
#   # Import specific realm by name
#   ./scripts/docker/keycloak-import.sh --realm-name my-custom-realm
#
#   # Import and skip if realm exists
#   ./scripts/docker/keycloak-import.sh --skip-if-exists
#
#   # Import with verbose output
#   ./scripts/docker/keycloak-import.sh --verbose
#
# Prerequisites:
#   - Docker Engine 24.0+
#   - Docker Compose V2
#   - Running Keycloak container (erpai-keycloak)
#   - Realm JSON file (default: infrastructure/docker/keycloak/realm-export.json)
#
# Environment Variables:
#   COMPOSE_FILES           - Override compose files (default: compose.base.yml compose.infrastructure.yml)
#   KEYCLOAK_CONTAINER      - Keycloak container name (default: erpai-keycloak)
#   KEYCLOAK_ADMIN          - Admin username (default: admin)
#   KEYCLOAK_ADMIN_PASSWORD - Admin password (default: admin)
#   REALM_FILE              - Path to realm JSON file
#   REALM_NAME              - Name of realm to import (default: from JSON file)
#   AUTO_CONFIRM            - Skip confirmation prompt (default: false)
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Keycloak operation error
#   4 - User cancelled
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
COMPOSE_FILES="-f compose.base.yml -f compose.infrastructure.yml"
KEYCLOAK_CONTAINER="${KEYCLOAK_CONTAINER:-erpai-keycloak}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_FILE="${REALM_FILE:-infrastructure/docker/keycloak/realm-export.json}"
REALM_NAME=""
FORCE_IMPORT=false
SKIP_IF_EXISTS=false
AUTO_CONFIRM="${AUTO_CONFIRM:-false}"
VERBOSE=""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_banner() {
    echo ""
    echo "=========================================="
    echo "  ERP AI Platform - Keycloak Import"
    echo "=========================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Options:
    --realm-file PATH        Path to realm JSON file (default: infrastructure/docker/keycloak/realm-export.json)
    --realm-name NAME        Name of realm to import (default: from JSON file)
    --force                  Overwrite existing realm
    --skip-if-exists         Skip import if realm already exists
    --no-confirm             Skip confirmation prompt
    --verbose, -v            Enable verbose output
    --help, -h               Show this help message

Examples:
    $(basename "$0")                                    # Import default realm
    $(basename "$0") --realm-file /path/to/realm.json   # Import custom realm
    $(basename "$0") --force                            # Overwrite existing realm
    $(basename "$0") --skip-if-exists                   # Skip if realm exists
    $(basename "$0") --realm-name my-realm --force      # Import with specific name
EOF
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Keycloak container is running
    if ! docker compose $COMPOSE_FILES ps keycloak --format "{{.Status}}" 2>/dev/null | grep -q "running\|healthy"; then
        log_error "Keycloak container is not running"
        log_info "Start it with: ./scripts/docker/start.sh --services keycloak"
        exit 2
    fi

    # Check if realm file exists
    if [ ! -f "$REALM_FILE" ]; then
        log_error "Realm file not found: $REALM_FILE"
        log_info "Specify a valid realm file with --realm-file"
        exit 2
    fi

    # Check if we can connect to Keycloak
    if ! curl -s -f "http://localhost:8080/health/ready" &> /dev/null; then
        log_error "Cannot connect to Keycloak at http://localhost:8080"
        log_info "Check if Keycloak is ready: docker compose $COMPOSE_FILES ps keycloak"
        exit 2
    fi

    # Extract realm name from JSON if not specified
    if [ -z "$REALM_NAME" ]; then
        REALM_NAME=$(grep -o '"realm": *"[^"]*"' "$REALM_FILE" | head -1 | sed 's/"realm": *"//;s/"//')
        if [ -z "$REALM_NAME" ]; then
            log_error "Could not extract realm name from $REALM_FILE"
            log_info "Specify realm name with --realm-name"
            exit 2
        fi
    fi

    log_success "Prerequisites check passed"
    log_info "Realm name: $REALM_NAME"
}

confirm_import() {
    if [ "$AUTO_CONFIRM" = true ]; then
        return 0
    fi

    echo ""
    log_info "Importing realm: $REALM_NAME"
    log_info "From file: $REALM_FILE"
    echo ""

    if [ "$FORCE_IMPORT" = true ]; then
        log_warning "This will OVERWRITE the existing realm '$REALM_NAME'"
    elif [ "$SKIP_IF_EXISTS" = true ]; then
        log_info "Will skip import if realm already exists"
    else
        log_warning "If realm '$REALM_NAME' exists, this may fail or merge configurations"
    fi

    echo ""
    read -p "Do you want to proceed? (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Import cancelled by user"
        exit 4
    fi
}

check_realm_exists() {
    log_info "Checking if realm '$REALM_NAME' exists..."

    local response=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$KEYCLOAK_ADMIN:$KEYCLOAK_ADMIN_PASSWORD" \
        "http://localhost:8080/admin/realms/$REALM_NAME" 2>/dev/null || echo "000")

    if [ "$response" = "200" ]; then
        log_warning "Realm '$REALM_NAME' already exists"
        return 0
    else
        log_info "Realm '$REALM_NAME' does not exist"
        return 1
    fi
}

get_admin_token() {
    log_info "Obtaining admin access token..."

    local token_response=$(curl -s -X POST \
        "http://localhost:8080/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=admin-cli" \
        -d "username=$KEYCLOAK_ADMIN" \
        -d "password=$KEYCLOAK_ADMIN_PASSWORD" 2>/dev/null || echo '{"error":"unavailable"}')

    local access_token=$(echo "$token_response" | grep -o '"access_token":"[^"]*"' | sed 's/"access_token":"//;s/"//' || true)

    if [ -z "$access_token" ]; then
        log_error "Failed to obtain admin access token"
        log_error "Check Keycloak admin credentials"
        exit 3
    fi

    echo "$access_token"
}

import_realm_via_api() {
    log_info "Importing realm via Keycloak Admin API..."

    local access_token=$(get_admin_token)

    # Check if realm exists
    if check_realm_exists; then
        if [ "$SKIP_IF_EXISTS" = true ]; then
            log_info "Skipping import (realm exists and --skip-if-exists specified)"
            return 0
        fi

        if [ "$FORCE_IMPORT" = false ]; then
            log_error "Realm '$REALM_NAME' already exists. Use --force to overwrite."
            exit 3
        fi

        log_warning "Overwriting existing realm '$REALM_NAME'..."
    fi

    # Import realm
    local import_response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        "http://localhost:8080/admin/realms" \
        -H "Authorization: Bearer $access_token" \
        -H "Content-Type: application/json" \
        -d @"$REALM_FILE" 2>/dev/null || echo "000")

    if [ "$import_response" = "201" ]; then
        log_success "Realm '$REALM_NAME' imported successfully"
    else
        log_error "Failed to import realm (HTTP $import_response)"
        log_info "Check Keycloak logs for details"
        exit 3
    fi
}

import_realm_via_cli() {
    log_info "Importing realm via Keycloak CLI..."

    # Copy realm file to Keycloak import directory
    local import_dir="/tmp/keycloak-import"
    docker exec "$KEYCLOAK_CONTAINER" mkdir -p "$import_dir" 2>/dev/null || true

    # Copy realm file to container
    docker cp "$REALM_FILE" "$KEYCLOAK_CONTAINER:$import_dir/realm-export.json" 2>/dev/null || {
        log_error "Failed to copy realm file to Keycloak container"
        exit 3
    }

    # Import using Keycloak CLI
    docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kc.sh import \
        --dir "$import_dir" \
        --override true 2>/dev/null || {
        log_error "Failed to import realm via CLI"
        exit 3
    }

    log_success "Realm '$REALM_NAME' imported successfully"
}

show_realm_info() {
    log_info "Realm information:"
    echo ""

    local access_token=$(get_admin_token)

    # Get realm info
    local realm_info=$(curl -s \
        -H "Authorization: Bearer $access_token" \
        "http://localhost:8080/admin/realms/$REALM_NAME" 2>/dev/null || echo "{}")

    local realm_enabled=$(echo "$realm_info" | grep -o '"enabled":[a-z]*' | sed 's/"enabled"://' || echo "unknown")
    local realm_display_name=$(echo "$realm_info" | grep -o '"displayName":"[^"]*"' | sed 's/"displayName"://;s/"//g' || echo "unknown")

    echo "  Realm: $REALM_NAME"
    echo "  Display Name: $realm_display_name"
    echo "  Enabled: $realm_enabled"
    echo ""

    # Get client count
    local client_count=$(curl -s \
        -H "Authorization: Bearer $access_token" \
        "http://localhost:8080/admin/realms/$REALM_NAME/clients" 2>/dev/null | grep -o '"id"' | wc -l || echo "0")

    echo "  Clients: $client_count"
    echo ""

    # Get user count
    local user_count=$(curl -s \
        -H "Authorization: Bearer $access_token" \
        "http://localhost:8080/admin/realms/$REALM_NAME/users?max=1" 2>/dev/null | grep -o '"id"' | wc -l || echo "0")

    echo "  Users: $user_count (approximate)"
    echo ""
}

# =============================================================================
# Argument Parsing
# =============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --realm-file)
            REALM_FILE="$2"
            shift 2
            ;;
        --realm-name)
            REALM_NAME="$2"
            shift 2
            ;;
        --force)
            FORCE_IMPORT=true
            shift
            ;;
        --skip-if-exists)
            SKIP_IF_EXISTS=true
            shift
            ;;
        --no-confirm)
            AUTO_CONFIRM=true
            shift
            ;;
        --verbose|-v)
            VERBOSE="--verbose"
            shift
            ;;
        --help|-h)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# =============================================================================
# Main Execution
# =============================================================================

cd "$PROJECT_ROOT"

print_banner

# Check prerequisites
check_prerequisites

# Confirm import
confirm_import

# Import realm
import_realm_via_api

# Show realm info
show_realm_info

log_success "Keycloak import completed successfully!"
log_info "Access Keycloak at: http://localhost:8080"
log_info "Admin credentials: $KEYCLOAK_ADMIN / $KEYCLOAK_ADMIN_PASSWORD"

exit 0
