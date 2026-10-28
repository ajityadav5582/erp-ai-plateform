package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import com.erp.platform.identity.application.mapper.BranchMapper;
import com.erp.platform.identity.domain.Branch;
import com.erp.platform.identity.domain.exception.BranchNotFoundException;
import com.erp.platform.identity.domain.exception.CannotDeleteBranchException;
import com.erp.platform.identity.domain.exception.DuplicateBranchCodeException;
import com.erp.platform.identity.domain.exception.DuplicateBranchNameException;
import com.erp.platform.identity.infrastructure.persistence.BranchRepository;
import com.erp.platform.identity.infrastructure.persistence.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BranchService.
 *
 * <p>All reads and writes are scoped to {@code (tenantId, companyId)} so that
 * switching the active company in the UI switches the branch list as well.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchMapper branchMapper;

    public BranchServiceImpl(BranchRepository branchRepository,
                             DepartmentRepository departmentRepository,
                             BranchMapper branchMapper) {
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.branchMapper = branchMapper;
    }

    @Override
    public BranchResponse createBranch(Long tenantId, Long companyId, CreateBranchRequest request) {
        // The domain stores the code upper-cased and the name trimmed, so the
        // uniqueness probe has to use the same form. Comparing the raw input let
        // "ktm" and "KTM" both pass the check and then collide on the unique
        // index as a 500 instead of a 409.
        String code = request.branchCode().trim().toUpperCase();
        String name = request.branchName().trim();

        if (branchRepository.existsByTenantIdAndCompanyIdAndBranchCode(tenantId, companyId, code)) {
            throw new DuplicateBranchCodeException(
                "Branch with code '" + code + "' already exists for this company"
            );
        }
        if (branchRepository.existsByTenantIdAndCompanyIdAndBranchName(tenantId, companyId, name)) {
            throw new DuplicateBranchNameException(
                "Branch with name '" + name + "' already exists for this company"
            );
        }

        Branch branch = branchMapper.toEntity(request, tenantId, companyId);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long tenantId, Long companyId, Long branchId) {
        Branch branch = requireBranch(tenantId, companyId, branchId);
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchByCode(Long tenantId, Long companyId, String branchCode) {
        Branch branch = branchRepository.findByTenantIdAndCompanyIdAndBranchCode(
                tenantId, companyId, branchCode.trim().toUpperCase())
            .orElseThrow(() -> new BranchNotFoundException(
                "Branch not found with code: " + branchCode + " for the selected company"
            ));
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchListResponse> listBranches(Long tenantId, Long companyId, Pageable pageable) {
        return branchRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
            .map(branchMapper::toListResponse);
    }

    @Override
    public BranchResponse updateBranch(Long tenantId, Long companyId, Long branchId, UpdateBranchRequest request) {
        Branch branch = requireBranch(tenantId, companyId, branchId);

        // Normalise exactly as the domain does before probing for duplicates,
        // and skip the probe when the value did not actually change.
        if (request.branchCode() != null && !request.branchCode().isBlank()) {
            String code = request.branchCode().trim().toUpperCase();
            if (!code.equals(branch.getBranchCode())
                    && branchRepository.existsByTenantIdAndCompanyIdAndBranchCode(tenantId, companyId, code)) {
                throw new DuplicateBranchCodeException(
                    "Branch with code '" + code + "' already exists for this company"
                );
            }
        }
        if (request.branchName() != null && !request.branchName().isBlank()) {
            String name = request.branchName().trim();
            if (!name.equalsIgnoreCase(branch.getBranchName())
                    && branchRepository.existsByTenantIdAndCompanyIdAndBranchName(tenantId, companyId, name)) {
                throw new DuplicateBranchNameException(
                    "Branch with name '" + name + "' already exists for this company"
                );
            }
        }

        // Mutate the managed entity in place. branch.toBuilder() drops the id and
        // version inherited from OptimisticLock, so save() called persist() and
        // every update inserted a second branch row instead of updating one.
        branch.updateDetails(
            request.branchCode(), request.branchName(), request.address(), request.wardNo());

        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public BranchResponse activateBranch(Long tenantId, Long companyId, Long branchId) {
        Branch branch = requireBranch(tenantId, companyId, branchId);
        branch.activate();
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public BranchResponse deactivateBranch(Long tenantId, Long companyId, Long branchId) {
        Branch branch = requireBranch(tenantId, companyId, branchId);
        branch.deactivate();
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public void deleteBranch(Long tenantId, Long companyId, Long branchId) {
        Branch branch = requireBranch(tenantId, companyId, branchId);

        // departments.branch_id is ON DELETE RESTRICT, so deleting a branch that
        // still has departments surfaced as a raw constraint violation (500).
        // Check up front and report a 409 the caller can act on.
        if (departmentRepository.existsByTenantIdAndBranchId(tenantId, branchId)) {
            throw new CannotDeleteBranchException(
                "Branch " + branchId + " still has departments. Move or delete them first.");
        }

        branchRepository.delete(branch);
    }

    /**
     * Loads a branch, rejecting it when it belongs to another company of the
     * same tenant. Without the company predicate a user who switched company
     * could still read or mutate the previous company's branches by id.
     */
    private Branch requireBranch(Long tenantId, Long companyId, Long branchId) {
        return branchRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));
    }
}
