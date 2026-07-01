# Common Testing Library

## Overview

The `common-testing` library provides testing utilities for the ERP AI Platform. It includes base test classes, test containers support, mock factories, test data builders, and common assertions.

## Purpose

This library provides standardized testing utilities that all microservices can use for unit and integration testing. It ensures consistency across the platform's test suite.

## Key Components

### Base Test Classes

- **BaseIntegrationTest** - Base annotation for integration tests
- **BaseUnitTest** - Base annotation for unit tests

### Test Containers

- **TestContainersConfig** - Pre-configured test containers (PostgreSQL)

### Mock Factories

- **MockFactories** - Factory methods for creating mocks and spies

### Test Data Builders

- **TestDataBuilders** - Builder methods for creating test data

### Common Assertions

- **CommonAssertions** - Custom assertions for platform types

## Usage

### Integration Tests

```java
@BaseIntegrationTest
class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldCreateInvoice() {
        // Test implementation
    }
}
```

### Unit Tests

```java
@BaseUnitTest
class InvoiceServiceUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void shouldCreateInvoice() {
        // Test implementation
    }
}
```

### Using Test Data Builders

```java
@Test
void shouldCreateInvoice() {
    // Given
    UUID invoiceId = TestDataBuilders.uuid();
    BigDecimal amount = TestDataBuilders.amount(100.0);

    // When
    Invoice invoice = invoiceService.createInvoice(invoiceId, amount);

    // Then
    assertThat(invoice).isNotNull();
}
```

### Using Mock Factories

```java
@Test
void shouldUseMockFactories() {
    // Given
    UUID fixedUuid = MockFactories.fixedUuid();
    SomeService mockService = MockFactories.mock(SomeService.class);

    // When/Then
    // ...
}
```

### Using Common Assertions

```java
@Test
void shouldAssertUuid() {
    // Given
    UUID uuid = TestDataBuilders.uuid();

    // Then
    CommonAssertions.assertThat(uuid)
        .isNotNull()
        .isNotNil();
}
```

## Dependencies

- Spring Boot 3.x
- Test Containers
- Mockito
- AssertJ
- JUnit 5
- Java 21
- common-core

## Testing

This library includes unit tests for all components. Run tests with:

```bash
./gradlew :libraries:common-testing:test
```
