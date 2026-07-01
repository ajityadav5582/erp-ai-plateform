# Common Core Library

## Overview

The `common-core` library provides the foundational abstractions and utilities used across all microservices in the ERP AI Platform. It contains no business logic—only reusable, framework-agnostic components.

## Purpose

This library is the bedrock of the platform's shared infrastructure. Every microservice depends on it for:

- Base entity definitions with audit and tenant awareness
- Common DTOs for API contracts (pagination, responses, problem details)
- Value objects for type-safe domain concepts
- Exception hierarchy for consistent error handling
- Utility classes for UUID, date/time, and string operations
- Multi-tenant context management
- Specification pattern for composable queries
- Filtering and sorting abstractions

## Architecture

```
com.erp.platform.common/
├── kernel/          # Base entities (BaseEntity, AuditableEntity, etc.)
├── model/vo/        # Value objects (Money, Email, Phone, Address, etc.)
├── dto/             # Data transfer objects (PageRequest, ResponseWrapper, etc.)
├── exception/       # Exception hierarchy (ErpException and subclasses)
├── utils/           # Utility classes (UuidUtils, DateTimeUtils)
├── context/         # Request context (RequestContext)
├── constants/       # Common constants
├── enums/           # Common enums (Status)
├── specification/   # Specification pattern
├── filter/          # Filtering abstractions
├── result/          # Result wrapper (functional success/failure)
├── tenancy/         # Multi-tenant context and resolution
└── vo/              # Additional value objects
```

## Key Components

### Base Entities

| Class | Description |
|-------|-------------|
| `BaseEntity<T>` | Abstract base with `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy` |
| `AuditableEntity<T>` | Extends BaseEntity with JPA auditing annotations |
| `SoftDeleteEntity<T>` | Adds `deletedAt`, `deletedBy` with `markDeleted()` and `restore()` |
| `TenantAwareEntity<T>` | Adds `tenantId` with `belongsToTenant()` check |

### DTOs

| Class | Description |
|-------|-------------|
| `PageRequest` | Standard pagination with page, size, sort |
| `PageResponse<T>` | Paginated response with content and pagination metadata |
| `ResponseWrapper<T>` | Standard API response with data and meta |
| `ProblemDetail` | RFC 7807 Problem Details for error responses |
| `Result<T>` | Functional result wrapper (Success/Failure) |

### Value Objects

| Class | Description |
|-------|-------------|
| `Money` | Type-safe monetary amount with currency validation |
| `Email` | Validated email address |
| `Phone` | Validated phone number |
| `Address` | Structured address value object |
| `Name` | Validated person/entity name |
| `Identifier` | Generic identifier with validation |
| `Percentage` | Type-safe percentage (0-100) |
| `Quantity` | Type-safe quantity with unit |
| `Url` | Validated URL |

### Exceptions

| Class | Description |
|-------|-------------|
| `ErpException` | Base exception with error code, details, timestamp |
| `BusinessException` | Business rule violations |
| `ResourceNotFoundException` | Resource not found (404) |
| `ValidationException` | Validation failures (422) |
| `ConflictException` | Resource conflicts (409) |
| `UnauthorizedException` | Authentication failures (401) |
| `ForbiddenException` | Authorization failures (403) |
| `ErpSystemException` | System/infrastructure errors (500) |

### Utilities

| Class | Description |
|-------|-------------|
| `UuidUtils` | UUID generation, validation, parsing, conversion |
| `DateTimeUtils` | UTC date/time conversion, formatting, parsing |
| `RequestContext` | Thread-local request context (requestId, correlationId, userId, etc.) |
| `TenantContext` | Thread-local tenant ID management |
| `TenantContextHolder` | Tenant context with event publishing |
| `TenantIsolation` | Tenant isolation utilities for queries |

## Dependencies

- Spring Context (for `@CreatedDate`, `@CreatedBy`, etc.)
- Jackson (for JSON serialization)
- Jakarta Validation API
- Apache Commons Lang

## Usage

```java
// Extend base entity
@Entity
public class Invoice extends AuditableEntity<Long> {
    // ...
}

// Use value objects
Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));
Money total = amount.add(Money.of(new BigDecimal("50.00"), Currency.getInstance("USD")));

// Handle results
Result<Invoice> result = invoiceService.findById(tenantId, invoiceId);
return result.handle(
    invoice -> ResponseWrapper.success(invoice),
    error -> ResponseWrapper.error(error)
);

// Pagination
PageRequest pageRequest = new PageRequest(0, 20, List.of("createdAt,desc"));
PageResponse<Invoice> page = invoiceRepository.findAll(tenantId, pageRequest);
```

## Standards Compliance

- Follows Clean Architecture principles
- Implements DDD patterns (Entities, Value Objects, Specifications)
- SOLID principles applied throughout
- Complete JavaDoc for all public APIs
- Thread-safe context management
- Immutable value objects where possible

## Version

1.0.0-SNAPSHOT
