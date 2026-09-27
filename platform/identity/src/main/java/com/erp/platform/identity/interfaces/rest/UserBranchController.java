package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.UserBranchService;
import com.erp.platform.identity.application.dto.AssignBranchRequest;
import com.erp.platform.identity.application.dto.UserBranchListResponse;
import com.erp.platform.identity.application.dto.UserBranchResponse;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user-branch assignments.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Assigning a branch to a user</li>
 *   <li>Removing a branch assignment from a user</li>
 *   <li>Listing branches assigned to a user</li>
 *   <li>Listing users assigned to a branch</li>
 * </ul>
 *
 * <p>All endpoints are tenant-scoped and require authentication.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/user-branches")
public class UserBranchController {

    private final UserBranchService userBranchService;
    private final CurrentTenantProvider currentTenantProvider;

    /**
     * Creates a new UserBranchController with the required dependencies.
     *
     * @param userBranchService the user-branch service
     * @param currentTenantProvider the current tenant provider
     */
    public UserBranchController(UserBranchService userBranchService, CurrentTenantProvider currentTenantProvider) {
        this.userBranchService = userBranchService;
        this.currentTenantProvider = currentTenantProvider;
    }

    /**
     * Assigns a branch to a user.
     *
     * <p>Requires TENANT_ADMIN or SUPER_ADMIN role.
     *
     * @param request the assignment request
     * @return the created user-branch assignment
     */
    @PostMapping("/assign")
    @RequirePermission("USER_BRANCH_CREATE")
    public ResponseEntity<UserBranchResponse> assignBranch(@Valid @RequestBody AssignBranchRequest request) {
        UserBranchResponse response = userBranchService.assignBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Removes a branch assignment from a user.
     *
     * <p>Requires TENANT_ADMIN or SUPER_ADMIN role.
     *
     * @param userBranchId the user-branch assignment ID
     * @return no content
     */
    @DeleteMapping("/{userBranchId}")
    @RequirePermission("USER_BRANCH_DELETE")
    public ResponseEntity<Void> removeBranch(@PathVariable Long userBranchId) {
        userBranchService.removeBranch(userBranchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists all branches assigned to a user.
     *
     * <p>Requires authentication.
     *
     * @param userId the user ID
     * @param page the page number (0-indexed, default: 0)
     * @param size the page size (default: 20)
     * @return the paginated list of branch assignments
     */
    @GetMapping("/user/{userId}")
    @RequirePermission("USER_BRANCH_READ")
    public ResponseEntity<UserBranchListResponse> listUserBranches(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        UserBranchListResponse response = userBranchService.listUserBranches(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all users assigned to a branch.
     *
     * <p>Requires authentication.
     *
     * @param branchId the branch ID
     * @param page the page number (0-indexed, default: 0)
     * @param size the page size (default: 20)
     * @return the paginated list of user assignments
     */
    @GetMapping("/branch/{branchId}")
    @RequirePermission("USER_BRANCH_READ")
    public ResponseEntity<UserBranchListResponse> listBranchUsers(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        UserBranchListResponse response = userBranchService.listBranchUsers(branchId, page, size);
        return ResponseEntity.ok(response);
    }
}
