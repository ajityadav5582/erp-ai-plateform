# ERP AI Platform - Kubernetes Configuration

Base Kubernetes manifests for production deployment.

## Structure

```
infrastructure/k8s/
├── namespace.yaml          # Platform namespace
├── configmap.yaml          # Application configuration
├── secret.yaml             # Secrets (template - use sealed-secrets or external-secrets)
├── gateway/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
├── postgres/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── redis/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── kafka/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── keycloak/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
├── minio/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── monitoring/
│   ├── prometheus/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── pvc.yaml
│   ├── grafana/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── pvc.yaml
│   └── loki/
│       ├── statefulset.yaml
│       ├── service.yaml
│       └── pvc.yaml
└── ingress/
    ├── gateway-ingress.yaml
    └── tls-secret.yaml
```

## Deployment

```bash
# Apply namespace and base configs
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/configmap.yaml

# Apply secrets (use sealed-secrets in production)
kubectl apply -f infrastructure/k8s/secret.yaml

# Deploy infrastructure
kubectl apply -f infrastructure/k8s/postgres/
kubectl apply -f infrastructure/k8s/redis/
kubectl apply -f infrastructure/k8s/kafka/
kubectl apply -f infrastructure/k8s/minio/
kubectl apply -f infrastructure/k8s/keycloak/

# Deploy applications
kubectl apply -f infrastructure/k8s/gateway/

# Deploy monitoring
kubectl apply -f infrastructure/k8s/monitoring/

# Deploy ingress
kubectl apply -f infrastructure/k8s/ingress/
```

## Notes

- Use sealed-secrets or external-secrets for production secrets
- Configure persistent volume claims for your cloud provider
- Adjust resource requests/limits based on load testing
- Enable pod security standards
- Configure network policies for service isolation
