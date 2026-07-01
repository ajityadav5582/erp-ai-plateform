package com.erp.platform.common.specification;

import java.io.Serializable;

/**
 * Specification pattern interface for filtering and querying.
 *
 * <p>Provides a composable way to define business rules for filtering
 * entities. Specifications can be combined using AND, OR, and NOT operators
 * to create complex query criteria.
 *
 * <p>This is a domain-driven design pattern that allows business rules
 * to be defined independently of the data source.
 *
 * @param <T> the type of entity to filter
 * @since 1.0.0
 */
@FunctionalInterface
public interface Specification<T> extends Serializable {

    /**
     * Tests whether the given entity satisfies this specification.
     *
     * @param entity the entity to test
     * @return true if the entity satisfies the specification
     */
    boolean isSatisfiedBy(T entity);

    /**
     * Combines this specification with another using AND logic.
     *
     * @param other the other specification
     * @return a new combined specification
     */
    default Specification<T> and(Specification<T> other) {
        return entity -> this.isSatisfiedBy(entity) && other.isSatisfiedBy(entity);
    }

    /**
     * Combines this specification with another using OR logic.
     *
     * @param other the other specification
     * @return a new combined specification
     */
    default Specification<T> or(Specification<T> other) {
        return entity -> this.isSatisfiedBy(entity) || other.isSatisfiedBy(entity);
    }

    /**
     * Negates this specification.
     *
     * @return a new negated specification
     */
    default Specification<T> not() {
        return entity -> !this.isSatisfiedBy(entity);
    }

    /**
     * Creates an always-true specification.
     *
     * @param <T> the entity type
     * @return a specification that always returns true
     */
    static <T> Specification<T> always() {
        return entity -> true;
    }

    /**
     * Creates an always-false specification.
     *
     * @param <T> the entity type
     * @return a specification that always returns false
     */
    static <T> Specification<T> never() {
        return entity -> false;
    }
}
