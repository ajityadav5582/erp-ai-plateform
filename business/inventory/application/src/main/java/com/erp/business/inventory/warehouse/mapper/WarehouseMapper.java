package com.erp.business.inventory.warehouse.mapper;

import com.erp.business.inventory.domain.Warehouse;
import com.erp.business.inventory.warehouse.dto.WarehouseResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class WarehouseMapper {

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getTenantId(),
                warehouse.getCompanyId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getType(),
                warehouse.getBranchId(),
                warehouse.getAddress(),
                warehouse.getPhone(),
                warehouse.getIsActive(),
                warehouse.getIsDefault(),
                toLocalDateTime(warehouse.getCreatedAt()),
                toLocalDateTime(warehouse.getUpdatedAt()),
                warehouse.getCreatedBy(),
                warehouse.getUpdatedBy(),
                warehouse.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
