package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.LocalLevelTypeResponse;
import com.erp.platform.identity.domain.LocalLevelType;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between LocalLevelType entity and DTOs.
 *
 * <p>Local level types are reference data seeded by Flyway migration
 * {@code V026__create_nepal_administrative_divisions.sql}. The mapper only
 * supports read-only conversions.
 *
 * @since 2.0.0
 */
@Component
public class LocalLevelTypeMapper {

    /**
     * Convert LocalLevelType entity to LocalLevelTypeResponse.
     *
     * @param localLevelType the local level type entity
     * @return the LocalLevelTypeResponse
     */
    public LocalLevelTypeResponse toResponse(LocalLevelType localLevelType) {
        return new LocalLevelTypeResponse(
            localLevelType.getId(),
            localLevelType.getName(),
            localLevelType.getNepaliName(),
            localLevelType.getCreatedAt(),
            localLevelType.getUpdatedAt()
        );
    }
}
