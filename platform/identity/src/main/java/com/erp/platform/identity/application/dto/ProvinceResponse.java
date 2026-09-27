package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ProvinceStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for province data.
 *
 * @since 1.0.0
 */
public record ProvinceResponse(
    Long id,
    String provinceCode,
    String provinceName,
    String nepaliName,
    String countryCode,
    ProvinceStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
