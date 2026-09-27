package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.OnboardingRequest;
import com.erp.platform.identity.application.dto.OnboardingResponse;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import com.erp.platform.tenant.domain.IsolationStrategy;
import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of OnboardingService.
 *
 * <p>Handles the complete tenant onboarding flow in a single transaction.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class OnboardingServiceImpl implements OnboardingService {

    private static final String TENANT_ADMIN_ROLE_CODE = RoleConstants.TENANT_ADMIN;
    private static final String ASSIGNED_BY = "onboarding";
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingServiceImpl(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OnboardingResponse onboard(OnboardingRequest request) {
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new DuplicateEmailException("User with email '" + request.email() + "' already exists");
        }
        // Step 1: Create tenant
        Tenant tenant = createTenant(request);

        // Step 2: Create the initial account for this internal tenant.
        User owner = createTenantOwner(tenant.getId(), request);

        Role ownerRole = roleRepository.findByTenantIdAndRoleCodeAndRoleType(
                        null, TENANT_ADMIN_ROLE_CODE, RoleType.SYSTEM)
                .orElseThrow(() -> new IllegalStateException(
                        "Platform SYSTEM TENANT_ADMIN role is not initialized"));

        // Steps 7 & 8: Assign OWNER role to the first user and make it the primary role
        assignOwnerRoleToUser(owner, ownerRole);

        return new OnboardingResponse(
                owner.getId(), owner.getUsername(), owner.getEmail(), owner.getFullName(),
                "Account created successfully. Please log in.");
    }

    private Tenant createTenant(OnboardingRequest request) {
        String workspaceName = request.firstName().trim() + "'s Workspace";
        String tenantCode = generateTenantCode(request.email().trim());

        // Ensure tenant code is unique
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenantCode);
        }

        Tenant tenant = Tenant.create(
                tenantCode,
                workspaceName,
                workspaceName,
                request.email().trim(),
                request.mobile(),
                "",
                "UTC",
                "USD",
                "en",
                IsolationStrategy.SCHEMA_PER_TENANT
        );

        tenant.activate(Instant.now());
        Tenant savedTenant = tenantRepository.save(tenant);

        if (savedTenant.getId() != null && savedTenant.getId() == 1L) {
            throw new IllegalStateException("Cannot onboard as bootstrap tenant (ID: 1). Bootstrap tenant is reserved.");
        }

        return savedTenant;
    }

    private String generateTenantCode(String seed) {
        String cleaned = seed.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleaned.isBlank()) {
            cleaned = "TENANT";
        }
        String code = cleaned.substring(0, Math.min(20, cleaned.length()));
        return code + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private User createTenantOwner(Long tenantId, OnboardingRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        String username = request.email().trim();

        User user = User.create(
                tenantId,
                username,
                request.email().trim(),
                passwordHash,
                request.firstName(),
                request.lastName(),
                request.mobile(),
                null,
                null,
                null,
                null,
                null
        );

        user.activate(Instant.now());
        return userRepository.save(user);
    }

    private void assignOwnerRoleToUser(User user, Role ownerRole) {
        Optional<UserRole> existingUserRole = userRoleRepository.findByUserIdAndTenantIdAndRoleId(
                user.getId(), user.getTenantId(), ownerRole.getId());
        if (existingUserRole.isPresent()) {
            UserRole ur = existingUserRole.get();
            if (!ur.isActive()) {
                ur.reactivate(ASSIGNED_BY, LocalDateTime.now(), null, true);
                userRoleRepository.save(ur);
            } else if (!ur.isPrimaryRole()) {
                ur.setAsPrimary();
                userRoleRepository.save(ur);
            }
        } else {
            UserRole userRole = UserRole.assign(
                    user.getId(),
                    ownerRole.getId(),
                    user.getTenantId(),
                    ASSIGNED_BY,
                    Instant.now(),
                    null,
                    true
            );
            userRoleRepository.save(userRole);
        }
    }

}
