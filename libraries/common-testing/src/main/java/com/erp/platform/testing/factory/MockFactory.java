package com.erp.platform.testing.factory;

import com.erp.platform.common.model.vo.Email;
import com.erp.platform.common.model.vo.Money;
import com.erp.platform.common.model.vo.Name;
import com.erp.platform.common.model.vo.Phone;
import com.erp.platform.security.principal.AuthenticatedUser;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Mock factory for creating test data.
 *
 * <p>Provides convenient methods for creating mock objects
 * and test data for unit and integration tests.
 *
 * @since 1.0.0
 */
public class MockFactory {

    private MockFactory() {
        // Utility class
    }

    /**
     * Creates a mock AuthenticatedUser.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return a mock user
     */
    public static AuthenticatedUser mockUser(String userId, Long tenantId) {
        return new AuthenticatedUser() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getEmail() {
                return userId + "@test.com";
            }

            @Override
            public String getFullName() {
                return "Test User";
            }

            @Override
            public Long getTenantId() {
                return tenantId;
            }

            @Override
            public Set<String> getRoles() {
                Set<String> roles = new HashSet<>();
                roles.add("USER");
                return roles;
            }

            @Override
            public Set<String> getPermissions() {
                Set<String> permissions = new HashSet<>();
                permissions.add("read");
                permissions.add("write");
                return permissions;
            }

            @Override
            public boolean hasRole(String role) {
                return getRoles().contains(role);
            }

            @Override
            public boolean hasPermission(String permission) {
                return getPermissions().contains(permission);
            }

            @Override
            public boolean hasAnyRole(String... roles) {
                for (String role : roles) {
                    if (hasRole(role)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasAllPermissions(String... permissions) {
                for (String permission : permissions) {
                    if (!hasPermission(permission)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Creates a test Name.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @return a Name instance
     */
    public static Name mockName(String firstName, String lastName) {
        return new Name(firstName, lastName);
    }

    /**
     * Creates a test Email.
     *
     * @param email the email string
     * @return an Email instance
     */
    public static Email mockEmail(String email) {
        return new Email(email);
    }

    /**
     * Creates a test Phone.
     *
     * @param phone the phone string
     * @return a Phone instance
     */
    public static Phone mockPhone(String phone) {
        return new Phone(phone);
    }

    /**
     * Creates a test Money.
     *
     * @param amount the amount
     * @param currency the currency
     * @return a Money instance
     */
    public static Money mockMoney(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), java.util.Currency.getInstance(currency));
    }
}
