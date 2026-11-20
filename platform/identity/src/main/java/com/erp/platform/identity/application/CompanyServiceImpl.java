package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.application.dto.CompanyResponse;
import com.erp.platform.identity.domain.Branch;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserBranch;
import com.erp.platform.identity.infrastructure.persistence.BranchRepository;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import com.erp.platform.identity.infrastructure.persistence.UserBranchRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.platform.security.authentication.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link CompanyService} with automatic branch and warehouse
 * provisioning on company creation.
 *
 * <p>All operations run inside a single {@code @Transactional} boundary so that
 * the bootstrap either succeeds completely or rolls back entirely.
 *
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final WarehouseService warehouseService;
    private final CurrentTenantProvider currentTenantProvider;

    public CompanyServiceImpl(
            CompanyRepository companyRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            UserBranchRepository userBranchRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) WarehouseService warehouseService,
            CurrentTenantProvider currentTenantProvider) {
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.warehouseService = warehouseService;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    public CompanyResponse createCompany(Long tenantId, Long creatorUserId, CreateCompanyRequest request) {
        Long resolvedTenantId = tenantId == null ? currentTenantProvider.getCurrentTenantId() : tenantId;
        Long resolvedCreatorUserId = creatorUserId == null ? getCurrentUserId() : creatorUserId;

        if (resolvedTenantId == null) {
            throw new IllegalStateException("Tenant ID must be set in the business context");
        }

        if (companyRepository.existsByTenantIdAndPanNumberIgnoreCase(resolvedTenantId, request.panNumber().trim())) {
            throw new IllegalStateException("A company with this PAN number already exists");
        }
        if (companyRepository.existsByTenantIdAndCompanyNameIgnoreCase(resolvedTenantId, request.companyName().trim())) {
            throw new IllegalStateException("A company with this name already exists");
        }

        String companyCode = "PAN-" + request.panNumber().trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (companyCode.length() > 50) {
            companyCode = companyCode.substring(0, 50);
        }

        Company company = Company.create(
                resolvedTenantId,
                companyCode,
                request.companyName(),
                request.panNumber(),
                request.city(),
                request.phone(),
                request.email(),
                request.companyType(),
                request.businessType(),
                request.fiscalYearType(),
                request.vatRegistered());
        company.updateVatRegistration(request.vatRegistrationNumber(), request.vatEffectiveFrom(), request.vatEffectiveTo());
        company.updateLocation(request.provinceId(), request.districtId(), request.localLevelId(), request.wardNo(), request.toleStreet());
        if (request.address() != null && !request.address().isBlank()) {
            company.updateDetails(null, null, null, null, request.address());
        }

        Company savedCompany = companyRepository.save(company);
        log.info("Created company {} (id={}) for tenant {}", savedCompany.getCompanyName(), savedCompany.getId(), resolvedTenantId);

        // ==================== BOOTSTRAP: Head Office Branch ====================
        Branch headOffice = Branch.create(resolvedTenantId, savedCompany.getId(), "HO", "Head Office");
        String branchAddress = savedCompany.getAddress() != null && !savedCompany.getAddress().isBlank()
                ? savedCompany.getAddress()
                : (savedCompany.getToleStreet() != null && !savedCompany.getToleStreet().isBlank()
                        ? savedCompany.getToleStreet()
                        : savedCompany.getCity());
        headOffice.updateDetails(null, "Head Office", branchAddress, savedCompany.getWardNo());
        headOffice.markAsHeadOffice();
        headOffice.updateGeography(savedCompany.getProvinceId(), savedCompany.getDistrictId(), savedCompany.getLocalLevelId());
        Branch savedBranch = branchRepository.save(headOffice);
        log.info("Created head office branch {} (id={}) for company {}", savedBranch.getBranchName(), savedBranch.getId(), savedCompany.getId());

        // ==================== BOOTSTRAP: Main Store Warehouse ====================
        if (warehouseService != null) {
            try {
            WarehouseService.WarehouseCreateRequest warehouseRequest =
                    new WarehouseService.WarehouseCreateRequest() {
                        @Override
                        public String getCode() { return "WH-MAIN"; }
                        @Override
                        public String getName() { return "Main Store"; }
                        @Override
                        public String getDescription() { return branchAddress; }
                        @Override
                        public String getAddress() { return branchAddress; }
                        @Override
                        public String getWarehouseType() { return "GENERAL"; }
                        @Override
                        public Boolean getIsActive() { return true; }
                        @Override
                        public Boolean getIsDefault() { return true; }
                        @Override
                        public Long getBranchId() { return savedBranch.getId(); }
                    };

            WarehouseService.WarehouseResponse warehouse = warehouseService.createWarehouse(
                    resolvedTenantId, savedCompany.getId(), warehouseRequest);
            if (warehouse != null && warehouse.getId() != null) {
                savedBranch.assignDefaultWarehouse(warehouse.getId());
                branchRepository.save(savedBranch);
                log.info("Created main store warehouse {} (id={}) for branch {}", warehouse.getCode(), warehouse.getId(), savedBranch.getId());
            }
        } catch (Exception ex) {
            log.warn("Failed to provision default warehouse for company {}: {}", savedCompany.getId(), ex.getMessage(), ex);
        }
        } else {
            log.warn("WarehouseService is not available; skipped warehouse bootstrap for company {}", savedCompany.getId());
        }

        // ==================== BOOTSTRAP: UserBranch Assignment ====================
        if (resolvedCreatorUserId != null) {
            userRepository.findByIdAndTenantId(resolvedCreatorUserId, resolvedTenantId).ifPresent(creator -> {
                boolean userUpdated = false;
                if (creator.getBranchId() == null) {
                    creator.updateBranch(savedBranch.getId());
                    userUpdated = true;
                    log.info("Set creator user {} branch to {}", creator.getId(), savedBranch.getId());
                }
                if (creator.getCompanyId() == null) {
                    creator.updateCompany(savedCompany.getId());
                    userUpdated = true;
                    log.info("Set creator user {} company to {}", creator.getId(), savedCompany.getId());
                }
                if (userUpdated) {
                    userRepository.save(creator);
                }

                if (!userBranchRepository.existsByTenantIdAndCompanyIdAndUserIdAndBranchId(
                        resolvedTenantId, savedCompany.getId(), resolvedCreatorUserId, savedBranch.getId())) {
                    UserBranch userBranch = UserBranch.assign(
                            UUID.randomUUID(), resolvedCreatorUserId, savedBranch.getId(), resolvedTenantId,
                            savedCompany.getId(), "system", Instant.now(), true);
                    userBranchRepository.save(userBranch);
                    log.info("Created primary user-branch assignment for user {} on branch {}", resolvedCreatorUserId, savedBranch.getId());
                }
            });
        }

        return CompanyResponse.from(savedCompany);
    }

    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            String userIdStr = principal.getUserId();
            if (userIdStr != null) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
