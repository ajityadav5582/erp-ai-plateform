package com.erp.platform.common.tenancy;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * JPA tenant filter implementation.
 *
 * <p>This filter adds tenant isolation to JPA criteria queries.
 *
 * @param <T> the entity type
 * @since 1.0.0
 */
public class JpaTenantFilter<T> implements TenantFilter<T> {

    private final String tenantField;

    /**
     * Creates a new JPA tenant filter.
     *
     * @param tenantField the tenant field name
     */
    public JpaTenantFilter(String tenantField) {
        this.tenantField = tenantField;
    }

    @Override
    public String apply(String query) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return query;
        }
        return query + " AND " + tenantField + " = :tenantId";
    }

    @Override
    public String getTenantField() {
        return tenantField;
    }

    /**
     * Creates a predicate for tenant filtering.
     *
     * @param root the query root
     * @param query the criteria query
     * @param builder the criteria builder
     * @return the tenant predicate
     */
    public Predicate createPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return builder.conjunction();
        }
        return builder.equal(root.get(tenantField), tenantId);
    }
}
