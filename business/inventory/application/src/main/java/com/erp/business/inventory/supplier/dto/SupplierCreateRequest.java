package com.erp.business.inventory.supplier.dto;

import com.erp.business.inventory.domain.SupplierType;
import jakarta.validation.constraints.NotBlank;
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
        String taxId,
        Integer paymentTermsDays,
        String currency
) {
}
