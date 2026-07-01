package com.erp.platform.data.specification;

import com.erp.platform.common.specification.Specification;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Specification executor for querying with specifications.
 *
 * <p>Provides methods to execute specifications against the data source.
 *
 * @param <T> the entity type
 * @since 1.0.0
 */
public interface SpecificationExecutor<T> extends Serializable {

    /**
     * Finds all entities matching the specification.
     *
     * @param spec the specification
     * @return list of matching entities
     */
    List<T> findAll(Specification<T> spec);

    /**
     * Finds one entity matching the specification.
     *
     * @param spec the specification
     * @return the entity, or empty if not found
     */
    Optional<T> findOne(Specification<T> spec);

    /**
     * Counts entities matching the specification.
     *
     * @param spec the specification
     * @return the count
     */
    long count(Specification<T> spec);

    /**
     * Checks if any entity matches the specification.
     *
     * @param spec the specification
     * @return true if any match, false otherwise
     */
    boolean exists(Specification<T> spec);
}
