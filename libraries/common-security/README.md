# Common Security Library

## Overview

The `common-security` library provides reusable security abstractions for the ERP AI Platform. It defines interfaces and annotations for authentication, authorization, and multi-tenant security without implementing any specific security mechanisms.

## Purpose

This library provides the foundational security abstractions that all microservices use for security. It does not implement JWT validation, Keycloak integration, OAuth2, or authentication providers - those belong to platform services.

## Key Components

### Authentication

- **AuthenticatedUser** - Abstraction for an authenticated user
- **TenantPrincipal** - Tenant principal combining tenant and user information

### Authorization

- **Permission** - Permission abstraction with domain:action:resource format
- **HasPermission** - Annotation for permission-based access control
- **HasRole** - Annotation for role-based access control

### Security Context

- **SecurityContext** - Security context abstraction for accessing current user and tenant

### Utilities

- **SecurityUtils** - Utility methods for common security operations

### Exceptions

- **SecurityException** - Base exception for security-related errors

## Usage

### Permission Format

Permissions follow the format: `{domain}:{action}:{resource}`

Examples:
- `invoice:read:invoice`
- `invoice:create:invoice`
- `payment:process:payment`

### Using Authorization Annotations

```java
@Service
public class InvoiceService {

    @HasPermission("invoice:read:invoice")
    public Invoice getInvoice(UUID id) {
        // ...
    }

    @HasRole("ADMIN")
    public void deleteInvoice(UUID id) {
        // ...
    }
}
```

### Using Security Context

```java
@Service
public class SomeService {

    private final SecurityContext securityContext;

    public SomeService(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public void doSomething() {
        if (securityContext.hasPermission("invoice:read:invoice")) {
            // ...
        }
    }
}
```

### Using Security Utilities

```java
@Service
public class SomeService {

    private final SecurityContext securityContext;

    public void sensitiveOperation() {
        SecurityUtils.requireAuthentication(securityContext);
        SecurityUtils.requirePermission(securityContext, "invoice:delete:invoice");
    }
}
```

## Dependencies

- Spring Boot 3.x
- Java 21
- common-core

## Testing

This library includes unit tests for all components. Run tests with:

```bash
./gradlew :libraries:common-security:test
```
