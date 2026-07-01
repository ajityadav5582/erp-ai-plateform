package com.erp.platform.security.utils;

import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.context.SecurityContext;
import com.erp.platform.security.exception.SecurityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityUtils}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @Test
    @DisplayName("Should throw exception when security context is null")
    void shouldThrowExceptionWhenSecurityContextIsNull() {
        // When/Then
        assertThatThrownBy(() -> SecurityUtils.requireAuthentication(null))
            .isInstanceOf(SecurityException.class)
            .hasMessage("User is not authenticated");
    }

    @Test
    @DisplayName("Should throw exception when user is not authenticated")
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        // Given
        when(securityContext.isAuthenticated()).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> SecurityUtils.requireAuthentication(securityContext))
            .isInstanceOf(SecurityException.class)
            .hasMessage("User is not authenticated");
    }

    @Test
    @DisplayName("Should throw exception when user lacks permission")
    void shouldThrowExceptionWhenUserLacksPermission() {
        // Given
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.hasPermission("invoice:read:invoice")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> SecurityUtils.requirePermission(securityContext, "invoice:read:invoice"))
            .isInstanceOf(SecurityException.class)
            .hasMessage("User does not have permission: invoice:read:invoice");
    }

    @Test
    @DisplayName("Should throw exception when user lacks role")
    void shouldThrowExceptionWhenUserLacksRole() {
        // Given
        when(securityContext.isAuthenticated()).thenReturn(true);
        when(securityContext.hasRole("ADMIN")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> SecurityUtils.requireRole(securityContext, "ADMIN"))
            .isInstanceOf(SecurityException.class)
            .hasMessage("User does not have role: ADMIN");
    }
}
