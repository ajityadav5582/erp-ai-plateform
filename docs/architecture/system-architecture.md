# System Architecture

## Overview

The ERP AI Platform is designed as a cloud-native, multi-tenant SaaS platform built on microservices architecture principles.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Applications                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │   Web App   │  │ Mobile App  │  │   Desktop   │  │   API     │  │
│  │ (React/TS)  │  │ (React Native)│ │   Client    │  │ Clients   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Spring Cloud Gateway)            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │   Routing   │  │   Rate      │  │   Auth      │  │   Circuit  │  │
│  │             │  │  Limiting   │  │  (OAuth2)   │  │   Breaker  │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Service Mesh                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │   Service   │  │   Service   │  │   Service   │  │  Service  │  │
│  │  Discovery  │  │   Config    │  │   mTLS      │  │  Traffic  │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│   Platform    │           │   Business    │           │      AI       │
│    Core       │           │   Modules     │           │   Services    │
├───────────────┤           ├───────────────┤           ├───────────────┤
│ • Tenancy     │           │ • Finance     │           │ • Agents      │
│ • Security    │           │ • HR          │           │ • Pipeline    │
│ • Events      │           │ • Inventory   │           │ • Models      │
│ • Kernel      │           │ • Sales       │           │ • Orchestr.   │
└───────────────┘           │ • Procurement │           └───────────────┘
                            │ • Manufacturing│
                            └───────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Data Layer                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │ PostgreSQL  │  │    Redis    │  │    Kafka    │  │   MinIO   │  │
│  │ (Primary)   │  │   (Cache)   │  │  (Events)   │  │ (Storage) │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Observability Stack                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │ Prometheus  │  │   Grafana   │  │    Tempo    │  │   Loki    │  │
│  │  (Metrics)  │  │ (Dashboard) │  │  (Traces)   │  │  (Logs)   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Key Design Decisions

### 1. Microservices Architecture
- Each business capability is a separate service
- Services own their data (Database per Service)
- Services communicate via events and APIs

### 2. Multi-Tenancy
- Shared database, shared schema with tenant discriminator
- Tenant context propagated via JWT claims
- Row-level security enforced at data layer

### 3. Event-Driven
- Kafka as the central event backbone
- Event sourcing for audit trails
- CQRS for read/write separation

### 4. API Gateway
- Single entry point for all clients
- Handles authentication, rate limiting, routing
- Protocol translation (HTTP/gRPC/WebSocket)

### 5. Observability
- OpenTelemetry for distributed tracing
- Micrometer for metrics
- Structured logging with correlation IDs

## Scalability

- Horizontal scaling at service level
- Database sharding by tenant
- Kafka partitioning for event throughput
- Redis clustering for cache scale
- Kubernetes for orchestration
