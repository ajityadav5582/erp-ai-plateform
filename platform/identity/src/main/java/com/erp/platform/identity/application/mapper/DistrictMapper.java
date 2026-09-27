package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateDistrictRequest;
import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.application.dto.DistrictResponse;
import com.erp.platform.identity.domain.District;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between District entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class DistrictMapper {

    /**
     * Convert CreateDistrictRequest to District entity.
     *
     * @param request the create request
     * @return the District entity
     */
    public District toEntity(CreateDistrictRequest request) {
        return District.create(
                request.provinceId(),
                request.districtCode(),
                request.districtName(),
                request.nepaliName(),
                request.countryCode()
        );
    }

    /**
     * Convert District entity to DistrictResponse.
     *
     * @param district the district entity
     * @return the DistrictResponse
     */
    public DistrictResponse toResponse(District district) {
        return new DistrictResponse(
                district.getId(),
                district.getProvinceId(),
                district.getDistrictCode(),
                district.getDistrictName(),
                district.getNepaliName(),
                district.getCountryCode(),
                district.getStatus(),
                toLocalDateTime(district.getCreatedAt()),
                toLocalDateTime(district.getUpdatedAt()),
                district.getCreatedBy(),
                district.getUpdatedBy(),
                district.getVersion()
        );
    }

    /**
     * Convert District entity to DistrictListResponse.
     *
     * @param district the district entity
     * @return the DistrictListResponse
     */
    public DistrictListResponse toListResponse(District district) {
        return new DistrictListResponse(
                district.getId(),
                district.getProvinceId(),
                district.getDistrictCode(),
                district.getDistrictName(),
                district.getNepaliName(),
                district.getCountryCode(),
                district.getStatus(),
                toLocalDateTime(district.getCreatedAt())
        );
    }

    /**
     * Convert Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return the local date time
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
