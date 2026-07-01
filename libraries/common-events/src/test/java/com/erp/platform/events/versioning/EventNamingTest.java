package com.erp.platform.events.versioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EventNaming}.
 */
class EventNamingTest {

    @Test
    @DisplayName("Should create event name following convention")
    void shouldCreateEventNameFollowingConvention() {
        // When
        String eventName = EventNaming.createName("invoice", "invoice", "created");

        // Then
        assertThat(eventName).isEqualTo("invoice.invoice.created");
    }

    @Test
    @DisplayName("Should create versioned event name")
    void shouldCreateVersionedEventName() {
        // When
        String versionedName = EventNaming.versionedName("invoice.created", "1");

        // Then
        assertThat(versionedName).isEqualTo("invoice.created.v1");
    }

    @Test
    @DisplayName("Should extract domain from event name")
    void shouldExtractDomainFromEventName() {
        // When
        String domain = EventNaming.extractDomain("invoice.created");

        // Then
        assertThat(domain).isEqualTo("invoice");
    }

    @Test
    @DisplayName("Should extract action from event name")
    void shouldExtractActionFromEventName() {
        // When
        String action = EventNaming.extractAction("invoice.created");

        // Then
        assertThat(action).isEqualTo("created");
    }

    @Test
    @DisplayName("Should return null for null event name")
    void shouldReturnNullForNullEventName() {
        // When
        String domain = EventNaming.extractDomain(null);
        String action = EventNaming.extractAction(null);

        // Then
        assertThat(domain).isNull();
        assertThat(action).isNull();
    }

    @Test
    @DisplayName("Should return null for blank event name")
    void shouldReturnNullForBlankEventName() {
        // When
        String domain = EventNaming.extractDomain("");
        String action = EventNaming.extractAction("   ");

        // Then
        assertThat(domain).isNull();
        assertThat(action).isNull();
    }
}
