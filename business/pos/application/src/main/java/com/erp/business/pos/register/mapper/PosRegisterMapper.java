package com.erp.business.pos.register.mapper;

import com.erp.business.pos.domain.PosRegister;
import com.erp.business.pos.register.dto.PosRegisterResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Maps the {@link PosRegister} aggregate to its read model.
 */
@Component
public class PosRegisterMapper {

    public PosRegisterResponse toResponse(PosRegister register) {
        return new PosRegisterResponse(
                register.getId(),
                register.getTenantId(),
                register.getCompanyId(),
                register.getRegisterCode(),
                register.getRegisterName(),
                register.getBranchId(),
                register.getBranchName(),
                register.getWarehouseId(),
                register.getWarehouseCode(),
                register.getWarehouseName(),
                register.getStatus(),
                register.getCurrentSessionId(),
                register.getNotes(),
                toLocalDateTime(register.getCreatedAt()),
                toLocalDateTime(register.getUpdatedAt()),
                register.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
