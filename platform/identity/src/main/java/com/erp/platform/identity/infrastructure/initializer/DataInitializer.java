package com.erp.platform.identity.infrastructure.initializer;

import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.platform.identity.application.BsCalendarService;

/** Initializes platform-wide master data and global SYSTEM role definitions. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SYSTEM_ASSIGNED_BY = "system";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final BsCalendarService bsCalendarService;

    @PersistenceContext
    private EntityManager entityManager;

    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            BsCalendarService bsCalendarService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.bsCalendarService = bsCalendarService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureMasterFiscalYearsExist();
        bsCalendarService.seedBsCalendarRange(2070, 2090);
        bsCalendarService.relinkFiscalYears();

        List<Permission> permissions = ensureSupportedPermissionsExist();

        Role superAdminRole = ensureSystemRoleExists(
                RoleConstants.SUPER_ADMIN, "Super Administrator",
                "System super administrator with full platform access across all tenants");
        Role tenantAdminRole = ensureSystemRoleExists(
                RoleConstants.TENANT_ADMIN, "Tenant Administrator",
                "Platform-managed administrator role assigned to a tenant's first user");
        assignPermissions(superAdminRole, permissions);
        Set<String> tenantAdminPermissionCodes = Set.of(
                "USER_VIEW", "USER_CREATE", "USER_UPDATE",
                "ROLE_VIEW", "ROLE_CREATE", "ROLE_UPDATE",
                "COMPANY_VIEW", "COMPANY_CREATE", "COMPANY_UPDATE");
        List<Permission> tenantAdminPermissions = permissions.stream()
                .filter(permission -> tenantAdminPermissionCodes.contains(permission.getPermissionCode()))
                .toList();
        assignPermissions(tenantAdminRole, tenantAdminPermissions);
    }

    private List<Permission> ensureSupportedPermissionsExist() {
        List<PermissionSeed> supported = List.of(
                new PermissionSeed("USER", "VIEW"),
                new PermissionSeed("USER", "CREATE"),
                new PermissionSeed("USER", "UPDATE"),
                new PermissionSeed("USER", "DEACTIVATE"),
                new PermissionSeed("ROLE", "VIEW"),
                new PermissionSeed("ROLE", "CREATE"),
                new PermissionSeed("ROLE", "UPDATE"),
                new PermissionSeed("ROLE", "DEACTIVATE"),
                new PermissionSeed("COMPANY", "VIEW"),
                new PermissionSeed("COMPANY", "CREATE"),
                new PermissionSeed("COMPANY", "UPDATE"),
                new PermissionSeed("PRODUCT", "VIEW"),
                new PermissionSeed("PRODUCT", "CREATE"),
                new PermissionSeed("PRODUCT", "UPDATE"),
                new PermissionSeed("PURCHASE", "VIEW"),
                new PermissionSeed("PURCHASE", "CREATE"),
                new PermissionSeed("PURCHASE", "APPROVE"),
                new PermissionSeed("SALES", "VIEW"),
                new PermissionSeed("SALES", "CREATE"),
                new PermissionSeed("SALES", "APPROVE"),
                new PermissionSeed("INVENTORY", "VIEW"),
                new PermissionSeed("INVENTORY", "ADJUST")
        );
        List<Permission> permissions = new ArrayList<>(supported.size());
        for (PermissionSeed seed : supported) {
            String code = Permission.generatePermissionCode(seed.resource(), seed.action());
            Permission permission = permissionRepository.findByPermissionCode(code)
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .permissionCode(code)
                            .permissionName(seed.resource() + " " + seed.action())
                            .resourceCode(seed.resource())
                            .actionCode(seed.action())
                            .description(seed.resource() + " " + seed.action().toLowerCase() + " permission")
                            .status(PermissionStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()));
            permissions.add(permission);
        }
        return permissions;
    }

    private void ensureMasterFiscalYearsExist() {
        List<MasterFiscalYearSeed> fiscalYears = List.of(
                new MasterFiscalYearSeed("FY 2078/79", "2078-79", "2078-04-01", "2079-03-32", "2021-07-16", "2022-07-16", 1),
                new MasterFiscalYearSeed("FY 2079/80", "2079-80", "2079-04-01", "2080-03-31", "2022-07-17", "2023-07-16", 2),
                new MasterFiscalYearSeed("FY 2080/81", "2080-81", "2080-04-01", "2081-03-31", "2023-07-17", "2024-07-15", 3),
                new MasterFiscalYearSeed("FY 2081/82", "2081-82", "2081-04-01", "2082-03-31", "2024-07-16", "2025-07-16", 4),
                new MasterFiscalYearSeed("FY 2082/83", "2082-83", "2082-04-01", "2083-03-31", "2025-07-17", "2026-07-16", 5),
                new MasterFiscalYearSeed("FY 2083/84", "2083-84", "2083-04-01", "2084-03-31", "2026-07-17", "2027-07-16", 6)
        );

        for (MasterFiscalYearSeed seed : fiscalYears) {
            Long count = entityManager.createQuery(
                            "select count(f) from MasterFiscalYear f where f.code = :code", Long.class)
                    .setParameter("code", seed.code())
                    .getSingleResult();
            if (count == 0) {
                entityManager.persist(MasterFiscalYear.create(
                        seed.name(), seed.code(), seed.startDateBs(), seed.endDateBs(),
                        LocalDate.parse(seed.startDateAd()), LocalDate.parse(seed.endDateAd()),
                        (short) seed.displayOrder()));
            }
        }
    }

    private Role ensureSystemRoleExists(String roleCode, String roleName, String description) {
        return roleRepository.findByTenantIdAndRoleCodeAndRoleType(null, roleCode, RoleType.SYSTEM)
                .orElseGet(() -> roleRepository.save(Role.createSystem(roleCode, roleName, description)));
    }

    private void assignPermissions(Role role, List<Permission> permissions) {
        List<Long> permissionIds = permissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
        rolePermissionRepository.deleteRolePermissionsExcept(role.getId(), permissionIds);
        for (Permission permission : permissions) {
            int inserted = rolePermissionRepository.insertOnConflictDoNothing(
                    role.getId(), permission.getId(), SYSTEM_ASSIGNED_BY, LocalDateTime.now());
            if (inserted == 0) {
                log.debug("Role permission already exists: roleId={}, permissionId={}",
                        role.getId(), permission.getId());
            }
        }
    }

    private record MasterFiscalYearSeed(
            String name,
            String code,
            String startDateBs,
            String endDateBs,
            String startDateAd,
            String endDateAd,
            int displayOrder) {
    }

    private record PermissionSeed(String resource, String action) {
    }
}
