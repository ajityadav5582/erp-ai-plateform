# Common Events Library

## Overview

The `common-events` library provides event infrastructure for the ERP AI Platform. It defines abstractions for domain events, integration events, event publishing, consuming, serialization, and versioning.

## Purpose

This library provides the foundational event abstractions that all microservices use for event-driven communication. It does not implement any specific messaging infrastructure (like Kafka) - that belongs to platform services.

## Key Components

### Domain Events

- **DomainEvent** - Base interface for all domain events
- **EventMetadata** - Metadata for events (tenant, correlation ID, timestamps)
- **EventPublisher** - Interface for publishing events
- **EventConsumer** - Interface for consuming events

### Integration Events

- **IntegrationEvent** - Interface for integration events used for cross-service communication

### Serialization

- **EventSerializer** - Interface for event serialization/deserialization

### Versioning

- **EventVersion** - Event versioning utilities
- **EventNaming** - Event naming conventions

## Usage

### Publishing a Domain Event

```java
// Define a domain event
public record InvoiceCreatedEvent(UUID invoiceId, BigDecimal amount) implements DomainEvent {
    @Override
    public String getAggregateId() {
        return invoiceId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Invoice";
    }

    @Override
    public Instant getOccurredOn() {
        return Instant.now();
    }
}

// Publish the event
eventPublisher.publish(new InvoiceCreatedEvent(invoiceId, amount));
```

### Consuming an Event

```java
@Component
public class InvoiceEventHandler implements EventConsumer<InvoiceCreatedEvent> {
    @Override
    public void consume(InvoiceCreatedEvent event) {
        // Handle the event
    }
}
```

### Event Naming Convention

Events follow the naming convention: `{domain}.{aggregate}.{action}`

Examples:
- `invoice.created`
- `invoice.updated`
- `invoice.cancelled`
- `payment.processed`

## Event Versioning

Events are versioned to support schema evolution:

```java
String versionedName = EventNaming.versionedName("invoice.created", "1");
// Result: "invoice.created.v1"
```

## Dependencies

- Spring Boot 3.x
- Java 21

## Testing

This library includes unit tests for all components. Run tests with:

```bash
./gradlew :libraries:common-events:test
```
