# ERP AI Platform - API Gateway Service

Enterprise-grade Spring Cloud Gateway providing a unified API entry point for the ERP AI Platform.

## Overview

The **API Gateway** is the single entry point for all client requests. It exposes a single port (8080) to the frontend and routes requests to downstream microservices (identity, inventory, tenant, etc.) using Spring Cloud Gateway.

## Architecture

```
┌─────────────┐
│   Frontend  │  (Next.js, port 3000)
└──────┬──────┘
       │  http://localhost:8080/api/v1/**
       ▼
┌─────────────┐
│ API Gateway │  (Spring Cloud Gateway, port 8080)
└──┬──┬──┬─────┘
   │  │  │
   ▼  ▼  ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Identity │ │Inventory │ │ Tenant   │
│ (8082)   │ │ (8083)   │ │ (8081)   │
└──────────┘ └──────────┘ └──────────┘
```

## Routing Configuration

The gateway routes requests based on path prefixes:

| Route Prefix            | Downstream Service | Target URI              |
|-------------------------|-------------------|-------------------------|
| `/api/v1/identity/**`   | Identity Service  | `identity-service:8082` |
| `/api/v1/auth/**`       | Identity Service  | `identity-service:8082` |
| `/api/v1/admin/**`      | Identity Service  | `identity-service:8082` |
| `/api/v1/inventory/**`  | Inventory Service | `inventory-service:8083`|
| `/api/v1/tenants/**`    | Tenant Service    | `tenant-service:8081`   |
| `/actuator/health`      | Identity Service  | passthrough             |

## Features

- **Single Port Exposure**: Frontend connects only to `http://localhost:8080`
- **Path-based Routing**: Routes to downstream services based on URL path
- **Header Propagation**: Forwards tenant, auth, correlation, and trace headers
- **Retry Support**: Configurable retries for transient failures
- **Request Timeout**: Configurable timeout per route
- **Health Check Passthrough**: Exposes `/actuator/health` for monitoring

## Building

```bash
# Build the gateway module
./gradlew :platform:gateway:bootJar

# Run locally
./gradlew :platform:gateway:bootRun
```

## Running with Docker Compose

```bash
docker compose up
```

The gateway service will be built and started on port 8080.

## Configuration

All configuration is in `src/main/resources/application.yml` and `application-local.yml`.

Key environment variables:

| Variable                  | Default                  | Description                          |
|---------------------------|--------------------------|--------------------------------------|
| `SERVER_PORT`             | `8080`                   | Gateway listen port                  |
| `IDENTITY_SERVICE_URI`    | `http://identity-service:8082` | Identity service target       |
| `INVENTORY_SERVICE_URI`   | `http://inventory-service:8083` | Inventory service target     |
| `TENANT_SERVICE_URI`      | `http://tenant-service:8081` | Tenant service target          |
| `SPRING_PROFILES_ACTIVE`  | `local`                  | Active Spring profile                |

## Local Development

### Prerequisites

1. PostgreSQL running on `localhost:5434`
2. Identity service running on `localhost:8082`
3. Inventory service running on `localhost:8083`
4. Tenant service running on `localhost:8081`

### Start Services

```bash
# Start all services with Docker Compose
docker compose up

# Or run services individually
./gradlew :platform:identity:bootRun &
./gradlew :business:inventory:application:bootRun &
./gradlew :platform:tenant:bootRun &
./gradlew :platform:gateway:bootRun
```

### Verify

```bash
# Check gateway health
curl http://localhost:8080/actuator/health

# Test identity route
curl http://localhost:8080/api/v1/identity/health

# Test inventory route
curl http://localhost:8080/api/v1/inventory/health
```

## Frontend Integration

The frontend is configured to use the gateway as its single API endpoint:

```typescript
// frontend/src/config/env.ts
API_BASE_URL: "http://localhost:8080/api/v1"
```

All API calls from the frontend go through the gateway, which routes to the appropriate downstream service.

## Adding New Routes

To add a new downstream service route, update `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: ${NEW_SERVICE_URI:http://new-service:8084}
          predicates:
            - Path=/api/v1/new/**
          filters:
            - StripPrefix=1
```

## Security Notes

- The gateway does **not** perform authentication itself; it forwards the `Authorization` header to downstream services.
- JWT validation happens in each downstream service.
- Tenant context is propagated via the `X-Tenant-Id` header.
