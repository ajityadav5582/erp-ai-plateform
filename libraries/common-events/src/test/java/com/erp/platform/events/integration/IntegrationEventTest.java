package com.erp.platform.events.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IntegrationEvent} implementations.
 */
class IntegrationEventTest {

    @Test
    @DisplayName("Should create integration event with all fields")
    void shouldCreateIntegrationEventWithAllFields() {
        // Given
        UUID eventId = UUID.randomUUID();
        String sourceService = "invoice-service";
        String destinationService = "payment-service";
        Instant occurredOn = Instant.now();

        // When
        TestIntegrationEvent event = new TestIntegrationEvent(
            eventId, sourceService, destinationService, occurredOn
        );

        // Then
        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getSourceService()).isEqualTo(sourceService);
        assertThat(event.getDestinationService()).isEqualTo(destinationService);
        assertThat(event.getOccurredOn()).isEqualTo(occurredOn);
    }

    @Test
    @DisplayName("Should create integration event with default occurredOn")
    void shouldCreateIntegrationEventWithDefaultOccurredOn() {
        // Given
        UUID eventId = UUID.randomUUID();

        // When
        TestIntegrationEvent event = new TestIntegrationEvent(
            eventId, "source", "destination"
        );

        // Then
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOccurredOn()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should create integration event with payload")
    void shouldCreateIntegrationEventWithPayload() {
        // Given
        UUID eventId = UUID.randomUUID();
        String payload = "{\"invoiceId\":\"123\"}";

        // When
        TestIntegrationEvent event = new TestIntegrationEvent(
            eventId, "source", "destination", payload
        );

        // Then
        assertThat(event.getPayload()).isEqualTo(payload);
    }

    // Test implementation of IntegrationEvent
    private record TestIntegrationEvent(
        UUID eventId,
        String sourceService,
        String destinationService,
        String payload,
        Instant occurredOn
    ) implements IntegrationEvent<Object> {
        TestIntegrationEvent(UUID eventId, String sourceService, String destinationService) {
            this(eventId, sourceService, destinationService, null, Instant.now());
        }

        TestIntegrationEvent(UUID eventId, String sourceService, String destinationService, Instant occurredOn) {
            this(eventId, sourceService, destinationService, null, occurredOn);
        }

        TestIntegrationEvent(UUID eventId, String sourceService, String destinationService, String payload) {
            this(eventId, sourceService, destinationService, payload, Instant.now());
        }
    }
}
