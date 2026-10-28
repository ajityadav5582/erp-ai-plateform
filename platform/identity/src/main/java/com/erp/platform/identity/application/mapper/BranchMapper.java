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
 * <p>A branch carries a free-text address and an optional ward number.
 *
 * @since 1.0.0
 */
@Component
public class BranchMapper {

    /**
     * Convert CreateBranchRequest to Branch entity.
     *
     * <p>Uses {@link Branch#updateDetails} rather than {@code toBuilder()} so the
     * company id is never lost and the entity keeps its invariants.
     *
     * @param request the create request
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @return the Branch entity
     */
    public Branch toEntity(CreateBranchRequest request, Long tenantId, Long companyId) {
        Branch branch = Branch.create(tenantId, companyId, request.branchCode(), request.branchName());
        branch.updateDetails(
                null, null,
                request.address(), request.wardNo());
        return branch;
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
                branch.getTenantId(),
                branch.getCompanyId(),
                branch.getBranchCode(),
                branch.getBranchName(),
                branch.getAddress(),
                branch.getWardNo(),
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
                branch.getCompanyId(),
                branch.getBranchCode(),
                branch.getBranchName(),
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
