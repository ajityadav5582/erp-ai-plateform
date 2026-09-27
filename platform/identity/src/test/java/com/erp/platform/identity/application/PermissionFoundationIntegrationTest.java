package com.erp.platform.identity.application;

import com.erp.platform.identity.domain.ActionEntity;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.ResourceMaster;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RolePermission;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * Integration test suite verifying the authorization foundation using permission codes.
 *
 * <p>Tests the following key rules:
 * <ul>
 *   <li>Authorization based strictly on permission codes (e.g. PRODUCT_VIEW, PRODUCT_CREATE, SALE_CREATE, SALE_REFUND, INVENTORY_ADJUST)</li>
 *   <li>Resolves active roles for the specified tenant</li>
 *   <li>Resolves permissions through RolePermission</li>
 *   <li>Filters out INACTIVE permissions</li>
 *   <li>Prevents cross-tenant permission checks</li>
 * </ul>
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PermissionFoundationIntegrationTest {

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRepository userRepository;

    private AuthorizationService authorizationService;

    // State tables
    private final List<UserRoleRecord> userRolesTable = new ArrayList<>();
    private final List<RoleRecord> rolesTable = new ArrayList<>();
    private final List<RolePermissionRecord> rolePermissionsTable = new ArrayList<>();
    private final List<PermissionRecord> permissionsTable = new ArrayList<>();

    private record UserRoleRecord(Long id, Long userId, Long roleId, Long tenantId, Instant revokedAt, Instant expiresAt) {}
    private record RoleRecord(Long id, Long tenantId, String roleCode) {}
    private record RolePermissionRecord(Long id, Long roleId, Long permissionId) {}
    private record PermissionRecord(Long id, String permissionCode, PermissionStatus status) {}

    @BeforeEach
    void setUp() {
        userRolesTable.clear();
        rolesTable.clear();
        rolePermissionsTable.clear();
        permissionsTable.clear();

        authorizationService = new AuthorizationService(currentTenantProvider, rolePermissionRepository, userRepository);

        setupStatefulRolePermissionRepository();
        seedTestData();
    }

    private void setupStatefulRolePermissionRepository() {
        // Query 1: findRoleIdsByUserIdAndTenantId
        lenient().when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    Long userId = inv.getArgument(0);
                    Long tenantId = inv.getArgument(1);
                    Instant now = Instant.now();

                    return userRolesTable.stream()
                            .filter(ur -> ur.userId().equals(userId)
                                    && ur.tenantId().equals(tenantId)
                                    && ur.revokedAt() == null
                                    && (ur.expiresAt() == null || ur.expiresAt().isAfter(now)))
                            .filter(ur -> rolesTable.stream().anyMatch(r -> r.id().equals(ur.roleId()) && r.tenantId().equals(tenantId)))
                            .map(UserRoleRecord::roleId)
                            .distinct()
                            .toList();
                });

        // Query 2: findPermissionCodesByRoleIdsAndTenantId
        lenient().when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(anyList(), anyLong()))
                .thenAnswer(inv -> {
                    List<Long> roleIds = inv.getArgument(0);
                    Long tenantId = inv.getArgument(1);

                    // Filter rolePermissions where roleId is in roleIds AND role belongs to tenantId
                    List<Long> validRoleIds = rolesTable.stream()
                            .filter(r -> roleIds.contains(r.id()) && r.tenantId().equals(tenantId))
                            .map(RoleRecord::id)
                            .toList();

                    List<Long> permissionIds = rolePermissionsTable.stream()
                            .filter(rp -> validRoleIds.contains(rp.roleId()))
                            .map(RolePermissionRecord::permissionId)
                            .toList();

                    return permissionsTable.stream()
                            .filter(p -> permissionIds.contains(p.id()) && p.status() == PermissionStatus.ACTIVE)
                            .map(PermissionRecord::permissionCode)
                            .distinct()
                            .toList();
                });
    }

    private void seedTestData() {
        // Tenant 1 Roles
        rolesTable.add(new RoleRecord(10L, 1L, "SALES_ROLE"));
        rolesTable.add(new RoleRecord(20L, 1L, "INVENTORY_ROLE"));

        // Tenant 2 Roles (Wrong tenant setup)
        rolesTable.add(new RoleRecord(30L, 2L, "TENANT2_ROLE"));

        // User 100 assigned to SALES_ROLE (10L) and INVENTORY_ROLE (20L) in Tenant 1
        userRolesTable.add(new UserRoleRecord(1000L, 100L, 10L, 1L, null, null));
        userRolesTable.add(new UserRoleRecord(1001L, 100L, 20L, 1L, null, null));

        // User 200 assigned to TENANT2_ROLE (30L) in Tenant 2
        userRolesTable.add(new UserRoleRecord(1002L, 200L, 30L, 2L, null, null));

        // Global Permissions
        permissionsTable.add(new PermissionRecord(501L, "PRODUCT_VIEW", PermissionStatus.ACTIVE));
        permissionsTable.add(new PermissionRecord(502L, "PRODUCT_CREATE", PermissionStatus.ACTIVE));
        permissionsTable.add(new PermissionRecord(503L, "SALE_CREATE", PermissionStatus.ACTIVE));
        permissionsTable.add(new PermissionRecord(504L, "SALE_REFUND", PermissionStatus.ACTIVE));
        permissionsTable.add(new PermissionRecord(505L, "INVENTORY_ADJUST", PermissionStatus.INACTIVE)); // INACTIVE permission

        // Role 10 (SALES_ROLE) has PRODUCT_VIEW and PRODUCT_CREATE
        rolePermissionsTable.add(new RolePermissionRecord(1L, 10L, 501L));
        rolePermissionsTable.add(new RolePermissionRecord(2L, 10L, 502L));

        // Role 20 (INVENTORY_ROLE) has SALE_CREATE and INVENTORY_ADJUST (inactive)
        rolePermissionsTable.add(new RolePermissionRecord(3L, 20L, 503L));
        rolePermissionsTable.add(new RolePermissionRecord(4L, 20L, 505L)); // Inactive permission mapping

        // Role 30 (Tenant 2) has SALE_REFUND
        rolePermissionsTable.add(new RolePermissionRecord(5L, 30L, 504L));
    }

    @Test
    @DisplayName("Permitted user: returns true when active permission is assigned to user role")
    void hasPermission_returnsTrue_forPermittedUser() {
        Long userId = 100L;
        Long tenantId = 1L;

        assertTrue(authorizationService.hasPermission(userId, tenantId, "PRODUCT_VIEW"));
        assertTrue(authorizationService.hasPermission(userId, tenantId, "PRODUCT_CREATE"));
    }

    @Test
    @DisplayName("Denied user: returns false when user lacks requested permission code")
    void hasPermission_returnsFalse_forDeniedUser() {
        Long userId = 100L;
        Long tenantId = 1L;

        // User 100 in Tenant 1 does not have SALE_REFUND
        assertFalse(authorizationService.hasPermission(userId, tenantId, "SALE_REFUND"));
    }

    @Test
    @DisplayName("Multiple roles: resolves permissions across all active user roles in the tenant")
    void hasPermission_returnsTrue_whenUserHasMultipleRoles() {
        Long userId = 100L;
        Long tenantId = 1L;

        // User 100 has Role 10 (PRODUCT_VIEW) and Role 20 (SALE_CREATE)
        assertTrue(authorizationService.hasPermission(userId, tenantId, "PRODUCT_VIEW"));
        assertTrue(authorizationService.hasPermission(userId, tenantId, "SALE_CREATE"));
    }

    @Test
    @DisplayName("Inactive permission: returns false when permission status is INACTIVE")
    void hasPermission_returnsFalse_whenPermissionIsInactive() {
        Long userId = 100L;
        Long tenantId = 1L;

        // INVENTORY_ADJUST is assigned to Role 20, but permission status is INACTIVE
        assertFalse(authorizationService.hasPermission(userId, tenantId, "INVENTORY_ADJUST"));
    }

    @Test
    @DisplayName("Wrong tenant: prevents cross-tenant permission checks")
    void hasPermission_returnsFalse_whenCheckedAgainstWrongTenant() {
        Long userId = 100L; // User belongs to Tenant 1
        Long wrongTenantId = 2L;

        // User 100 has PRODUCT_CREATE in Tenant 1, but check is executed for Tenant 2
        assertFalse(authorizationService.hasPermission(userId, wrongTenantId, "PRODUCT_CREATE"));
    }
}
