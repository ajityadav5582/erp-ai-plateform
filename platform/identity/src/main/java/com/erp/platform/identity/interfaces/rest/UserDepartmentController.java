package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.UserDepartmentService;
import com.erp.platform.identity.application.dto.AssignDepartmentRequest;
import com.erp.platform.identity.application.dto.UserDepartmentListResponse;
import com.erp.platform.identity.application.dto.UserDepartmentResponse;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-department assignment operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Assigning a department to a user</li>
 *   <li>Removing a department assignment from a user</li>
 *   <li>Listing departments for a user</li>
 *   <li>Listing users in a department</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/identity/user-departments")
public class UserDepartmentController {

    private final UserDepartmentService userDepartmentService;

    public UserDepartmentController(UserDepartmentService userDepartmentService) {
        this.userDepartmentService = userDepartmentService;
    }

    /**
     * Assigns a department to a user.
     *
     * @param request the assignment request
     * @return the created assignment
     */
    @PostMapping("/assign")
    @RequirePermission("USER_DEPARTMENT_CREATE")
    public ResponseEntity<UserDepartmentResponse> assignDepartment(@Valid @RequestBody AssignDepartmentRequest request) {
        UserDepartmentResponse response = userDepartmentService.assignDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Removes a department assignment from a user.
     *
     * @param userDepartmentId the assignment ID to remove
     * @return no content
     */
    @DeleteMapping("/{userDepartmentId}")
    @RequirePermission("USER_DEPARTMENT_DELETE")
    public ResponseEntity<Void> removeDepartment(@PathVariable Long userDepartmentId) {
        userDepartmentService.removeDepartment(userDepartmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists all department assignments for a user.
     *
     * @param userId the user ID
     * @param page the page number (default 0)
     * @param size the page size (default 20)
     * @return paginated list of department assignments
     */
    @GetMapping("/user/{userId}")
    @RequirePermission("USER_DEPARTMENT_READ")
    public ResponseEntity<UserDepartmentListResponse> listUserDepartments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserDepartmentListResponse response = userDepartmentService.listUserDepartments(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all user assignments for a department.
     *
     * @param departmentId the department ID
     * @param page the page number (default 0)
     * @param size the page size (default 20)
     * @return paginated list of user assignments
     */
    @GetMapping("/department/{departmentId}")
    @RequirePermission("USER_DEPARTMENT_READ")
    public ResponseEntity<UserDepartmentListResponse> listDepartmentUsers(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserDepartmentListResponse response = userDepartmentService.listDepartmentUsers(departmentId, page, size);
        return ResponseEntity.ok(response);
    }
}
