package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.LocalLevelListResponse;
import com.erp.platform.identity.application.dto.LocalLevelResponse;
import com.erp.platform.identity.domain.LocalLevel;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between LocalLevel entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class LocalLevelMapper {

    /**
     * Convert LocalLevel entity to LocalLevelResponse.
     *
     * @param localLevel the local level entity
     * @return the LocalLevelResponse
     */
    public LocalLevelResponse toResponse(LocalLevel localLevel) {
        return new LocalLevelResponse(
                localLevel.getMunicipalityId(),
                localLevel.getName(),
                localLevel.getNepaliName(),
                localLevel.getDistrictId(),
                localLevel.getLocalLevelTypeId(),
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Convert LocalLevel entity to LocalLevelListResponse.
     *
     * @param localLevel the local level entity
     * @return the LocalLevelListResponse
     */
    public LocalLevelListResponse toListResponse(LocalLevel localLevel) {
        return new LocalLevelListResponse(
                localLevel.getMunicipalityId(),
                localLevel.getName(),
                localLevel.getNepaliName(),
                localLevel.getDistrictId(),
                localLevel.getLocalLevelTypeId(),
                null
        );
    }
}
