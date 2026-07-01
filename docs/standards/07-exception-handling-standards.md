# Exception Handling Standards

## 1. Purpose

These standards ensure consistent, informative, and secure exception handling across the ERP SaaS platform. Proper exception handling improves debuggability, user experience, and system reliability.

## 2. Exception Handling Principles

- **Fail fast:** Detect and report errors as early as possible
- **Fail safe:** Ensure system remains in consistent state after failure
- **Meaningful messages:** Provide actionable error information
- **Secure:** Never expose internal details to external clients
- **Idempotent:** Retry-safe operations
- **Traceable:** All exceptions must be traceable via correlation IDs

## 3. Exception Hierarchy

### 3.1 Base Exception Classes

```java
// Base exception for all application exceptions
public abstract class ErpException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;
    
    protected ErpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Map.of();
    }
    
    protected ErpException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    protected ErpException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Map.of();
    }
}

// Business exception for domain rule violations
public class BusinessException extends ErpException {
    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public BusinessException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}

// Validation exception for input validation failures
public class ValidationException extends ErpException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
    
    public ValidationException(String message, List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", message, Map.of("fieldErrors", fieldErrors));
    }
}

// Not found exception
public class ResourceNotFoundException extends ErpException {
    public ResourceNotFoundException(String resourceType, Long id) {
        super("NOT_FOUND", 
              String.format("%s with id %d not found", resourceType, id));
    }
}

// Conflict exception
public class ConflictException extends ErpException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}

// Unauthorized exception
public class UnauthorizedException extends ErpException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}

// Forbidden exception
public class ForbiddenException extends ErpException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
```

### 3.2 Domain-Specific Exceptions

```java
// Finance domain
public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException(BigDecimal available, BigDecimal required) {
        super("INSUFFICIENT_FUNDS", 
              String.format("Insufficient funds: available=%s, required=%s", 
                           available, required),
              Map.of("available", available, "required", required));
    }
}

public class DuplicateInvoiceException extends ConflictException {
    public DuplicateInvoiceException(String invoiceNumber) {
        super(String.format("Invoice with number %s already exists", invoiceNumber));
    }
}

// Inventory domain
public class InsufficientInventoryException extends BusinessException {
    public InsufficientInventoryException(Long productId, int available, int required) {
        super("INSUFFICIENT_INVENTORY",
              String.format("Insufficient inventory for product %d: available=%d, required=%d",
                           productId, available, required),
              Map.of("productId", productId, "available", available, "required", required));
    }
}
```

## 4. Exception Handling Patterns

### 4.1 Service Layer Exception Handling

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    
    public Invoice create(CreateInvoiceCommand command) {
        // Validate business rules
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer", command.customerId()));
        
        if (!customer.isActive()) {
            throw new BusinessException("CUSTOMER_INACTIVE", 
                "Cannot create invoice for inactive customer");
        }
        
        // Check for duplicate
        invoiceRepository.findByTenantIdAndInvoiceNumber(
            command.tenantId(), command.invoiceNumber())
            .ifPresent(inv -> {
                throw new DuplicateInvoiceException(command.invoiceNumber());
            });
        
        // Create invoice
        Invoice invoice = Invoice.create(command);
        return invoiceRepository.save(invoice);
    }
}
```

### 4.2 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ErpException.class)
    public ResponseEntity<ErrorResponse> handleErpException(ErpException ex, 
                                                            HttpServletRequest request) {
        log.error("Business exception: {} | requestId: {}", 
                 ex.getMessage(), request.getAttribute("requestId"), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .error(ErrorDetail.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .requestId((String) request.getAttribute("requestId"))
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(getHttpStatus(ex.getErrorCode())).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed: {} | requestId: {}", 
                ex.getMessage(), request.getAttribute("requestId"));
        
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build())
            .toList();
        
        ErrorResponse error = ErrorResponse.builder()
            .error(ErrorDetail.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .details(Map.of("fieldErrors", fieldErrors))
                .requestId((String) request.getAttribute("requestId"))
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        log.warn("Access denied: {} | requestId: {}", 
                ex.getMessage(), request.getAttribute("requestId"));
        
        ErrorResponse error = ErrorResponse.builder()
            .error(ErrorDetail.builder()
                .code("FORBIDDEN")
                .message("Access denied")
                .requestId((String) request.getAttribute("requestId"))
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        log.error("Unexpected error: {} | requestId: {}", ex.getMessage(), requestId, ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .error(ErrorDetail.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .requestId(requestId)
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private HttpStatus getHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

### 4.3 Error Response DTOs

```java
@Data
@Builder
public class ErrorResponse {
    private ErrorDetail error;
}

@Data
@Builder
public class ErrorDetail {
    private String code;
    private String message;
    private Map<String, Object> details;
    private String requestId;
    private Instant timestamp;
}

@Data
@Builder
public class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
}
```

## 5. Exception Anti-Patterns

### 5.1 Prohibited Patterns

| Pattern | Problem | Solution |
|---------|---------|----------|
| Empty catch block | Swallows errors silently | Log and rethrow or handle properly |
| Catching `Exception` | Too broad, hides errors | Catch specific exceptions |
| `e.printStackTrace()` | Not structured, not logged | Use SLF4J logger |
| Returning null on error | Causes NPE downstream | Throw exception or use Optional |
| Generic error messages | Not actionable | Include context in message |
| Exposing stack traces | Security risk | Sanitize in production |
| Checked exceptions everywhere | Boilerplate code | Use runtime exceptions with documentation |

### 5.2 Example of Bad Practice

```java
// BAD
try {
    return invoiceRepository.findById(id).get();
} catch (Exception e) {
    return null;  // NPE later!
}
```

```java
// GOOD
try {
    return invoiceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
} catch (DataAccessException e) {
    throw new ErpException("DATABASE_ERROR", "Failed to fetch invoice", e);
}
```

## 6. Retry and Resilience

### 6.1 Retry Pattern

```java
@Service
@RequiredArgsConstructor
public class PaymentGatewayClient {
    private final WebClient webClient;
    
    @Retryable(
        value = {TransientDataAccessException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public PaymentResponse processPayment(PaymentRequest request) {
        return webClient.post()
            .uri("/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .block();
    }
    
    @Recover
    public PaymentResponse handleRetryFailure(TransientDataAccessException ex, 
                                              PaymentRequest request) {
        throw new BusinessException("PAYMENT_GATEWAY_UNAVAILABLE",
            "Payment gateway unavailable after retries", Map.of("request", request));
    }
}
```

### 6.2 Circuit Breaker

```java
@Service
@RequiredArgsConstructor
public class InventoryServiceClient {
    private final WebClient webClient;
    
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallback")
    public InventoryStatus checkInventory(Long productId, int quantity) {
        return webClient.get()
            .uri("/inventory/{productId}?quantity={quantity}", productId, quantity)
            .retrieve()
            .bodyToMono(InventoryStatus.class)
            .block();
    }
    
    public InventoryStatus fallback(Long productId, int quantity, Exception ex) {
        log.warn("Inventory service unavailable, using fallback for product: {}", productId);
        return InventoryStatus.unavailable();
    }
}
```

## 7. Exception Logging

### 7.1 What to Log

- **All exceptions:** At minimum, ERROR level
- **Business exceptions:** INFO level with context
- **Stack traces:** Always for unexpected exceptions
- **Context:** Tenant ID, user ID, request ID, operation

### 7.2 What NOT to Log

- Sensitive data (passwords, tokens, PII)
- Full request/response bodies
- Internal implementation details in production

### 7.3 Logging Template

```java
// Business exception - INFO level
log.info("Business exception: {} | tenantId: {} | userId: {} | details: {}", 
        ex.getMessage(), tenantId, userId, ex.getDetails());

// Technical exception - ERROR level with stack trace
log.error("Technical error processing invoice: {} | tenantId: {} | requestId: {}", 
         invoiceId, tenantId, requestId, ex);
```

## 8. Exception Handling Checklist

- [ ] Custom exception hierarchy defined
- [ ] Business exceptions extend `BusinessException`
- [ ] Global exception handler configured
- [ ] Error responses follow standard format
- [ ] No sensitive data in error messages
- [ ] Correlation ID included in all errors
- [ ] Appropriate HTTP status codes mapped
- [ ] Retry configured for transient failures
- [ ] Circuit breaker for external dependencies
- [ ] Stack traces logged server-side only
