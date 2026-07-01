# Common Observability Library

## Overview

The `common-observability` library provides reusable observability components for the ERP AI Platform. It includes correlation ID support, structured logging utilities, logging constants, trace helpers, and metrics abstractions.

## Purpose

This library provides the foundational observability abstractions that all microservices use for monitoring and tracing. It does not implement Prometheus or OpenTelemetry yet - those belong to platform services.

## Key Components

### Correlation ID

- **CorrelationIdUtils** - Utilities for generating and managing correlation IDs

### Logging

- **StructuredLoggingUtils** - Utilities for structured logging
- **LoggingConstants** - Standardized logging constants (MDC keys)

### Tracing

- **TraceHelpers** - Utilities for distributed tracing

### Metrics

- **MetricsAbstractions** - Interface for metrics collection

## Usage

### Correlation ID

```java
// Generate a new correlation ID
String correlationId = CorrelationIdUtils.generate();

// Validate a correlation ID
if (CorrelationIdUtils.isValid(correlationId)) {
    // ...
}
```

### Structured Logging

```java
// Create a structured log message
Map<String, Object> logEntry = StructuredLoggingUtils.logMessage(
    "Processing invoice",
    Map.entry("invoiceId", invoiceId),
    Map.entry("tenantId", tenantId)
);

// Create an error log entry
Map<String, Object> errorLog = StructuredLoggingUtils.errorLog(
    "Failed to process invoice",
    exception
);
```

### Tracing

```java
// Generate trace and span IDs
String traceId = TraceHelpers.generateTraceId();
String spanId = TraceHelpers.generateSpanId();

// Validate trace ID
if (TraceHelpers.isValidTraceId(traceId)) {
    // ...
}
```

### Metrics

```java
// Metrics will be implemented by platform services
// This interface provides the abstraction
metricsAbstractions.incrementCounter("invoice.created");
metricsAbstractions.recordTimer("invoice.processing.time", durationMs);
```

## Dependencies

- Spring Boot 3.x
- Java 21
- common-core

## Testing

This library includes unit tests for all components. Run tests with:

```bash
./gradlew :libraries:common-observability:test
```
