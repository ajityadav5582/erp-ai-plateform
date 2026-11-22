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
        String legalName,
        String phone,
        String email,
        String website,
        String address,
        CompanyType companyType,
        BusinessType businessType,
        FiscalYearType fiscalYearType,
        Boolean vatRegistered,
        String vatRegistrationNumber,
        java.time.LocalDate vatEffectiveFrom,
        java.time.LocalDate vatEffectiveTo,
        String wardNo,
        String toleStreet,
        Short provinceId,
        Short districtId,
        Short localLevelId,
        String bankName,
        String bankAccountName,
        String bankAccountNumber,
        String bankBranch,
        String qrCodeUrl,
        CompanyStatus status
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyCode(),
                company.getCompanyName(),
                company.getPanNumber(),
                company.getCity(),
                company.getLegalName(),
                company.getPhone(),
                company.getEmail(),
                company.getWebsite(),
                company.getAddress(),
                company.getCompanyType(),
                company.getBusinessType(),
                company.getFiscalYearType(),
                company.getVatRegistered(),
                company.getVatRegistrationNumber(),
                company.getVatEffectiveFrom(),
                company.getVatEffectiveTo(),
                company.getWardNo(),
                company.getToleStreet(),
                company.getProvinceId(),
                company.getDistrictId(),
                company.getLocalLevelId(),
                company.getBankName(),
                company.getBankAccountName(),
                company.getBankAccountNumber(),
                company.getBankBranch(),
                company.getQrCodeUrl(),
                company.getStatus()
        );
    }
}
