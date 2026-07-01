package com.erp.platform.testing.mock;

import org.mockito.Mockito;

import java.util.UUID;

/**
 * Mock factories for the ERP AI Platform.
 *
 * <p>This class provides factory methods for creating
 * commonly used mocks.
 *
 * @since 1.0.0
 */
public final class MockFactories {

    private MockFactories() {
        // Utility class
    }

    /**
     * Creates a mock UUID.
     *
     * @return a fixed UUID for testing
     */
    public static UUID mockUuid() {
        return UUID.fromString("12345678-1234-1234-1234-123456789012");
    }

    /**
     * Creates a spy of the given object.
     *
     * @param <T> the type
     * @param object the object to spy
     * @return the spy
     */
    public static <T> T spy(T object) {
        return Mockito.spy(object);
    }

    /**
     * Creates a mock of the given class.
     *
     * @param <T> the type
     * @param clazz the class to mock
     * @return the mock
     */
    public static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz);
    }
}
