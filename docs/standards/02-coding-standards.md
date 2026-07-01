# Coding Standards

## 1. Purpose

These coding standards ensure consistency, readability, and maintainability across the ERP SaaS platform codebase. All Java and Spring Boot code must adhere to these standards.

## 2. General Principles

- **Readability first:** Code is read more than written
- **KISS (Keep It Simple, Stupid):** Prefer simple solutions over clever ones
- **DRY (Don't Repeat Yourself):** Extract common logic into reusable components
- **YAGNI (You Aren't Gonna Need It):** Don't build for hypothetical future requirements
- **SOLID principles:** Apply consistently across all modules

## 3. Java Coding Standards

### 3.1 Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Package | Lowercase, dot-separated | `com.erp.finance.application` |
| Class | PascalCase | `InvoiceService`, `CustomerRepository` |
| Interface | PascalCase, descriptive | `PaymentProcessor`, `InvoiceRepository` |
| Method | camelCase, verb-first | `calculateTotal()`, `findByTenantId()` |
| Variable | camelCase | `invoiceTotal`, `customerId` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| Enum | PascalCase | `InvoiceStatus`, `PaymentMethod` |
| Enum values | UPPER_SNAKE_CASE | `PENDING`, `APPROVED`, `REJECTED` |

### 3.2 Class Design

- **Single Responsibility:** Each class should have one reason to change
- **Maximum class size:** 500 lines (excluding tests)
- **Maximum method size:** 50 lines
- **Maximum parameter count:** 5 parameters (use parameter objects for more)
- **Immutability:** Prefer immutable objects; use `final` where possible
- **Composition over inheritance:** Favor composition for code reuse

### 3.3 Access Modifiers

- **Fields:** Always `private`
- **Methods:** `public` only for API surface; `protected` for inheritance; `private` for internal use
- **Classes:** `public` only for API-exposed classes; package-private by default
- **Avoid:** `public` fields, package-private fields in public classes

### 3.4 Code Organization

```java
// Class structure order:
// 1. Constants
// 2. Static fields
// 3. Instance fields
// 4. Constructors
// 5. Public methods
// 6. Protected methods
// 7. Package-private methods
// 8. Private methods
// 9. Inner classes
```

### 3.5 Comments and Documentation

- **Javadoc required for:** All public classes, interfaces, methods, and fields
- **Inline comments:** Only for non-obvious logic; explain "why", not "what"
- **TODO comments:** Must include ticket/issue number: `// TODO(ERP-123): Implement retry logic`
- **Avoid:** Commented-out code, redundant comments

### 3.6 Exception Handling

- **Checked exceptions:** For recoverable conditions (e.g., `InsufficientInventoryException`)
- **Runtime exceptions:** For programming errors (e.g., `IllegalArgumentException`, `NullPointerException`)
- **Never:** Catch generic `Exception` or `Throwable` without rethrowing
- **Always:** Include context in exception messages

### 3.7 Streams and Lambdas

- **Prefer:** Method references over lambdas when possible
- **Avoid:** Deeply nested streams (max 3 levels)
- **Use:** `Optional` to avoid null checks, but don't overuse
- **Side effects:** Keep stream operations pure; avoid mutations

```java
// Good
list.stream()
    .filter(Invoice::isPending)
    .map(Invoice::getTotal)
    .reduce(BigDecimal::add)
    .orElse(BigDecimal.ZERO);

// Bad - deeply nested
result = list.stream()
    .filter(...)
    .map(...)
    .filter(...)
    .map(...)
    .collect(...);
```

### 3.8 Concurrency

- **Prefer:** `CompletableFuture` over raw threads
- **Avoid:** `synchronized` blocks; use `ReentrantLock` or concurrent utilities
- **Thread safety:** Document thread-safety guarantees in Javadoc
- **Immutable objects:** Default to immutable for shared state

## 4. Spring Boot Specific Standards

### 4.1 Dependency Injection

- **Constructor injection:** Always preferred over field injection
- **Lombok:** Use `@RequiredArgsConstructor` for constructor injection
- **Avoid:** `@Autowired` on fields (use constructor injection instead)

```java
// Good
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
}

// Bad
@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;
}
```

### 4.2 Configuration

- **Externalize:** All configurable values in `application.yml` or environment variables
- **Type-safe config:** Use `@ConfigurationProperties` with validation
- **Profiles:** Use Spring profiles for environment-specific config
- **Secrets:** Never hardcode secrets; use environment variables or vault

```java
@ConfigurationProperties(prefix = "erp.invoice")
@Data
@Validated
public class InvoiceProperties {
    @NotBlank
    private String defaultCurrency;
    
    @Min(1)
    @Max(365)
    private int defaultDueDays;
    
    @Min(0)
    private BigDecimal minimumAmount;
}
```

### 4.3 REST Controllers

- **Thin controllers:** Controllers should delegate to services
- **DTOs:** Use separate request/response DTOs, not entities
- **Validation:** Use `@Valid` on request bodies
- **Exception handling:** Use `@RestControllerAdvice` for global exception handling
- **Status codes:** Return appropriate HTTP status codes

```java
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### 4.4 Data Access

- **Repositories:** Extend `JpaRepository` or custom interfaces
- **Query methods:** Use derived query methods; use `@Query` only when necessary
- **Pagination:** Always use `Pageable` for list endpoints
- **Projections:** Use interface-based projections for read-only queries
- **Transactions:** Use `@Transactional` at service layer, not repository

```java
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND i.total > :amount")
    Page<Invoice> findLargeInvoices(@Param("tenantId") Long tenantId, 
                                     @Param("amount") BigDecimal amount, 
                                     Pageable pageable);
}
```

### 4.5 Testing

- **Test naming:** `methodName_condition_expectedResult`
- **Unit tests:** One test class per production class
- **Mocking:** Use Mockito; mock external dependencies only
- **Assertions:** Use AssertJ for fluent assertions
- **Coverage:** Minimum 80% line coverage for new code

```java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    @Mock
    private InvoiceRepository invoiceRepository;
    
    @InjectMocks
    private InvoiceService invoiceService;
    
    @Test
    void createInvoice_whenValidRequest_shouldReturnInvoice() {
        // Given
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
            .customerId(1L)
            .total(new BigDecimal("100.00"))
            .build();
        
        Invoice invoice = Invoice.builder()
            .id(1L)
            .customerId(1L)
            .total(new BigDecimal("100.00"))
            .build();
        
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        
        // When
        InvoiceResponse response = invoiceService.create(request);
        
        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTotal()).isEqualTo(new BigDecimal("100.00"));
        verify(invoiceRepository).save(any(Invoice.class));
    }
}
```

## 5. Code Quality Tools

### 5.1 Static Analysis

- **SpotBugs:** Enable all bug patterns
- **PMD:** Enable recommended rules + custom rules
- **Checkstyle:** Use Google Java Style Guide with customizations
- **SonarQube:** Enforce quality gate on PRs

### 5.2 Code Formatting

- **Formatter:** Google Java Format (AOSP style, 2-space indent)
- **Import order:** Static imports first, then java.*, then javax.*, then third-party, then project
- **Line length:** 120 characters maximum
- **Trailing whitespace:** Not allowed

### 5.3 IDE Configuration

- **EditorConfig:** Use project `.editorconfig` file
- **IntelliJ IDEA:** Import `intellij-java-google-style.xml`
- **Eclipse:** Use Google Java Format plugin

## 6. Prohibited Patterns

| Pattern | Reason | Alternative |
|---------|--------|-------------|
| `public` fields | Breaks encapsulation | Use getters/setters or records |
| `null` returns | Causes NPEs | Use `Optional` or empty collections |
| Magic numbers | Reduces readability | Use named constants |
| God classes | Violates SRP | Split into focused classes |
| Anemic domain models | Misses OOP benefits | Add behavior to domain objects |
| `System.out/err` | Not structured | Use SLF4J logging |
| Hardcoded URLs | Reduces portability | Use configuration properties |
| `@Transactional` on controllers | Wrong layer | Move to service layer |
| Returning entities from controllers | Leaks internals | Use DTOs |

## 7. Code Review Checklist

- [ ] Follows naming conventions
- [ ] No code smells (long methods, large classes)
- [ ] Proper exception handling
- [ ] No hardcoded values
- [ ] Unit tests included with adequate coverage
- [ ] Javadoc for public APIs
- [ ] No security vulnerabilities
- [ ] Performance considerations addressed
- [ ] Multi-tenant isolation maintained
