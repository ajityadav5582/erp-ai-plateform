package com.erp.platform.security.token;

import com.erp.platform.security.authentication.AuthenticatedUser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link AuthenticatedUser} implementation backed by platform access-token claims.
 *
 * <p>Populated by the shared
 * {@link com.erp.platform.security.filter.PlatformJwtAuthenticationFilter} after a
 * successful signature/expiry validation. Permissions come straight from the token,
 * so authorization never requires a cross-service call.
 *
 * @since 1.0.0
 */
public class TokenAuthenticatedUser implements AuthenticatedUser {

    private final String userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final Long tenantId;
    private final Set<String> roles;
    private final Set<String> permissions;

    public TokenAuthenticatedUser(String userId, String username, String email, String fullName,
                                  Long tenantId, Collection<String> roles, Collection<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.tenantId = tenantId;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    /**
     * Convenience factory from parsed platform token claims.
     *
     * @param claims the validated token claims
     * @return the authenticated user
     */
    public static TokenAuthenticatedUser from(PlatformTokenClaims claims) {
        return new TokenAuthenticatedUser(
                claims.userId(),
                claims.username(),
                claims.email(),
                claims.fullName(),
                claims.tenantId(),
                claims.roles(),
                claims.permissions());
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public Collection<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    @Override
    public Collection<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public boolean hasAnyRole(String... roleNames) {
        for (String role : roleNames) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(String... permissionNames) {
        for (String permission : permissionNames) {
            if (!permissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the granted permissions as a list, for building a JWT claim.
     */
    public List<String> permissionList() {
        return List.copyOf(permissions);
    }
}
