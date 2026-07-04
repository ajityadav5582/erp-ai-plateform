# MinIO Infrastructure

MinIO is the object storage solution for the ERP AI Platform. It provides S3-compatible storage for documents, attachments, imports, exports, and backups.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `minio/minio:latest` |
| **Ports** | 9000 (API), 9001 (Console) |
| **Container** | `erpai-minio` |
| **Purpose** | Object storage (S3-compatible) |
| **Owner** | Platform Team |

## Features

- **S3-Compatible API:** Works with any S3 client library
- **High Performance:** Optimized for high-throughput workloads
- **Kubernetes-Native:** Designed for cloud-native deployments
- **Encryption:** Server-side encryption support
- **Versioning:** Object versioning for data protection
- **Bucket Policies:** Fine-grained access control

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MINIO_ROOT_USER` | `minioadmin` | Root username |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | Root password |
| `MINIO_REGION` | `us-east-1` | Default region |

### Buckets

The following buckets are created automatically by `minio-init`:

| Bucket | Access | Purpose |
|--------|--------|---------|
| `dev.tenant.documents` | Private | Document storage |
| `dev.tenant.attachments` | Private | File attachments |
| `dev.tenant.imports` | Private | Data import files |
| `dev.tenant.exports` | Private | Data export files |
| `dev.tenant.templates` | Public | Document templates |
| `dev.tenant.reports` | Private | Generated reports |
| `dev.tenant.backups` | Private | System backups |
| `dev.tenant.avatars` | Public | User avatars |

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                         MinIO                                   │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│   Services  │  │   Gateway   │  │  Exporters  │          │
│     │ (Upload/    │  │ (Presigned │  │ (Prometheus)│          │
│     │  Download)  │  │  URLs)     │  │             │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **Microservices** | Upload/download files via S3 API |
| **API Gateway** | Generates presigned URLs for secure access |
| **MinIO Client (mc)** | Used by `minio-init` to create buckets |
| **Prometheus** | Scrapes MinIO metrics for monitoring |

## Quick Start

```bash
# Start MinIO
docker compose -f compose.base.yml -f compose.infrastructure.yml up minio

# Access MinIO Console
open http://localhost:9001

# Initialize buckets
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile infrastructure up minio-init
```

## Access Methods

### 1. Direct S3 API

```bash
# Using AWS CLI
aws --endpoint-url http://localhost:9000 s3 ls s3://dev.tenant.documents

# Using MinIO Client (mc)
mc alias set local http://localhost:9000 minioadmin minioadmin
mc ls local/dev.tenant.documents
```

### 2. Presigned URLs

The gateway can generate presigned URLs for temporary access:

```java
// Spring Boot example
@Value("${minio.endpoint}")
private String minioEndpoint;

public String generatePresignedUrl(String bucket, String objectName, Duration expiry) {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .bucket(bucket)
            .object(objectName)
            .expiry(expiry)
            .build()
    );
}
```

### 3. Spring Boot Integration

```yaml
minio:
  endpoint: http://minio:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: dev.tenant.documents
```

## Monitoring

MinIO metrics are exposed at `/minio/v2/metrics/cluster` and collected by Prometheus:

| Metric | Description |
|--------|-------------|
| `minio_bucket_usage_total` | Total bucket usage in bytes |
| `minio_http_requests_total` | HTTP request count |
| `minio_http_errors_total` | HTTP error count |
| `minio_process_cpu_total` | CPU usage |
| `minio_process_memory_usage` | Memory usage |

View in Grafana: **Dashboards → Infrastructure Overview**

## Backup & Recovery

See [`backups/README.md`](backups/README.md) for detailed backup procedures.

## Troubleshooting

### MinIO Not Starting

```bash
# Check MinIO logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs minio

# Check if ports are available
docker compose -f compose.base.yml -f compose.infrastructure.yml ps minio
```

### Bucket Not Found

```bash
# List all buckets
docker compose exec minio mc ls local/

# Create bucket manually if needed
docker compose exec minio mc mb local/dev.tenant.documents
```

### Access Denied

Check bucket policies in `infrastructure/minio/config/policy/`:
- `public-read.json` — Public read access
- `private.json` — Private access (owner only)
