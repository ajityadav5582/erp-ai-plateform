package com.erp.platform.testing.builder;

/**
 * Abstract base class for test data builders.
 *
 * * Provides common functionality for test data builders including
 * fluent API support and default value management.
 *
 * @param <T> the type of object being built
 * @param <B> the builder type (for fluent API)
 * @since 1.0.0
 */
public abstract class AbstractTestDataBuilder<T, B extends AbstractTestDataBuilder<T, B>>
    implements TestDataBuilder<T> {

    /**
     * Returns this builder for fluent API chaining.
     *
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    /**
     * Resets the builder to default values.
     *
     * @return this builder for chaining
     */
    @Override
    public abstract B reset();
}
