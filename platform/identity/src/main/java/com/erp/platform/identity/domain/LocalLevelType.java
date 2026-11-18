package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Local level type master data entity mapped to {@code master_local_level_types} table.
 *
 * <p>Represents the type of a local level in Nepal (e.g. Metropolitan City,
 * Sub-Metropolitan City, Municipality, Rural Municipality). This is reference
 * data seeded by Flyway migration {@code V026__create_nepal_administrative_divisions.sql}.
 *
 * @since 2.0.0
 */
@Entity
@Table(name = "master_local_level_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class LocalLevelType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Local level type ID (primary key, smallint).
     */
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "smallint")
    private Short id;

    /**
     * Local level type name in English.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Local level type name in Nepali (Devanagari script).
     */
    @Column(name = "nepali_name", nullable = false, length = 150)
    private String nepaliName;

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

    // ==================== Factory Methods ====================

    public static LocalLevelType create(Short id, String name, String nepaliName, Instant createdAt, Instant updatedAt) {
        return LocalLevelType.builder()
                .id(id)
                .name(name != null ? name.trim() : null)
                .nepaliName(nepaliName != null ? nepaliName.trim() : null)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
