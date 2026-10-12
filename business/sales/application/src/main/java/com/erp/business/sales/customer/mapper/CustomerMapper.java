package com.erp.business.sales.customer.mapper;

import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.dto.CustomerResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Maps the {@link Customer} aggregate to its read model.
 *
 * <p>A hand-written mapper rather than a reflection-based one: the field set is
 * small and stable, and an explicit mapping fails to compile when a field is
 * added, instead of silently dropping it from the response.
 */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getTenantId(),
                customer.getCompanyId(),
                customer.getName(),
                customer.getCode(),
                customer.getType(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getPanNumber(),
                customer.getVatRegistered(),
                customer.getCreditLimit(),
                customer.getPaymentTermsDays(),
                customer.getCurrency(),
                customer.getIsActive(),
                toLocalDateTime(customer.getCreatedAt()),
                toLocalDateTime(customer.getUpdatedAt()),
                customer.getCreatedBy(),
                customer.getUpdatedBy(),
                customer.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
