package com.erp.platform.data.query;

import com.erp.platform.common.filter.Filter;
import com.erp.platform.data.specification.SpecificationBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Utility class for building queries and specifications.
 *
 * <p>Provides helper methods for converting platform abstractions
 * to Spring Data JPA constructs.
 *
 * @since 1.0.0
 */
public final class QueryUtils {

    private QueryUtils() {
        // Utility class
    }

    /**
     * Creates a Spring Data PageRequest from a platform PageRequest.
     *
     * @param pageRequest the platform page request
     * @return a Spring Data PageRequest
     */
    public static org.springframework.data.domain.PageRequest toPageRequest(
            com.erp.platform.common.dto.PageRequest pageRequest) {
        return PageRequest.of(
            pageRequest.getPageOrDefault(),
            pageRequest.getSizeOrDefault(),
            toSort(pageRequest.getSorts())
        );
    }

    /**
     * Converts platform Sort objects to Spring Data Sort.
     *
     * @param sorts the platform sort objects
     * @return a Spring Data Sort
     */
    public static Sort toSort(List<com.erp.platform.common.dto.PageRequest.Sort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.by("createdAt").descending();
        }

        List<Sort.Order> orders = sorts.stream()
            .map(sort -> {
                Sort.Direction direction = sort.direction() == com.erp.platform.common.dto.PageRequest.Direction.ASC
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
                return new Sort.Order(direction, sort.property());
            })
            .toList();

        return Sort.by(orders);
    }

    /**
     * Creates a JPA Specification from a platform Filter.
     *
     * @param filter the platform filter
     * @param <T> the entity type
     * @return a JPA Specification
     */
    public static <T> Specification<T> toSpecification(Filter<T> filter) {
        SpecificationBuilder<T> builder = new SpecificationBuilder<>();

        for (Filter.Criterion criterion : filter.getCriteria()) {
            builder.filter(criterion.field(), criterion.operator(), criterion.value());
        }

        return builder.build();
    }

    /**
     * Creates a JPA Specification with tenant filtering.
     *
     * @param tenantId the tenant ID
     * @param <T> the entity type
     * @return a JPA Specification that filters by tenant ID
     */
    public static <T> Specification<T> withTenantFilter(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Creates a JPA Specification combining tenant filter with another specification.
     *
     * @param tenantId the tenant ID
     * @param specification the additional specification
     * @param <T> the entity type
     * @return a combined JPA Specification
     */
    public static <T> Specification<T> withTenantFilter(
            Long tenantId,
            Specification<T> specification) {
        return QueryUtils.<T>withTenantFilter(tenantId).and(specification);
    }
}
