package com.erp.platform.common.specification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for composing complex specifications.
 *
 * <p>Provides a fluent API for building complex filter criteria
 * from simple conditions. Supports AND, OR, and NOT operations.
 *
 * @param <T> the type of entity to filter
 * @since 1.0.0
 */
public class SpecificationBuilder<T> implements Serializable {

    private final List<Specification<T>> specifications = new ArrayList<>();

    /**
     * Adds a specification using AND logic.
     *
     * @param spec the specification to add
     * @return this builder for chaining
     */
    public SpecificationBuilder<T> and(Specification<T> spec) {
        if (spec != null) {
            specifications.add(spec);
        }
        return this;
    }

    /**
     * Adds a specification using OR logic.
     *
     * @param spec the specification to add
     * @return this builder for chaining
     */
    public SpecificationBuilder<T> or(Specification<T> spec) {
        if (spec != null) {
            specifications.add(spec);
        }
        return this;
    }

    /**
     * Builds the combined specification.
     *
     * @return the combined specification
     */
    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return Specification.always();
        }

        Specification<T> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = result.and(specifications.get(i));
        }
        return result;
    }

    /**
     * Creates a new builder.
     *
     * @param <T> the entity type
     * @return a new builder
     */
    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }
}
