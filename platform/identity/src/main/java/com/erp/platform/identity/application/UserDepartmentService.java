package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignDepartmentRequest;
import com.erp.platform.identity.application.dto.UserDepartmentListResponse;
import com.erp.platform.identity.application.dto.UserDepartmentResponse;
import com.erp.platform.identity.domain.Department;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserDepartment;
import com.erp.platform.identity.domain.exception.UserDepartmentNotFoundException;
import com.erp.platform.identity.domain.exception.UserDepartmentOperationException;
import com.erp.platform.identity.infrastructure.persistence.DepartmentRepository;
import com.erp.platform.identity.infrastructure.persistence.UserDepartmentRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user-department assignments.
 *
 * <p>Provides business logic for assigning users to departments,
 * removing assignments, and listing department memberships.
 * All operations are tenant-scoped for data isolation.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class UserDepartmentService {

    private static final String SYSTEM_ASSIGNED_BY = "system";

    private final UserDepartmentRepository userDepartmentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public UserDepartmentService(
            UserDepartmentRepository userDepartmentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.userDepartmentRepository = userDepartmentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    /**
     * Assigns a department to a user.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>User must exist and belong to the current tenant</li>
     *   <li>Department must exist and belong to the current tenant</li>
     *   <li>Assignment must not already exist</li>
     *   <li>Only active departments can be assigned</li>
     * </ul>
     *
     * @param request the assignment request
     * @return the created assignment response
     * @throws UserDepartmentOperationException if business rules are violated
     */
    public UserDepartmentResponse assignDepartment(AssignDepartmentRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate user exists and belongs to current tenant
        User user = userRepository.findByIdAndTenantId(request.userId(), tenantId)
                .orElseThrow(() -> new UserDepartmentNotFoundException("User not found with ID: " + request.userId()));

        // Validate department exists and belongs to current tenant
        Department department = departmentRepository.findById(request.departmentId())
                .filter(dept -> dept.getTenantId().equals(tenantId))
                .orElseThrow(() -> new UserDepartmentNotFoundException("Department not found with ID: " + request.departmentId()));

        // Validate department is active
        if (!department.getStatus().equals(com.erp.platform.identity.domain.DepartmentStatus.ACTIVE)) {
            throw new UserDepartmentOperationException("Cannot assign user to inactive department");
        }

        // Check if assignment already exists
        if (userDepartmentRepository.existsByTenantIdAndUserIdAndDepartmentId(
                tenantId, request.userId(), request.departmentId())) {
            throw new UserDepartmentOperationException(
                    "User is already assigned to this department");
        }

        // Create assignment
        UserDepartment assignment = UserDepartment.assign(
                UUID.randomUUID(),
                request.userId(),
                request.departmentId(),
                tenantId,
                SYSTEM_ASSIGNED_BY,
                Instant.now(),
                request.isPrimary()
        );

        UserDepartment savedAssignment = userDepartmentRepository.save(assignment);
        return mapToResponse(savedAssignment, user, department);
    }

    /**
     * Removes a department assignment from a user.
     *
     * @param userDepartmentId the assignment ID to remove
     * @throws UserDepartmentNotFoundException if assignment not found
     * @throws UserDepartmentOperationException if assignment belongs to different tenant
     */
    public void removeDepartment(Long userDepartmentId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        UserDepartment assignment = userDepartmentRepository.findById(userDepartmentId)
                .orElseThrow(() -> new UserDepartmentNotFoundException(userDepartmentId));

        if (!assignment.getTenantId().equals(tenantId)) {
            throw new UserDepartmentOperationException(
                    "Cannot remove department assignment from different tenant");
        }

        userDepartmentRepository.delete(assignment);
    }

    /**
     * Lists all department assignments for a user.
     *
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return paginated list of department assignments
     */
    @Transactional(readOnly = true)
    public UserDepartmentListResponse listUserDepartments(Long userId, int page, int size) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate user exists and belongs to current tenant
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UserDepartmentNotFoundException("User not found with ID: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<UserDepartment> assignmentPage = userDepartmentRepository.findByTenantIdAndUserId(tenantId, userId, pageable);

        List<UserDepartmentResponse> responses = assignmentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return UserDepartmentListResponse.from(assignmentPage, responses);
    }

    /**
     * Lists all user assignments for a department.
     *
     * @param departmentId the department ID
     * @param page the page number
     * @param size the page size
     * @return paginated list of user assignments
     */
    @Transactional(readOnly = true)
    public UserDepartmentListResponse listDepartmentUsers(Long departmentId, int page, int size) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate department exists and belongs to current tenant
        Department department = departmentRepository.findById(departmentId)
                .filter(dept -> dept.getTenantId().equals(tenantId))
                .orElseThrow(() -> new UserDepartmentNotFoundException("Department not found with ID: " + departmentId));

        Pageable pageable = PageRequest.of(page, size);
        Page<UserDepartment> assignmentPage = userDepartmentRepository.findByTenantIdAndDepartmentId(
                tenantId, departmentId, pageable);

        List<UserDepartmentResponse> responses = assignmentPage.getContent().stream()
                .map(assignment -> mapToResponse(assignment, department))
                .toList();

        return UserDepartmentListResponse.from(assignmentPage, responses);
    }

    // ==============
    // Helper Methods
    // ==============

    private UserDepartmentResponse mapToResponse(UserDepartment assignment, User user, Department department) {
        return new UserDepartmentResponse(
                assignment.getUserDepartmentId(),
                assignment.getUserId(),
                user.getUsername(),
                assignment.getDepartmentId(),
                department.getDepartmentCode(),
                department.getDepartmentName(),
                assignment.isPrimary(),
                assignment.getAssignedBy(),
                assignment.getAssignedAt()
        );
    }

    private UserDepartmentResponse mapToResponse(UserDepartment assignment, Department department) {
        User user = userRepository.findById(assignment.getUserId())
                .orElse(null);
        if (user == null) {
            return new UserDepartmentResponse(
                    assignment.getUserDepartmentId(),
                    assignment.getUserId(),
                    "Unknown",
                    assignment.getDepartmentId(),
                    department.getDepartmentCode(),
                    department.getDepartmentName(),
                    assignment.isPrimary(),
                    assignment.getAssignedBy(),
                    assignment.getAssignedAt()
            );
        }
        return mapToResponse(assignment, user, department);
    }

    private UserDepartmentResponse mapToResponse(UserDepartment assignment) {
        Department department = departmentRepository.findById(assignment.getDepartmentId())
                .orElse(null);
        if (department == null) {
            return new UserDepartmentResponse(
                    assignment.getUserDepartmentId(),
                    assignment.getUserId(),
                    "Unknown",
                    assignment.getDepartmentId(),
                    "Unknown",
                    "Unknown",
                    assignment.isPrimary(),
                    assignment.getAssignedBy(),
                    assignment.getAssignedAt()
            );
        }
        return mapToResponse(assignment, department);
    }
}
