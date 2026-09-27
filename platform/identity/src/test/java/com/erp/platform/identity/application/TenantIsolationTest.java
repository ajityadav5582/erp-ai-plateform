package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.mapper.UserRoleMapper;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    @Mock
    private AuthorizationService authorizationService;

    private UserRoleServiceImpl userRoleService;

    @BeforeEach
    void setUp() {
        userRoleService = new UserRoleServiceImpl(
                userRoleRepository,
                userRepository,
                roleRepository,
                new UserRoleMapper(),
                currentTenantProvider,
                authorizationService
        );
    }

    @Test
    void assignRoleRejectsUserFromAnotherTenant() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantA = 1L;
        Long tenantB = 2L;
        User user = user(userId, tenantB);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantA);
        when(userRepository.findByIdAndTenantId(userId, tenantA)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> userRoleService.assignRole(new AssignRoleRequest(userId, roleId, null, false, "admin")));
        verify(roleRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRoleRejectsRoleFromAnotherTenant() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantA = 1L;
        Long tenantB = 2L;
        User user = user(userId, tenantA);
        Role role = role(roleId, tenantB);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantA);
        when(userRepository.findByIdAndTenantId(userId, tenantA)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(roleId, tenantA)).thenReturn(Optional.of(role));

        assertThrows(IllegalStateException.class,
                () -> userRoleService.assignRole(new AssignRoleRequest(userId, roleId, null, false, "admin")));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRoleUsesTenantScopedRepositoriesAndLongIdentity() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantId = 10L;
        Long assignmentId = 42L;
        User user = user(userId, tenantId);
        Role role = role(roleId, tenantId);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(roleId, tenantId)).thenReturn(Optional.of(role));
        doReturn(false).when(userRoleRepository).existsActiveByUserIdAndTenantIdAndRoleId(
                eq(userId), eq(tenantId), eq(roleId), any(Instant.class));
        doAnswer(invocation -> {
            UserRole saved = invocation.getArgument(0);
            saved.setId(assignmentId);
            return saved;
        }).when(userRoleRepository).save(any(UserRole.class));

        userRoleService.assignRole(new AssignRoleRequest(userId, roleId, null, false, "admin"));

        verify(userRoleRepository).save(argThat(assignment ->
                assignment.getId() != null
                        && assignment.getId().equals(assignmentId)
                        && assignment.getUserId().equals(userId)
                        && assignment.getRoleId().equals(roleId)
                        && assignment.getTenantId().equals(tenantId)));
    }

    @Test
    void removeRoleUsesTenantScopedLookup() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantId = 10L;
        UserRole assignment = UserRole.assign(
                userId, roleId, tenantId, "admin", Instant.now(), null, false);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRoleRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId))
                .thenReturn(Optional.of(assignment));

        userRoleService.removeRole(new RemoveRoleRequest(userId, roleId, "admin", "no longer required"));

        assertTrue(assignment.isRevoked());
        verify(userRoleRepository).findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
        verify(userRoleRepository).save(assignment);
    }

    private User user(Long id, Long tenantId) {
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        return user;
    }

    private Role role(Long id, Long tenantId) {
        Role role = mock(Role.class);
        when(role.getTenantId()).thenReturn(tenantId);
        return role;
    }
}
