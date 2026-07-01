# Security Architecture

## Overview

Security is designed into every layer of the ERP AI Platform following a defense-in-depth approach.

## Security Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Security Layers                             │
├─────────────────────────────────────────────────────────────────────┤
│  1. Network Security    │ Firewalls, Network Policies, mTLS         │
│  2. Identity & Access   │ Keycloak, OAuth2, OIDC, RBAC             │
│  3. Application Security │ Input validation, Output encoding        │
│  4. Data Security       │ Encryption, Masking, Tokenization        │
│  5. Infrastructure       │ Hardened OS, Minimal images, Secrets     │
└─────────────────────────────────────────────────────────────────────┘
```

## Identity & Access Management

### Keycloak Integration

- OAuth 2.0 / OpenID Connect provider
- Multi-factor authentication (MFA)
- Social login providers
- Fine-grained authorization

### Authentication Flow

```
Client ──▶ API Gateway ──▶ Keycloak
   │              │              │
   │              │◀─────────────┘
   │              │
   │◀─────────────┘
   │
   ▼ (with JWT)
Service ──▶ Validate JWT ──▶ Process Request
```

### Authorization Model

- **Roles**: Platform-level roles (admin, user, service)
- **Permissions**: Fine-grained resource permissions
- **Scopes**: OAuth2 scopes for API access
- **Tenant Isolation**: Users can only access their tenant data

## Data Security

### Encryption

| Layer | Method | Algorithm |
|-------|--------|-----------|
| At Rest (DB) | TDE | AES-256 |
| At Rest (Files) | SSE | AES-256 |
| In Transit | TLS | TLS 1.3 |
| Secrets | Vault | AES-256-GCM |

### Data Masking

- PII fields masked in logs
- Test data anonymization
- Dynamic data masking for non-production

### Row-Level Security

```sql
-- PostgreSQL RLS policy
CREATE POLICY tenant_isolation ON invoices
    FOR ALL
    TO application_user
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
```

## API Security

### Rate Limiting

- Per-user rate limits
- Per-tenant rate limits
- Burst allowance
- Distributed rate limiting via Redis

### Input Validation

- Request size limits
- Content type validation
- Schema validation (JSON Schema)
- SQL injection prevention (parameterized queries)

### Output Encoding

- JSON response sanitization
- XML external entity (XXE) prevention
- Cross-site scripting (XSS) prevention

## Network Security

### Zero Trust

- No implicit trust based on network location
- Every request authenticated and authorized
- Service-to-service authentication via mTLS

### Network Policies

```yaml
# Kubernetes NetworkPolicy example
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: erpai-platform
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress: []
```

## Secrets Management

- External secrets operator (ESO)
- AWS Secrets Manager / HashiCorp Vault
- Secrets never in code or environment variables
- Automatic rotation for supported secrets

## Compliance

### Standards

- SOC 2 Type II
- GDPR
- PCI DSS (for payment processing)
- ISO 27001

### Audit Logging

- All authentication events
- All authorization decisions
- Data access logs
- Configuration changes
- Admin actions

### Vulnerability Management

- Dependency scanning (Dependabot)
- Container image scanning (Trivy)
- SAST/DAST in CI/CD
- Regular penetration testing
