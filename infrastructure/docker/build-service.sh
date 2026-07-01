#!/bin/bash
# =============================================================================
# ERP AI Platform - Docker Build Script
# Builds Docker images for microservices with optimization
# =============================================================================

set -euo pipefail

# -------------------- Configuration --------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Default values
SERVICE_NAME="${1:-}"
SERVICE_MODULE="${2:-}"
IMAGE_TAG="${3:-latest}"
BUILD_ARGS="${BUILD_ARGS:-}"
PUSH="${PUSH:-false}"
CACHE_FROM="${CACHE_FROM:-}"
CACHE_TO="${CACHE_TO:-}"

# -------------------- Colors --------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# -------------------- Functions --------------------
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

usage() {
    cat <<EOF
Usage: $0 <service-name> <service-module> [image-tag] [options]

Arguments:
  service-name    Name of the service (e.g., finance-service)
  service-module  Gradle module path (e.g., business:finance:interfaces)
  image-tag       Docker image tag (default: latest)

Options:
  BUILD_ARGS      Additional build arguments (e.g., "JAVA_VERSION=21")
  PUSH            Push image to registry (default: false)
  CACHE_FROM      Cache from image (e.g., registry.example.com/service:buildcache)
  CACHE_TO        Cache to image (e.g., registry.example.com/service:buildcache)

Examples:
  # Build finance service
  $0 finance-service business:finance:interfaces 1.0.0

  # Build and push with cache
  PUSH=true CACHE_FROM=registry.example.com/finance:buildcache \\
    CACHE_TO=registry.example.com/finance:buildcache \\
    $0 finance-service business:finance:interfaces 1.0.0

  # Build with custom Java version
  BUILD_ARGS="JAVA_VERSION=21" $0 finance-service business:finance:interfaces 1.0.0
EOF
    exit 1
}

# -------------------- Validation --------------------
if [ -z "${SERVICE_NAME}" ] || [ -z "${SERVICE_MODULE}" ]; then
    log_error "Service name and module are required"
    usage
fi

# -------------------- Build Info --------------------
log_info "Building Docker image for ${SERVICE_NAME}"
log_info "Service module: ${SERVICE_MODULE}"
log_info "Image tag: ${IMAGE_TAG}"
log_info "Project root: ${PROJECT_ROOT}"

# -------------------- Build Arguments --------------------
BUILD_ARG_ARGS=()
for arg in ${BUILD_ARGS}; do
    BUILD_ARG_ARGS+=("--build-arg" "${arg}")
done

# -------------------- Docker Build --------------------
cd "${PROJECT_ROOT}"

DOCKER_ARGS=(
    build
    --file "${SCRIPT_DIR}/Dockerfile.service"
    --tag "${SERVICE_NAME}:${IMAGE_TAG}"
    --label "org.opencontainers.image.created=$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    --label "org.opencontainers.image.revision=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
)

# Add build args
if [ ${#BUILD_ARG_ARGS[@]} -gt 0 ]; then
    DOCKER_ARGS+=("${BUILD_ARG_ARGS[@]}")
fi

# Add cache options
if [ -n "${CACHE_FROM}" ]; then
    DOCKER_ARGS+=("--cache-from" "${CACHE_FROM}")
fi

if [ -n "${CACHE_TO}" ]; then
    DOCKER_ARGS+=("--cache-to" "type=registry,ref=${CACHE_TO},mode=max")
fi

# Set build context
DOCKER_ARGS+=(".")

# Set build args for service
DOCKER_ARGS+=(
    "--build-arg" "SERVICE_NAME=${SERVICE_NAME}"
    "--build-arg" "SERVICE_MODULE=${SERVICE_MODULE}"
    "--build-arg" "APP_VERSION=${IMAGE_TAG}"
    "--build-arg" "BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    "--build-arg" "VCS_REF=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
)

log_info "Running Docker build..."
docker "${DOCKER_ARGS[@]}"

# -------------------- Push (optional) --------------------
if [ "${PUSH}" = "true" ]; then
    REGISTRY="${REGISTRY:-}"
    if [ -n "${REGISTRY}" ]; then
        IMAGE_PATH="${REGISTRY}/${SERVICE_NAME}:${IMAGE_TAG}"
    else
        IMAGE_PATH="${SERVICE_NAME}:${IMAGE_TAG}"
    fi

    log_info "Tagging image as ${IMAGE_PATH}"
    docker tag "${SERVICE_NAME}:${IMAGE_TAG}" "${IMAGE_PATH}"

    log_info "Pushing image to registry..."
    docker push "${IMAGE_PATH}"

    log_info "Image pushed successfully: ${IMAGE_PATH}"
fi

# -------------------- Summary --------------------
log_info "Build complete!"
log_info "Image: ${SERVICE_NAME}:${IMAGE_TAG}"
log_info "Size: $(docker images ${SERVICE_NAME}:${IMAGE_TAG} --format '{{.Size}}')"

# -------------------- Security Scan (optional) --------------------
if [ "${SCAN:-false}" = "true" ]; then
    log_info "Running security scan with Trivy..."
    trivy image --severity HIGH,CRITICAL "${SERVICE_NAME}:${IMAGE_TAG}" || \
        log_warn "Security scan found vulnerabilities (non-blocking)"
fi
