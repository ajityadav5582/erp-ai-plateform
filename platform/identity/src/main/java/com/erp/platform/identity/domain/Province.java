package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

/**
 * Province/State aggregate root for geographic management.
 *
 * <p>Represents a province or state within a country for organizational
 * and geographic data management. Provinces serve as containers for
 * local bodies and help organize branches, departments, and users
 * by geographic region.
 *
 * <p>Provinces are master data shared across tenants and are therefore
 * not tenant-scoped.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "provinces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Province extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Unique province code within the tenant.
     */
    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    /**
     * Province name.
     */
    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    /**
     * Nepali name of the province.
     */
    @Column(name = "nepali_name", length = 100)
    private String nepaliName;

    /**
     * Country code (ISO 3166-1 alpha-3).
     */
    @Column(name = "country_code", length = 3)
    private String countryCode;

    /**
     * Current status of the province.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProvinceStatus status;

    // ==================== Domain Methods ====================

    public void activate() {
        if (this.status == ProvinceStatus.ACTIVE) {
            throw new IllegalStateException("Province is already active");
        }
        this.status = ProvinceStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != ProvinceStatus.ACTIVE) {
            throw new IllegalStateException("Only active provinces can be deactivated");
        }
        this.status = ProvinceStatus.INACTIVE;
    }

    // ==================== Factory Methods ====================

    public static Province create(String provinceCode, String provinceName, String nepaliName, String countryCode) {
        validateProvinceCode(provinceCode);
        validateProvinceName(provinceName);

        return Province.builder()
                .provinceCode(provinceCode.trim().toUpperCase())
                .provinceName(provinceName.trim())
                .nepaliName(nepaliName != null ? nepaliName.trim() : null)
                .countryCode(countryCode != null ? countryCode.trim().toUpperCase() : null)
                .status(ProvinceStatus.ACTIVE)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateProvinceCode(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            throw new IllegalArgumentException("Province code must not be blank");
        }
        if (provinceCode.length() > 20) {
            throw new IllegalArgumentException("Province code must not exceed 20 characters");
        }
    }

    private static void validateProvinceName(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) {
            throw new IllegalArgumentException("Province name must not be blank");
        }
        if (provinceName.length() > 100) {
            throw new IllegalArgumentException("Province name must not exceed 100 characters");
        }
    }
}
