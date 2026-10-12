package com.erp.business.sales.customer.dto;

import com.erp.business.sales.customer.domain.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model for a customer, returned by every customer endpoint.
 *
 * <p>A flat record rather than the entity so the persistence type never leaks past
 * the service boundary and the JSON shape is stable independent of JPA.
 */
public record CustomerResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String name,
        String code,
        CustomerType type,
        String email,
        String phone,
        String address,
        String panNumber,
        Boolean vatRegistered,
        BigDecimal creditLimit,
        Integer paymentTermsDays,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
