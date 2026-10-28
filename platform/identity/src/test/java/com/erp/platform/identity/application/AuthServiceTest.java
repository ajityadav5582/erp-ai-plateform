package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.LoginRequest;
import com.erp.platform.identity.application.dto.RegisterRequest;
import com.erp.platform.identity.application.dto.TokenResponse;
import com.erp.platform.identity.application.security.JwtProperties;
import com.erp.platform.identity.application.security.JwtService;
import com.erp.platform.identity.application.security.PasswordResetProperties;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.domain.exception.InvalidCredentialsException;
import com.erp.platform.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.RefreshTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    @Mock
    private AuthorizationService authorizationService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                userRoleRepository,
                roleRepository,
                jwtService,
                passwordEncoder,
                jwtProperties,
                passwordResetProperties,
                currentTenantProvider,
                authorizationService
        );
    }

    @Test
    void register_successful() {
        RegisterRequest request = new RegisterRequest(
                1L,
                "testuser",
                "test@example.com",
                "Password123!",
                "Test",
                "User",
                null,
                null,
                null,
                null,
                null,
                "Browser",
                "127.0.0.1"
        );

        when(userRepository.existsByTenantIdAndEmail(1L, "test@example.com")).thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(1L, "testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password");

        User savedUser = User.create(
                1L,
                "testuser",
                "test@example.com",
                "hashed_password",
                "Test",
                "User",
                null, null, null, null, null
        );
        savedUser.setId(100L);
        savedUser.activate(java.time.Instant.now());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(roleRepository.findByTenantId(eq(1L), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));
        when(userRoleRepository.findActiveByUserIdAndTenantId(any(), eq(1L), any())).thenReturn(Collections.emptyList());

        when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), any(), any())).thenReturn("access_token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw_refresh_token");
        when(jwtService.generateRefreshTokenId()).thenReturn(UUID.randomUUID().toString());
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);

        TokenResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("raw_refresh_token", response.refreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_requiresAdminAuthorization() {
        RegisterRequest request = new RegisterRequest(
                1L, "testuser", "test@example.com", "Password123!",
                "Test", "User", null, null, null, null, null, null, null
        );

        when(userRepository.existsByTenantIdAndEmail(1L, "test@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(request));
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest(
                1L, "testuser", "duplicate@example.com", "Password123!",
                "Test", "User", null, null, null, null, null, null, null
        );

        when(userRepository.existsByTenantIdAndEmail(1L, "duplicate@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
    }

    @Test
    void login_successful() {
        LoginRequest request = new LoginRequest(1L, "testuser", "Password123!", "Device", "127.0.0.1");

        User user = User.create(
                1L, "testuser", "test@example.com", "hashed_password",
                "Test", "User", null, null, null, null, null
        );
        user.setId(100L);
        user.activate(java.time.Instant.now());

        when(userRepository.findByTenantIdAndUsername(1L, "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(userRoleRepository.findActiveByUserIdAndTenantId(any(), eq(1L), any())).thenReturn(Collections.emptyList());
        when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), any(), any())).thenReturn("access_token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw_refresh_token");
        when(jwtService.generateRefreshTokenId()).thenReturn(UUID.randomUUID().toString());
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        verify(userRepository).save(user);
    }

    @Test
    void login_invalidPassword_throwsException() {
        LoginRequest request = new LoginRequest(1L, "testuser", "WrongPassword", null, null);

        User user = User.create(
                1L, "testuser", "test@example.com", "hashed_password",
                "Test", "User", null, null, null, null, null
        );
        user.activate(java.time.Instant.now());

        when(userRepository.findByTenantIdAndUsername(1L, "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_password")).thenReturn(false);
        when(passwordResetProperties.getMaxFailedLoginAttempts()).thenReturn(5);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
