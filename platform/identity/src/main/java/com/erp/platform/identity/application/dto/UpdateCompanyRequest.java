package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(max = 200) String companyName,
        @Size(max = 30) String panNumber,
        @Size(max = 100) String city,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 200) String legalName,
        @Size(max = 200) String website,
        @Size(max = 500) String address,
        CompanyType companyType,
        BusinessType businessType,
        FiscalYearType fiscalYearType,
        Boolean vatRegistered
) {
}
