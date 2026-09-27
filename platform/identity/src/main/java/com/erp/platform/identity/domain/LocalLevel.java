package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Local level (municipality / rural municipality / etc.) aggregate root.
 *
 * <p>Represents the lowest level of geographic organization below a district.
 * A local level is referenced by a branch via {@code local_level_id} so that
 * the branch's location can be derived from province -> district -> local level.
 *
 * <p>The entity maps to the {@code local_levels} table whose primary key is the
 * {@code municipality_id} (Nepal local-level identifier).
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "local_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class LocalLevel {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the local level (e.g. "NP-MUN-001").
     * This is the primary key of the table.
     */
    @Id
    @Column(name = "municipality_id", nullable = false, updatable = false, length = 50)
    private String municipalityId;

    /**
     * Name of the local level.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Nepali name of the local level.
     */
    @Column(name = "nepali_name", length = 100)
    private String nepaliName;

    /**
     * The district this local level belongs to.
     */
    @Column(name = "district_id", nullable = false)
    private Long districtId;

    /**
     * The type of local level (e.g. MUNICIPALITY, RURAL_MUNICIPALITY).
     */
    @Column(name = "local_level_type_id", nullable = false, length = 50)
    private String localLevelTypeId;

    // ==================== Factory Methods ====================

    public static LocalLevel create(String municipalityId, String name, String nepaliName, Long districtId, String localLevelTypeId) {
        validateMunicipalityId(municipalityId);
        validateName(name);
        validateDistrictId(districtId);
        validateLocalLevelTypeId(localLevelTypeId);

        return LocalLevel.builder()
                .municipalityId(municipalityId.trim())
                .name(name.trim())
                .nepaliName(nepaliName != null ? nepaliName.trim() : null)
                .districtId(districtId)
                .localLevelTypeId(localLevelTypeId.trim())
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateMunicipalityId(String municipalityId) {
        if (municipalityId == null || municipalityId.isBlank()) {
            throw new IllegalArgumentException("Municipality ID must not be blank");
        }
        if (municipalityId.length() > 50) {
            throw new IllegalArgumentException("Municipality ID must not exceed 50 characters");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Local level name must not be blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Local level name must not exceed 100 characters");
        }
    }

    private static void validateDistrictId(Long districtId) {
        if (districtId == null || districtId <= 0) {
            throw new IllegalArgumentException("District ID must be a positive number");
        }
    }

    private static void validateLocalLevelTypeId(String localLevelTypeId) {
        if (localLevelTypeId == null || localLevelTypeId.isBlank()) {
            throw new IllegalArgumentException("Local level type ID must not be blank");
        }
        if (localLevelTypeId.length() > 50) {
            throw new IllegalArgumentException("Local level type ID must not exceed 50 characters");
        }
    }
}
