package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import com.erp.platform.identity.application.mapper.BranchMapper;
import com.erp.platform.identity.domain.Branch;
import com.erp.platform.identity.domain.exception.BranchNotFoundException;
import com.erp.platform.identity.domain.exception.DuplicateBranchCodeException;
import com.erp.platform.identity.domain.exception.DuplicateBranchNameException;
import com.erp.platform.identity.infrastructure.persistence.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of BranchService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    public BranchServiceImpl(BranchRepository branchRepository, BranchMapper branchMapper) {
        this.branchRepository = branchRepository;
        this.branchMapper = branchMapper;
    }

    @Override
    public BranchResponse createBranch(Long tenantId, CreateBranchRequest request) {
        if (branchRepository.existsByTenantIdAndBranchCode(tenantId, request.branchCode())) {
            throw new DuplicateBranchCodeException(
                "Branch with code '" + request.branchCode() + "' already exists in this tenant"
            );
        }
        if (branchRepository.existsByTenantIdAndBranchName(tenantId, request.branchName())) {
            throw new DuplicateBranchNameException(
                "Branch with name '" + request.branchName() + "' already exists in this tenant"
            );
        }

        Branch branch = branchMapper.toEntity(request, tenantId);
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long tenantId, UUID branchId) {
        Branch branch = branchRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchByCode(Long tenantId, String branchCode) {
        Branch branch = branchRepository.findByTenantIdAndBranchCode(tenantId, branchCode)
            .orElseThrow(() -> new BranchNotFoundException(
                "Branch not found with code: " + branchCode
            ));
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchListResponse> listBranches(Long tenantId, Pageable pageable) {
        return branchRepository.findByTenantId(tenantId, pageable)
            .map(branchMapper::toListResponse);
    }

    @Override
    public BranchResponse updateBranch(Long tenantId, UUID branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));

        if (request.branchCode() != null
                && !request.branchCode().equalsIgnoreCase(branch.getBranchCode())
                && branchRepository.existsByTenantIdAndBranchCode(tenantId, request.branchCode())) {
            throw new DuplicateBranchCodeException(
                "Branch with code '" + request.branchCode() + "' already exists in this tenant"
            );
        }
        if (request.branchName() != null
                && !request.branchName().equalsIgnoreCase(branch.getBranchName())
                && branchRepository.existsByTenantIdAndBranchName(tenantId, request.branchName())) {
            throw new DuplicateBranchNameException(
                "Branch with name '" + request.branchName() + "' already exists in this tenant"
            );
        }

        Branch updated = branch.toBuilder()
            .branchCode(request.branchCode() != null ? request.branchCode() : branch.getBranchCode())
            .branchName(request.branchName() != null ? request.branchName() : branch.getBranchName())
            .email(request.email() != null ? request.email() : branch.getEmail())
            .phone(request.phone() != null ? request.phone() : branch.getPhone())
            .address(request.address() != null ? request.address() : branch.getAddress())
            .city(request.city() != null ? request.city() : branch.getCity())
            .state(request.state() != null ? request.state() : branch.getState())
            .country(request.country() != null ? request.country() : branch.getCountry())
            .postalCode(request.postalCode() != null ? request.postalCode() : branch.getPostalCode())
            .timezone(request.timezone() != null ? request.timezone() : branch.getTimezone())
            .currency(request.currency() != null ? request.currency() : branch.getCurrency())
            .managerId(request.managerId() != null ? request.managerId() : branch.getManagerId())
            .build();

        Branch savedBranch = branchRepository.save(updated);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public BranchResponse activateBranch(Long tenantId, UUID branchId) {
        Branch branch = branchRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));
        branch.activate();
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public BranchResponse deactivateBranch(Long tenantId, UUID branchId) {
        Branch branch = branchRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));
        branch.deactivate();
        Branch savedBranch = branchRepository.save(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    public void deleteBranch(Long tenantId, UUID branchId) {
        Branch branch = branchRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .orElseThrow(() -> new BranchNotFoundException("Branch not found with ID: " + branchId));
        branchRepository.delete(branch);
    }
}
