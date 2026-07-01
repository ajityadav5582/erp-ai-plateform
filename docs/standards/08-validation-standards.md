# Validation Standards

## 1. Purpose

These standards ensure consistent, comprehensive, and secure input validation across the ERP SaaS platform. Proper validation prevents security vulnerabilities, data corruption, and poor user experience.

## 2. Validation Principles

- **Defense in depth:** Validate at multiple layers (client, API, service, database)
- **Fail fast:** Validate early, reject invalid input immediately
- **Whitelist over blacklist:** Accept known-good, reject unknown
- **Context-aware:** Validate based on business context (tenant, user role)
- **Clear errors:** Provide specific, actionable error messages
- **Internationalization:** Support multiple locales for error messages

## 3. Validation Layers

### 3.1 Layer Overview

```
Client-side Validation → API Gateway → Controller → Service → Repository → Database
       (UX)                (Rate limit)   (DTO)     (Business)  (Query)   (Constraints)
```

### 3.2 Layer Responsibilities

| Layer | Responsibility | Technology |
|-------|---------------|------------|
| Client | UX validation, format hints | JavaScript/TypeScript |
| API Gateway | Rate limiting, basic auth | Kong/Spring Cloud Gateway |
| Controller | DTO validation, type checking | Jakarta Validation |
| Service | Business rules, cross-field validation | Custom validators |
| Repository | Query constraints | JPA/Hibernate |
| Database | Integrity constraints | CHECK, FOREIGN KEY, UNIQUE |

## 4. Jakarta Validation (Bean Validation)

### 4.1 Request DTO Validation

```java
@Data
@Builder
public class CreateInvoiceRequest {
    @NotBlank(message = "Invoice number is required")
    @Size(min = 3, max = 50, message = "Invoice number must be 3-50 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Invoice number must be uppercase alphanumeric")
    private String invoiceNumber;
    
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;
    
    @NotNull(message = "Total is required")
    @DecimalMin(value = "0.01", message = "Total must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Total format invalid")
    private BigDecimal total;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase ISO code")
    private String currency;
    
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
    
    @Valid
    @NotEmpty(message = "At least one line item is required")
    private List<CreateInvoiceLineItemRequest> lineItems;
}

@Data
@Builder
public class CreateInvoiceLineItemRequest {
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Unit price format invalid")
    private BigDecimal unitPrice;
}
```

### 4.2 Custom Validators

```java
// Custom annotation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaxIdValidator.class)
public @interface ValidTaxId {
    String message() default "Invalid tax ID format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class ValidTaxIdValidator implements ConstraintValidator<ValidTaxId, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotBlank for required check
        }
        // Validate tax ID format (example: US EIN)
        return value.matches("^\\d{2}-\\d{7}$");
    }
}

// Usage
@Data
public class CompanyRequest {
    @NotBlank
    @ValidTaxId
    private String taxId;
}
```

### 4.3 Cross-Field Validation

```java
public class InvoiceRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return CreateInvoiceRequest.class.equals(clazz);
    }
    
    @Override
    public Set<ConstraintViolation<T>> validate(T value, Class<?>... groups) {
        CreateInvoiceRequest request = (CreateInvoiceRequest) value;
        Set<ConstraintViolation<T>> violations = new HashSet<>();
        
        // Cross-field validation: total = sum of line items
        if (request.getLineItems() != null) {
            BigDecimal calculatedTotal = request.getLineItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (request.getTotal().compareTo(calculatedTotal) != 0) {
                violations.add(createViolation("total", 
                    "Total must equal sum of line items"));
            }
        }
        
        return violations;
    }
}
```

## 5. Service Layer Validation

### 5.1 Business Rule Validation

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    
    public Invoice create(CreateInvoiceCommand command) {
        // Validate customer exists and is active
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer", command.customerId()));
        
        if (!customer.isActive()) {
            throw new BusinessException("CUSTOMER_INACTIVE",
                "Cannot create invoice for inactive customer");
        }
        
        // Validate tenant access
        if (!customer.getTenantId().equals(command.tenantId())) {
            throw new ForbiddenException("Customer does not belong to tenant");
        }
        
        // Validate duplicate invoice number
        invoiceRepository.findByTenantIdAndInvoiceNumber(
            command.tenantId(), command.invoiceNumber())
            .ifPresent(inv -> {
                throw new DuplicateInvoiceException(command.invoiceNumber());
            });
        
        // Validate credit limit
        BigDecimal outstandingInvoices = invoiceRepository.sumTotalByCustomerIdAndStatus(
            command.customerId(), InvoiceStatus.PENDING);
        
        if (customer.getCreditLimit().compareTo(outstandingInvoices.add(command.total())) < 0) {
            throw new BusinessException("CREDIT_LIMIT_EXCEEDED",
                String.format("Credit limit exceeded: limit=%s, outstanding=%s, requested=%s",
                    customer.getCreditLimit(), outstandingInvoices, command.total()));
        }
        
        // Create invoice
        return invoiceRepository.save(Invoice.create(command));
    }
}
```

### 5.2 Validation Groups

```java
// Define validation groups
public interface Create {}
public interface Update {}
public interface Patch {}

// Apply groups to DTO
@Data
public class CustomerRequest {
    @NotBlank(groups = {Create.class, Update.class})
    @Email(groups = {Create.class, Update.class})
    private String email;
    
    @NotBlank(groups = Create.class)
    @Size(min = 10, max = 100, groups = {Create.class, Update.class})
    private String address;
    
    @Null(groups = Create.class)  // ID must be null on create
    @NotNull(groups = Update.class)  // ID must be present on update
    private Long id;
}

// Use in controller
@PostMapping
public ResponseEntity<CustomerResponse> create(
        @Validated(Create.class) @RequestBody CustomerRequest request) {
    // ...
}

@PutMapping("/{id}")
public ResponseEntity<CustomerResponse> update(
        @PathVariable Long id,
        @Validated(Update.class) @RequestBody CustomerRequest request) {
    // ...
}
```

## 6. Database Constraints

### 6.1 Column Constraints

```sql
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Check constraints
    CONSTRAINT chk_invoices_total_positive CHECK (total > 0),
    CONSTRAINT chk_invoices_subtotal_positive CHECK (subtotal >= 0),
    CONSTRAINT chk_invoices_tax_non_negative CHECK (tax_amount >= 0),
    CONSTRAINT chk_invoices_currency_length CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_invoices_status_valid 
        CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'CANCELLED', 'OVERDUE')),
    CONSTRAINT chk_invoices_due_date 
        CHECK (due_date >= created_at::DATE)
);
```

### 6.2 Unique Constraints

```sql
-- Unique invoice number per tenant
CREATE UNIQUE INDEX uk_invoices_tenant_number 
ON invoices(tenant_id, invoice_number) 
WHERE deleted_at IS NULL;

-- Unique customer email per tenant
CREATE UNIQUE INDEX uk_customers_tenant_email 
ON customers(tenant_id, LOWER(email)) 
WHERE deleted_at IS NULL;
```

## 7. Security Validation

### 7.1 Input Sanitization

```java
@Component
public class InputSanitizer {
    public String sanitize(String input) {
        if (input == null) return null;
        // Remove control characters
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                    .trim();
    }
    
    public boolean containsSqlInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("select") && lower.contains("from") ||
               lower.contains("union") && lower.contains("select") ||
               lower.contains("drop") && lower.contains("table") ||
               lower.contains("delete") && lower.contains("from") ||
               lower.contains("insert") && lower.contains("into");
    }
    
    public boolean containsXss(String input) {
        if (input == null) return false;
        return input.matches(".*<script.*?>.*|.*javascript:.*|.*on\\w+\\s*=.*");
    }
}
```

### 7.2 SQL Injection Prevention

```java
// GOOD: Parameterized query
@Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND i.invoiceNumber = :invoiceNumber")
Optional<Invoice> findByTenantIdAndInvoiceNumber(
    @Param("tenantId") Long tenantId, 
    @Param("invoiceNumber") String invoiceNumber);

// BAD: String concatenation
@Query("SELECT i FROM Invoice i WHERE i.tenantId = " + tenantId + " AND i.invoiceNumber = '" + invoiceNumber + "'")
```

### 7.3 XSS Prevention

```java
@Component
public class XssPreventionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                String value = super.getParameter(name);
                return sanitize(value);
            }
            
            @Override
            public String[] getParameterValues(String name) {
                String[] values = super.getParameterValues(name);
                if (values != null) {
                    return Arrays.stream(values)
                        .map(this::sanitize)
                        .toArray(String[]::new);
                }
                return values;
            }
            
            private String sanitize(String value) {
                if (value == null) return null;
                return HtmlUtils.htmlEscape(value);
            }
        };
        filterChain.doFilter(wrappedRequest, response);
    }
}
```

## 8. Validation Error Response

### 8.1 Standard Error Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "fieldErrors": [
        {
          "field": "invoiceNumber",
          "message": "Invoice number must be 3-50 characters",
          "rejectedValue": "AB"
        },
        {
          "field": "total",
          "message": "Total must be greater than zero",
          "rejectedValue": "-100.00"
        }
      ]
    },
    "requestId": "req-abc123",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 8.2 Field Error DTO

```java
@Data
@Builder
public class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
}
```

## 9. Validation Checklist

- [ ] DTOs have Jakarta Validation annotations
- [ ] Custom validators for complex rules
- [ ] Cross-field validation implemented
- [ ] Business rules validated in service layer
- [ ] Database constraints defined
- [ ] Error messages are clear and actionable
- [ ] No sensitive data in error messages
- [ ] Internationalization support for errors
- [ ] Input sanitization for XSS/SQL injection
- [ ] Validation groups for different operations
