package com.erp.business.sales.customer.dto;

import com.erp.business.sales.customer.domain.CustomerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to update a customer.
 *
 * <p>All fields are optional in the sense that a null clears an optional value or
 * leaves a required one unchanged; the domain {@code update(...)} decides which.
 * The whole row is submitted, matching how a master-data edit form behaves.
 */
public record UpdateCustomerRequest(

        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name,

        @Size(max = 64, message = "Customer code must not exceed 64 characters")
        String code,

        CustomerType type,

        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,

        String address,

        @Pattern(regexp = "\\d{9}", message = "PAN number must be exactly 9 digits")
        String panNumber,

        Boolean vatRegistered,

        @DecimalMin(value = "0", message = "Credit limit must not be negative")
        BigDecimal creditLimit,

        @PositiveOrZero(message = "Payment terms days must not be negative")
        Integer paymentTermsDays,

        @Size(max = 8, message = "Currency must not exceed 8 characters")
        String currency
) {
}
