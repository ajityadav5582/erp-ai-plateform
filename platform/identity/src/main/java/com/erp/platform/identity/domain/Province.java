package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Province/State aggregate root for geographic management.
 *
 * <p>Represents a province or state within a country for organizational
 * and geographic data management. Provinces serve as containers for
 * local bodies and help organize branches, departments, and users
 * by geographic region.
 *
 * <p>Provinces are master data shared across tenants and are therefore
 * not tenant-scoped. Mapped to {@code master_provinces}.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "master_provinces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Province implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Province ID (primary key, smallint).
     */
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "smallint")
    private Short id;

    /**
     * Original master province name.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Unique province code.
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
    @Column(name = "nepali_name", length = 150)
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

    /**
     * Creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (name == null) name = provinceName;
        if (provinceName == null) provinceName = name;
        if (status == null) status = ProvinceStatus.ACTIVE;
        if (version == null) version = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (name == null) name = provinceName;
        if (provinceName == null) provinceName = name;
    }

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

        String trimmedName = provinceName.trim();
        return Province.builder()
                .provinceCode(provinceCode.trim().toUpperCase())
                .provinceName(trimmedName)
                .name(trimmedName)
                .nepaliName(nepaliName != null ? nepaliName.trim() : null)
                .countryCode(countryCode != null ? countryCode.trim().toUpperCase() : null)
                .status(ProvinceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
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
