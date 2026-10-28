package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.domain.CompanyStatus;
import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;

public record CompanyResponse(
        Long id,
        String companyCode,
        String companyName,
        String panNumber,
        String city,
        String phone,
        String email,
         CompanyType companyType,
         BusinessType businessType,
         FiscalYearType fiscalYearType,
        Boolean vatRegistered,
        CompanyStatus status
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyCode(),
                company.getCompanyName(),
                company.getPanNumber(),
                company.getCity(),
                company.getPhone(),
                company.getEmail(),
                 company.getCompanyType(),
                 company.getBusinessType(),
                 company.getFiscalYearType(),
                company.getVatRegistered(),
                company.getStatus()
        );
    }
}
