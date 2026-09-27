package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateProvinceRequest;
import com.erp.platform.identity.application.dto.ProvinceListResponse;
import com.erp.platform.identity.application.dto.ProvinceResponse;
import com.erp.platform.identity.domain.Province;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between Province entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class ProvinceMapper {

    /**
     * Convert CreateProvinceRequest to Province entity.
     *
     * @param request the create request
     * @return the Province entity
     */
    public Province toEntity(CreateProvinceRequest request) {
        return Province.create(
                request.provinceCode(),
                request.provinceName(),
                null,
                request.countryCode()
        );
    }

    /**
     * Convert Province entity to ProvinceResponse.
     *
     * @param province the province entity
     * @return the ProvinceResponse
     */
    public ProvinceResponse toResponse(Province province) {
        return new ProvinceResponse(
                province.getId(),
                province.getProvinceCode(),
                province.getProvinceName(),
                province.getNepaliName(),
                province.getCountryCode(),
                province.getStatus(),
                toLocalDateTime(province.getCreatedAt()),
                toLocalDateTime(province.getUpdatedAt()),
                province.getCreatedBy(),
                province.getUpdatedBy(),
                province.getVersion()
        );
    }

    /**
     * Convert Province entity to ProvinceListResponse.
     *
     * @param province the province entity
     * @return the ProvinceListResponse
     */
    public ProvinceListResponse toListResponse(Province province) {
        return new ProvinceListResponse(
                province.getId(),
                province.getProvinceCode(),
                province.getProvinceName(),
                province.getNepaliName(),
                province.getCountryCode(),
                province.getStatus(),
                toLocalDateTime(province.getCreatedAt())
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
