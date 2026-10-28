package com.erp.business.sales.customer.dto;

import com.erp.business.sales.customer.domain.CustomerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to create a customer.
 *
 * <p>Tenant and company are deliberately absent: they are read from the
 * authenticated request context, never from the body, so a caller cannot create a
 * customer in another company. The PAN/VAT invariant (a VAT-registered customer
 * must carry a PAN) is enforced in the domain, not duplicated here.
 */
public record CreateCustomerRequest(

        @NotBlank(message = "Customer name is required")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Customer code is required")
        @Size(max = 64, message = "Customer code must not exceed 64 characters")
        String code,

        @NotNull(message = "Customer type is required")
        CustomerType type,

        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,

        String address,

        @Pattern(regexp = "\\d{9}", message = "PAN number must be exactly 9 digits")
        String panNumber,

        @NotNull(message = "VAT registration flag is required")
        Boolean vatRegistered,

        @DecimalMin(value = "0", message = "Credit limit must not be negative")
        BigDecimal creditLimit,

        @PositiveOrZero(message = "Payment terms days must not be negative")
        Integer paymentTermsDays,

        @Size(max = 8, message = "Currency must not exceed 8 characters")
        String currency
) {
}
