package com.erp.business.inventory.unit.dto;

import com.erp.business.inventory.domain.UnitDimension;

import java.time.LocalDateTime;

/**
 * Full representation of a unit returned by every read endpoint.
 *
 * <p>The list endpoint returns this same shape rather than a slimmer projection:
 * units are few per company (tens, not thousands), so a dedicated list DTO would
 * save nothing and would create two shapes for the frontend to keep in sync.
 */
public record UnitResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String name,
        String code,
        UnitDimension dimension,
        String symbol,
        Integer decimalScale,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
