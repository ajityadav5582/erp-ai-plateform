package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 30) String panNumber,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50) String phone,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull CompanyType companyType,
        @NotNull FiscalYearType fiscalYearType,
        @NotNull Boolean vatRegistered
) {
}
