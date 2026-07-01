package com.erp.platform.common.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic filter criteria for querying entities.
 *
 * <p>Provides a type-safe way to define filter criteria that can be
 * translated to various query mechanisms (JPA Criteria, QueryDSL, etc.).
 *
 * @param <T> the type of entity being filtered
 * @since 1.0.0
 */
public class Filter<T> implements Serializable {

    private final List<Criterion> criteria = new ArrayList<>();

    /**
     * Adds a filter criterion.
     *
     * @param criterion the criterion to add
     * @return this filter for chaining
     */
    public Filter<T> add(Criterion criterion) {
        if (criterion != null) {
            criteria.add(criterion);
        }
        return this;
    }

    /**
     * Returns all criteria.
     *
     * @return the list of criteria
     */
    public List<Criterion> getCriteria() {
        return new ArrayList<>(criteria);
    }

    /**
     * Checks if the filter has any criteria.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    /**
     * Creates a new empty filter.
     *
     * @param <T> the entity type
     * @return a new empty filter
     */
    public static <T> Filter<T> empty() {
        return new Filter<>();
    }

    /**
     * Single filter criterion.
     *
     * @param field the field name
     * @param operator the comparison operator
     * @param value the filter value
     */
    public record Criterion(
        String field,
        Operator operator,
        Object value
    ) implements Serializable {
    }

    /**
     * Filter operators.
     */
    public enum Operator implements Serializable {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS,
        LIKE,
        IN,
        NOT_IN,
        IS_NULL,
        IS_NOT_NULL,
        BETWEEN
    }
}
