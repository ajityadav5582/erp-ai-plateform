# ERP AI Platform - Docker Infrastructure

Local development environment using Docker Compose.

## Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache and session store |
| Kafka | 29092 | Event streaming (KRaft mode) |
| Schema Registry | 8081 | Kafka schema management |
| Keycloak | 8080 | Identity and access management |
| MinIO | 9000/9001 | Object storage (S3 compatible) |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics visualization |
| Tempo | 3200 | Distributed tracing |
| Loki | 3100 | Log aggregation |

## Quick Start

```bash
# Copy environment file
cp infrastructure/docker/.env.example .env

# Start all services
docker compose up -d

# Verify services
docker compose ps

# View logs
docker compose logs -f [service-name]

# Stop services
docker compose down

# Stop and remove volumes (WARNING: data loss)
docker compose down -v
```

## Access URLs

- Grafana: http://localhost:3000 (admin/admin)
- Keycloak: http://localhost:8080 (admin/admin)
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

## Environment Variables

See `.env.example` for all available configuration options.
