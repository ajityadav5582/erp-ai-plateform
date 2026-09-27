package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user-branch assignment is not found.
 *
 * <p>This exception is raised when attempting to access, update, or delete
 * a user-branch assignment that does not exist in the system.
 *
 * @since 1.0.0
 */
public class UserBranchNotFoundException extends UserBranchOperationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new UserBranchNotFoundException with a descriptive message.
     *
     * @param userBranchId the user-branch assignment ID that was not found
     */
    public UserBranchNotFoundException(Long userBranchId) {
        super("User-branch assignment not found with ID: " + userBranchId);
    }

    /**
     * Creates a new UserBranchNotFoundException with a descriptive message.
     *
     * @param userId the user ID
     * @param branchId the branch ID
     */
    public UserBranchNotFoundException(Long userId, Long branchId) {
        super("User-branch assignment not found for user ID: " + userId + " and branch ID: " + branchId);
    }

    public UserBranchNotFoundException(String message) {
        super(message);
    }
}
