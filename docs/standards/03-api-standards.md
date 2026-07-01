# API Standards

## 1. Purpose

These standards ensure consistency, security, and usability across all REST APIs in the ERP SaaS platform. All API design and implementation must follow these guidelines.

## 2. API Design Principles

- **RESTful:** Follow REST architectural constraints
- **Consistent:** Uniform naming, structure, and behavior across all endpoints
- **Versioned:** All APIs must be versioned from day one
- **Secure:** Authentication, authorization, and input validation on every endpoint
- **Documented:** OpenAPI 3.0 specification for every API
- **Idempotent:** Safe operations should be idempotent
- **Stateless:** No server-side session state

## 3. URL Structure

### 3.1 Base URL Pattern

```
https://api.erp-platform.com/v1/{tenant-context}/{resource}
```

### 3.2 Resource Naming

- **Plural nouns:** Use plural for resource collections
- **Lowercase:** All lowercase with hyphens for multi-word resources
- **No verbs:** URLs represent resources, not actions
- **Hierarchical:** Use nesting for relationships (max 2 levels)

```
# Good
GET    /api/v1/invoices
GET    /api/v1/invoices/{id}
GET    /api/v1/invoices/{id}/payments
POST   /api/v1/invoices

# Bad
GET    /api/v1/getInvoice
GET    /api/v1/invoice/list
POST   /api/v1/createInvoice
GET    /api/v1/invoices/{id}/payments/transactions/refunds
```

### 3.3 Versioning

- **URL path versioning:** `/api/v1/`, `/api/v2/`
- **No version in headers or query params**
- **Support:** Maintain at least 2 major versions
- **Deprecation:** Minimum 6-month deprecation notice

## 4. HTTP Methods

| Method | Usage | Idempotent | Safe |
|--------|-------|------------|------|
| `GET` | Retrieve resource(s) | Yes | Yes |
| `POST` | Create resource | No | No |
| `PUT` | Full update of resource | Yes | No |
| `PATCH` | Partial update of resource | No | No |
| `DELETE` | Delete resource | Yes | No |

## 5. HTTP Status Codes

### 5.1 Success Codes

| Code | Usage |
|------|-------|
| `200 OK` | Successful GET, PUT, PATCH, or DELETE |
| `201 Created` | Successful POST (include `Location` header) |
| `202 Accepted` | Async operation accepted |
| `204 No Content` | Successful DELETE with no body |

### 5.2 Client Error Codes

| Code | Usage |
|------|-------|
| `400 Bad Request` | Invalid request body or parameters |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated but not authorized |
| `404 Not Found` | Resource doesn't exist |
| `409 Conflict` | Resource conflict (duplicate, state conflict) |
| `422 Unprocessable Entity` | Validation failed |
| `429 Too Many Requests` | Rate limit exceeded |
| `499 Client Closed Request` | Client disconnected |

### 5.3 Server Error Codes

| Code | Usage |
|------|-------|
| `500 Internal Server Error` | Unexpected server error |
| `502 Bad Gateway` | Upstream service error |
| `503 Service Unavailable` | Temporary unavailability |
| `504 Gateway Timeout` | Upstream service timeout |

## 6. Request Standards

### 6.1 Headers

```http
# Required headers
Content-Type: application/json
Accept: application/json
X-Tenant-ID: {tenant-id}
X-Request-ID: {uuid}
X-Correlation-ID: {uuid}

# Optional headers
Accept-Language: en-US
X-Forwarded-For: {client-ip}
```

### 6.2 Query Parameters

- **Filtering:** `?status=PENDING&createdAfter=2024-01-01`
- **Sorting:** `?sort=createdAt,desc`
- **Pagination:** `?page=0&size=20`
- **Fields selection:** `?fields=id,invoiceNumber,total`

### 6.3 Request Body

- **Content-Type:** Always `application/json`
- **Encoding:** UTF-8
- **Format:** JSON with camelCase field names
- **Dates:** ISO 8601 format (`2024-01-15T10:30:00Z`)
- **Numbers:** Use strings for monetary values to avoid precision loss

```json
{
  "invoiceNumber": "INV-2024-001",
  "customerId": 123,
  "total": "1500.00",
  "currency": "USD",
  "dueDate": "2024-02-15",
  "lineItems": [
    {
      "description": "Product A",
      "quantity": 2,
      "unitPrice": "500.00"
    }
  ]
}
```

## 7. Response Standards

### 7.1 Response Structure

```json
{
  "data": { ... },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 7.2 Paginated Response

```json
{
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 7.3 Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "total",
        "message": "Total must be greater than zero",
        "rejectedValue": "-100.00"
      }
    ],
    "requestId": "uuid",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

## 8. Error Handling

### 8.1 Error Response Format

All errors must follow this structure:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": [],
    "requestId": "uuid",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 8.2 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 422 | Input validation failed |
| `NOT_FOUND` | 404 | Resource not found |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `CONFLICT` | 409 | Resource conflict |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |
| `SERVICE_UNAVAILABLE` | 503 | Dependency unavailable |

### 8.3 Error Handling Middleware

- Global exception handler using `@RestControllerAdvice`
- Map exceptions to appropriate HTTP status codes
- Include `requestId` in all error responses for tracing
- Never expose stack traces or internal details in production

## 9. Authentication & Authorization

### 9.1 Authentication

- **Method:** OAuth 2.0 / OIDC via Keycloak
- **Token:** Bearer token in `Authorization` header
- **Expiration:** Access tokens: 15 minutes, Refresh tokens: 24 hours
- **Revocation:** Support token revocation on logout

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 9.2 Authorization

- **Model:** RBAC (Role-Based Access Control)
- **Enforcement:** Method-level security with `@PreAuthorize`
- **Tenant isolation:** All queries filtered by tenant context
- **Principle of least privilege:** Default deny, explicit allow

```java
@PreAuthorize("hasRole('FINANCE_ADMIN') or hasRole('FINANCE_USER')")
@GetMapping("/{id}")
public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
    // Tenant isolation handled by interceptor
}
```

## 10. Rate Limiting

- **Default:** 1000 requests per minute per tenant
- **Burst:** Allow 2x burst for 30 seconds
- **Headers:** Return `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **Response:** `429 Too Many Requests` with `Retry-After` header

## 11. API Documentation

### 11.1 OpenAPI Specification

- **Format:** OpenAPI 3.0.3
- **Location:** `docs/api/openapi.yaml`
- **Tool:** Springdoc OpenAPI
- **Content:** All endpoints, schemas, examples, error responses

### 11.2 Documentation Requirements

- **Every endpoint:** Must have description, parameters, request/response examples
- **Error responses:** Document all possible error codes
- **Authentication:** Document required scopes/roles
- **Examples:** Provide realistic request/response examples

## 12. API Testing Standards

- **Contract tests:** Verify API contracts using Spring Cloud Contract
- **Integration tests:** Test full request/response cycle
- **Security tests:** Verify authentication and authorization
- **Performance tests:** Load test critical endpoints
- **Chaos tests:** Test failure scenarios

## 13. API Versioning Strategy

```
Phase 1: Introduce v2 with breaking changes
Phase 2: Mark v1 endpoints as deprecated
Phase 3: Add deprecation headers to v1 responses
Phase 4: Maintain v1 for 6 months
Phase 5: Remove v1 after deprecation period
```

## 14. API Gateway Standards

- **Rate limiting:** Enforced at gateway level
- **Authentication:** Token validation at gateway
- **Routing:** Path-based routing to microservices
- **Caching:** Cache GET responses where appropriate
- **Logging:** Log all requests with correlation IDs
