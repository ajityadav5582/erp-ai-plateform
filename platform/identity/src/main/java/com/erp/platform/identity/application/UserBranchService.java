package com.erp.platform.identity.application;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.AssignBranchRequest;
import com.erp.platform.identity.application.dto.UserBranchListResponse;
import com.erp.platform.identity.application.dto.UserBranchResponse;
import com.erp.platform.identity.domain.Branch;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserBranch;
import com.erp.platform.identity.domain.exception.UserBranchNotFoundException;
import com.erp.platform.identity.domain.exception.UserBranchOperationException;
import com.erp.platform.identity.infrastructure.persistence.BranchRepository;
import com.erp.platform.identity.infrastructure.persistence.UserBranchRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user-branch assignments.
 *
 * <p>This service handles the assignment of users to branches within a tenant.
 * It enforces tenant isolation by validating that both user and branch belong
 * to the same tenant before creating assignments.
 *
 * <p>Key business rules:
 * <ul>
 *   <li>A user can be assigned to multiple branches</li>
 *   <li>A branch can have multiple users</li>
 *   <li>User and branch must belong to the same tenant</li>
 *   <li>Only one primary branch per user</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Service
public class UserBranchService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_FIELD = "assignedAt";
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);

    private final UserBranchRepository userBranchRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final CurrentTenantProvider currentTenantProvider;
    private final CurrentCompanyProvider currentCompanyProvider;

    /**
     * Creates a new UserBranchService with the required dependencies.
     *
     * @param userBranchRepository the user-branch repository
     * @param userRepository the user repository
     * @param branchRepository the branch repository
     * @param currentTenantProvider the current tenant provider
     * @param currentCompanyProvider the current company provider
     */
    public UserBranchService(
            UserBranchRepository userBranchRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            CurrentTenantProvider currentTenantProvider,
            CurrentCompanyProvider currentCompanyProvider) {
        this.userBranchRepository = userBranchRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.currentTenantProvider = currentTenantProvider;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    /**
     * Assigns a branch to a user.
     *
     * <p>Validates that:
     * <ul>
     *   <li>The user exists and belongs to the current tenant</li>
     *   <li>The branch exists and belongs to the current tenant</li>
     *   <li>The assignment does not already exist</li>
     * </ul>
     *
     * <p>If the assignment is marked as primary, any existing primary branch
     * for the user will be unset.
     *
     * @param request the assignment request
     * @return the created user-branch assignment response
     * @throws UserBranchOperationException if validation fails
     */
    @Transactional
    public UserBranchResponse assignBranch(AssignBranchRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate user exists and belongs to current tenant
        User user = userRepository.findByIdAndTenantId(request.userId(), tenantId)
                .orElseThrow(() -> new UserBranchNotFoundException(
                        "User not found with ID: " + request.userId()));

        // Validate the branch exists and belongs to the active company. Without the
        // company predicate a user could be assigned to a branch of a sibling
        // company owned by the same tenant.
        Long companyId = currentCompanyProvider.getCurrentCompanyIdOrNull();
        Branch branch = (companyId != null
                ? branchRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.branchId())
                : branchRepository.findByTenantIdAndId(tenantId, request.branchId()))
                .orElseThrow(() -> new UserBranchNotFoundException(
                        "Branch not found with ID: " + request.branchId()));

        // Check if assignment already exists
        if (userBranchRepository.existsByTenantIdAndUserIdAndBranchId(tenantId, request.userId(), request.branchId())) {
            throw new UserBranchNotFoundException(
                    "Branch is already assigned to user");
        }

        // Handle primary branch designation
        boolean isPrimary = request.isPrimary() != null && request.isPrimary();
        if (isPrimary) {
            // Remove primary designation from existing primary branch
            userBranchRepository.findByTenantIdAndUserIdAndIsPrimaryTrue(tenantId, request.userId())
                    .ifPresent(existingPrimary -> {
                        existingPrimary.removePrimaryDesignation();
                        userBranchRepository.save(existingPrimary);
                    });
        }

        // Create the assignment
        UserBranch userBranch = UserBranch.assign(
                UUID.randomUUID(),
                request.userId(),
                request.branchId(),
                tenantId,
                "system",
                Instant.now(),
                isPrimary
        );

        UserBranch saved = userBranchRepository.save(userBranch);

        return mapToResponse(saved, user, branch);
    }

    /**
     * Removes a branch assignment from a user.
     *
     * @param userBranchId the user-branch assignment ID
     * @throws UserBranchNotFoundException if the assignment is not found
     */
    @Transactional
    public void removeBranch(Long userBranchId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        UserBranch userBranch = userBranchRepository.findByIdAndTenantId(userBranchId, tenantId)
                .orElseThrow(() -> new UserBranchNotFoundException(userBranchId));

        userBranchRepository.delete(userBranch);
    }

    /**
     * Lists all branch assignments for a user.
     *
     * @param userId the user ID
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return the paginated list of branch assignments
     */
    @Transactional(readOnly = true)
    public UserBranchListResponse listUserBranches(Long userId, int page, int size) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate user exists and belongs to current tenant
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UserBranchNotFoundException(
                        "User not found with ID: " + userId));

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<UserBranch> userBranchPage = userBranchRepository.findByTenantIdAndUserId(tenantId, userId, pageable);

        // Resolve the branches in one query instead of one findById per row (N+1),
        // and without loading branches from other tenants or other companies.
        Long companyId = currentCompanyProvider.getCurrentCompanyIdOrNull();
        Map<Long, Branch> branches = loadBranches(tenantId, companyId,
                userBranchPage.getContent().stream().map(UserBranch::getBranchId).collect(Collectors.toSet()));

        List<UserBranchResponse> responses = userBranchPage.getContent().stream()
                .map(ub -> mapToResponse(ub, user, branches.get(ub.getBranchId())))
                .collect(Collectors.toList());

        return new UserBranchListResponse(
                responses,
                userBranchPage.getNumber(),
                userBranchPage.getSize(),
                userBranchPage.getTotalElements(),
                userBranchPage.getTotalPages(),
                userBranchPage.isLast()
        );
    }

    /**
     * Lists all user assignments for a branch.
     *
     * @param branchId the branch ID
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return the paginated list of user assignments
     */
    @Transactional(readOnly = true)
    public UserBranchListResponse listBranchUsers(Long branchId, int page, int size) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate the branch exists and belongs to the active company, so the
        // assignment list cannot be used to enumerate another company's staff.
        Long companyId = currentCompanyProvider.getCurrentCompanyIdOrNull();
        Branch branch = (companyId != null
                ? branchRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, branchId)
                : branchRepository.findByTenantIdAndId(tenantId, branchId))
                .orElseThrow(() -> new UserBranchNotFoundException(
                        "Branch not found with ID: " + branchId));

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<UserBranch> userBranchPage = userBranchRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable);

        Map<Long, User> users = loadUsers(tenantId,
                userBranchPage.getContent().stream().map(UserBranch::getUserId).collect(Collectors.toSet()));

        List<UserBranchResponse> responses = userBranchPage.getContent().stream()
                .map(ub -> mapToResponse(ub, users.get(ub.getUserId()), branch))
                .collect(Collectors.toList());

        return new UserBranchListResponse(
                responses,
                userBranchPage.getNumber(),
                userBranchPage.getSize(),
                userBranchPage.getTotalElements(),
                userBranchPage.getTotalPages(),
                userBranchPage.isLast()
        );
    }

    /**
     * Loads the given branches for the tenant in a single query.
     *
     * @param tenantId the owning tenant
     * @param companyId the active company, or null to fall back to tenant scope
     * @param branchIds the branch ids referenced by the page
     * @return branch id to branch, containing only rows owned by that scope
     */
    private Map<Long, Branch> loadBranches(Long tenantId, Long companyId, Set<Long> branchIds) {
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return (companyId != null
                ? branchRepository.findAllByIdInAndTenantIdAndCompanyId(branchIds, tenantId, companyId)
                : branchRepository.findAllByIdInAndTenantId(branchIds, tenantId)).stream()
                .collect(Collectors.toMap(Branch::getId, b -> b));
    }

    /**
     * Loads the given users for the tenant in a single query.
     *
     * @param tenantId the owning tenant
     * @param userIds the user ids referenced by the page
     * @return user id to user, containing only rows owned by the tenant
     */
    private Map<Long, User> loadUsers(Long tenantId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdInAndTenantId(userIds, tenantId).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * Maps a UserBranch entity to a UserBranchResponse DTO.
     *
     * @param userBranch the user-branch entity
     * @param user the user entity
     * @param branch the branch entity
     * @return the response DTO
     */
    private UserBranchResponse mapToResponse(UserBranch userBranch, User user, Branch branch) {
        return new UserBranchResponse(
                userBranch.getUserBranchId(),
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                userBranch.getBranchId(),
                branch != null ? branch.getBranchCode() : null,
                branch != null ? branch.getBranchName() : null,
                userBranch.isPrimary(),
                userBranch.getAssignedBy(),
                userBranch.getAssignedAt()
        );
    }
}
