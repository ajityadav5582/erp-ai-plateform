package com.erp.platform.testing.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockFactories}.
 */
class MockFactoriesTest {

    @Test
    @DisplayName("Should create fixed mock UUID")
    void shouldCreateFixedMockUuid() {
        // When
        UUID uuid = MockFactories.mockUuid();

        // Then
        assertThat(uuid).isEqualTo(UUID.fromString("12345678-1234-1234-1234-123456789012"));
    }

    @Test
    @DisplayName("Should create mock")
    void shouldCreateMock() {
        // When
        Runnable mock = MockFactories.mock(Runnable.class);

        // Then
        assertThat(mock).isNotNull();
    }

    @Test
    @DisplayName("Should create spy")
    void shouldCreateSpy() {
        // Given
        Runnable runnable = () -> {};

        // When
        Runnable spy = MockFactories.spy(runnable);

        // Then
        assertThat(spy).isNotNull();
        Mockito.verifyNoInteractions(spy);
    }
}
