package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.LocalLevelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Local Level Type master data.
 *
 * <p>Provides read-only data access methods for local level type master data.
 * Local level types are reference data seeded by Flyway migration and are not
 * modified through application CRUD operations.
 *
 * @since 2.0.0
 */
public interface LocalLevelTypeRepository extends JpaRepository<LocalLevelType, Short> {

    /**
     * Finds a local level type by its name.
     *
     * @param name the local level type name
     * @return the local level type if found, empty otherwise
     */
    Optional<LocalLevelType> findByName(String name);

    /**
     * Finds all local level types ordered by name.
     *
     * @return list of all local level types ordered by name
     */
    List<LocalLevelType> findAllByOrderByNameAsc();

    /**
     * Checks if a local level type exists by name.
     *
     * @param name the local level type name to check
     * @return true if the local level type exists, false otherwise
     */
    boolean existsByName(String name);
}
