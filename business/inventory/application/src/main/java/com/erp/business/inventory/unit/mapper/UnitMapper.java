package com.erp.business.inventory.unit.mapper;

import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.unit.dto.UnitResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts {@link Unit} entities into their wire representation.
 *
 * <p>Timestamps are converted from the {@code Instant} the audit base class
 * stores to {@code LocalDateTime} in the server's zone. This matches the supplier
 * mapper exactly; doing it in one mapper rather than per DTO is what stops the
 * two endpoints from disagreeing about a timezone.
 */
@Component
public class UnitMapper {

    public UnitResponse toResponse(Unit unit) {
        return new UnitResponse(
                unit.getId(),
                unit.getTenantId(),
                unit.getCompanyId(),
                unit.getName(),
                unit.getCode(),
                unit.getDimension(),
                unit.getSymbol(),
                unit.getDecimalScale(),
                unit.getIsActive(),
                toLocalDateTime(unit.getCreatedAt()),
                toLocalDateTime(unit.getUpdatedAt()),
                unit.getCreatedBy(),
                unit.getUpdatedBy(),
                unit.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
