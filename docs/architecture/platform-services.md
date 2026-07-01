# Platform Services Architecture

## 1. Purpose

This document defines the platform services that form the foundation of the ERP AI Platform. These services provide cross-cutting capabilities that all business modules depend on.

## 2. Platform Service Principles

- **Single Responsibility:** Each service has one clear purpose
- **Loose Coupling:** Services communicate via events and APIs
- **High Cohesion:** Related functionality grouped together
- **Database per Service:** Each service owns its data
- **API First:** All capabilities exposed via well-defined APIs
- **Cloud Native:** Designed for Kubernetes deployment

## 3. Platform Services

### 3.1 API Gateway Service

**Purpose:** Single entry point for all client requests. Handles routing, authentication, rate limiting, and request/response transformation.

**Responsibilities:**
- Route requests to appropriate backend services
- Authenticate requests via OAuth 2.0 / OIDC
- Authorize requests based on roles and permissions
- Rate limiting per tenant and per user
- Request/response logging and metrics
- Circuit breaking for downstream services
- API versioning and deprecation headers
- CORS handling
- Request/response transformation

**Why it exists:**
- Centralized security enforcement
- Simplified client integration (single endpoint)
- Consistent cross-cutting concerns
- Observability and monitoring entry point

**Dependencies:**
- Keycloak (authentication)
- Tenant Service (tenant resolution)
- All business services (routing targets)

**APIs:**
- `/api/v1/gateway/routes` - Route management
- `/api/v1/gateway/health` - Health check

---

### 3.2 Config Service

**Purpose:** Centralized configuration management for all services. Provides dynamic configuration updates without service restarts.

**Responsibilities:**
- Store and distribute application configurations
- Environment-specific configuration (dev, staging, prod)
- Feature flags and toggles
- Tenant-specific configuration overrides
- Configuration versioning and audit trail
- Hot reload of configuration changes
- Configuration validation

**Why it exists:**
- Eliminate configuration duplication across services
- Enable dynamic feature toggles
- Support multi-tenant configuration variations
- Centralized audit of configuration changes

**Dependencies:**
- PostgreSQL (configuration storage)
- Redis (caching)
- All services (configuration consumers)

**APIs:**
- `/api/v1/config/{serviceName}` - Get service configuration
- `/api/v1/config/tenant/{tenantId}` - Get tenant configuration
- `/api/v1/config/feature/{featureName}` - Get feature flag status

---

### 3.3 Discovery Service

**Purpose:** Service registry for dynamic service discovery. Enables services to find and communicate with each other without hardcoded endpoints.

**Responsibilities:**
- Service registration and deregistration
- Health checking of registered services
- Service instance metadata (version, region, capacity)
- Load balancing information
- Service discovery queries
- Service metadata management

**Why it exists:**
- Enable dynamic scaling of services
- Support blue-green and canary deployments
- Eliminate hardcoded service URLs
- Enable service mesh integration

**Dependencies:**
- PostgreSQL (service registry storage)
- All services (registration and discovery)

**APIs:**
- `/api/v1/discovery/services` - List all services
- `/api/v1/discovery/services/{serviceName}` - Get service instances
- `/api/v1/discovery/register` - Register service instance
- `/api/v1/discovery/heartbeat` - Service heartbeat

---

### 3.4 Tenant Service

**Purpose:** Tenant lifecycle management and tenant context propagation. The foundation of multi-tenancy.

**Responsibilities:**
- Tenant provisioning and deprovisioning
- Tenant metadata management (name, domain, plan, status)
- Tenant configuration management
- Tenant context propagation (via headers and MDC)
- Tenant health monitoring
- Tenant resource allocation
- Tenant migration between isolation strategies

**Why it exists:**
- Centralized tenant management
- Consistent tenant context across services
- Tenant lifecycle automation
- Multi-tenancy enforcement foundation

**Dependencies:**
- PostgreSQL (tenant metadata)
- All services (tenant context consumers)

**APIs:**
- `/api/v1/tenants` - Tenant CRUD
- `/api/v1/tenants/{tenantId}/provision` - Provision tenant
- `/api/v1/tenants/{tenantId}/deprovision` - Deprovision tenant
- `/api/v1/tenants/{tenantId}/context` - Get tenant context

---

### 3.5 User Service

**Purpose:** User identity management and authentication. Manages user accounts, credentials, and identity providers.

**Responsibilities:**
- User registration and management
- User profile management
- Password management (hashing, reset, expiration)
- Identity provider integration (Keycloak)
- User status management (active, suspended, locked)
- User session management
- User audit logging

**Why it exists:**
- Centralized user identity
- Consistent authentication across services
- User lifecycle management
- Integration with enterprise identity providers

**Dependencies:**
- Keycloak (identity provider)
- PostgreSQL (user metadata)
- Authorization Service (role assignment)

**APIs:**
- `/api/v1/users` - User CRUD
- `/api/v1/users/{userId}/profile` - User profile
- `/api/v1/users/{userId}/password` - Password management
- `/api/v1/users/{userId}/status` - User status

---

### 3.6 Authorization Service

**Purpose:** Fine-grained access control and permission management. Enforces RBAC and ABAC policies.

**Responsibilities:**
- Role management (CRUD)
- Permission management (CRUD)
- Role-permission assignment
- User-role assignment
- Policy evaluation (RBAC, ABAC)
- Permission caching
- Access control decision logging
- Integration with Spring Security

**Why it exists:**
- Centralized authorization logic
- Consistent access control across services
- Fine-grained permission management
- Audit trail for access decisions

**Dependencies:**
- PostgreSQL (roles, permissions, assignments)
- User Service (user context)
- Tenant Service (tenant context)
- All services (authorization enforcement)

**APIs:**
- `/api/v1/authz/roles` - Role management
- `/api/v1/authz/permissions` - Permission management
- `/api/v1/authz/check` - Permission check
- `/api/v1/authz/user/{userId}/roles` - User roles

---

### 3.7 Workflow Service

**Purpose:** Business process orchestration and workflow management. Enables configurable business workflows.

**Responsibilities:**
- Workflow definition and management
- Workflow instance execution
- State machine management
- Task assignment and tracking
- Workflow history and audit
- SLA monitoring and escalation
- Workflow templates
- Integration with business events

**Why it exists:**
- Encode business processes as configurable workflows
- Reduce hardcoded business logic
- Enable process automation
- Provide visibility into process execution

**Dependencies:**
- PostgreSQL (workflow definitions and instances)
- Kafka (event-driven triggers)
- All business services (workflow participants)

**APIs:**
- `/api/v1/workflows` - Workflow CRUD
- `/api/v1/workflows/{workflowId}/start` - Start workflow
- `/api/v1/workflows/instances/{instanceId}` - Get instance
- `/api/v1/workflows/instances/{instanceId}/tasks` - Get tasks

---

### 3.8 Notification Service

**Purpose:** Multi-channel notification delivery. Handles email, SMS, push notifications, and in-app notifications.

**Responsibilities:**
- Notification template management
- Multi-channel delivery (email, SMS, push, in-app)
- Notification scheduling and batching
- Delivery tracking and retry
- Notification preferences management
- Template rendering (Thymeleaf, FreeMarker)
- Integration with third-party providers (SendGrid, Twilio)
- Notification history and audit

**Why it exists:**
- Centralized notification management
- Consistent notification experience
- Template management and localization
- Delivery reliability and tracking

**Dependencies:**
- PostgreSQL (notifications, templates, preferences)
- Kafka (event consumption)
- External providers (SendGrid, Twilio, Firebase)

**APIs:**
- `/api/v1/notifications` - Send notification
- `/api/v1/notifications/templates` - Template management
- `/api/v1/notifications/preferences` - User preferences
- `/api/v1/notifications/history` - Notification history

---

### 3.9 Audit Service

**Purpose:** Comprehensive audit logging and compliance tracking. Records all significant actions for compliance and security.

**Responsibilities:**
- Audit event collection
- Audit log storage and indexing
- Audit trail querying
- Compliance report generation
- Data retention management
- Audit log immutability enforcement
- Integration with SIEM systems
- Anomaly detection on audit logs

**Why it exists:**
- Regulatory compliance (SOX, GDPR, etc.)
- Security incident investigation
- User activity tracking
- Data access auditing

**Dependencies:**
- PostgreSQL (audit logs)
- Elasticsearch (log indexing and search)
- Kafka (event consumption)
- All services (audit event producers)

**APIs:**
- `/api/v1/audit/logs` - Query audit logs
- `/api/v1/audit/reports` - Generate compliance reports
- `/api/v1/audit/export` - Export audit data

---

### 3.10 File Service

**Purpose:** Centralized file storage, management, and delivery. Handles documents, images, and binary files.

**Responsibilities:**
- File upload and download
- File metadata management
- Storage backend abstraction (MinIO, S3)
- File versioning
- Access control and sharing
- Virus scanning integration
- Thumbnail generation
- CDN integration
- Storage quota management

**Why it exists:**
- Centralized file management
- Consistent access control
- Storage backend flexibility
- File lifecycle management

**Dependencies:**
- MinIO (object storage)
- PostgreSQL (file metadata)
- Authorization Service (access control)
- All services (file operations)

**APIs:**
- `/api/v1/files/upload` - Upload file
- `/api/v1/files/{fileId}/download` - Download file
- `/api/v1/files/{fileId}/metadata` - File metadata
- `/api/v1/files/{fileId}/share` - Share file

---

### 3.11 Report Service

**Purpose:** Report generation, scheduling, and delivery. Provides standardized and custom reporting capabilities.

**Responsibilities:**
- Report template management
- Report generation (PDF, Excel, CSV)
- Scheduled report execution
- Report delivery (email, download)
- Report caching
- Data aggregation and summarization
- Report parameter management
- Report history and versioning

**Why it exists:**
- Centralized reporting capabilities
- Consistent report formats
- Scheduled report delivery
- Report performance optimization

**Dependencies:**
- PostgreSQL (report metadata, cached results)
- Redis (report caching)
- File Service (report storage)
- All business services (data sources)

**APIs:**
- `/api/v1/reports` - Report CRUD
- `/api/v1/reports/{reportId}/generate` - Generate report
- `/api/v1/reports/{reportId}/schedule` - Schedule report
- `/api/v1/reports/{reportId}/download` - Download report

---

### 3.12 Localization Service

**Purpose:** Multi-language and multi-region support. Manages translations, locales, and regional settings.

**Responsibilities:**
- Translation management (CRUD)
- Locale detection and negotiation
- Regional format management (date, time, number, currency)
- Translation key management
- Translation import/export
- Fallback locale handling
- Translation caching
- Integration with translation management systems

**Why it exists:**
- Enable global market expansion
- Consistent localization across services
- Centralized translation management
- Support for regional formats

**Dependencies:**
- PostgreSQL (translations, locales)
- Redis (translation caching)
- All services (localization consumers)

**APIs:**
- `/api/v1/localization/translations` - Translation CRUD
- `/api/v1/localization/locales` - Locale management
- `/api/v1/localization/translate` - Translate text
- `/api/v1/localization/formats` - Regional formats

---

### 3.13 Country Configuration Service

**Purpose:** Country-specific business rules and configurations. Provides reusable country-specific capabilities.

**Responsibilities:**
- Country metadata management
- Country-specific business rules
- Tax configuration per country
- Regulatory requirement management
- Currency and exchange rate management
- Holiday calendar management
- Country-specific validation rules
- Country feature enablement

**Why it exists:**
- Enable rapid expansion to new countries
- Isolate country-specific logic
- Reusable country configurations
- Compliance with local regulations

**Dependencies:**
- PostgreSQL (country configurations)
- All business services (country-specific rules)

**APIs:**
- `/api/v1/countries` - Country list
- `/api/v1/countries/{countryCode}/config` - Country configuration
- `/api/v1/countries/{countryCode}/tax` - Tax configuration
- `/api/v1/countries/{countryCode}/holidays` - Holiday calendar

---

### 3.14 License Service

**Purpose:** License management and entitlement enforcement. Manages module licenses, user seats, and feature access.

**Responsibilities:**
- License key generation and validation
- Module license management
- User seat allocation and tracking
- Feature entitlement enforcement
- License expiration management
- Usage tracking and reporting
- License upgrade/downgrade
- Trial license management

**Why it exists:**
- Monetization enforcement
- Feature access control
- Usage tracking for billing
- License compliance

**Dependencies:**
- PostgreSQL (licenses, entitlements)
- Tenant Service (tenant context)
- Feature Toggle Service (feature access)
- All services (license enforcement)

**APIs:**
- `/api/v1/licenses` - License CRUD
- `/api/v1/licenses/{licenseId}/validate` - Validate license
- `/api/v1/licenses/{licenseId}/usage` - Usage tracking
- `/api/v1/licenses/entitlements` - Check entitlements

---

### 3.15 Feature Toggle Service

**Purpose:** Feature flag management and evaluation. Enables gradual feature rollouts and A/B testing.

**Responsibilities:**
- Feature flag CRUD
- Feature flag evaluation (boolean, percentage, user-based)
- Feature flag targeting (tenant, user, region)
- Feature flag history and audit
- Feature flag dependencies
- Gradual rollout management
- Kill switch functionality
- Feature flag analytics

**Why it exists:**
- Enable gradual feature rollouts
- Reduce deployment risk
- Support A/B testing
- Enable feature gating by plan

**Dependencies:**
- PostgreSQL (feature flags, evaluations)
- Redis (flag caching)
- All services (feature flag evaluation)

**APIs:**
- `/api/v1/features` - Feature flag CRUD
- `/api/v1/features/{featureKey}/evaluate` - Evaluate flag
- `/api/v1/features/{featureKey}/rollout` - Manage rollout
- `/api/v1/features/audit` - Feature flag audit

---

## 4. Platform Service Interactions

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   Tenant  │ │    User   │ │    Auth   │
        │  Service  │ │  Service  │ │  Service  │
        └───────────┘ └───────────┘ └───────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   Config  │ │Discovery  │ │  Feature  │
        │  Service  │ │  Service  │ │  Toggle   │
        └───────────┘ └───────────┘ └───────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │Workflow   │ │Notification│ │   Audit   │
        │ Service   │ │  Service  │ │  Service  │
        └───────────┘ └───────────┘ └───────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │   File    │ │  Report   │ │Localization│
        │  Service  │ │  Service  │ │  Service  │
        └───────────┘ └───────────┘ └───────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │  Country  │ │  License  │ │  Business │
        │ Config    │ │  Service  │ │  Services │
        │  Service  │ │           │ │           │
        └───────────┘ └───────────┘ └───────────┘
```

## 5. Service Communication Patterns

### 5.1 Synchronous Communication

- **REST:** For request/response patterns
- **gRPC:** For high-performance internal communication
- **GraphQL:** For complex data fetching (future)

### 5.2 Asynchronous Communication

- **Kafka:** For event-driven communication
- **Event patterns:** Event notification, event-carried state transfer, event sourcing

### 5.3 Service Mesh (Future)

- **Istio/Linkerd:** For service-to-service communication
- **mTLS:** For service authentication
- **Traffic management:** Canary, blue-green deployments
- **Observability:** Distributed tracing, metrics

## 6. Data Management

### 6.1 Database Strategy

- **Database per Service:** Each service owns its database
- **Schema per Tenant:** For multi-tenant data isolation
- **Shared Database (future):** For read models and reporting

### 6.2 Data Flow

```
Business Service → Kafka → Downstream Services
Business Service → API Gateway → Client
Business Service → File Service → MinIO
Business Service → Report Service → Cached Reports
```

## 7. Observability

### 7.1 Metrics

- **Micrometer:** Metrics collection
- **Prometheus:** Metrics storage
- **Grafana:** Metrics visualization

### 7.2 Tracing

- **OpenTelemetry:** Distributed tracing
- **Jaeger/Tempo:** Trace storage and visualization

### 7.3 Logging

- **Structured logging:** JSON format
- **Loki:** Log aggregation
- **Grafana:** Log visualization

## 8. Deployment Architecture

### 8.1 Kubernetes Deployment

- **Namespace per environment:** dev, staging, prod
- **Namespace per service group:** platform, business, ai
- **Resource quotas:** Per namespace
- **Network policies:** Service-to-service communication rules

### 8.2 Scaling

- **Horizontal Pod Autoscaler:** Based on CPU/memory/custom metrics
- **Cluster Autoscaler:** Node pool scaling
- **Service scaling:** Independent scaling per service
