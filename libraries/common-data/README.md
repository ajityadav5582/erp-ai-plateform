# Common Data Library

## Overview

The `common-data` library provides reusable persistence components for all microservices in the ERP AI Platform. It contains no business logic—only data access abstractions and configurations.

## Purpose

This library standardizes data access patterns across the platform:

- Base repository interfaces with tenant-aware operations
- JPA specification builder for type-safe queries
- Query utilities for pagination and sorting
- Flyway base configuration for database migrations
- JPA configuration with auditing enabled
- Optimistic locking support for concurrent updates
- Auditing configuration for automatic field population

## Architecture

```
com.erp.platform.data/
├── repository/       # Base repository interfaces
├── specification/    # JPA specification builder
├── query/            # Query utilities
├── config/           # Flyway and JPA configurations
├── lock/             # Optimistic locking support
└── audit/            # Auditing configuration
```

## Key Components

### Base Repository

| Interface | Description |
|-----------|-------------|
| `BaseRepository<T, ID>` | Base repository with tenant-aware CRUD operations |
| `JpaRepository<T, ID>` | Extended JPA repository interface |

### Specification Builder

| Class | Description |
|-------|-------------|
| `SpecificationBuilder<T>` | Builds JPA Specifications from Filter criteria |

### Query Utilities

| Class | Description |
|-------|-------------|
| `QueryUtils` | Converts platform abstractions to Spring Data JPA constructs |

### Configuration

| Class | Description |
|-------|-------------|
| `FlywayBaseConfig` | Base Flyway configuration with safe migration strategy |
| `JpaConfig` | Base JPA configuration with auditing enabled |
| `AuditingConfig` | Auditor aware implementation for automatic field population |

### Locking

| Class | Description |
|-------|-------------|
| `OptimisticLock<T>` | Entity with optimistic locking support via `@Version` |

## Dependencies

- Spring Data JPA
- Hibernate Core
- Flyway Core
- PostgreSQL Driver
- Common Core

## Usage

```java
// Extend base repository
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Custom query methods
    List<Invoice> findByStatus(InvoiceStatus status);
}

// Use specification builder
SpecificationBuilder<Invoice> builder = new SpecificationBuilder<>();
builder.filter("status", Filter.Operator.EQUALS, "ACTIVE");
builder.filter("total", Filter.Operator.GREATER_THAN, new BigDecimal("1000"));

Specification<Invoice> spec = builder.build();
Page<Invoice> results = invoiceRepository.findAll(spec, pageRequest);

// Use query utilities
PageRequest pageRequest = QueryUtils.toPageRequest(platformPageRequest);
Specification<Invoice> tenantSpec = QueryUtils.withTenantFilter(tenantId);
```

## Standards Compliance

- Follows Clean Architecture principles
- Tenant isolation enforced at repository level
- Optimistic locking for concurrent updates
- Automatic auditing of created/updated fields
- Flyway for version-controlled migrations

## Version

1.0.0-SNAPSHOT
