package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Province;
import com.erp.platform.identity.domain.ProvinceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Province aggregate.
 *
 * <p>Provides data access methods for province management operations.
 *
 * @since 1.0.0
 */
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    /**
     * Finds a province by its unique province code.
     *
     * @param provinceCode the province code
     * @return the province if found, empty otherwise
     */
    Optional<Province> findByProvinceCode(String provinceCode);

    /**
     * Finds all active provinces.
     *
     * @return list of active provinces
     */
    List<Province> findByStatus(ProvinceStatus status);

    /**
     * Finds all active provinces, ordered by name.
     *
     * @return list of active provinces ordered by name
     */
    List<Province> findByStatusOrderByProvinceName(ProvinceStatus status);

    /**
     * Finds active provinces paginated.
     *
     * @param status the province status
     * @param pageable the pagination parameters
     * @return a page of active provinces
     */
    Page<Province> findByStatus(ProvinceStatus status, Pageable pageable);

    /**
     * Finds provinces by country code.
     *
     * @param countryCode the country code
     * @return list of provinces for the country
     */
    List<Province> findByCountryCode(String countryCode);

    /**
     * Finds active provinces by country code, ordered by name.
     *
     * @param countryCode the country code
     * @return list of active provinces for the country
     */
    List<Province> findByCountryCodeAndStatusOrderByProvinceName(String countryCode, ProvinceStatus status);

    /**
     * Checks if a province code exists.
     *
     * @param provinceCode the province code to check
     * @return true if the province code exists, false otherwise
     */
    boolean existsByProvinceCode(String provinceCode);
}
