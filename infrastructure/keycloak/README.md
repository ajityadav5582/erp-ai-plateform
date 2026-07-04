# Keycloak Infrastructure

Keycloak is the identity and access management solution for the ERP AI Platform. It handles authentication, authorization, and user management for all services.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `quay.io/keycloak/keycloak:26.2` |
| **Port** | 8080 |
| **Container** | `erpai-keycloak` |
| **Purpose** | Identity and access management |
| **Owner** | Platform Team |

## Features

- **Single Sign-On (SSO):** Users authenticate once and access all services
- **OAuth 2.0 / OpenID Connect:** Standard protocols for authentication
- **User Federation:** LDAP, Active Directory integration
- **Role-Based Access Control (RBAC):** Fine-grained authorization
- **Multi-tenancy:** Isolated realms per tenant
- **Session Management:** Centralized session handling

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KEYCLOAK_ADMIN` | `admin` | Admin console username |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Admin console password |
| `KC_DB` | `dev-mem` | Database type (dev-mem for development) |
| `KC_HTTP_RELATIVE_PATH` | `/` | HTTP relative path |
| `KC_HOSTNAME` | `keycloak.localhost` | Hostname for Keycloak |

### Development Mode

In development, Keycloak uses an in-memory H2 database (`dev-mem`). For production, configure a PostgreSQL database:

```yaml
KC_DB: postgres
KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
KC_DB_USERNAME: keycloak
KC_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
```

## Realms

A **realm** is a space where users, roles, and groups are managed. The platform uses:

| Realm | Purpose |
|-------|---------|
| `erpai` | Main realm for all platform users |

### Clients

| Client ID | Type | Purpose |
|-----------|------|---------|
| `erpai-gateway` | confidential | API Gateway (backend services) |
| `erpai-web` | public | Web application |
| `erpai-mobile` | public | Mobile application |
| `erpai-services` | confidential | Service-to-service communication |

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                        Keycloak                                 │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│   Users     │  │   Clients   │  │   Roles     │          │
│     │  (SSO)      │  │ (OAuth2)    │  │  (RBAC)     │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│   Gateway   │  │  Services   │  │   Web/Mobile│          │
│     │  (Auth)     │  │  (Auth)     │  │   (Auth)    │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **API Gateway** | Validates JWT tokens issued by Keycloak |
| **Microservices** | Use Keycloak for authentication and authorization |
| **PostgreSQL** | Stores user and realm data (in production) |
| **Prometheus** | Scrapes Keycloak metrics for monitoring |

## Quick Start

```bash
# Start Keycloak
docker compose -f compose.base.yml -f compose.infrastructure.yml up keycloak

# Access Admin Console
open http://localhost:8080/

# Default credentials
# Username: admin
# Password: admin
```

## Authentication Flow

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  User   │────▶│ Gateway │────▶│Keycloak │────▶│ Service │
│         │     │         │     │         │     │         │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
     │               │               │               │
     │  1. Request   │               │               │
     │──────────────▶│               │               │
     │               │ 2. Redirect to│               │
     │               │   Keycloak    │               │
     │               │──────────────▶│               │
     │               │               │ 3. Authenticate│
     │               │               │◀──────────────│
     │               │               │ 4. Token      │
     │               │◀──────────────│──────────────▶│
     │ 5. Response   │               │               │
     │◀──────────────│               │               │
```

## Service Integration

### Spring Boot Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/erpai
          jwk-set-uri: http://keycloak:8080/realms/erpai/protocol/openid-connect/certs
```

### Gateway Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: finance-service
          uri: lb://finance-service
          predicates:
            - Path=/api/finance/**
          filters:
            - TokenRelay=
```

## Monitoring

Keycloak metrics are exposed at `/realms/erpai/metrics` and collected by Prometheus:

| Metric | Description |
|--------|-------------|
| `keycloak_users_total` | Total number of users |
| `keycloak_sessions_total` | Active sessions |
| `keycloak_login_events_total` | Login events |
| `keycloak_token_requests_total` | Token requests |

View in Grafana: **Dashboards → Infrastructure Overview**

## Troubleshooting

### Keycloak Not Starting

```bash
# Check Keycloak logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs keycloak

# Check if port 8080 is available
docker compose -f compose.base.yml -f compose.infrastructure.yml ps keycloak
```

### Admin Console Access Denied

```bash
# Reset admin password
docker compose exec keycloak /opt/keycloak/bin/kc.sh create-admin-user \
  --user admin \
  --password new_password
```

### Token Validation Failing

```bash
# Check Keycloak is reachable from gateway
docker compose exec gateway wget -qO- http://keycloak:8080/realms/erpai/.well-known/openid-configuration

# Check JWT token expiration
# Tokens are valid for 5 minutes by default
```
