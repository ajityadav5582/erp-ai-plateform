package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ProvinceStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for province list data (summary view).
 *
 * @since 1.0.0
 */
public record ProvinceListResponse(
    Long id,
    String provinceCode,
    String provinceName,
    String nepaliName,
    String countryCode,
    ProvinceStatus status,
    LocalDateTime createdAt
) {
}
