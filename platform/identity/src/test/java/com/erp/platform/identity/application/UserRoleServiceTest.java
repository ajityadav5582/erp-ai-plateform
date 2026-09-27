package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

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
    private UserRoleMapper userRoleMapper;

    @BeforeEach
    void setUp() {
        userRoleMapper = new UserRoleMapper();
        userRoleService = new UserRoleServiceImpl(
                userRoleRepository,
                userRepository,
                roleRepository,
                userRoleMapper,
                currentTenantProvider,
                authorizationService
        );
    }

    @Test
    void assignRoleUsesLongIdAndTenantScopedRepositories() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantId = 10L;
        Long assignmentId = 42L;
        User user = user(userId, tenantId);
        Role role = role(roleId, tenantId);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(roleId, tenantId)).thenReturn(Optional.of(role));
        doReturn(false).when(userRoleRepository)
                .existsActiveByUserIdAndTenantIdAndRoleId(eq(userId), eq(tenantId), eq(roleId), any(Instant.class));
        doAnswer(invocation -> {
            UserRole saved = invocation.getArgument(0);
            saved.setId(assignmentId);
            return saved;
        }).when(userRoleRepository).save(any(UserRole.class));

        userRoleService.assignRole(new AssignRoleRequest(userId, roleId, null, false, "admin"));

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(captor.capture());
        UserRole saved = captor.getValue();
        assertEquals(assignmentId, saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals(roleId, saved.getRoleId());
        assertEquals(tenantId, saved.getTenantId());
        assertEquals("admin", saved.getAssignedBy());
        assertFalse(saved.isPrimaryRole());
        verify(userRepository).findByIdAndTenantId(userId, tenantId);
        verify(roleRepository).findByIdAndTenantId(roleId, tenantId);
    }

    @Test
    void assignRoleAllowsMultipleRolesForTheSameUser() {
        Long userId = 1L;
        Long tenantId = 10L;
        Long firstRoleId = 2L;
        Long secondRoleId = 3L;
        User user = user(userId, tenantId);
        Role firstRole = role(firstRoleId, tenantId);
        Role secondRole = role(secondRoleId, tenantId);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(firstRoleId, tenantId)).thenReturn(Optional.of(firstRole));
        when(roleRepository.findByIdAndTenantId(secondRoleId, tenantId)).thenReturn(Optional.of(secondRole));
        doReturn(false).when(userRoleRepository)
                .existsActiveByUserIdAndTenantIdAndRoleId(eq(userId), eq(tenantId), anyLong(), any(Instant.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(userRoleRepository).save(any(UserRole.class));

        userRoleService.assignRole(new AssignRoleRequest(userId, firstRoleId, null, false, "admin"));
        userRoleService.assignRole(new AssignRoleRequest(userId, secondRoleId, null, false, "admin"));

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, times(2)).save(captor.capture());
        assertEquals(List.of(firstRoleId, secondRoleId),
                captor.getAllValues().stream().map(UserRole::getRoleId).toList());
    }

    @Test
    void assignRoleRejectsActiveDuplicateAssignment() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantId = 10L;
        User user = user(userId, tenantId);
        Role role = role(roleId, tenantId);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(roleId, tenantId)).thenReturn(Optional.of(role));
        doReturn(true).when(userRoleRepository)
                .existsActiveByUserIdAndTenantIdAndRoleId(eq(userId), eq(tenantId), eq(roleId), any(Instant.class));

        assertThrows(IllegalStateException.class,
                () -> userRoleService.assignRole(new AssignRoleRequest(userId, roleId, null, false, "admin")));
        verify(userRoleRepository, never()).save(any(UserRole.class));
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
    void assignRolePrimaryRemovesPrimaryDesignationFromOtherAssignments() {
        Long userId = 1L;
        Long tenantId = 10L;
        Long currentPrimaryRoleId = 2L;
        Long newPrimaryRoleId = 3L;
        User user = user(userId, tenantId);
        Role newPrimaryRole = role(newPrimaryRoleId, tenantId);
        UserRole currentPrimary = UserRole.assign(
                userId, currentPrimaryRoleId, tenantId, "admin", Instant.now(), null, true);
        currentPrimary.setId(20L);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndTenantId(newPrimaryRoleId, tenantId)).thenReturn(Optional.of(newPrimaryRole));
        when(userRoleRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of(currentPrimary));
        doReturn(false).when(userRoleRepository)
                .existsActiveByUserIdAndTenantIdAndRoleId(
                        eq(userId), eq(tenantId), eq(newPrimaryRoleId), any(Instant.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(userRoleRepository).save(any(UserRole.class));

        userRoleService.assignRole(new AssignRoleRequest(userId, newPrimaryRoleId, null, true, "admin"));

        assertFalse(currentPrimary.isPrimaryRole());
        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, times(2)).save(captor.capture());
        assertEquals(currentPrimary, captor.getAllValues().get(0));
        assertEquals(newPrimaryRoleId, captor.getAllValues().get(1).getRoleId());
        assertTrue(captor.getAllValues().get(1).isPrimaryRole());
    }

    @Test
    void removeRolePreservesRevocationMetadata() {
        Long userId = 1L;
        Long roleId = 2L;
        Long tenantId = 10L;
        UserRole assignment = UserRole.assign(
                userId, roleId, tenantId, "admin", Instant.now(), null, false);
        assignment.setId(30L);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRoleRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId))
                .thenReturn(Optional.of(assignment));

        userRoleService.removeRole(new RemoveRoleRequest(
                userId, roleId, "admin", "access no longer required"));

        assertNotNull(assignment.getRevokedAt());
        assertEquals("admin", assignment.getRevokedBy());
        assertEquals("access no longer required", assignment.getRevokeReason());
        verify(userRoleRepository).save(assignment);
    }

    @Test
    void listUserRolesIsTenantScoped() {
        Long userId = 1L;
        Long tenantId = 10L;
        User user = user(userId, tenantId);
        UserRole assignment = UserRole.assign(
                userId, 2L, tenantId, "system", Instant.now(), null, true);
        Page<UserRole> page = new PageImpl<>(List.of(assignment));

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserIdAndTenantId(userId, tenantId, Pageable.unpaged()))
                .thenReturn(page);

        UserRoleListResponse response = userRoleService.listUserRoles(userId, Pageable.unpaged());

        assertEquals(1, response.totalElements());
        verify(userRoleRepository).findByUserIdAndTenantId(userId, tenantId, Pageable.unpaged());
    }

    @Test
    void getPrimaryRoleAndListActiveRolesAreTenantScoped() {
        Long userId = 1L;
        Long tenantId = 10L;
        User user = user(userId, tenantId);

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findPrimaryByUserIdAndTenantId(eq(userId), eq(tenantId), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(userRoleRepository.findActiveByUserIdAndTenantId(eq(userId), eq(tenantId), any(Instant.class)))
                .thenReturn(List.of());

        assertNull(userRoleService.getPrimaryRole(userId));
        assertTrue(userRoleService.listActiveRoles(userId).isEmpty());
        verify(userRoleRepository).findPrimaryByUserIdAndTenantId(eq(userId), eq(tenantId), any(Instant.class));
        verify(userRoleRepository).findActiveByUserIdAndTenantId(eq(userId), eq(tenantId), any(Instant.class));
    }

    private User user(Long id, Long tenantId) {
        User user = mock(User.class);
        lenient().when(user.getTenantId()).thenReturn(tenantId);
        return user;
    }

    private Role role(Long id, Long tenantId) {
        Role role = mock(Role.class);
        lenient().when(role.getTenantId()).thenReturn(tenantId);
        return role;
    }
}
