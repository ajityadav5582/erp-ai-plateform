package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(max = 200) String companyName,
        @Pattern(regexp = "^[0-9]{9}$", message = "Nepal PAN must be exactly 9 digits")
        @Size(max = 30) String panNumber,
        @Size(max = 100) String city,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 200) String legalName,
        @Size(max = 200) String website,
        @Size(max = 500) String address,
        @Size(max = 20) String wardNo,
        @Size(max = 200) String toleStreet,
        Short provinceId,
        Short districtId,
        Short localLevelId,
        CompanyType companyType,
        BusinessType businessType,
        FiscalYearType fiscalYearType,
        Boolean vatRegistered,
        @Size(max = 30) String vatRegistrationNumber,
        java.time.LocalDate vatEffectiveFrom,
        java.time.LocalDate vatEffectiveTo,
        @Size(max = 100) String bankName,
        @Size(max = 150) String bankAccountName,
        @Size(max = 50) String bankAccountNumber,
        @Size(max = 100) String bankBranch,
        @Size(max = 500) String qrCodeUrl
) {
    public UpdateCompanyRequest(
            String companyName, String panNumber, String city, String phone, String email,
            String legalName, String website, String address, String wardNo, String toleStreet,
            Short provinceId, Short districtId, Short localLevelId,
            CompanyType companyType, BusinessType businessType, FiscalYearType fiscalYearType,
            Boolean vatRegistered, String vatRegistrationNumber,
            java.time.LocalDate vatEffectiveFrom, java.time.LocalDate vatEffectiveTo
    ) {
        this(companyName, panNumber, city, phone, email, legalName, website, address, wardNo, toleStreet,
             provinceId, districtId, localLevelId, companyType, businessType, fiscalYearType,
             vatRegistered, vatRegistrationNumber, vatEffectiveFrom, vatEffectiveTo,
             null, null, null, null, null);
    }
}
