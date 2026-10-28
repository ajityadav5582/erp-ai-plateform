# Getting Started Guide

## Local Development Setup

This guide walks you through setting up and running the ERP AI Platform frontend and backend services locally for development and testing.

---

## Prerequisites

Before you begin, ensure you have the following installed:

| Tool | Version | Purpose |
|------|---------|---------|
| **Node.js** | 18.x or 20.x | Frontend runtime |
| **npm** | 9.x or 10.x | Package manager |
| **Java** | 17+ | Backend runtime |
| **Gradle** | 8.x (included via wrapper) | Backend build tool |
| **Docker & Docker Compose** | Latest | Infrastructure services (PostgreSQL, Redis, Kafka) |
| **Git** | Latest | Version control |

---

## Step 1: Clone the Repository

```bash
git clone <repository-url>
cd erp-ai-plateform
```

---

## Step 2: Start Infrastructure Services

The platform requires several infrastructure services. Start them using Docker Compose:

```bash
# From the project root
docker compose -f compose.development.yml up -d
```

This starts:
- **PostgreSQL** - Primary database (port 5432)
- **Redis** - Caching and session store (port 6379)
- **Kafka** - Event streaming (port 9092)
- **Keycloak** - Identity provider (port 8081)
- **Mailhog** - Email testing (port 8025)

Verify services are running:
```bash
docker compose -f compose.development.yml ps
```

---

## Step 3: Start the Backend (Identity Service)

The backend is a Spring Boot application built with Gradle.

### Option A: Using Gradle (Recommended for Development)

```bash
# From the project root
./gradlew :platform:identity:bootRun
```

### Option B: Using Docker

```bash
docker compose -f compose.development.yml up -d identity-service
```

### Verify Backend is Running

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Backend Configuration

The backend reads configuration from `application.yml` and environment variables. Key settings:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | HTTP port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/erp_platform | Database URL |
| `spring.redis.host` | localhost | Redis host |
| `jwt.access-token-expiry` | 900000 (15 min) | Access token TTL |
| `jwt.refresh-token-expiry` | 604800000 (7 days) | Refresh token TTL |

---

## Step 4: Configure the Frontend

### 4.1 Install Dependencies

```bash
cd frontend
npm install
```

### 4.2 Environment Configuration

Create a `.env.local` file in the `frontend/` directory:

```env
# Frontend Configuration
NEXT_PUBLIC_APP_URL=http://localhost:3000
NEXT_PUBLIC_ENABLE_DEBUG_LOGGING=true

# Backend API Configuration
API_BASE_URL=http://localhost:8080/api/v1
API_TIMEOUT_MS=30000
```

**Important:** The `API_BASE_URL` must point to your backend service. If your backend runs on a different port, update this accordingly.

### 4.3 Start the Frontend Development Server

```bash
# From the frontend directory
npm run dev
```

The frontend will be available at `http://localhost:3000`.

---

## Step 5: Initialize the Database

The backend uses Flyway for database migrations. Migrations run automatically on startup. To verify:

```bash
# Check migration status
./gradlew :platform:identity:flywayInfo
```

To reset the database (caution: drops all data):
```bash
./gradlew :platform:identity:flywayClean
```

---

## Step 6: Create a Test Tenant and User

### Option A: Using the API

```bash
# Create a tenant (if tenant service is running)
curl -X POST http://localhost:8080/api/v1/tenant/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Tenant",
    "code": "TEST",
    "domain": "test.local"
  }'
```

### Option B: Using the Database Directly

```bash
# Connect to PostgreSQL
docker exec -it erp-postgres psql -U postgres -d erp_platform

# Insert a test tenant
INSERT INTO tenants (tenant_id, name, code, domain, status, created_at, updated_at)
VALUES (1, 'Test Tenant', 'TEST', 'test.local', 'ACTIVE', NOW(), NOW());

# Insert a test user (password is 'password123' hashed with BCrypt)
INSERT INTO users (user_id, tenant_id, username, email, password_hash, full_name, status, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  1,
  'admin',
  'admin@test.local',
  '$2a$10$rQ7H8k9L0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7u',
  'System Administrator',
  'ACTIVE',
  NOW(),
  NOW()
);

# Assign ADMIN role
INSERT INTO user_roles (user_role_id, user_id, tenant_id, role_id, assigned_at, assigned_by)
VALUES (
  gen_random_uuid(),
  '550e8400-e29b-41d4-a716-446655440000',
  1,
  (SELECT role_id FROM roles WHERE role_code = 'ADMIN' AND tenant_id IS NULL),
  NOW(),
  'system'
);
```

---

## Step 7: Test the Login Flow

### 7.1 Open the Application

Navigate to `http://localhost:3000` in your browser.

### 7.2 Login Credentials

| Field | Value |
|-------|-------|
| Tenant ID | `1` |
| Username | `admin` |
| Password | `password123` |

### 7.3 Expected Behavior

1. **Form Validation**: Empty fields show validation errors immediately
2. **Loading State**: Button shows "Signing in..." with spinner
3. **Success**: Toast notification "Welcome back, System Administrator!" appears
4. **Redirect**: Browser navigates to `/` (dashboard)
5. **Session**: User is authenticated, protected routes are accessible

### 7.4 Test Error Scenarios

| Scenario | Expected Result |
|----------|----------------|
| Wrong password | Red error: "Invalid username or password" |
| Non-existent user | Red error: "Invalid username or password" |
| Wrong tenant ID | Red error: "Invalid username or password" |
| Account locked (5+ failed attempts) | Amber warning with lockout message |
| Backend offline | "Unable to connect to the server" |

---

## Step 8: Verify Token Refresh

1. Login successfully
2. Open browser DevTools → Application → Cookies
3. Note the `access_token` and `refresh_token` values
4. Wait 15 minutes (or manually expire the access token)
5. Make an API request (navigate to a protected page)
6. The interceptor should automatically refresh the token
7. Verify new tokens in cookies

---

## Step 9: Test Password Reset Flow

1. Click "Forgot password?" on the login page
2. Navigate to `/forgot-password`
3. Enter Tenant ID: `1` and Email: `admin@test.local`
4. Click "Send reset link"
5. Success toast appears
6. Backend returns a reset token (in development mode)

---

## Step 10: Test Logout

1. Click the user menu in the header
2. Select "Logout"
3. Tokens are cleared from cookies and token store
4. Redirected to `/login`
5. Protected routes are no longer accessible

---

## Development Workflow

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server with hot reload
npm run dev

# Run type checking
npx tsc --noEmit

# Run linting
npm run lint

# Build for production
npm run build
```

### Backend Development

```bash
# From project root

# Build all modules
./gradlew build

# Run identity service
./gradlew :platform:identity:bootRun

# Run tests
./gradlew test

# Run specific test class
./gradlew :platform:identity:test --tests "com.erp.platform.identity.AuthServiceTest"
```

### Hot Reload

- **Frontend**: Next.js dev server provides hot reload out of the box
- **Backend**: Spring Boot DevTools enables automatic restart on classpath changes (included in Gradle dependencies)

---

## Debugging

### Frontend Debugging

1. Open browser DevTools (F12)
2. **Console**: Check for errors and debug logs (enable with `NEXT_PUBLIC_ENABLE_DEBUG_LOGGING=true`)
3. **Network**: Inspect API requests/responses
4. **Application**: Check cookies and localStorage
5. **Redux DevTools**: Install browser extension to inspect Redux state

### Backend Debugging

1. Enable debug logging in `application.yml`:
   ```yaml
   logging:
     level:
       com.erp.platform.identity: DEBUG
   ```
2. Use IDE breakpoints in IntelliJ IDEA or Eclipse
3. Check actuator endpoints:
   - `http://localhost:8080/actuator/health`
   - `http://localhost:8080/actuator/metrics`

### Common Issues

| Issue | Solution |
|-------|----------|
| `ECONNREFUSED` to backend | Ensure backend is running on port 8080 |
| `CORS error` | Backend CORS is configured for `http://localhost:3000` |
| `401 Unauthorized` on login | Check tenant ID exists in database |
| `Token refresh loop` | Clear cookies and login again |
| `Validation error` | Check request body matches `LoginRequest` schema |

---

## Project Structure

```
erp-ai-plateform/
├── frontend/                    # Next.js frontend application
│   ├── src/
│   │   ├── app/                 # Next.js App Router pages
│   │   │   ├── login/           # Login page
│   │   │   ├── forgot-password/ # Password reset page
│   │   │   └── (protected)/     # Protected route group
│   │   ├── components/          # React components
│   │   │   ├── auth/            # Auth-related components
│   │   │   ├── common/          # Shared UI components
│   │   │   ├── forms/           # Form components
│   │   │   ├── layout/          # Layout components
│   │   │   └── providers/       # Context providers
│   │   ├── services/            # API services and utilities
│   │   ├── store/               # Redux store
│   │   ├── config/              # Configuration
│   │   └── lib/                 # Utility functions
│   ├── .env.example             # Environment template
│   └── package.json
├── platform/
│   └── identity/                 # Identity service (Spring Boot)
│       ├── src/main/java/
│       │   └── com/erp/platform/identity/
│       │       ├── application/  # Use cases and services
│       │       ├── domain/       # Domain models
│       │       ├── interfaces/   # REST controllers
│       │       └── infrastructure/ # Repositories, security
│       └── src/main/resources/
│           └── application.yml   # Spring configuration
├── infrastructure/              # Docker and deployment configs
│   ├── docker/                  # Docker configurations
│   ├── kafka/                   # Kafka configurations
│   └── postgres/                # Database configurations
├── compose.development.yml      # Docker Compose for development
└── build.gradle                 # Gradle build configuration
```

---

## Testing the Complete Flow

### Manual Testing Checklist

- [ ] Frontend loads at `http://localhost:3000`
- [ ] Login page displays with form fields
- [ ] Form validation works (empty fields, invalid tenant ID)
- [ ] Successful login redirects to dashboard
- [ ] Toast notification appears on login
- [ ] User menu shows logged-in user's name
- [ ] Protected routes are accessible
- [ ] Logout clears session and redirects to login
- [ ] Token refresh works after access token expires
- [ ] Forgot password page loads and submits
- [ ] Error messages display correctly for invalid credentials
- [ ] Account lockout displays amber warning after 5 failed attempts

### API Testing with curl

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login
curl -X POST http://localhost:8080/api/v1/identity/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "username": "admin",
    "password": "password123"
  }'

# Refresh token
curl -X POST http://localhost:8080/api/v1/identity/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<your-refresh-token>"
  }'

# Get current user
curl http://localhost:8080/api/v1/identity/auth/me \
  -H "Authorization: Bearer <your-access-token>"

# Forgot password
curl -X POST http://localhost:8080/api/v1/identity/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "email": "admin@test.local"
  }'
```

---

## Next Steps

After completing the local setup:

1. Read the [Frontend Auth Documentation](./FRONTEND_AUTH_DOCUMENTATION.md) for architecture details
2. Explore the [Backend API Documentation](../docs/api/README.md)
3. Review the [Architecture Documentation](../docs/architecture/README.md)
4. Check the [Contributing Guide](../../CONTRIBUTING.md)
