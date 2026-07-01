package com.erp.platform.events.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DomainEvent} implementations.
 */
class DomainEventTest {

    @Test
    @DisplayName("Should create domain event with all fields")
    void shouldCreateDomainEventWithAllFields() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "Invoice";
        String eventType = "InvoiceCreated";
        Instant occurredOn = Instant.now();

        // When
        TestDomainEvent event = new TestDomainEvent(
            eventId, aggregateId, aggregateType, eventType, occurredOn
        );

        // Then
        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId.toString());
        assertThat(event.getAggregateType()).isEqualTo(aggregateType);
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getOccurredOn()).isEqualTo(occurredOn);
    }

    @Test
    @DisplayName("Should create domain event with default occurredOn")
    void shouldCreateDomainEventWithDefaultOccurredOn() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        // When
        TestDomainEvent event = new TestDomainEvent(
            eventId, aggregateId, "Invoice", "InvoiceCreated"
        );

        // Then
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOccurredOn()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should create event metadata with all fields")
    void shouldCreateEventMetadataWithAllFields() {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-123";
        String correlationId = "corr-456";

        // When
        EventMetadata metadata = EventMetadata.builder()
            .eventId(eventId)
            .tenantId(tenantId)
            .correlationId(correlationId)
            .source("test-service")
            .build();

        // Then
        assertThat(metadata.eventId()).isEqualTo(eventId);
        assertThat(metadata.tenantId()).isEqualTo(tenantId);
        assertThat(metadata.correlationId()).isEqualTo(correlationId);
        assertThat(metadata.source()).isEqualTo("test-service");
    }

    @Test
    @DisplayName("Should create event metadata with defaults")
    void shouldCreateEventMetadataWithDefaults() {
        // When
        EventMetadata metadata = EventMetadata.builder().build();

        // Then
        assertThat(metadata.eventId()).isNotNull();
        assertThat(metadata.tenantId()).isNull();
        assertThat(metadata.correlationId()).isNull();
        assertThat(metadata.source()).isNull();
    }

    @Test
    @DisplayName("Should create event metadata with occurredOn")
    void shouldCreateEventMetadataWithOccurredOn() {
        // Given
        Instant occurredOn = Instant.now();

        // When
        EventMetadata metadata = EventMetadata.builder()
            .occurredOn(occurredOn)
            .build();

        // Then
        assertThat(metadata.occurredOn()).isEqualTo(occurredOn);
    }

    // Test implementation of DomainEvent
    private record TestDomainEvent(
        UUID eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        Instant occurredOn
    ) implements DomainEvent<Object> {
        TestDomainEvent(UUID eventId, String aggregateId, String aggregateType, String eventType) {
            this(eventId, aggregateId, aggregateType, eventType, Instant.now());
        }
    }
}
