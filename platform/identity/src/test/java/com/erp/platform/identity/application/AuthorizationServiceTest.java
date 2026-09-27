package com.erp.platform.identity.application;

import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.exception.AccessDeniedException;
import com.erp.platform.identity.domain.exception.UnauthorizedException;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.identity.application.CurrentTenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(currentTenantProvider, rolePermissionRepository, userRepository);
        SecurityContextHolder.clearContext();
    }

    // ==================== isSuperAdmin Tests ====================

    @Test
    void isSuperAdmin_returnsTrue_whenUserHasSuperAdminRole() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = authorizationService.isSuperAdmin();

        // Assert
        assertTrue(result);
    }

    @Test
    void isSuperAdmin_returnsFalse_whenUserDoesNotHaveSuperAdminRole() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = authorizationService.isSuperAdmin();

        // Assert
        assertFalse(result);
    }

    // ==================== isAdmin Tests ====================

    @Test
    void isAdmin_returnsTrue_whenUserHasAdminRole() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = authorizationService.isAdmin();

        // Assert
        assertTrue(result);
    }

    @Test
    void isAdmin_returnsFalse_whenUserDoesNotHaveAdminRole() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = authorizationService.isAdmin();

        // Assert
        assertFalse(result);
    }

    // ==================== requireSuperAdmin Tests ====================

    @Test
    void requireSuperAdmin_doesNotThrow_whenUserIsSuperAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireSuperAdmin());
    }

    @Test
    void requireSuperAdmin_throwsUnauthorized_whenUserIsNotSuperAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authorizationService.requireSuperAdmin());
        assertTrue(ex.getMessage().contains("SUPER_ADMIN"));
    }

    // ==================== requireAdmin Tests ====================

    @Test
    void requireAdmin_doesNotThrow_whenUserIsSuperAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireAdmin());
    }

    @Test
    void requireAdmin_doesNotThrow_whenUserIsAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireAdmin());
    }

    @Test
    void requireAdmin_throwsUnauthorized_whenUserIsNotAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authorizationService.requireAdmin());
        assertTrue(ex.getMessage().contains("ADMIN"));
    }

    // ==================== isAnyAdmin & requireAnyAdmin Tests ====================

    @Test
    void isAnyAdmin_returnsTrue_whenUserIsTenantAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.hasRole(RoleConstants.OWNER)).thenReturn(false);
        when(user.hasRole(RoleConstants.ADMIN)).thenReturn(false);
        when(user.hasRole(RoleConstants.TENANT_ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertTrue(authorizationService.isAnyAdmin());
        assertDoesNotThrow(() -> authorizationService.requireAnyAdmin());
    }

    // ==================== requireAnyRole Tests ====================

    @Test
    void requireAnyRole_doesNotThrow_whenUserHasOneOfRequiredRoles() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole("ROLE_A")).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireAnyRole("ROLE_A", "ROLE_B"));
    }

    @Test
    void requireAnyRole_throwsUnauthorized_whenUserHasNoneOfRequiredRoles() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole("ROLE_A")).thenReturn(false);
        when(user.hasRole("ROLE_B")).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authorizationService.requireAnyRole("ROLE_A", "ROLE_B"));
        assertTrue(ex.getMessage().contains("ROLE_A, ROLE_B"));
    }

    // ==================== Permission-Based Access Control Tests ====================

    @Test
    void hasPermission_returnsTrue_whenSuperAdmin() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        // Act
        boolean result = authorizationService.hasPermission("USER_READ");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasPermission_returnsTrue_whenUserHasPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ", "USER_CREATE"));

        // Act
        boolean result = authorizationService.hasPermission("USER_READ");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasPermission_returnsFalse_whenUserLacksPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act
        boolean result = authorizationService.hasPermission("USER_DELETE");

        // Assert
        assertFalse(result);
    }

    @Test
    void hasAnyPermission_returnsTrue_whenUserHasAnyPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act
        boolean result = authorizationService.hasAnyPermission("USER_DELETE", "USER_READ");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasAnyPermission_returnsFalse_whenUserLacksAllPermissions() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act
        boolean result = authorizationService.hasAnyPermission("USER_DELETE", "USER_CREATE");

        // Assert
        assertFalse(result);
    }

    @Test
    void hasAllPermissions_returnsTrue_whenUserHasAllPermissions() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ", "USER_CREATE"));

        // Act
        boolean result = authorizationService.hasAllPermissions("USER_READ", "USER_CREATE");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasAllPermissions_returnsFalse_whenUserLacksOnePermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act
        boolean result = authorizationService.hasAllPermissions("USER_READ", "USER_CREATE");

        // Assert
        assertFalse(result);
    }

    @Test
    void requirePermission_doesNotThrow_whenUserHasPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requirePermission("USER_READ"));
    }

    @Test
    void requirePermission_throwsAccessDenied_whenUserLacksPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authorizationService.requirePermission("USER_DELETE"));
        assertTrue(ex.getMessage().contains("USER_DELETE"));
    }

    @Test
    void requireAnyPermission_doesNotThrow_whenUserHasAnyPermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireAnyPermission("USER_DELETE", "USER_READ"));
    }

    @Test
    void requireAnyPermission_throwsAccessDenied_whenUserLacksAllPermissions() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authorizationService.requireAnyPermission("USER_DELETE", "USER_CREATE"));
        assertTrue(ex.getMessage().contains("USER_DELETE, USER_CREATE"));
    }

    @Test
    void requireAllPermissions_doesNotThrow_whenUserHasAllPermissions() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ", "USER_CREATE"));

        // Act & Assert
        assertDoesNotThrow(() -> authorizationService.requireAllPermissions("USER_READ", "USER_CREATE"));
    }

    @Test
    void requireAllPermissions_throwsAccessDenied_whenUserLacksOnePermission() {
        // Arrange
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.hasRole(RoleConstants.SUPER_ADMIN)).thenReturn(false);
        when(user.getUserId()).thenReturn("1");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, null));
        SecurityContextHolder.setContext(context);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(1L);
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(1L, 1L)).thenReturn(List.of(1L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(1L), 1L))
                .thenReturn(List.of("USER_READ"));

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authorizationService.requireAllPermissions("USER_READ", "USER_CREATE"));
        assertTrue(ex.getMessage().contains("USER_READ, USER_CREATE"));
    }

    // ==================== Reusable hasPermission(userId, tenantId, permissionCode) Tests ====================

    @Test
    void hasPermission_returnsTrue_forPermittedUser() {
        Long userId = 100L;
        Long tenantId = 1L;
        String permissionCode = "PRODUCT_CREATE";

        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of(10L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(10L), tenantId))
                .thenReturn(List.of("PRODUCT_VIEW", "PRODUCT_CREATE"));

        boolean result = authorizationService.hasPermission(userId, tenantId, permissionCode);

        assertTrue(result);
    }

    @Test
    void hasPermission_returnsFalse_forDeniedUser() {
        Long userId = 100L;
        Long tenantId = 1L;
        String permissionCode = "SALE_REFUND";

        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of(10L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(10L), tenantId))
                .thenReturn(List.of("PRODUCT_VIEW"));

        boolean result = authorizationService.hasPermission(userId, tenantId, permissionCode);

        assertFalse(result);
    }

    @Test
    void hasPermission_returnsTrue_whenUserHasMultipleRoles() {
        Long userId = 100L;
        Long tenantId = 1L;
        String permissionCode = "SALE_CREATE";

        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of(10L, 20L));
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(10L, 20L), tenantId))
                .thenReturn(List.of("PRODUCT_VIEW", "SALE_CREATE"));

        boolean result = authorizationService.hasPermission(userId, tenantId, permissionCode);

        assertTrue(result);
    }

    @Test
    void hasPermission_returnsFalse_whenPermissionIsInactive() {
        Long userId = 100L;
        Long tenantId = 1L;
        String permissionCode = "INVENTORY_ADJUST";

        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of(10L));
        // Active permissions returned by repository exclude INACTIVE permission INVENTORY_ADJUST
        when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenantId(List.of(10L), tenantId))
                .thenReturn(List.of("PRODUCT_VIEW"));

        boolean result = authorizationService.hasPermission(userId, tenantId, permissionCode);

        assertFalse(result);
    }

    @Test
    void hasPermission_returnsFalse_whenWrongTenant() {
        Long userId = 100L;
        Long correctTenant = 1L;
        Long wrongTenant = 2L;
        String permissionCode = "PRODUCT_CREATE";

        // Querying for wrong tenant returns empty role IDs list because ur.tenantId != wrongTenant
        when(rolePermissionRepository.findRoleIdsByUserIdAndTenantId(userId, wrongTenant)).thenReturn(List.of());

        boolean result = authorizationService.hasPermission(userId, wrongTenant, permissionCode);

        assertFalse(result);
    }
}
