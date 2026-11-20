package com.erp.platform.identity.application.dto;

public record DistrictListResponse(
        Long id,
        Long tenantId,
        Long provinceId,
        String districtCode,
        String districtName,
        String nepaliName,
        String countryCode,
        String status
) {}
