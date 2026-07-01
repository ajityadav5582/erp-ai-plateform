package com.erp.platform.testing.assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CommonAssertions}.
 */
class CommonAssertionsTest {

    @Test
    @DisplayName("Should assert UUID is not null")
    void shouldAssertUuidIsNotNull() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When/Then
        CommonAssertions.assertThat(uuid).isNotNull();
    }

    @Test
    @DisplayName("Should assert UUID is not nil")
    void shouldAssertUuidIsNotNil() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When/Then
        CommonAssertions.assertThat(uuid).isNotNil();
    }

    @Test
    @DisplayName("Should fail when UUID is nil")
    void shouldFailWhenUuidIsNil() {
        // Given
        UUID nilUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When/Then
        assertThatThrownBy(() -> CommonAssertions.assertThat(nilUuid).isNotNil())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected UUID not to be nil");
    }
}
