package com.erp.platform.identity.application.dto;

import java.time.Instant;

/**
 * Response DTO for local level type master data.
 *
 * <p>Local level types are reference data seeded by Flyway migration
 * {@code V026__create_nepal_administrative_divisions.sql}. The entity is
 * read-only: create/update/delete operations are not supported.
 *
 * @since 2.0.0
 */
public record LocalLevelTypeResponse(
    Short id,
    String name,
    String nepaliName,
    Instant createdAt,
    Instant updatedAt
) {
}
