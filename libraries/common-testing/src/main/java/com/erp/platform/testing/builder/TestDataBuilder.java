package com.erp.platform.testing.builder;

/**
 * Test data builder interface.
 *
 * <p>Provides a fluent API for building test data objects.
 * Implementations should provide sensible defaults and
 * allow customization of specific fields.
 *
 * @param <T> the type of object being built
 * @since 1.0.0
 */
public interface TestDataBuilder<T> {

    /**
     * Builds the test data object.
     *
     * @return the built object
     */
    T build();

    /**
     * Resets the builder to default values.
     *
     * @return this builder for chaining
     */
    TestDataBuilder<T> reset();
}
