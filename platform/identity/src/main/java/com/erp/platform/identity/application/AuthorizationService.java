package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.PermissionCheckRequest;
import com.erp.platform.identity.application.dto.PermissionCheckResponse;
import com.erp.platform.identity.application.dto.ResourceActionDto;
import com.erp.platform.identity.application.dto.UserPermissionsResponse;
import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.Resource;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.exception.AccessDeniedException;
import com.erp.platform.identity.domain.exception.UnauthorizedException;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.security.authentication.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for authorization checks.
 *
 * <p>Provides role-based access control (RBAC) utilities for the identity service.
 * Distinguishes between platform-level SUPER_ADMIN and tenant-level roles.
 *
 * <p>SUPER_ADMIN (platform-level):
 * <ul>
 *   <li>Has full access across all tenants</li>
 *   <li>Can manage all tenants, users, roles, etc.</li>
 *   <li>Bypasses tenant isolation checks</li>
 * </ul>
 *
 * <p>Tenant-level roles (OWNER, ADMIN, MANAGER, CASHIER, INVENTORY_MANAGER, ACCOUNTANT):
 * <ul>
 *   <li>Can only operate within their own tenant</li>
 *   <li>Authorization is based on permission codes, not role names</li>
 *   <li>Cannot access other tenants' data</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final CurrentTenantProvider currentTenantProvider;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    /**
     * Checks if the current authenticated user is a SUPER_ADMIN.
     *
     * <p>SUPER_ADMIN is a platform-level role that has full access across all tenants.
     *
     * @return true if the current user has SUPER_ADMIN role
     * @since 1.0.0
     */
    public boolean isSuperAdmin() {
        return hasRole(RoleConstants.SUPER_ADMIN);
    }

    /**
     * Checks if the current authenticated user has the OWNER role.
     *
     * <p>OWNER is a tenant-level role with full access within their tenant.
     *
     * @return true if the current user has OWNER role
     * @since 1.0.0
     */
    public boolean isOwner() {
        return hasRole(RoleConstants.OWNER);
    }

    /**
     * Checks if the current authenticated user has the ADMIN role.
     *
     * <p>ADMIN is a tenant-level role with management access within their tenant.
     *
     * @return true if the current user has ADMIN role
     * @since 1.0.0
     */
    public boolean isAdmin() {
        return hasRole(RoleConstants.ADMIN);
    }

    /**
     * Checks if the current authenticated user has any administrative role (SUPER_ADMIN, OWNER, TENANT_ADMIN, or ADMIN).
     *
     * <p>This is useful for operations that can be performed by platform or tenant administrators.
     *
     * @return true if the current user has an administrative role
     * @since 1.0.0
     */
    public boolean isAnyAdmin() {
        return isSuperAdmin() || isOwner() || isAdmin() || hasRole(RoleConstants.TENANT_ADMIN);
    }

    /**
     * Requires that the current user is a SUPER_ADMIN.
     *
     * <p>Throws {@link UnauthorizedException} if the user is not a SUPER_ADMIN.
     *
     * @throws UnauthorizedException if the user is not a SUPER_ADMIN
     * @since 1.0.0
     */
    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw UnauthorizedException.forRole(RoleConstants.SUPER_ADMIN);
        }
    }

    /**
     * Requires that the current user has the OWNER role.
     *
     * <p>Throws {@link UnauthorizedException} if the user is not an OWNER.
     *
     * @throws UnauthorizedException if the user is not an OWNER
     * @since 1.0.0
     */
    public void requireOwner() {
        if (!isOwner()) {
            throw UnauthorizedException.forRole(RoleConstants.OWNER);
        }
    }

    /**
     * Requires that the current user has the ADMIN role.
     *
     * <p>Throws {@link UnauthorizedException} if the user is not an ADMIN.
     *
     * @throws UnauthorizedException if the user is not an ADMIN
     * @since 1.0.0
     */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw UnauthorizedException.forRole(RoleConstants.ADMIN);
        }
    }

    /**
     * Requires that the current user has any administrative role (SUPER_ADMIN, OWNER, TENANT_ADMIN, or ADMIN).
     *
     * <p>Throws {@link UnauthorizedException} if the user does not have any administrative role.
     *
     * @throws UnauthorizedException if the user is not an admin
     * @since 1.0.0
     */
    public void requireAnyAdmin() {
        if (!isAnyAdmin()) {
            throw UnauthorizedException.forAnyRole(RoleConstants.SUPER_ADMIN, RoleConstants.OWNER, RoleConstants.TENANT_ADMIN, RoleConstants.ADMIN);
        }
    }

    /**
     * Requires that the current user has one of the specified roles.
     *
     * <p>Throws {@link UnauthorizedException} if the user does not have any of the roles.
     *
     * @param roles the required roles
     * @throws UnauthorizedException if the user does not have any of the roles
     * @since 1.0.0
     */
    public void requireAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return;
            }
        }
        throw UnauthorizedException.forAnyRole(roles);
    }

    /**
     * Checks if the current authenticated user has the specified role.
     *
     * @param role the role to check
     * @return true if the user has the role
     * @since 1.0.0
     */
    private boolean hasRole(String role) {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser();
        return authenticatedUser != null && authenticatedUser.hasRole(role);
    }

    // ==================== Permission-Based Access Control ====================

    /**
     * Checks if the current authenticated user has the specified permission for a Resource and Action.
     *
     * @param resource the business resource
     * @param action the action on the resource
     * @return true if access is granted
     */
    public boolean hasPermission(Resource resource, Action action) {
        if (resource == null || action == null) {
            return false;
        }
        String code = Permission.generatePermissionCode(resource, action);
        return hasPermission(code);
    }

    /**
     * Checks if the current authenticated user has the specified permission code.
     *
     * <p>Normalizes permission strings (e.g. USER_READ, USER:READ, USER:READ:USER) to standard RESOURCE_ACTION format.
     * SUPER_ADMIN bypasses all permission checks.
     *
     * @param permissionCode the permission code to check
     * @return true if the user has the permission
     * @since 1.0.0
     */
    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        if (isAnyAdmin()) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions();
        String normalized = normalizePermissionCode(permissionCode);
        if (userPermissions.contains(normalized) || userPermissions.contains(permissionCode)) {
            return true;
        }

        if (normalized.endsWith("_READ")) {
            String viewCode = normalized.substring(0, normalized.length() - 5) + "_VIEW";
            if (userPermissions.contains(viewCode)) return true;
        } else if (normalized.endsWith("_VIEW")) {
            String readCode = normalized.substring(0, normalized.length() - 5) + "_READ";
            if (userPermissions.contains(readCode)) return true;
        }

        return false;
    }

    /**
     * Reusable authorization method to check if a user has a specific permission code in a tenant.
     *
     * <p>Resolves the user's active roles for the specified tenant and verifies if the permission
     * is assigned and ACTIVE through RolePermission, preventing cross-tenant access.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param permissionCode the permission code (e.g. PRODUCT_VIEW, PRODUCT_CREATE, SALE_CREATE, SALE_REFUND, INVENTORY_ADJUST)
     * @return true if access is granted, false otherwise
     * @since 1.0.0
     */
    public boolean hasPermission(Long userId, Long tenantId, String permissionCode) {
        if (userId == null || tenantId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }

        Set<String> userPermissions = getUserPermissions(tenantId, userId);
        if (userPermissions.isEmpty()) {
            return false;
        }

        String normalized = normalizePermissionCode(permissionCode);
        return userPermissions.contains(normalized) || userPermissions.contains(permissionCode);
    }

    /**
     * Checks if the current authenticated user has any of the specified permissions.
     *
     * @param permissionCodes the permission codes to check
     * @return true if the user has at least one of the permissions
     * @since 1.0.0
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        if (isSuperAdmin()) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions();
        for (String permissionCode : permissionCodes) {
            if (permissionCode == null) continue;
            String normalized = normalizePermissionCode(permissionCode);
            if (userPermissions.contains(normalized) || userPermissions.contains(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the current authenticated user has all of the specified permissions.
     *
     * @param permissionCodes the permission codes to check
     * @return true if the user has all of the permissions
     * @since 1.0.0
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        if (isSuperAdmin()) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions();
        for (String permissionCode : permissionCodes) {
            if (permissionCode == null) return false;
            String normalized = normalizePermissionCode(permissionCode);
            if (!userPermissions.contains(normalized) && !userPermissions.contains(permissionCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requires that the current user has the specified permission.
     *
     * @param permissionCode the required permission code
     * @throws AccessDeniedException if the user does not have the permission
     * @since 1.0.0
     */
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw AccessDeniedException.forPermission(permissionCode);
        }
    }

    /**
     * Requires that the current user has the specified permission for Resource and Action.
     *
     * @param resource the resource
     * @param action the action
     * @throws AccessDeniedException if the user does not have the permission
     */
    public void requirePermission(Resource resource, Action action) {
        if (!hasPermission(resource, action)) {
            String code = Permission.generatePermissionCode(resource, action);
            throw AccessDeniedException.forPermission(code);
        }
    }

    /**
     * Requires that the current user has any of the specified permissions.
     *
     * @param permissionCodes the required permission codes
     * @throws AccessDeniedException if the user does not have any of the permissions
     * @since 1.0.0
     */
    public void requireAnyPermission(String... permissionCodes) {
        if (!hasAnyPermission(permissionCodes)) {
            throw AccessDeniedException.forAnyPermission(permissionCodes);
        }
    }

    /**
     * Requires that the current user has all of the specified permissions.
     *
     * @param permissionCodes the required permission codes
     * @throws AccessDeniedException if the user does not have all of the permissions
     * @since 1.0.0
     */
    public void requireAllPermissions(String... permissionCodes) {
        if (!hasAllPermissions(permissionCodes)) {
            throw AccessDeniedException.forAllPermissions(permissionCodes);
        }
    }

    /**
     * Gets all active permission codes assigned to the current user in the current tenant.
     *
     * @return set of permission codes
     */
    public Set<String> getCurrentUserPermissions() {
        return getUserPermissions();
    }

    /**
     * Gets all active tenant-scoped permissions for the specified user ID and tenant ID.
     *
     * @param tenantId the tenant ID
     * @param userId the user primary key ID
     * @return set of active permission codes
     */
    public Set<String> getUserPermissions(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return Set.of();
        }
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, tenantId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        List<String> permissionCodes = rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(roleIds, tenantId);
        return permissionCodes.stream().collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Generates a complete UserPermissionsResponse object for UI consumption.
     *
     * @return UserPermissionsResponse containing tenant ID, user ID, active roles, permission codes, and structured Resource-Action pairs.
     */
    public UserPermissionsResponse getUserPermissionsResponse() {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        if (tenantId == null && authenticatedUser.getTenantId() != null) {
            tenantId = authenticatedUser.getTenantId();
        }

        Set<String> permissionCodes = getUserPermissions();
        List<String> roles = new ArrayList<>(authenticatedUser.getRoles());

        List<ResourceActionDto> resourceActions = new ArrayList<>();
        for (String code : permissionCodes) {
            ResourceActionDto dto = parseResourceAction(code);
            if (dto != null) {
                resourceActions.add(dto);
            }
        }

        Long userId;
        try {
            userId = Long.valueOf(authenticatedUser.getUserId());
        } catch (Exception e) {
            userId = null;
        }

        return new UserPermissionsResponse(
                userId,
                tenantId,
                roles,
                permissionCodes,
                resourceActions
        );
    }

    /**
     * Evaluates a single permission check request for UI components.
     *
     * @param request the permission check request
     * @return PermissionCheckResponse indicating whether access is granted
     */
    public PermissionCheckResponse evaluatePermission(PermissionCheckRequest request) {
        if (request == null) {
            return PermissionCheckResponse.denied(null, null, null);
        }

        if (request.resource() != null && request.action() != null) {
            boolean granted = hasPermission(request.resource(), request.action());
            String code = Permission.generatePermissionCode(request.resource(), request.action());
            return granted
                    ? PermissionCheckResponse.granted(code, request.resource(), request.action())
                    : PermissionCheckResponse.denied(code, request.resource(), request.action());
        }

        if (request.permissionCode() != null && !request.permissionCode().isBlank()) {
            boolean granted = hasPermission(request.permissionCode());
            ResourceActionDto pair = parseResourceAction(request.permissionCode());
            Resource r = pair != null ? pair.resource() : null;
            Action a = pair != null ? pair.action() : null;
            return granted
                    ? PermissionCheckResponse.granted(request.permissionCode(), r, a)
                    : PermissionCheckResponse.denied(request.permissionCode(), r, a);
        }

        return PermissionCheckResponse.denied(null, null, null);
    }

    /**
     * Batch evaluates multiple permission check requests for UI components.
     *
     * @param requests list of permission check requests
     * @return list of PermissionCheckResponse objects
     */
    public List<PermissionCheckResponse> evaluatePermissions(List<PermissionCheckRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(this::evaluatePermission)
                .collect(Collectors.toList());
    }

    /**
     * Gets all permission codes assigned to the current user's roles in the current tenant.
     *
     * <p>Queries the database to find all permissions assigned to the user's active roles.
     *
     * @return set of permission codes
     * @since 1.0.0
     */
    private Set<String> getUserPermissions() {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser == null) {
            return Set.of();
        }

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        if (tenantId == null && authenticatedUser.getTenantId() != null) {
            tenantId = authenticatedUser.getTenantId();
        }
        if (tenantId == null) {
            log.warn("Cannot check permissions: no tenant context");
            return Set.of();
        }

        // JWT subjects carry the user's numeric primary key.
        Long userId;
        try {
            userId = Long.parseLong(authenticatedUser.getUserId());
        } catch (NumberFormatException e) {
            log.warn("Cannot check permissions: invalid user ID format: {}", authenticatedUser.getUserId());
            return Set.of();
        }

        // Get user's role assignments in the current tenant
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserIdAndTenantId(
                userId, tenantId);

        if (roleIds.isEmpty()) {
            return Set.of();
        }

        // Get all permission codes assigned to those roles
        List<String> permissionCodes = rolePermissionRepository
                .findPermissionCodesByRoleIdsAndTenantId(roleIds, tenantId);

        return permissionCodes.stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Normalizes permission strings like "USER:READ", "user_read", "USER:READ:USER" to "USER_READ".
     */
    private String normalizePermissionCode(String code) {
        if (code == null) return "";
        String trimmed = code.trim().toUpperCase();
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 2) {
                return parts[0] + "_" + parts[1];
            }
        }
        return trimmed;
    }

    /**
     * Parses a permission code into a ResourceActionDto pair if matched.
     * Supports multi-word compound resource names (e.g. ACCOUNTS_PAYABLE_APPROVE).
     */
    private ResourceActionDto parseResourceAction(String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = normalizePermissionCode(code);
        for (Action action : Action.values()) {
            String suffix = "_" + action.getCode();
            if (normalized.endsWith(suffix)) {
                String resourcePart = normalized.substring(0, normalized.length() - suffix.length());
                try {
                    Resource resource = Resource.valueOf(resourcePart);
                    return new ResourceActionDto(resource, action, normalized);
                } catch (IllegalArgumentException ignored) {
                    // try next matching action
                }
            }
        }
        return null;
    }


    /**
     * Gets the current authenticated user from the security context.
     *
     * @return the authenticated user, or null if not authenticated
     */
    private AuthenticatedUser getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return (AuthenticatedUser) principal;
        }
        return null;
    }
}
