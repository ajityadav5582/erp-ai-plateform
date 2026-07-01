package com.erp.platform.security.tenant;

import java.util.Objects;

/**
 * Represents a tenant principal in the system.
 *
 * <p>This class combines tenant information with user information
 * for multi-tenant authorization.
 *
 * @since 1.0.0
 */
public final class TenantPrincipal {

    private final String tenantId;
    private final String userId;
    private final String username;
    private final String email;

    private TenantPrincipal(String tenantId, String userId, String username, String email) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    /**
     * Creates a new tenant principal.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param email the email
     * @return the tenant principal
     */
    public static TenantPrincipal of(String tenantId, String userId, String username, String email) {
        return new TenantPrincipal(tenantId, userId, username, email);
    }

    /**
     * Returns the tenant ID.
     *
     * @return the tenant ID
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * Returns the user ID.
     *
     * @return the user ID
     */
    public String userId() {
        return userId;
    }

    /**
     * Returns the username.
     *
     * @return the username
     */
    public String username() {
        return username;
    }

    /**
     * Returns the email.
     *
     * @return the email
     */
    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantPrincipal that = (TenantPrincipal) o;
        return Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, userId);
    }

    @Override
    public String toString() {
        return "TenantPrincipal{" +
               "tenantId='" + tenantId + '\'' +
               ", userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               '}';
    }
}
