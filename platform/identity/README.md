# Identity Module

## Overview

The Identity module is the central authentication and authorization service for the ERP AI Platform. It provides user management, role-based access control (RBAC), JWT-based authentication, and password reset functionality.

## Architecture

The Identity module follows Clean Architecture principles with clear separation of concerns:

```
platform/identity/
├── domain/                    # Domain layer (entities, events, exceptions)
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── RefreshToken.java
│   ├── PasswordResetToken.java
│   ├── event/                 # Domain events
│   └── exception/             # Domain exceptions
├── application/               # Application layer (services, DTOs, mappers)
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   ├── UserService.java
│   ├── RoleService.java
│   ├── PermissionService.java
│   ├── dto/                   # Data Transfer Objects
│   ├── mapper/                # Object mappers
│   └── security/              # JWT, password encoding, properties
├── infrastructure/            # Infrastructure layer (repositories, persistence)
│   └── persistence/
│       ├── UserRepository.java
│       ├── RoleRepository.java
│       ├── RefreshTokenRepository.java
│       └── PasswordResetTokenRepository.java
└── interfaces/                # Interface layer (REST controllers, filters)
    └── rest/
        ├── AuthController.java
        ├── RoleController.java
        ├── PermissionController.java
        ├── UserRoleController.java
        ├── filter/
        │   └── JwtAuthenticationFilter.java
        ├── config/
        │   └── SecurityConfig.java
        └── GlobalExceptionHandler.java
```

## Key Components

### Domain Entities

| Entity | Description |
|--------|-------------|
| `User` | Aggregate root representing a system user with credentials, status, and audit fields |
| `Role` | Represents a role with permissions (SYSTEM, TENANT, CUSTOM) |
| `Permission` | Represents a permission with resource, action, and effect |
| `UserRole` | Join entity linking users to roles with validity periods |
| `RefreshToken` | JWT refresh token with rotation support |
| `PasswordResetToken` | Token for password reset flow |

### Application Services

| Service | Responsibility |
|---------|---------------|
| `AuthService` | Authentication, token management, password reset |
| `UserService` | User CRUD operations |
| `RoleService` | Role management |
| `PermissionService` | Permission management |

### Security Features

- **JWT Authentication**: Stateless authentication with access and refresh tokens
- **Refresh Token Rotation**: Old refresh tokens are revoked when new ones are issued
- **Password Hashing**: BCrypt with configurable strength
- **Account Lockout**: Temporary lockout after configurable failed login attempts
- **Password Reset**: Secure password reset with time-limited tokens

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Authenticate user and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout (revoke refresh token) |
| POST | `/api/v1/auth/logout-all` | Logout from all devices |
| POST | `/api/v1/auth/forgot-password` | Initiate password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |

### Role Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/roles` | Create a new role |
| GET | `/api/v1/roles` | List all roles |
| GET | `/api/v1/roles/{id}` | Get role by ID |
| PUT | `/api/v1/roles/{id}` | Update role |
| DELETE | `/api/v1/roles/{id}` | Delete role |
| POST | `/api/v1/roles/{id}/permissions` | Assign permission to role |
| DELETE | `/api/v1/roles/{id}/permissions/{permissionId}` | Remove permission from role |

### Permission Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/permissions` | Create a new permission |
| GET | `/api/v1/permissions` | List all permissions |
| GET | `/api/v1/permissions/{id}` | Get permission by ID |
| PUT | `/api/v1/permissions/{id}` | Update permission |
| DELETE | `/api/v1/permissions/{id}` | Delete permission |

### User-Role Assignment

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/{userId}/roles/{roleId}` | Assign role to user |
| DELETE | `/api/v1/users/{userId}/roles/{roleId}` | Remove role from user |
| GET | `/api/v1/users/{userId}/roles` | Get user's roles |

## Configuration

### Application Properties

```yaml
# JWT Configuration
auth:
  jwt:
    secret: your-secret-key-here
    access-token-expiry-ms: 900000      # 15 minutes
    refresh-token-expiry-ms: 604800000  # 7 days
    issuer: erp-ai-platform

# Password Reset Configuration
auth:
  password-reset:
    token-expiry-ms: 3600000            # 1 hour
    max-failed-login-attempts: 5
    lockout-duration-ms: 900000         # 15 minutes
    return-token-in-response: false     # true for development only
```

### Security Configuration

The module uses Spring Security with the following configuration:

- **Public Endpoints**: `/api/v1/auth/**`, `/actuator/health`, `/actuator/info`
- **Protected Endpoints**: All other endpoints require JWT authentication
- **CORS**: Configured for frontend origin
- **CSRF**: Disabled (stateless JWT authentication)

## Domain Events

The Identity module publishes the following domain events:

| Event | Description |
|-------|-------------|
| `domain.user.created` | Published when a new user is created |
| `domain.user.logged_in` | Published when a user logs in |
| `domain.user.logged_out` | Published when a user logs out |
| `domain.password.reset` | Published when a password is reset |
| `domain.role.created` | Published when a new role is created |
| `domain.role.updated` | Published when a role is updated |

### Publishing Events

Use the `IdentityEventPublisher` to publish events:

```java
@Autowired
private IdentityEventPublisher eventPublisher;

// Publish user created event
eventPublisher.publishUserCreated(tenantId, userId, username, email, fullName);

// Publish login event
eventPublisher.publishUserLoggedIn(tenantId, userId, username, deviceInfo, ipAddress);
```

## Multi-Tenancy

The Identity module is multi-tenant aware:

- All data is scoped by `tenantId`
- JWT tokens contain the tenant ID claim
- Repositories filter by tenant automatically
- `TenantContext` is used to resolve the current tenant

## Password Reset Flow

1. User requests password reset with email
2. System generates a secure reset token (hashed before storage)
3. Token is stored with expiry time
4. Reset link is sent to user's email (or returned in dev mode)
5. User clicks link and submits new password
6. System validates token, updates password, and marks token as used

## Refresh Token Rotation

For enhanced security, the Identity module implements refresh token rotation:

1. User logs in and receives access + refresh tokens
2. When access token expires, user uses refresh token to get new tokens
3. Old refresh token is revoked and a new one is issued
4. If a refresh token is reused, it is rejected (prevents token theft)

## Development

### Prerequisites

- Java 21+
- Gradle 8.14+
- PostgreSQL (for persistence)

### Building

```bash
# Compile the module
./gradlew :platform:identity:compileJava

# Run tests
./gradlew :platform:identity:test

# Build JAR
./gradlew :platform:identity:build
```

### Adding a New Domain Event

1. Create the event record in `domain/event/`
2. Implement the `DomainEvent` interface
3. Add a factory method (`of()`) for easy creation
4. Add a publish method in `IdentityEventPublisher`
5. Call the publish method from the relevant service

### Adding a New REST Endpoint

1. Create DTOs in `application/dto/`
2. Add method to the service interface
3. Implement the method in the service implementation
4. Create or update the controller
5. Add exception handling in `GlobalExceptionHandler` if needed

## Error Handling

The module uses a centralized exception handling approach:

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `AuthenticationException` | 401 | Authentication failed |
| `InvalidCredentialsException` | 401 | Invalid username or password |
| `AccountLockedException` | 423 | Account temporarily locked |
| `InvalidRefreshTokenException` | 401 | Invalid or expired refresh token |
| `PasswordResetTokenException` | 400 | Invalid password reset token |
| `UserNotFoundException` | 404 | User not found |
| `RoleNotFoundException` | 404 | Role not found |
| `PermissionNotFoundException` | 404 | Permission not found |

## Testing

The module currently has no test files. To add tests:

1. Create test classes in `src/test/java/`
2. Use `@SpringBootTest` for integration tests
3. Use `@MockBean` for mocking dependencies
4. Follow the project's testing standards in `docs/standards/09-testing-standards.md`

## Dependencies

- `common-core`: Tenant context, UUID utilities
- `common-data`: Auditing, JPA repositories
- `common-events`: Domain events, event publishing
- `common-security`: Password encoding, security utilities
- Spring Boot Starter Web
- Spring Boot Starter Security
- Spring Boot Starter Data JPA
- JJWT (for JWT handling)
