# ERP AI Platform

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-ready-blue.svg)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Enterprise-grade AI-powered ERP SaaS platform built with modern cloud-native technologies.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Overview

The ERP AI Platform is a next-generation enterprise resource planning system designed for multi-tenant SaaS deployment. It combines traditional ERP capabilities with cutting-edge AI features to deliver intelligent business automation.

### Key Features

- **Multi-Tenancy**: Secure isolation for thousands of tenants
- **AI Agents**: Intelligent automation and decision support
- **ERP Modules**: Finance, HR, Inventory, Sales, Procurement, Manufacturing
- **Event-Driven**: Real-time event streaming with Kafka
- **Cloud Native**: Kubernetes-native with auto-scaling
- **Observable**: Full-stack observability with OpenTelemetry
- **Secure**: Enterprise-grade security with Keycloak

## Architecture

```
erp-ai-platform/
├── platform/           # Core platform services
│   ├── core/          # Domain, application, infrastructure, interfaces
│   └── shared/        # Shared libraries (kernel, security, tenancy, events)
├── business/          # ERP business modules
│   ├── finance/       # Financial management
│   ├── hr/            # Human resources
│   ├── inventory/     # Inventory management
│   ├── sales/         # Sales and CRM
│   ├── procurement/   # Procurement and sourcing
│   └── manufacturing/ # Production management
├── ai/                # AI and machine learning
│   ├── agents/        # AI agent framework
│   ├── pipeline/      # ML pipelines
│   ├── models/        # Model management
│   └── orchestration/ # Workflow orchestration
├── integration/       # Integration layer
│   ├── gateway/       # API Gateway
│   ├── events/        # Event handling
│   ├── messaging/     # Message processing
│   └── external/      # External integrations
├── libraries/         # Shared libraries
│   ├── common/        # Common utilities
│   ├── data/          # Data access (JPA, Redis, Kafka, Flyway, MinIO)
│   ├── observability/ # Metrics, tracing, logging
│   └── testing/       # Test utilities
├── frontend/          # Web and mobile applications
├── docs/              # Documentation
├── scripts/           # Utility scripts
└── infrastructure/    # Infrastructure as code
    ├── docker/        # Docker Compose configuration
    ├── k8s/           # Kubernetes manifests
    └── terraform/     # Terraform modules
```

## Technology Stack

### Backend
- **Java 21** with virtual threads and structured concurrency
- **Spring Boot 3.x** for application framework
- **Spring Cloud** for distributed systems patterns
- **Spring Cloud Gateway** for API gateway
- **PostgreSQL 16** for primary data store
- **Redis 7** for caching and sessions
- **Kafka (KRaft)** for event streaming
- **Keycloak 26** for identity and access management
- **MinIO** for object storage
- **Flyway** for database migrations

### Frontend
- **Next.js 14** with App Router
- **TypeScript 5.x**
- **Tailwind CSS** for styling
- **React Native** for mobile

### Infrastructure
- **Docker & Docker Compose** for local development
- **Kubernetes** for container orchestration
- **Terraform** for infrastructure as code
- **Prometheus + Grafana** for metrics
- **Tempo** for distributed tracing
- **Loki** for log aggregation
- **OpenTelemetry** for observability

### Build & CI/CD
- **Gradle Kotlin DSL** for build automation
- **GitHub Actions** for CI/CD
- **SonarQube** for code quality

## Quick Start

### Prerequisites

- Java 21 (Temurin/OpenJDK)
- Docker & Docker Compose
- Git

### Setup

```bash
# Clone the repository
git clone https://github.com/erpai/platform.git
cd platform

# Copy environment configuration
cp infrastructure/docker/.env.example .env

# Start infrastructure services
docker compose up -d

# Build the project
./gradlew build

# Run tests
./gradlew test

# Start development environment
./scripts/setup/setup-dev-environment.sh
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Keycloak | http://localhost:8080 | admin/admin |
| MinIO | http://localhost:9001 | minioadmin/minioadmin |
| Prometheus | http://localhost:9090 | - |

## Documentation

- [Architecture](docs/architecture/README.md) - System design and architecture
- [API Documentation](docs/api/README.md) - API reference
- [Deployment Guide](docs/deployment/README.md) - Deployment instructions
- [Development Guide](docs/development/README.md) - Development practices
- [Runbooks](docs/runbooks/README.md) - Operational procedures

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Security

For security issues, please see [SECURITY.md](SECURITY.md) or email security@erpai.internal.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- Documentation: [docs.erpai.internal](https://docs.erpai.internal)
- Issues: [GitHub Issues](https://github.com/erpai/platform/issues)
- Discussions: [GitHub Discussions](https://github.com/erpai/platform/discussions)
