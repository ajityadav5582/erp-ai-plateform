package com.erp.platform.data.specification;

import com.erp.platform.common.filter.Filter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JPA Specifications from Filter criteria.
 *
 * <p>Converts the platform's generic {@link Filter} into JPA Criteria API
 * specifications, enabling type-safe query construction.
 *
 * @param <T> the entity type
 * @since 1.0.0
 */
public class SpecificationBuilder<T> {

    private final List<Filter.Criterion> criteria = new ArrayList<>();

    /**
     * Adds a filter criterion.
     *
     * @param field the entity field name
     * @param operator the filter operator
     * @param value the filter value
     * @return this builder for chaining
     */
    public SpecificationBuilder<T> filter(String field, Filter.Operator operator, Object value) {
        criteria.add(new Filter.Criterion(field, operator, value));
        return this;
    }

    /**
     * Builds a JPA Specification from the accumulated criteria.
     *
     * @return a JPA Specification
     */
    public Specification<T> build() {
        if (criteria.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Filter.Criterion criterion : criteria) {
                predicates.add(createPredicate(root, cb, criterion));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private Predicate createPredicate(
            jakarta.persistence.criteria.Root<T> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Filter.Criterion criterion) {

        String field = criterion.field();
        Filter.Operator operator = criterion.operator();
        Object value = criterion.value();

        return switch (operator) {
            case EQUALS -> cb.equal(root.get(field), value);
            case NOT_EQUALS -> cb.notEqual(root.get(field), value);
            case GREATER_THAN -> cb.greaterThan(root.get(field), (Comparable) value);
            case GREATER_THAN_OR_EQUALS -> cb.greaterThanOrEqualTo(root.get(field), (Comparable) value);
            case LESS_THAN -> cb.lessThan(root.get(field), (Comparable) value);
            case LESS_THAN_OR_EQUALS -> cb.lessThanOrEqualTo(root.get(field), (Comparable) value);
            case LIKE -> cb.like(root.get(field), value.toString());
            case IN -> root.get(field).in((Iterable<?>) value);
            case NOT_IN -> cb.not(root.get(field).in((Iterable<?>) value));
            case IS_NULL -> cb.isNull(root.get(field));
            case IS_NOT_NULL -> cb.isNotNull(root.get(field));
            case BETWEEN -> {
                if (value instanceof Iterable<?> iterable) {
                    var iterator = iterable.iterator();
                    yield cb.between(root.get(field),
                        (Comparable) iterator.next(),
                        (Comparable) iterator.next());
                } else {
                    throw new IllegalArgumentException("BETWEEN operator requires an Iterable with two values");
                }
            }
        };
    }
}
