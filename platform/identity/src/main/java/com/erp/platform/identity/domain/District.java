package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

/**
 * District aggregate root for geographic management.
 *
 * <p>Represents a district within a province/state. Districts are used to
 * further organize branches, departments, and users by geographic region.
 * Each district belongs to exactly one province.
 *
 * <p>Districts are master data shared across tenants and are therefore
 * not tenant-scoped.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "districts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class District extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Unique district code within the tenant.
     */
    @Column(name = "district_code", nullable = false, length = 20)
    private String districtCode;

    /**
     * District name.
     */
    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    /**
     * Nepali name of the district.
     */
    @Column(name = "nepali_name", length = 100)
    private String nepaliName;

    /**
     * The province this district belongs to.
     */
    @Column(name = "province_id", nullable = false)
    private Long provinceId;

    /**
     * Country code (ISO 3166-1 alpha-3).
     */
    @Column(name = "country_code", length = 3)
    private String countryCode;

    /**
     * Current status of the district.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DistrictStatus status;

    // ==================== Domain Methods ====================

    public void activate() {
        if (this.status == DistrictStatus.ACTIVE) {
            throw new IllegalStateException("District is already active");
        }
        this.status = DistrictStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != DistrictStatus.ACTIVE) {
            throw new IllegalStateException("Only active districts can be deactivated");
        }
        this.status = DistrictStatus.INACTIVE;
    }

    // ==================== Factory Methods ====================

    public static District create(Long provinceId, String districtCode, String districtName, String nepaliName, String countryCode) {
        validateProvinceId(provinceId);
        validateDistrictCode(districtCode);
        validateDistrictName(districtName);

        return District.builder()
                .provinceId(provinceId)
                .districtCode(districtCode.trim().toUpperCase())
                .districtName(districtName.trim())
                .nepaliName(nepaliName != null ? nepaliName.trim() : null)
                .countryCode(countryCode != null ? countryCode.trim().toUpperCase() : null)
                .status(DistrictStatus.ACTIVE)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateProvinceId(Long provinceId) {
        if (provinceId == null || provinceId <= 0) {
            throw new IllegalArgumentException("Province ID must be a positive number");
        }
    }

    private static void validateDistrictCode(String districtCode) {
        if (districtCode == null || districtCode.isBlank()) {
            throw new IllegalArgumentException("District code must not be blank");
        }
        if (districtCode.length() > 20) {
            throw new IllegalArgumentException("District code must not exceed 20 characters");
        }
    }

    private static void validateDistrictName(String districtName) {
        if (districtName == null || districtName.isBlank()) {
            throw new IllegalArgumentException("District name must not be blank");
        }
        if (districtName.length() > 100) {
            throw new IllegalArgumentException("District name must not exceed 100 characters");
        }
    }
}
