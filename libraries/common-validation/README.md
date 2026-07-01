# Common Validation Library

## Overview

The `common-validation` library provides reusable validation annotations, custom validators, error codes, and validation utilities for the ERP AI Platform.

## Purpose

This library provides standardized validation components that all microservices can use for input validation. It includes custom annotations for common validation scenarios and utilities for building validation constraints.

## Key Components

### Validation Annotations

- **NotBlank** - Validates that a string is not null and not blank
- **NotNegative** - Validates that a numeric value is not negative
- **Positive** - Validates that a numeric value is strictly positive
- **ValidTenantId** - Validates that a string is a valid tenant ID
- **ValidEmail** - Validates that a string is a valid email address
- **ValidPhone** - Validates that a string is a valid phone number
- **ValidNepaliPhone** - Validates that a string is a valid Nepali phone number

### Error Codes

- **ValidationErrorCode** - Enumeration of standardized validation error codes
- **ValidationError** - Class representing a validation error with field, code, and message

### Utilities

- **ValidationUtils** - Utility methods for common validation operations
- **ConstraintHelper** - Helper methods for building validation constraints

## Usage

### Using Validation Annotations

```java
public record CreateInvoiceRequest(
    @NotBlank(message = "Customer name is required")
    String customerName,

    @ValidEmail
    String customerEmail,

    @ValidNepaliPhone
    String customerPhone,

    @Positive
    BigDecimal amount,

    @NotNegative
    BigDecimal discount
) {}
```

### Using Validation Utilities

```java
List<ValidationError> errors = ValidationUtils.combine(
    ValidationUtils.validateNotBlank(request.customerName(), "customerName"),
    ValidationUtils.validateEmail(request.customerEmail(), "customerEmail"),
    ValidationUtils.validatePositive(request.amount(), "amount")
);

if (!errors.isEmpty()) {
    throw new ValidationException(errors);
}
```

### Using Constraint Helper

```java
List<ValidationError> errors = new ArrayList<>();
if (request.customerName() == null) {
    errors.add(ConstraintHelper.required("customerName"));
}
if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
    errors.add(ConstraintHelper.custom("amount", "Amount must be positive"));
}
```

## Dependencies

- Spring Boot 3.x
- Jakarta Validation API
- Java 21
- common-core

## Testing

This library includes unit tests for all components. Run tests with:

```bash
./gradlew :libraries:common-validation:test
```
