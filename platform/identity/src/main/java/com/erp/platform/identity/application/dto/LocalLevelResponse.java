package com.erp.platform.identity.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for local level data.
 *
 * @since 1.0.0
 */
public record LocalLevelResponse(
    String municipalityId,
    String name,
    String nepaliName,
    Long districtId,
    String localLevelTypeId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
