package com.erp.platform.identity.application.security;

import com.erp.platform.identity.application.AuthorizationService;
import com.erp.platform.identity.domain.exception.AccessDeniedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionAspectTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private PermissionAspect permissionAspect;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        permissionAspect = new PermissionAspect(authorizationService);
    }

    // ==================== Single Permission Tests ====================

    @Test
    void checkPermission_allowsAccess_whenUserHasPermission() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation("USER_READ");
        Method method = TestController.class.getMethod("readUser");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasPermission("USER_READ")).thenReturn(true);

        // Act
        permissionAspect.checkPermission(joinPoint, annotation);

        // Assert - no exception thrown
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkPermission_throwsAccessDenied_whenUserLacksPermission() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation("USER_READ");
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasPermission("USER_READ")).thenReturn(false);

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> permissionAspect.checkPermission(joinPoint, annotation));
        assertTrue(ex.getMessage().contains("USER_READ"));
    }

    @Test
    void checkPermission_allowsAccess_whenUserIsSuperAdmin() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation("USER_READ");
        when(authorizationService.isSuperAdmin()).thenReturn(true);

        // Act
        permissionAspect.checkPermission(joinPoint, annotation);

        // Assert - no exception thrown, bypasses permission check
        verify(authorizationService, never()).hasPermission(anyString());
    }

    // ==================== Multiple Permissions (requireAll = false) Tests ====================

    @Test
    void checkPermission_allowsAccess_whenUserHasAnyOfMultiplePermissions() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation(new String[]{"USER_READ", "USER_CREATE"}, false);
        Method method = TestController.class.getMethod("readUser");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasAnyPermission("USER_READ", "USER_CREATE")).thenReturn(true);

        // Act
        permissionAspect.checkPermission(joinPoint, annotation);

        // Assert - no exception thrown
    }

    @Test
    void checkPermission_throwsAccessDenied_whenUserLacksAllMultiplePermissions() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation(new String[]{"USER_READ", "USER_CREATE"}, false);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasAnyPermission("USER_READ", "USER_CREATE")).thenReturn(false);

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> permissionAspect.checkPermission(joinPoint, annotation));
        assertTrue(ex.getMessage().contains("USER_READ, USER_CREATE"));
    }

    // ==================== Multiple Permissions (requireAll = true) Tests ====================

    @Test
    void checkPermission_allowsAccess_whenUserHasAllMultiplePermissions() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation(new String[]{"USER_READ", "USER_CREATE"}, true);
        Method method = TestController.class.getMethod("readUser");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasAllPermissions("USER_READ", "USER_CREATE")).thenReturn(true);

        // Act
        permissionAspect.checkPermission(joinPoint, annotation);

        // Assert - no exception thrown
    }

    @Test
    void checkPermission_throwsAccessDenied_whenUserLacksOneOfMultiplePermissions() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation(new String[]{"USER_READ", "USER_CREATE"}, true);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasAllPermissions("USER_READ", "USER_CREATE")).thenReturn(false);

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> permissionAspect.checkPermission(joinPoint, annotation));
        assertTrue(ex.getMessage().contains("USER_READ, USER_CREATE"));
    }

    // ==================== Type-Level Permission Tests ====================

    @Test
    void checkTypePermission_appliesTypeLevelPermission_whenMethodHasNoAnnotation() throws Throwable {
        // Arrange
        RequirePermission annotation = createAnnotation("ROLE_READ");
        Method method = TypeLevelTestController.class.getMethod("listRoles");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(authorizationService.isSuperAdmin()).thenReturn(false);
        when(authorizationService.hasPermission("ROLE_READ")).thenReturn(true);

        // Act
        permissionAspect.checkTypePermission(joinPoint, annotation);

        // Assert - no exception thrown
    }

    @Test
    void checkTypePermission_skips_whenMethodHasOwnAnnotation() throws Throwable {
        // Arrange
        RequirePermission typeAnnotation = createAnnotation("ROLE_READ");
        Method method = TypeLevelTestController.class.getMethod("createRole");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        // Act - should not throw even without permission check
        permissionAspect.checkTypePermission(joinPoint, typeAnnotation);

        // Assert - method-level annotation takes precedence
        verify(authorizationService, never()).hasPermission(anyString());
    }

    // ==================== Helper Methods ====================

    private RequirePermission createAnnotation(String permission) {
        return new RequirePermission() {
            @Override
            public String[] value() {
                return new String[]{permission};
            }

            @Override
            public boolean requireAll() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequirePermission.class;
            }
        };
    }

    private RequirePermission createAnnotation(String[] permissions, boolean requireAll) {
        return new RequirePermission() {
            @Override
            public String[] value() {
                return permissions;
            }

            @Override
            public boolean requireAll() {
                return requireAll;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequirePermission.class;
            }
        };
    }

    // ==================== Test Controllers ====================

    @RequirePermission("USER_READ")
    static class TestController {
        public String readUser() {
            return "user";
        }
    }

    @RequirePermission("ROLE_READ")
    static class TypeLevelTestController {
        public String listRoles() {
            return "roles";
        }

        @RequirePermission("ROLE_CREATE")
        public String createRole() {
            return "role";
        }
    }
}
