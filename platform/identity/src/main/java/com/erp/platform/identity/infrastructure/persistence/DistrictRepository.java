package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.District;
import com.erp.platform.identity.domain.DistrictStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for District aggregate.
 *
 * <p>Provides data access methods for district management operations.
 * Districts are organized under provinces.
 *
 * @since 1.0.0
 */
public interface DistrictRepository extends JpaRepository<District, Long> {

    /**
     * Finds a district by its unique district code.
     *
     * @param districtCode the district code
     * @return the district if found, empty otherwise
     */
    Optional<District> findByDistrictCode(String districtCode);

    /**
     * Finds all districts for a province.
     *
     * @param provinceId the province ID
     * @return list of districts for the province
     */
    List<District> findByProvinceId(Long provinceId);

    /**
     * Finds active districts for a province, ordered by name.
     *
     * @param provinceId the province ID
     * @return list of active districts for the province
     */
    List<District> findByProvinceIdAndStatusOrderByDistrictName(Long provinceId, DistrictStatus status);

    /**
     * Finds active districts for a province, paginated.
     *
     * @param provinceId the province ID
     * @param status the district status
     * @param pageable the pagination parameters
     * @return a page of active districts for the province
     */
    Page<District> findByProvinceIdAndStatus(Long provinceId, DistrictStatus status, Pageable pageable);

    /**
     * Finds all active districts.
     *
     * @param status the district status
     * @return list of active districts
     */
    List<District> findByStatus(DistrictStatus status);

    /**
     * Finds all active districts, ordered by name.
     *
     * @param status the district status
     * @return list of active districts ordered by name
     */
    List<District> findByStatusOrderByDistrictName(DistrictStatus status);

    /**
     * Finds districts by country code.
     *
     * @param countryCode the country code
     * @return list of districts for the country
     */
    List<District> findByCountryCode(String countryCode);

    /**
     * Checks if a district code exists.
     *
     * @param districtCode the district code to check
     * @return true if the district code exists, false otherwise
     */
    boolean existsByDistrictCode(String districtCode);

    /**
     * Counts the number of districts for a province.
     *
     * @param provinceId the province ID
     * @return the count of districts for the province
     */
    long countByProvinceId(Long provinceId);
}
