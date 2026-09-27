package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.LocalLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for LocalLevel aggregate.
 *
 * <p>Provides data access methods for local level (municipality / rural
 * municipality) management. Local levels are organized under districts.
 *
 * @since 1.0.0
 */
public interface LocalLevelRepository extends JpaRepository<LocalLevel, String> {

    /**
     * Finds a local level by its municipality id.
     *
     * @param municipalityId the municipality id
     * @return the local level if found, empty otherwise
     */
    Optional<LocalLevel> findByMunicipalityId(String municipalityId);

    /**
     * Finds all local levels for a district.
     *
     * @param districtId the district ID
     * @return list of local levels for the district
     */
    List<LocalLevel> findByDistrictId(Long districtId);

    /**
     * Finds all local levels for a district, ordered by name.
     *
     * @param districtId the district ID
     * @return list of local levels for the district ordered by name
     */
    List<LocalLevel> findByDistrictIdOrderByName(Long districtId);

    /**
     * Finds local levels by local level type id.
     *
     * @param localLevelTypeId the local level type id
     * @return list of local levels of the given type
     */
    List<LocalLevel> findByLocalLevelTypeId(String localLevelTypeId);

    /**
     * Finds local levels by district id and local level type id.
     *
     * @param districtId the district ID
     * @param localLevelTypeId the local level type id
     * @return list of local levels matching both criteria
     */
    List<LocalLevel> findByDistrictIdAndLocalLevelTypeId(Long districtId, String localLevelTypeId);

    /**
     * Counts the number of local levels for a district.
     *
     * @param districtId the district ID
     * @return the count of local levels for the district
     */
    long countByDistrictId(Long districtId);

    /**
     * Checks if a municipality id exists.
     *
     * @param municipalityId the municipality id to check
     * @return true if the municipality id exists, false otherwise
     */
    boolean existsByMunicipalityId(String municipalityId);
}
