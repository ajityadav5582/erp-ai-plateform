package com.erp.business.inventory.supplier.mapper;

import com.erp.business.inventory.domain.Supplier;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getTenantId(),
                supplier.getCompanyId(),
                supplier.getName(),
                supplier.getCode(),
                supplier.getType(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getTaxId(),
                supplier.getPaymentTermsDays(),
                supplier.getCurrency(),
                supplier.getIsActive(),
                toLocalDateTime(supplier.getCreatedAt()),
                toLocalDateTime(supplier.getUpdatedAt()),
                supplier.getCreatedBy(),
                supplier.getUpdatedBy(),
                supplier.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
