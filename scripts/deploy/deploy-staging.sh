#!/bin/bash
# ERP AI Platform - Staging Deployment Script
# Deploys the platform to the staging environment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "ERP AI Platform - Staging Deployment"
echo "=========================================="
echo ""

# Configuration
NAMESPACE="erpai-platform-staging"
IMAGE_TAG="${1:-latest}"
HELM_CHART="$PROJECT_ROOT/infrastructure/helm/erpai-platform"

# Pre-deployment checks
echo "Running pre-deployment checks..."

# Check kubectl connectivity
kubectl cluster-info >/dev/null 2>&1 || { echo "ERROR: Cannot connect to Kubernetes cluster"; exit 1; }

# Check namespace exists
kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || {
    echo "Creating namespace $NAMESPACE..."
    kubectl create namespace "$NAMESPACE"
}

echo "Pre-deployment checks passed."
echo ""

# Deploy using Helm
echo "Deploying to staging..."
helm upgrade --install erpai-platform "$HELM_CHART" \
    --namespace "$NAMESPACE" \
    --values "$HELM_CHART/values-staging.yaml" \
    --set image.tag="$IMAGE_TAG" \
    --wait \
    --timeout 10m

echo ""
echo "=========================================="
echo "Staging Deployment Complete!"
echo "=========================================="
echo ""
echo "Access URLs:"
echo "  - API Gateway: https://staging-api.erpai.internal"
echo "  - Grafana: https://staging-grafana.erpai.internal"
echo ""
