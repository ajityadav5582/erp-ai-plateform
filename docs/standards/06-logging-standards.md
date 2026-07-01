# Logging Standards

## 1. Purpose

These standards ensure consistent, structured, and actionable logging across the ERP SaaS platform. Proper logging is critical for debugging, monitoring, and compliance.

## 2. Logging Principles

- **Structured logging:** JSON format for machine parsing
- **Contextual:** Include correlation IDs, tenant IDs, user IDs
- **Appropriate levels:** Use correct log levels for severity
- **No sensitive data:** Never log PII, secrets, or credentials
- **Performance:** Avoid expensive string concatenation in log messages
- **Actionable:** Logs should enable debugging without reproducing issues

## 3. Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| `TRACE` | Very detailed diagnostic info | SQL queries, internal state |
| `DEBUG` | Detailed diagnostic info | Method entry/exit, variable values |
| `INFO` | Business-relevant events | Invoice created, payment processed |
| `WARN` | Potential issues | Deprecated API usage, slow query |
| `ERROR` | Error conditions | Exception caught, operation failed |
| `OFF` | Disable logging | Never use in application code |

### 3.1 Level Guidelines

- **TRACE:** Development only; disable in production
- **DEBUG:** Development and staging; selective in production
- **INFO:** Always enabled; business events and milestones
- **WARN:** Always enabled; recoverable issues
- **ERROR:** Always enabled; failures requiring attention

## 4. Log Format

### 4.1 JSON Structure

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "service": "invoice-service",
  "instance": "invoice-service-7d9f4b8c5-x2kqp",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "tenantId": 12345,
  "userId": 67890,
  "className": "InvoiceService",
  "methodName": "create",
  "message": "Invoice created successfully",
  "invoiceId": 1001,
  "invoiceNumber": "INV-2024-001",
  "total": "1500.00",
  "durationMs": 45,
  "thread": "http-nio-8080-exec-1"
}
```

### 4.2 Required Fields

| Field | Description | Example |
|-------|-------------|---------|
| `timestamp` | ISO 8601 UTC | `2024-01-15T10:30:00.123Z` |
| `level` | Log level | `INFO` |
| `service` | Service name | `invoice-service` |
| `instance` | Pod/instance ID | `invoice-service-7d9f4b8c5-x2kqp` |
| `traceId` | Distributed trace ID | `4bf92f3577b34da6a3ce929d0e0e4736` |
| `spanId` | Current span ID | `00f067aa0ba902b7` |
| `tenantId` | Tenant identifier | `12345` |
| `className` | Logging class | `InvoiceService` |
| `methodName` | Logging method | `create` |
| `message` | Log message | `Invoice created successfully` |

### 4.3 Optional Fields

| Field | Description | Example |
|-------|-------------|---------|
| `userId` | User identifier | `67890` |
| `requestId` | Request correlation ID | `req-abc123` |
| `durationMs` | Operation duration | `45` |
| `errorCode` | Business error code | `INVOICE_NOT_FOUND` |
| `stackTrace` | Exception stack trace | (truncated) |

## 5. Logging Implementation

### 5.1 Logback Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <logLevel/>
                <loggerName/>
                <threadName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <appender name="ASYNC_CONSOLE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE"/>
        <queueSize>512</queueSize>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="ASYNC_CONSOLE"/>
    </root>
</configuration>
```

### 5.2 MDC (Mapped Diagnostic Context)

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    
    public Invoice create(CreateInvoiceRequest request) {
        // Set MDC context
        MDC.put("tenantId", String.valueOf(tenantContextHolder.getTenantId()));
        MDC.put("userId", String.valueOf(securityContextHolder.getUserId()));
        MDC.put("requestId", request.requestId());
        
        try {
            log.info("Creating invoice for customer: {}", request.customerId());
            // ... business logic
            log.info("Invoice created successfully: {}", invoice.getId());
            return invoice;
        } catch (Exception e) {
            log.error("Failed to create invoice for customer: {}", request.customerId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

### 5.3 Structured Logging Helper

```java
@Component
public class LoggingHelper {
    private static final Logger log = LoggerFactory.getLogger(LoggingHelper.class);
    
    public void logBusinessEvent(String event, Map<String, Object> data) {
        log.info("Business event: {} | data: {}", event, toJson(data));
    }
    
    public void logMetric(String name, double value, Map<String, String> tags) {
        log.info("Metric: {}={} | tags: {}", name, value, toJson(tags));
    }
    
    private String toJson(Object obj) {
        // Use Jackson or similar
        return new ObjectMapper().writeValueAsString(obj);
    }
}
```

## 6. What to Log

### 6.1 Always Log

- Application startup and shutdown
- Configuration changes
- Business events (order created, payment processed)
- Security events (login, logout, permission denied)
- Error conditions with stack traces
- External API calls (request/response summary)
- Database query performance (slow queries)

### 6.2 Never Log

- Passwords or credentials
- API keys, tokens, secrets
- Full credit card numbers (PAN)
- Social Security Numbers
- Full request/response bodies (may contain PII)
- Session identifiers
- Encryption keys

### 6.3 Sanitization

```java
public class LogSanitizer {
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "secret", "token", "apiKey", "creditCard", 
        "ssn", "cvv", "pin"
    );
    
    public static Map<String, Object> sanitize(Map<String, Object> data) {
        return data.entrySet().stream()
            .filter(entry -> !SENSITIVE_FIELDS.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
```

## 7. Performance Logging

### 7.1 Method Timing

```java
@Aspect
@Component
@Slf4j
public class PerformanceLoggingAspect {
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("Method {} executed in {}ms", 
                    joinPoint.getSignature().toShortString(), duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Method {} failed after {}ms", 
                     joinPoint.getSignature().toShortString(), duration, e);
            throw e;
        }
    }
}
```

### 7.2 Slow Query Detection

```java
@Component
public class DataSourceProxyLogger {
    private static final Logger log = LoggerFactory.getLogger("SQL_LOGGER");
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    @Around("execution(* com.zaxxer.hikari.pool.HikariProxyConnection.*(..))")
    public Object logQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("Slow query detected: {}ms | method: {}", 
                        duration, joinPoint.getSignature().toShortString());
            }
        }
    }
}
```

## 8. Audit Logging

### 8.1 Audit Events

```java
public enum AuditEventType {
    USER_LOGIN,
    USER_LOGOUT,
    INVOICE_CREATED,
    INVOICE_UPDATED,
    INVOICE_DELETED,
    PAYMENT_PROCESSED,
    PERMISSION_DENIED,
    DATA_EXPORTED
}
```

### 8.2 Audit Log Structure

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "service": "invoice-service",
  "audit": true,
  "auditEventType": "INVOICE_CREATED",
  "tenantId": 12345,
  "userId": 67890,
  "actorIp": "192.168.1.100",
  "resourceType": "Invoice",
  "resourceId": 1001,
  "changes": {
    "status": {"old": null, "new": "CREATED"},
    "total": {"old": null, "new": "1500.00"}
  }
}
```

### 8.3 Audit Logging Aspect

```java
@Aspect
@Component
@Slf4j
public class AuditLoggingAspect {
    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditEventType eventType = auditable.value();
        Object[] args = joinPoint.getArgs();
        
        // Log before
        log.info("Audit: {} | args: {}", eventType, sanitize(args));
        
        try {
            Object result = joinPoint.proceed();
            // Log after with result
            log.info("Audit: {} completed | result: {}", eventType, sanitize(result));
            return result;
        } catch (Exception e) {
            log.error("Audit: {} failed", eventType, e);
            throw e;
        }
    }
}
```

## 9. Correlation and Tracing

### 9.1 Correlation ID Propagation

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

### 9.2 Distributed Tracing

- **Tool:** OpenTelemetry with Jaeger/Tempo
- **Propagation:** Propagate trace context across services
- **Sampling:** 100% in dev/staging, 10% in production
- **Span attributes:** Include tenant ID, user ID, operation type

## 10. Log Aggregation

### 10.1 Stack

- **Collection:** Fluent Bit or Filebeat
- **Aggregation:** Loki or Elasticsearch
- **Visualization:** Grafana or Kibana
- **Retention:** 30 days hot, 1 year cold

### 10.2 Queries

```logql
# Find errors for a specific tenant
{service="invoice-service", tenantId="12345"} |= "ERROR"

# Find slow requests
{service="invoice-service"} | json | durationMs > 1000

# Find audit events
{service="invoice-service", audit="true"} | json | auditEventType="INVOICE_DELETED"
```

## 11. Logging Checklist

- [ ] JSON format with required fields
- [ ] MDC context set (tenantId, userId, traceId)
- [ ] Appropriate log level used
- [ ] No sensitive data in logs
- [ ] Performance logging for slow operations
- [ ] Audit logging for sensitive operations
- [ ] Correlation ID propagated
- [ ] Error logs include stack traces
- [ ] Business events logged at INFO level
