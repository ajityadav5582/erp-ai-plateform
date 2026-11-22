package com.erp.business.inventory.supplier.dto;

import com.erp.business.inventory.domain.SupplierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierCreateRequest(
        @NotBlank(message = "Supplier name is required")
        @Size(max = 255, message = "Supplier name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Supplier code is required")
        @Size(max = 64, message = "Supplier code must not exceed 64 characters")
        String code,

        SupplierType type,

        String email,
        String phone,
        String address,

        @Pattern(regexp = "^$|^[0-9]{9}$", message = "Supplier PAN must be exactly 9 digits")
        String taxId,

        Integer paymentTermsDays,
        String currency,
        com.erp.business.inventory.domain.Supplier.VatRegistrationStatus vatRegistrationStatus,

        @Pattern(regexp = "^$|^[0-9]{9}$", message = "VAT registration number must be exactly 9 digits")
        String vatRegistrationNumber,

        java.time.LocalDate vatEffectiveFrom,
        java.time.LocalDate vatEffectiveTo
) {
}
