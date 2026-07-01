# Testing Standards

## 1. Purpose

These standards ensure comprehensive, reliable, and maintainable testing across the ERP SaaS platform. Testing is a shared responsibility and integral to the development process.

## 2. Testing Principles

- **Test pyramid:** More unit tests, fewer integration tests, minimal UI tests
- **Fast feedback:** Unit tests run in milliseconds, integration in seconds
- **Isolation:** Tests should not depend on external systems
- **Repeatability:** Tests must produce same results every time
- **Independence:** Tests should not depend on each other
- **Readability:** Tests serve as documentation
- **Maintainability:** Tests should be easy to update

## 3. Test Pyramid

```
        /\
       /UI\         E2E Tests (5%)
      /----\
     /Inte-\       Integration Tests (15%)
    /gration\
   /--------\
  /   Unit   \     Unit Tests (80%)
 /____________\
```

## 4. Unit Testing

### 4.1 Scope

- Test individual classes/methods in isolation
- Mock all external dependencies
- Focus on business logic
- Fast execution (< 10ms per test)

### 4.2 Naming Convention

```java
// Pattern: methodName_condition_expectedResult
@Test
void calculateTotal_whenItemsPresent_shouldReturnSum() {
    // ...
}

@Test
void calculateTotal_whenNoItems_shouldReturnZero() {
    // ...
}

@Test
void calculateTotal_whenNegativeItem_shouldThrowException() {
    // ...
}
```

### 4.3 Structure (AAA Pattern)

```java
@Test
void createInvoice_whenValidRequest_shouldReturnInvoice() {
    // Arrange - Set up test data and mocks
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
    
    // Act - Execute the method under test
    InvoiceResponse response = invoiceService.create(request);
    
    // Assert - Verify the results
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getTotal()).isEqualTo(new BigDecimal("100.00"));
    verify(invoiceRepository).save(any(Invoice.class));
}
```

### 4.4 Mocking Guidelines

```java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private InvoiceService invoiceService;
    
    @Test
    void createInvoice_whenCustomerNotFound_shouldThrowException() {
        // Arrange
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
            .customerId(999L)
            .total(new BigDecimal("100.00"))
            .build();
        
        when(customerRepository.findById(999L))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> invoiceService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer");
        
        verify(invoiceRepository, never()).save(any());
    }
}
```

### 4.5 Assertion Libraries

- **AssertJ:** Fluent assertions (preferred)
- **Hamcrest:** Matcher-based assertions
- **JUnit 5:** Built-in assertions

```java
// AssertJ (preferred)
assertThat(response)
    .isNotNull()
    .extracting(InvoiceResponse::getId, InvoiceResponse::getTotal)
    .containsExactly(1L, new BigDecimal("100.00"));

// Avoid JUnit 4 style
assertEquals(1L, response.getId());  // Less readable
```

## 5. Integration Testing

### 5.1 Scope

- Test multiple components together
- Use real or embedded databases
- Test repository and service interactions
- Verify transaction boundaries

### 5.2 Configuration

```java
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoiceServiceIntegrationTest {
    
    @Autowired
    private InvoiceService invoiceService;
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
    }
    
    @Test
    void createInvoice_shouldPersistToDatabase() {
        // Arrange
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
            .customerId(1L)
            .total(new BigDecimal("100.00"))
            .build();
        
        // Act
        InvoiceResponse response = invoiceService.create(request);
        
        // Assert
        assertThat(response.getId()).isNotNull();
        
        Invoice persisted = invoiceRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getTotal()).isEqualTo(new BigDecimal("100.00"));
    }
}
```

### 5.3 Test Containers

```java
@Testcontainers
@SpringBootTest
class InvoiceServicePostgresIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    // Tests...
}
```

## 6. API Testing

### 6.1 REST Assured

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InvoiceControllerIT {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }
    
    @Test
    void createInvoice_whenValidRequest_shouldReturn201() {
        given()
            .header("Content-Type", "application/json")
            .header("X-Tenant-ID", "1")
            .body("""
                {
                    "invoiceNumber": "INV-001",
                    "customerId": 1,
                    "total": "100.00",
                    "currency": "USD",
                    "dueDate": "2024-02-15",
                    "lineItems": [
                        {
                            "description": "Product A",
                            "quantity": 1,
                            "unitPrice": "100.00"
                        }
                    ]
                }
                """)
        .when()
            .post("/invoices")
        .then()
            .statusCode(201)
            .body("invoiceNumber", equalTo("INV-001"))
            .body("total", equalTo("100.00"));
    }
}
```

### 6.2 MockMvc

```java
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private InvoiceService invoiceService;
    
    @Test
    void getInvoice_whenExists_shouldReturn200() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
            .id(1L)
            .invoiceNumber("INV-001")
            .total(new BigDecimal("100.00"))
            .build();
        
        when(invoiceService.getById(1L)).thenReturn(response);
        
        mockMvc.perform(get("/api/v1/invoices/1")
                .header("X-Tenant-ID", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
            .andExpect(jsonPath("$.total").value("100.00"));
    }
}
```

## 7. Contract Testing

### 7.1 Spring Cloud Contract

```yaml
# src/test/resources/contracts/invoice/create-invoice.shouldCreate.groovy
package contracts.invoice

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "create_invoice_should_create"
    description "Creates a new invoice"
    
    request {
        method POST()
        url "/api/v1/invoices"
        headers {
            contentType(applicationJson())
            header("X-Tenant-ID", "1")
        }
        body(
            invoiceNumber: "INV-001",
            customerId: 1,
            total: "100.00",
            currency: "USD",
            dueDate: "2024-02-15"
        )
    }
    
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body(
            id: anyPositiveLong(),
            invoiceNumber: "INV-001",
            total: "100.00",
            currency: "USD"
        )
    }
}
```

## 8. Test Data Management

### 8.1 Test Fixtures

```java
@Component
public class InvoiceTestFixtures {
    
    public Invoice createInvoice(Long tenantId, Long customerId) {
        return Invoice.builder()
            .tenantId(tenantId)
            .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8))
            .customerId(customerId)
            .total(new BigDecimal("100.00"))
            .currency("USD")
            .status(InvoiceStatus.PENDING)
            .dueDate(LocalDate.now().plusDays(30))
            .build();
    }
    
    public CreateInvoiceRequest createInvoiceRequest() {
        return CreateInvoiceRequest.builder()
            .invoiceNumber("INV-001")
            .customerId(1L)
            .total(new BigDecimal("100.00"))
            .currency("USD")
            .dueDate(LocalDate.now().plusDays(30))
            .lineItems(List.of(
                CreateInvoiceLineItemRequest.builder()
                    .description("Product A")
                    .quantity(1)
                    .unitPrice(new BigDecimal("100.00"))
                    .build()
            ))
            .build();
    }
}
```

### 8.2 Test Data Builders

```java
public class InvoiceTestBuilder {
    private Long tenantId = 1L;
    private Long customerId = 1L;
    private BigDecimal total = new BigDecimal("100.00");
    private InvoiceStatus status = InvoiceStatus.PENDING;
    
    public InvoiceTestBuilder withTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    public InvoiceTestBuilder withTotal(BigDecimal total) {
        this.total = total;
        return this;
    }
    
    public Invoice build() {
        return Invoice.builder()
            .tenantId(tenantId)
            .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8))
            .customerId(customerId)
            .total(total)
            .status(status)
            .build();
    }
}

// Usage
Invoice invoice = new InvoiceTestBuilder()
    .withTenantId(2L)
    .withTotal(new BigDecimal("500.00"))
    .build();
```

## 9. Test Coverage

### 9.1 Coverage Requirements

| Type | Minimum Coverage |
|------|-----------------|
| Unit tests | 80% line, 70% branch |
| Integration tests | 60% line |
| Overall | 75% line |

### 9.2 Excluded from Coverage

- DTOs (getters/setters)
- Configuration classes
- Generated code
- Test classes themselves

### 9.3 JaCoCo Configuration

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/*Configuration.*</exclude>
        </excludes>
    </configuration>
</plugin>
```

## 10. Performance Testing

### 10.1 Load Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvoiceLoadTest {
    
    @Test
    void createInvoices_underLoad_shouldMaintainPerformance() {
        // Use Gatling or JMeter for load testing
        // Target: 1000 requests/second with < 200ms p95 latency
    }
}
```

### 10.2 Gatling Example

```scala
class InvoiceSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
  
  val scn = scenario("Create Invoice")
    .exec(
      http("Create Invoice")
        .post("/api/v1/invoices")
        .header("X-Tenant-ID", "1")
        .body(StringBody("""{...}""")).asJson
        .check(status.is(201))
    )
  
  setUp(
    scn.inject(rampUsersPerSec(10) to 100 during (60 seconds))
  ).protocols(httpProtocol)
}
```

## 11. Test Automation

### 11.1 CI Pipeline

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with: { java-version: '17' }
      - run: ./gradlew test
      - uses: actions/upload-artifact@v3
        with: { name: test-results, path: build/reports/tests }
  
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env: { POSTGRES_PASSWORD: test }
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with: { java-version: '17' }
      - run: ./gradlew integrationTest
```

## 12. Testing Checklist

- [ ] Unit tests for all business logic
- [ ] Integration tests for repository and service interactions
- [ ] API tests for all endpoints
- [ ] Contract tests for service boundaries
- [ ] Test coverage meets minimum requirements
- [ ] Tests are independent and repeatable
- [ ] No flaky tests
- [ ] Test data is managed properly
- [ ] Performance tests for critical paths
- [ ] Security tests for authentication/authorization
