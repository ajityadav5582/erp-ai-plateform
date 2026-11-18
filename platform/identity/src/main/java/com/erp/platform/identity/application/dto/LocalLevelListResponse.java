package com.erp.platform.identity.application.dto;

public record LocalLevelListResponse(
        String municipalityId,
        String name,
        String nepaliName,
        Long districtId,
        String localLevelTypeId
) {}
