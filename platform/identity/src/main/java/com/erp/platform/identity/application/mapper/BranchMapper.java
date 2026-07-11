package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.domain.Branch;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between Branch entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class BranchMapper {

    /**
     * Convert CreateBranchRequest to Branch entity.
     *
     * @param request the create request
     * @param tenantId the tenant ID
     * @return the Branch entity
     */
    public Branch toEntity(CreateBranchRequest request, Long tenantId) {
        Branch branch = Branch.create(tenantId, request.branchCode(), request.branchName());
        return branch.toBuilder()
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .country(request.country())
                .postalCode(request.postalCode())
                .timezone(request.timezone())
                .currency(request.currency())
                .managerId(request.managerId())
                .build();
    }

    /**
     * Convert Branch entity to BranchResponse.
     *
     * @param branch the branch entity
     * @return the BranchResponse
     */
    public BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getBranchId(),
                branch.getTenantId(),
                branch.getBranchCode(),
                branch.getBranchName(),
                branch.getEmail(),
                branch.getPhone(),
                branch.getAddress(),
                branch.getCity(),
                branch.getState(),
                branch.getCountry(),
                branch.getPostalCode(),
                branch.getTimezone(),
                branch.getCurrency(),
                branch.getManagerId(),
                branch.getStatus(),
                toLocalDateTime(branch.getCreatedAt()),
                toLocalDateTime(branch.getUpdatedAt()),
                branch.getCreatedBy(),
                branch.getUpdatedBy(),
                branch.getVersion()
        );
    }

    /**
     * Convert Branch entity to BranchListResponse.
     *
     * @param branch the branch entity
     * @return the BranchListResponse
     */
    public BranchListResponse toListResponse(Branch branch) {
        return new BranchListResponse(
                branch.getId(),
                branch.getBranchId(),
                branch.getBranchCode(),
                branch.getBranchName(),
                branch.getCity(),
                branch.getCountry(),
                branch.getManagerId(),
                branch.getStatus(),
                toLocalDateTime(branch.getCreatedAt())
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
