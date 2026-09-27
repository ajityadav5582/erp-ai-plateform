package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for account registration.
 *
 * <p>Tenant provisioning is internal to the SaaS platform. No Company or
 * tenant identifier is accepted from the signup request.
 *
 * <p>Example payload:
 * <pre>
 * {
 *   "firstName": "John",
 *   "lastName": "Smith",
 *   "email": "john@example.com",
 *   "mobile": "+9779812345678",
 *   "password": "Password@123",
 *   "confirmPassword": "Password@123"
 * }
 * </pre>
 *
 * @param firstName       the user's first name
 * @param lastName        the user's last name
 * @param email           the user's email address
 * @param mobile          the user's mobile or phone number
 * @param password        the raw user password
 * @param confirmPassword the password confirmation
 *
 * @since 1.0.0
 */
public record OnboardingRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Mobile or phone number is required")
        @Size(max = 20, message = "Mobile or phone number must not exceed 20 characters")
        String mobile,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 6, max = 100, message = "Confirm password must be between 6 and 100 characters")
        String confirmPassword
) {
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
