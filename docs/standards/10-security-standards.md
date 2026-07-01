# Security Standards

## 1. Purpose

These standards ensure the ERP SaaS platform is secure by design, protecting sensitive business data and maintaining customer trust. Security is everyone's responsibility.

## 2. Security Principles

- **Defense in depth:** Multiple layers of security controls
- **Least privilege:** Minimum permissions required
- **Secure by default:** Safe defaults, opt-in for risky features
- **Fail securely:** Errors don't leak information
- **Don't trust client input:** Validate and sanitize everything
- **Security in depth:** Security at every layer

## 3. Authentication

### 3.1 Standards

- **Protocol:** OAuth 2.0 / OIDC via Keycloak
- **Token type:** JWT (JSON Web Token)
- **Algorithm:** RS256 (asymmetric)
- **Access token TTL:** 15 minutes
- **Refresh token TTL:** 24 hours
- **Token storage:** HttpOnly, Secure, SameSite cookies (web) or secure storage (mobile)

### 3.2 Implementation

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        
        if (token != null && jwtProvider.validateToken(token)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### 3.3 Password Policy

- **Minimum length:** 12 characters
- **Complexity:** Upper, lower, number, special character
- **Hashing:** bcrypt with cost factor 12
- **History:** Prevent reuse of last 5 passwords
- **Expiration:** 90 days
- **Account lockout:** 5 failed attempts, 15-minute lockout

## 4. Authorization

### 4.1 RBAC Model

```java
public enum Role {
    SUPER_ADMIN,      // Full platform access
    TENANT_ADMIN,     // Full tenant access
    FINANCE_ADMIN,    // Finance module admin
    FINANCE_USER,     // Finance module user
    INVENTORY_ADMIN,  // Inventory module admin
    INVENTORY_USER,   // Inventory module user
    HR_ADMIN,         // HR module admin
    HR_USER,          // HR module user
    SALES_ADMIN,      // Sales module admin
    SALES_USER,       // Sales module user
    VIEWER            // Read-only access
}
```

### 4.2 Permission-Based Access

```java
@PreAuthorize("hasAuthority('invoice:read') or hasAuthority('invoice:write')")
@GetMapping("/{id}")
public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
    // ...
}

@PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('FINANCE_ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    // ...
}
```

### 4.3 Method Security

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // Enable @PreAuthorize, @PostAuthorize, @Secured
}
```

## 5. Data Protection

### 5.1 Encryption at Rest

- **Database:** Transparent Data Encryption (TDE) enabled
- **Backups:** Encrypted with AES-256
- **File storage:** Server-side encryption (SSE-S3 or similar)
- **Key management:** AWS KMS or HashiCorp Vault

### 5.2 Encryption in Transit

- **TLS:** TLS 1.3 minimum, TLS 1.2 acceptable
- **Cipher suites:** Strong ciphers only (no RC4, DES, 3DES)
- **Certificates:** Valid certificates from trusted CA
- **HSTS:** Enable HTTP Strict Transport Security

### 5.3 Sensitive Data Handling

```java
// Encrypt sensitive fields
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "tax_id")
private String taxId;

// Mask in logs
public class DataMasker {
    public static String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}
```

## 6. Input Validation & Sanitization

### 6.1 Validation

- **All inputs validated:** Never trust client input
- **Whitelist approach:** Accept known-good, reject unknown
- **Type checking:** Validate types before processing
- **Length limits:** Enforce maximum lengths
- **Format validation:** Regex for patterns (email, phone, etc.)

### 6.2 Sanitization

```java
@Component
public class InputSanitizer {
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Whitelist.none());
    }
    
    public String sanitizeSql(String input) {
        if (input == null) return null;
        // Parameterized queries prevent SQL injection
        // This is defense-in-depth
        return input.replaceAll("[';\"\\\\]", "");
    }
}
```

### 6.3 SQL Injection Prevention

```java
// GOOD: Parameterized query
@Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = :number")
Optional<Invoice> findByInvoiceNumber(@Param("number") String number);

// BAD: String concatenation
String query = "SELECT * FROM invoices WHERE invoice_number = '" + number + "'";
```

## 7. Output Encoding

### 7.1 XSS Prevention

```java
@RestControllerAdvice
public class XssProtectionAdvice {
    
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, 
            new StringTrimmerEditor(true));
    }
}
```

### 7.2 Content Security Policy

```java
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; " +
            "font-src 'self' data:; connect-src 'self'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()");
        
        filterChain.doFilter(request, response);
    }
}
```

## 8. API Security

### 8.1 Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-ID");
        String key = "rate-limit:" + tenantId;
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        
        if (count > 1000) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            return;
        }
        
        response.setHeader("X-RateLimit-Limit", "1000");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(1000 - count));
        
        filterChain.doFilter(request, response);
    }
}
```

### 8.2 CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.erp-platform.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setMaxAge(3600L);
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

## 9. Secrets Management

### 9.1 Principles

- **Never hardcode secrets:** Use environment variables or vault
- **Rotate regularly:** API keys, passwords, certificates
- **Least access:** Secrets accessible only to services that need them
- **Audit access:** Log all secret access

### 9.2 Implementation

```java
@Configuration
@ConfigurationProperties(prefix = "vault")
@Data
public class VaultConfig {
    private String url;
    private String token;
    private String path;
}

// Use Spring Vault for secret retrieval
@Bean
public VaultTemplate vaultTemplate(VaultConfig config) {
    return new VaultTemplate(
        new VaultEndpoint(config.getUrl()),
        new VaultTokenAuthentication(config.getToken())
    );
}
```

## 10. Security Monitoring

### 10.1 Audit Logging

```java
@Aspect
@Component
@Slf4j
public class SecurityAuditAspect {
    
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String eventType = auditable.value();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        log.info("SECURITY_AUDIT: event={} user={} tenant={} ip={}",
            eventType,
            auth != null ? auth.getName() : "anonymous",
            tenantContextHolder.getTenantId(),
            requestContextHolder.getClientIp());
        
        return joinPoint.proceed();
    }
}
```

### 10.2 Security Events

| Event | Log Level | Alert |
|-------|-----------|-------|
| Failed login | WARN | > 5 in 5 minutes |
| Permission denied | WARN | > 10 in 5 minutes |
| Token refresh | INFO | - |
| Password change | INFO | - |
| Admin action | INFO | - |
| Data export | INFO | - |

## 11. Vulnerability Management

### 11.1 Dependencies

- **Scanning:** OWASP Dependency-Check, Snyk
- **Updates:** Monthly dependency updates
- **Critical CVEs:** Patch within 48 hours
- **High CVEs:** Patch within 1 week

### 11.2 SAST/DAST

- **SAST:** SonarQube in CI pipeline
- **DAST:** OWASP ZAP scans on staging
- **Penetration testing:** Quarterly for production

## 12. Compliance

### 12.1 Standards

- **SOC 2 Type II:** Annual audit
- **GDPR:** Data privacy compliance
- **PCI DSS:** If handling payment cards
- **ISO 27001:** Information security management

### 12.2 Data Retention

| Data Type | Retention | Reason |
|-----------|-----------|--------|
| Audit logs | 7 years | Compliance |
| Financial records | 7 years | Tax compliance |
| User data | Until deletion request | GDPR |
| Backups | 30 days | Disaster recovery |

## 13. Security Checklist

- [ ] OAuth 2.0 / OIDC implemented
- [ ] RBAC with least privilege
- [ ] All inputs validated and sanitized
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (output encoding, CSP)
- [ ] CSRF protection enabled
- [ ] Sensitive data encrypted at rest
- [ ] TLS 1.3 enforced
- [ ] Security headers configured
- [ ] Rate limiting implemented
- [ ] Audit logging for sensitive operations
- [ ] Secrets in vault, not in code
- [ ] Dependency scanning in CI
- [ ] Security headers configured
- [ ] CORS properly configured
