package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when a user is not found.
 *
 * <p>Reports {@code 404 NOT_FOUND}.
 *
 * @since 1.0.0
 */
public class UserNotFoundException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    /**
     * Creates a UserNotFoundException for a specific user ID.
     *
     * @param userId the user ID that was not found
     * @return the exception
     * @since 1.0.0
     */
    public static UserNotFoundException byUserId(Long userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    /**
     * Creates a UserNotFoundException for a specific user UUID.
     *
     * @param userUuid the user UUID that was not found
     * @return the exception
     * @since 1.0.0
     */
    public static UserNotFoundException byUserUuid(UUID userUuid) {
        return new UserNotFoundException("User not found with UUID: " + userUuid);
    }
}
