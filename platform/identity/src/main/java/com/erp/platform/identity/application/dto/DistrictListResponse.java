package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.DistrictStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for district list data (summary view).
 *
 * @since 1.0.0
 */
public record DistrictListResponse(
    Long id,
    Long provinceId,
    String districtCode,
    String districtName,
    String nepaliName,
    String countryCode,
    DistrictStatus status,
    LocalDateTime createdAt
) {
}
