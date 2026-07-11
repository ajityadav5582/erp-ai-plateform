package com.erp.platform.identity.application.security;

import com.erp.platform.security.authentication.AuthenticatedUser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link AuthenticatedUser} implementation backed by JWT access-token claims.
 *
 * <p>Constructed by the {@link com.erp.platform.identity.interfaces.rest.filter.JwtAuthenticationFilter}
 * after a successful token validation and exposed to downstream code via the request
 * (and, where applicable, the security context).
 *
 * @since 1.0.0
 */
public class JwtAuthenticatedUser implements AuthenticatedUser {

    private final String userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final Long tenantId;
    private final Set<String> roles;
    private final Set<String> permissions;

    public JwtAuthenticatedUser(String userId, String username, String email, String fullName,
                                Long tenantId, Collection<String> roles, Collection<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.tenantId = tenantId;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
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
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!this.permissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience factory from parsed JWT claims.
     */
    public static JwtAuthenticatedUser from(AccessTokenClaims claims) {
        return new JwtAuthenticatedUser(
                claims.userId(),
                claims.username(),
                claims.email(),
                claims.fullName(),
                claims.tenantId(),
                claims.roles(),
                List.of());
    }
}
