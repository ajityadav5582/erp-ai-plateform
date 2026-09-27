package com.erp.platform.identity.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for local level list data (summary view).
 *
 * <p>A local level is referenced by a branch via {@code municipality_id} so
 * that the branch's location can be derived from province -> district
 * -> local level.
 *
 * @since 1.0.0
 */
public record LocalLevelListResponse(
    String municipalityId,
    String name,
    String nepaliName,
    Long districtId,
    String localLevelTypeId,
    LocalDateTime createdAt
) {
}
