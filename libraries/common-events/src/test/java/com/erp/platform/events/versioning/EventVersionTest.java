package com.erp.platform.events.versioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EventVersion}.
 */
class EventVersionTest {

    @Test
    @DisplayName("Should parse valid version string")
    void shouldParseValidVersionString() {
        // When
        EventVersion version = EventVersion.parse("1.0.0");

        // Then
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.patch()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should parse version with two parts")
    void shouldParseVersionWithTwoParts() {
        // When
        EventVersion version = EventVersion.parse("2.1");

        // Then
        assertThat(version.major()).isEqualTo(2);
        assertThat(version.minor()).isEqualTo(1);
        assertThat(version.patch()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should parse version with single part")
    void shouldParseVersionWithSinglePart() {
        // When
        EventVersion version = EventVersion.parse("3");

        // Then
        assertThat(version.major()).isEqualTo(3);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.patch()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return current version")
    void shouldReturnCurrentVersion() {
        // When
        EventVersion current = EventVersion.current();

        // Then
        assertThat(current).isNotNull();
        assertThat(current.major()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should compare versions correctly")
    void shouldCompareVersionsCorrectly() {
        // Given
        EventVersion v1 = EventVersion.parse("1.0.0");
        EventVersion v2 = EventVersion.parse("2.0.0");
        EventVersion v1_1 = EventVersion.parse("1.1.0");

        // Then
        assertThat(v1.isLessThan(v2)).isTrue();
        assertThat(v2.isGreaterThan(v1)).isTrue();
        assertThat(v1.isEqualTo(v1_1)).isFalse();
        assertThat(v1.isLessThan(v1_1)).isTrue();
    }

    @Test
    @DisplayName("Should format version as string")
    void shouldFormatVersionAsString() {
        // Given
        EventVersion version = EventVersion.parse("1.2.3");

        // When
        String formatted = version.toString();

        // Then
        assertThat(formatted).isEqualTo("1.2.3");
    }
}
