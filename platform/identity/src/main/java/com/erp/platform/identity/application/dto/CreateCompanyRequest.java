package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank(message = "PAN number is required")
        @Pattern(regexp = "^[0-9]{9}$", message = "Nepal PAN must be exactly 9 digits")
        @Size(max = 30) String panNumber,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50) String phone,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull CompanyType companyType,
        @NotNull BusinessType businessType,
        @NotNull FiscalYearType fiscalYearType,
        @NotNull Boolean vatRegistered,
        @Size(max = 30) String vatRegistrationNumber,
        java.time.LocalDate vatEffectiveFrom,
        java.time.LocalDate vatEffectiveTo,
        @Size(max = 500) String address,
        @Size(max = 20) String wardNo,
        @Size(max = 200) String toleStreet,
        Short provinceId,
        Short districtId,
        Short localLevelId,
        @Size(max = 100) String bankName,
        @Size(max = 150) String bankAccountName,
        @Size(max = 50) String bankAccountNumber,
        @Size(max = 100) String bankBranch,
        @Size(max = 500) String qrCodeUrl
) {
    public CreateCompanyRequest(
            String companyName, String panNumber, String city, String phone, String email,
            CompanyType companyType, BusinessType businessType, FiscalYearType fiscalYearType,
            Boolean vatRegistered, String vatRegistrationNumber,
            java.time.LocalDate vatEffectiveFrom, java.time.LocalDate vatEffectiveTo,
            String address, String wardNo, String toleStreet,
            Short provinceId, Short districtId, Short localLevelId
    ) {
        this(companyName, panNumber, city, phone, email, companyType, businessType, fiscalYearType,
             vatRegistered, vatRegistrationNumber, vatEffectiveFrom, vatEffectiveTo,
             address, wardNo, toleStreet, provinceId, districtId, localLevelId,
             null, null, null, null, null);
    }
}
