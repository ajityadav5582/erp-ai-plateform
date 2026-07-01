# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability, please follow these steps:

1. **Do NOT** open a public issue on GitHub
2. Email security concerns to: security@erpai.internal
3. Include the following information:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Security Response Timeline

- **Critical**: Response within 24 hours, fix within 7 days
- **High**: Response within 48 hours, fix within 14 days
- **Medium**: Response within 1 week, fix within 30 days
- **Low**: Response within 2 weeks, fix within next release

## Security Best Practices

### Authentication & Authorization
- All services use OAuth 2.0 / OIDC via Keycloak
- JWT tokens with short expiration (15 minutes)
- Refresh tokens with rotation
- Multi-factor authentication enforced for admin accounts

### Data Protection
- Encryption at rest for all databases
- TLS 1.3 for all service communication
- Secrets managed via external secret stores
- PII data classified and protected

### Network Security
- Zero-trust network architecture
- Service mesh for mTLS (future)
- Network policies for pod isolation
- API Gateway for rate limiting and WAF

### Compliance
- SOC 2 Type II compliant architecture
- GDPR data protection principles
- Regular security audits
- Penetration testing per release

## Security Contacts

- Security Team: security@erpai.internal
- Platform Team: platform@erpai.internal
- Emergency: +1-XXX-XXX-XXXX
