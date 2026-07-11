# Tenant Service

## Overview

The Tenant Service is the core service for multi-tenant management in the ERP AI Platform. It provides tenant lifecycle management, data isolation strategies, and serves as the foundation for all other services in the platform.

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                         Interfaces Layer                         │
│  REST Controllers, GraphQL Resolvers, Message Handlers            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Application Layer                           │
│  Commands, Queries, Handlers, DTOs                              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Domain Layer                              │
│  Entities, Value Objects, Domain Events, Domain Exceptions        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Infrastructure Layer                         │
│  Repositories, Event Publishers, External Services                │
└─────────────────────────────────────────────────────────────────┘
```

## Domain Model

### Tenant Aggregate Root

The [`Tenant`](`platform/tenant/src/main/java/com/erp/platform/tenant/domain/Tenant.java`) entity is the aggregate root for tenant management.

#### Identity

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Database primary key (inherited from OptimisticLock) |
| `tenantId` | UUID | Unique business identifier for API access and routing |
| `tenantCode` | String | Human-readable unique code for display purposes |
| `legalName` | String | Organization's legal name for official documents |
| `displayName` | String | Display name for UI and user-facing contexts |

#### Business Information

| Field | Type | Description |
|-------|------|-------------|
| `email` | String | Primary email for notifications and support |
| `phone` | String | Primary phone for emergency contact |
| `website` | String | Organization's website URL |

#### Lifecycle

| Field | Type | Description |
|-------|------|-------------|
| `status` | TenantStatus | Current lifecycle state |
| `isolationStrategy` | IsolationStrategy | Data isolation approach |
| `activatedAt` | Instant | When tenant became active |
| `suspendedAt` | Instant | When tenant was suspended |
| `deactivatedAt` | Instant | When tenant was deactivated |

#### Localization

| Field | Type | Description |
|-------|------|-------------|
| `timezone` | String | Tenant's timezone (e.g., "Asia/Katmandu") |
| `currency` | String | Default currency (ISO 4217, e.g., "NPR", "USD") |
| `language` | String | Default language (ISO 639-1, e.g., "en", "ne") |

#### Branding

| Field | Type | Description |
|-------|------|-------------|
| `logoUrl` | String | URL to tenant's logo image |
| `faviconUrl` | String | URL to tenant's favicon |

#### Subscription

| Field | Type | Description |
|-------|------|-------------|
| `subscriptionPlanId` | UUID | Reference to subscription plan (for future implementation) |

## Domain Enums

### TenantStatus

```java
public enum TenantStatus {
    TRIAL,      // In trial period, limited access
    PENDING,    // Newly created, awaiting activation
    ACTIVE,     // Fully operational
    SUSPENDED,  // Temporarily suspended, access blocked
    EXPIRED,    // Subscription expired, access blocked
    DEACTIVATED,// Permanently deactivated, access blocked
    ARCHIVED    // Archived for compliance, read-only
}
```

### IsolationStrategy

```java
public enum IsolationStrategy {
    SHARED_SCHEMA,     // All tenants share schema with row-level security
    SCHEMA_PER_TENANT, // Each tenant has its own schema
    DATABASE_PER_TENANT // Each tenant has its own database
}
```

## Business Logic

### State Transitions

```
┌─────────┐     activate()     ┌─────────┐
│ PENDING │ ──────────────────▶ │ ACTIVE  │
└─────────┘                     └─────────┘
     │                                │
     │                             suspend()
     │                                ▼
     │                           ┌──────────┐
     │                           │SUSPENDED │
     │                           └──────────┘
     │                                │
     │                           reactivate()
     │                                ▲
     │                            ┌───┘
     │                             │
     │                             │
     ▼                             │
┌─────────┐                    ┌──────────┐
│  TRIAL  │ ─── activate() ──▶ │  ACTIVE  │
└─────────┘                    └──────────┘
     │                                │
     │                             suspend()
     │                                ▼
     │                           ┌──────────┐
     │                           │SUSPENDED │
     │                           └──────────┘
     │                                │
     │                           reactivate()
     │                                ▲
     │                            ┌───┘
     │                             │
     ▼                             │
┌─────────┐                    ┌──────────┐
│ EXPIRED │ ─── activate() ──▶ │  ACTIVE  │
└─────────┘                    └──────────┘
     │                                │
     │                             suspend()
     │                                ▼
     │                           ┌──────────┐
     │                           │SUSPENDED │
     │                           └──────────┘
     │                                │
     │                           reactivate()
     │                                ▲
     │                            ┌───┘
     │                             │
     ▼                             │
┌──────────┐                   ┌──────────┐
│DEACTIVATED│                   │  ACTIVE  │
└──────────┘                   └──────────┘
     │                                │
     │                             suspend()
     │                                ▼
     │                           ┌──────────┐
     │                           │SUSPENDED │
     │                           └──────────┘
     │                                │
     │                           reactivate()
     │                                ▲
     │                            ┌───┘
     │                             │
     ▼                             │
┌──────────┐                   ┌──────────┐
│ARCHIVED  │◀── archive() ─────│ ANY STATE │
└──────────┘                   └──────────┘
```

### Domain Methods

| Method | Description | Valid From States |
|--------|-------------|-------------------|
| `create()` | Factory method to create tenant in PENDING status | N/A (creates new) |
| `activate(Instant)` | Transitions to ACTIVE, sets activatedAt | PENDING, TRIAL, EXPIRED |
| `suspend(Instant)` | Transitions to SUSPENDED, sets suspendedAt | ACTIVE |
| `reactivate(Instant)` | Transitions to ACTIVE, clears suspendedAt | SUSPENDED |
| `deactivate(Instant)` | Transitions to DEACTIVATED, sets deactivatedAt | ACTIVE, SUSPENDED |
| `archive(Instant)` | Transitions to ARCHIVED (terminal state) | Any state |
| `expire(Instant)` | Transitions to EXPIRED | Any non-terminal state |

### Status Check Methods

| Method | Description |
|--------|-------------|
| `isActive()` | Returns true if status is ACTIVE |
| `isSuspended()` | Returns true if status is SUSPENDED |
| `isPending()` | Returns true if status is PENDING |
| `isTrial()` | Returns true if status is TRIAL |
| `isExpired()` | Returns true if status is EXPIRED |
| `isDeactivated()` | Returns true if status is DEACTIVATED |
| `isArchived()` | Returns true if status is ARCHIVED |

### State Validation Methods

| Method | Description |
|--------|-------------|
| `canBeActivated()` | Returns true if status is PENDING, TRIAL, or EXPIRED |
| `canBeSuspended()` | Returns true if status is ACTIVE |
| `canBeReactivated()` | Returns true if status is SUSPENDED |
| `canBeDeactivated()` | Returns true if status is ACTIVE or SUSPENDED |

### Update Methods

| Method | Description |
|--------|-------------|
| `updateBranding(displayName, logoUrl, faviconUrl)` | Updates branding information (null-safe) |
| `updateContactInfo(email, phone, website)` | Updates contact information (null-safe) |
| `updateLocalization(timezone, currency, language)` | Updates localization settings (null-safe) |

### Subscription Methods

| Method | Description |
|--------|-------------|
| `assignSubscriptionPlan(subscriptionPlanId)` | Associates a subscription plan |
| `removeSubscriptionPlan()` | Removes subscription plan association |
| `hasSubscriptionPlan()` | Returns true if subscription plan is assigned |

## Business Invariants

The Tenant entity enforces the following business rules:

1. **Cannot activate an already active tenant** - Throws `CannotActivateTenantException`
2. **Cannot suspend a pending tenant** - Throws `CannotSuspendTenantException`
3. **Cannot reactivate a tenant that is not suspended** - Throws `CannotReactivateTenantException`
4. **Cannot deactivate a tenant that is not active or suspended** - Throws `CannotDeactivateTenantException`
5. **Cannot activate a suspended, deactivated, or archived tenant** - Throws `CannotActivateTenantException`
6. **Cannot archive a tenant that is already archived** - Silently returns (idempotent)

## Request Flow

### Create Tenant Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Client    │────▶│  REST Controller  │────▶│ Command Handler │
└─────────────┘     └──────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Client    │◀────│  REST Controller  │◀────│ Command Handler │
└─────────────┘     └──────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   Tenant.create()    │
                                              │  (Factory Method)    │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   TenantRepository   │
                                              │      .save()         │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │ TenantCreatedEvent   │
                                              │   (Domain Event)     │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │  Event Publisher     │
                                              │   (Kafka)            │
                                              └─────────────────────┘
```

### Activate Tenant Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Client    │────▶│  REST Controller  │────▶│ Command Handler │
└─────────────┘     └──────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   TenantRepository   │
                                              │      .findById()     │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   tenant.activate()  │
                                              │  (Domain Method)     │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   TenantRepository   │
                                              │      .save()         │
                                              └─────────────────────┘
```

### Suspend Tenant Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Client    │────▶│  REST Controller  │────▶│ Command Handler │
└─────────────┘     └──────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   TenantRepository   │
                                              │      .findById()     │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   tenant.suspend()   │
                                              │  (Domain Method)     │
                                              └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │   TenantRepository   │
                                              │      .save()         │
                                              └─────────────────────┘
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tenants` | Create a new tenant |
| GET | `/api/v1/tenants/{tenantId}` | Get tenant by ID |
| POST | `/api/v1/tenants/{tenantId}/activate` | Activate a tenant |
| POST | `/api/v1/tenants/{tenantId}/suspend` | Suspend a tenant |
| POST | `/api/v1/tenants/{tenantId}/reactivate` | Reactivate a suspended tenant |
| POST | `/api/v1/tenants/{tenantId}/deactivate` | Deactivate a tenant |
| POST | `/api/v1/tenants/{tenantId}/archive` | Archive a tenant |
| PUT | `/api/v1/tenants/{tenantId}/branding` | Update tenant branding |
| PUT | `/api/v1/tenants/{tenantId}/contact` | Update tenant contact info |
| PUT | `/api/v1/tenants/{tenantId}/localization` | Update tenant localization |

## Domain Events

| Event | Description | Trigger |
|-------|-------------|---------|
| `TenantCreatedEvent` | Published when a new tenant is created | `Tenant.create()` |
| `TenantActivatedEvent` | Published when a tenant is activated | `tenant.activate()` |
| `TenantSuspendedEvent` | Published when a tenant is suspended | `tenant.suspend()` |
| `TenantReactivatedEvent` | Published when a tenant is reactivated | `tenant.reactivate()` |
| `TenantDeactivatedEvent` | Published when a tenant is deactivated | `tenant.deactivate()` |
| `TenantArchivedEvent` | Published when a tenant is archived | `tenant.archive()` |

## Future Entities

The following entities are planned for future implementation as part of the Tenant aggregate:

- **SubscriptionPlan** - Defines available plans with features and limits
- **Subscription** - Tenant's active subscription with billing cycle
- **TenantDomain** - Custom domain configuration for white-labeling
- **TenantFeature** - Feature flags and capabilities for tenant
- **TenantSetting** - Configuration settings for tenant
- **TenantBranding** - Extended branding options (colors, themes)

## Running the Service

```bash
# Build the service
./gradlew :platform:tenant:build

# Run the service
./gradlew :platform:tenant:bootRun
```

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.x** - Application framework
- **Jakarta Persistence** - ORM
- **Lombok** - Boilerplate reduction
- **Kafka** - Event streaming
- **PostgreSQL** - Database
- **Gradle** - Build tool
