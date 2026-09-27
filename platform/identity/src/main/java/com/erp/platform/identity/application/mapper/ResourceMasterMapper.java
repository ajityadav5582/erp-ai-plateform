package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateResourceMasterRequest;
import com.erp.platform.identity.application.dto.ResourceMasterListResponse;
import com.erp.platform.identity.application.dto.ResourceMasterResponse;
import com.erp.platform.identity.application.dto.UpdateResourceMasterRequest;
import com.erp.platform.identity.domain.ResourceMaster;
import com.erp.platform.identity.domain.ResourceStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between ResourceMaster entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class ResourceMasterMapper {

    /**
     * Convert CreateResourceMasterRequest to ResourceMaster entity.
     *
     * @param request the create request
     * @return the ResourceMaster entity
     */
    public ResourceMaster toEntity(CreateResourceMasterRequest request) {
        return ResourceMaster.create(
                request.resourceCode(),
                request.resourceName(),
                request.module(),
                request.description(),
                request.status(),
                request.isSystem()
        );
    }

    /**
     * Convert ResourceMaster entity to ResourceMasterResponse.
     *
     * @param resource the resource entity
     * @return the ResourceMasterResponse
     */
    public ResourceMasterResponse toResponse(ResourceMaster resource) {
        return new ResourceMasterResponse(
                resource.getId(),
                resource.getResourceCode(),
                resource.getResourceName(),
                resource.getModule(),
                resource.getDescription(),
                resource.getStatus(),
                resource.getIsSystem(),
                toLocalDateTime(resource.getCreatedAt()),
                toLocalDateTime(resource.getUpdatedAt()),
                resource.getCreatedBy(),
                resource.getUpdatedBy(),
                resource.getVersion()
        );
    }

    /**
     * Convert ResourceMaster entity to ResourceMasterListResponse.
     *
     * @param resource the resource entity
     * @return the ResourceMasterListResponse
     */
    public ResourceMasterListResponse toListResponse(ResourceMaster resource) {
        return new ResourceMasterListResponse(
                resource.getId(),
                resource.getResourceCode(),
                resource.getResourceName(),
                resource.getModule(),
                resource.getStatus(),
                resource.getIsSystem(),
                toLocalDateTime(resource.getCreatedAt())
        );
    }

    /**
     * Apply update fields from UpdateResourceMasterRequest to an existing ResourceMaster.
     *
     * @param resource the existing resource entity
     * @param request  the update request
     * @return the updated ResourceMaster entity
     */
    public ResourceMaster applyUpdate(ResourceMaster resource, UpdateResourceMasterRequest request) {
        return resource.toBuilder()
                .resourceCode(request.resourceCode() != null ? request.resourceCode().trim().toUpperCase() : resource.getResourceCode())
                .resourceName(request.resourceName() != null ? request.resourceName().trim() : resource.getResourceName())
                .module(request.module() != null ? request.module().trim().toUpperCase() : resource.getModule())
                .description(request.description() != null ? request.description().trim() : resource.getDescription())
                .status(request.status() != null ? request.status() : resource.getStatus())
                .isSystem(request.isSystem() != null ? request.isSystem() : resource.getIsSystem())
                .build();
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
