package com.erp.platform.identity.infrastructure.initializer;

import com.erp.platform.identity.domain.DataScope;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.IsolationStrategy;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.platform.identity.application.BsCalendarService;

/**
 * Initializes platform-wide master data and global SYSTEM role definitions.
 *
 * <p>On first bootstrap this initializer creates exactly two platform SYSTEM roles:
 * {@code SUPER_ADMIN} and {@code TENANT_ADMIN}. No other roles are seeded here;
 * tenant-specific roles are created by {@link com.erp.platform.identity.infrastructure.initializer.DummyDataInitializer}
 * (when enabled) or by tenant administrators through the Role screen.
 */
@Component
public class DataInitializer implements MasterDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SYSTEM_ASSIGNED_BY = "system";

    /**
     * Superseded purchase-requisition code -> {code, resource, action} on the
     * {@code PR} resource that replaced it. Consumed by
     * {@link #renameLegacyPurchasePermissions()}.
     */
    private static final Map<String, String[]> LEGACY_PURCHASE_PERMISSION_CODES = Map.ofEntries(
            Map.entry("PURCHASE_VIEW", new String[]{"PR_VIEW", "PR", "VIEW"}),
            Map.entry("PURCHASE_CREATE", new String[]{"PR_CREATE", "PR", "CREATE"}),
            Map.entry("PURCHASE_APPROVE", new String[]{"PR_APPROVE", "PR", "APPROVE"}),
            Map.entry("PURCHASE_ORDER", new String[]{"PR_CONVERT_TO_PO", "PR", "CONVERT_TO_PO"}),
            Map.entry("PURCHASE_CANCEL", new String[]{"PR_CANCEL", "PR", "CANCEL"}),
            Map.entry("PURCHASE_DELETE", new String[]{"PR_DELETE", "PR", "DELETE"}),
            Map.entry("PURCHASE_ADMIN", new String[]{"PR_ADMIN", "PR", "ADMIN"}),
            Map.entry("PURCHASE_REQUISITION_VIEW", new String[]{"PR_VIEW", "PR", "VIEW"}),
            Map.entry("PURCHASE_REQUISITION_CREATE", new String[]{"PR_CREATE", "PR", "CREATE"}),
            Map.entry("PURCHASE_REQUISITION_APPROVE", new String[]{"PR_APPROVE", "PR", "APPROVE"}),
            Map.entry("PURCHASE_REQUISITION_AWARD", new String[]{"PR_CONVERT_TO_PO", "PR", "CONVERT_TO_PO"}),
            Map.entry("PURCHASE_REQUISITION_CANCEL", new String[]{"PR_CANCEL", "PR", "CANCEL"}),
            Map.entry("PURCHASE_REQUISITION_DELETE", new String[]{"PR_DELETE", "PR", "DELETE"}),
            Map.entry("PURCHASE_REQUISITION_ADMIN", new String[]{"PR_ADMIN", "PR", "ADMIN"}));

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final BsCalendarService bsCalendarService;
    private final TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            BsCalendarService bsCalendarService,
            TenantRepository tenantRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.bsCalendarService = bsCalendarService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public String stepName() {
        return "platform-master-data";
    }

    @Override
    public String stepVersion() {
        return "1";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    @Transactional
    public void seed() {
        // Seed a default system tenant so that SYSTEM roles have a non-null tenant_id
        // (required by migration V33 which makes roles.tenant_id NOT NULL).
        Long defaultTenantId = ensureDefaultTenantExists();

        // Seed BS calendar range and relink fiscal years (required by downstream modules).
        ensureMasterFiscalYearsExist();
        bsCalendarService.seedBsCalendarRange(2070, 2090);
        bsCalendarService.relinkFiscalYears();

        // Ensure supported permissions exist (renames legacy codes first).
        List<Permission> permissions = ensureSupportedPermissionsExist();

        // Create exactly two platform SYSTEM roles: SUPER_ADMIN and TENANT_ADMIN.
        Role superAdminRole = ensureSystemRoleExists(
                null,
                RoleConstants.SUPER_ADMIN, "Super Administrator",
                "System super administrator with full platform access across all tenants");
        Role tenantAdminRole = ensureSystemRoleExists(
                null,
                RoleConstants.TENANT_ADMIN, "Tenant Administrator",
                "Platform-managed administrator role assigned to a tenant's first user");

        assignPermissions(superAdminRole, permissions);
        assignPermissions(tenantAdminRole, permissions);

        log.info("Seeded system roles: SUPER_ADMIN and TENANT_ADMIN (tenantId={})", defaultTenantId);
    }

    /**
     * Ensures a default system tenant exists for platform-level SYSTEM roles.
     * After migration V33, {@code roles.tenant_id} is NOT NULL, so system roles
     * must reference a tenant. This method finds or creates a tenant with code
     * {@code "default"} to serve as the anchor for platform-wide system roles.
     */
    private Long ensureDefaultTenantExists() {
        try {
            Optional<Tenant> existing = tenantRepository.findByTenantCode("default");
            if (existing.isPresent()) {
                return existing.get().getId();
            }
            Tenant tenant = Tenant.create(
                    "default",
                    "Default System Tenant",
                    "Default",
                    "system@erp-ai.local",
                    null,
                    null,
                    "UTC",
                    "USD",
                    "en",
                    IsolationStrategy.SCHEMA_PER_TENANT
            );
            tenant.activate(Instant.now());
            return tenantRepository.save(tenant).getId();
        } catch (Exception e) {
            log.warn("Failed to create default system tenant, will retry with null tenantId: {}", e.getMessage());
            return null;
        }
    }

    private Role ensureSystemRoleExists(Long tenantId, String roleCode, String roleName, String description) {
        Optional<Role> existing = (tenantId == null)
                ? roleRepository.findByTenantIdIsNullAndRoleCodeAndRoleType(roleCode, RoleType.SYSTEM)
                : roleRepository.findByTenantIdAndRoleCodeAndRoleType(tenantId, roleCode, RoleType.SYSTEM);
        return existing.orElseGet(() -> roleRepository.save(Role.createSystem(tenantId, roleCode, roleName, description)));
    }

    private void assignPermissions(Role role, List<Permission> permissions) {
        List<Long> permissionIds = permissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
        rolePermissionRepository.deleteRolePermissionsExcept(role.getId(), permissionIds);
        for (Permission permission : permissions) {
            int inserted = rolePermissionRepository.insertOnConflictDoNothing(
                    role.getId(), permission.getId(), SYSTEM_ASSIGNED_BY, LocalDateTime.now(), DataScope.ALL.name());
            if (inserted == 0) {
                log.debug("Role permission already exists: roleId={}, permissionId={}",
                        role.getId(), permission.getId());
            }
        }
    }


    private void ensureMasterFiscalYearsExist() {
        record MasterFiscalYearSeed(String name, String code, String startDateBs, String endDateBs, String startDateAd, String endDateAd, int displayOrder) {}
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
                        java.time.LocalDate.parse(seed.startDateAd()), java.time.LocalDate.parse(seed.endDateAd()),
                        (short) seed.displayOrder()));
            }
        }
    }

    private List<Permission> ensureSupportedPermissionsExist() {
        renameLegacyPurchasePermissions();

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
                new PermissionSeed("PR", "VIEW"),
                new PermissionSeed("PR", "CREATE"),
                new PermissionSeed("PR", "EDIT"),
                new PermissionSeed("PR", "SUBMIT"),
                new PermissionSeed("PR", "APPROVE"),
                new PermissionSeed("PR", "REJECT"),
                new PermissionSeed("PR", "CANCEL"),
                new PermissionSeed("PR", "CONVERT_TO_PO"),
                new PermissionSeed("PR", "DELETE"),
                new PermissionSeed("PR", "ADMIN"),
                new PermissionSeed("PURCHASE_ORDER", "VIEW"),
                new PermissionSeed("PURCHASE_ORDER", "CREATE"),
                new PermissionSeed("PURCHASE_ORDER", "APPROVE"),
                new PermissionSeed("PURCHASE_ORDER", "RECEIVE"),
                new PermissionSeed("PURCHASE_ORDER", "CANCEL"),
                new PermissionSeed("PURCHASE_ORDER", "DELETE"),
                new PermissionSeed("GOODS_RECEIPT", "VIEW"),
                new PermissionSeed("GOODS_RECEIPT", "CREATE"),
                new PermissionSeed("GOODS_RECEIPT", "CANCEL"),
                new PermissionSeed("PURCHASE_INVOICE", "VIEW"),
                new PermissionSeed("PURCHASE_INVOICE", "CREATE"),
                new PermissionSeed("PURCHASE_INVOICE", "CANCEL"),
                new PermissionSeed("SUPPLIER_PAYMENT", "VIEW"),
                new PermissionSeed("SUPPLIER_PAYMENT", "CREATE"),
                new PermissionSeed("SUPPLIER_PAYMENT", "CANCEL"),
                new PermissionSeed("SALES", "VIEW"),
                new PermissionSeed("SALES", "CREATE"),
                new PermissionSeed("SALES", "APPROVE"),
                new PermissionSeed("CUSTOMER", "VIEW"),
                new PermissionSeed("CUSTOMER", "CREATE"),
                new PermissionSeed("CUSTOMER", "UPDATE"),
                new PermissionSeed("CUSTOMER", "DELETE"),
                new PermissionSeed("SALES_ORDER", "VIEW"),
                new PermissionSeed("SALES_ORDER", "CREATE"),
                new PermissionSeed("SALES_ORDER", "EDIT"),
                new PermissionSeed("SALES_ORDER", "CONFIRM"),
                new PermissionSeed("SALES_ORDER", "CANCEL"),
                new PermissionSeed("SALES_ORDER", "CLOSE"),
                new PermissionSeed("SALES_DELIVERY", "VIEW"),
                new PermissionSeed("SALES_DELIVERY", "CREATE"),
                new PermissionSeed("SALES_DELIVERY", "POST"),
                new PermissionSeed("SALES_DELIVERY", "CANCEL"),
                new PermissionSeed("SALES_INVOICE", "VIEW"),
                new PermissionSeed("SALES_INVOICE", "CREATE"),
                new PermissionSeed("SALES_INVOICE", "ISSUE"),
                new PermissionSeed("SALES_INVOICE", "CANCEL"),
                new PermissionSeed("CUSTOMER_RECEIPT", "VIEW"),
                new PermissionSeed("CUSTOMER_RECEIPT", "CREATE"),
                new PermissionSeed("CUSTOMER_RECEIPT", "CANCEL"),
                new PermissionSeed("SALES_RETURN", "VIEW"),
                new PermissionSeed("SALES_RETURN", "CREATE"),
                new PermissionSeed("SALES_RETURN", "POST"),
                new PermissionSeed("SALES_RETURN", "CANCEL"),
                new PermissionSeed("POS_REGISTER", "VIEW"),
                new PermissionSeed("POS_REGISTER", "CREATE"),
                new PermissionSeed("POS_REGISTER", "UPDATE"),
                new PermissionSeed("POS_REGISTER", "MANAGE"),
                new PermissionSeed("POS_SESSION", "VIEW"),
                new PermissionSeed("POS_SESSION", "OPEN"),
                new PermissionSeed("POS_SESSION", "CLOSE"),
                new PermissionSeed("POS", "SCAN"),
                new PermissionSeed("POS_TRANSACTION", "VIEW"),
                new PermissionSeed("POS_TRANSACTION", "CHECKOUT"),
                new PermissionSeed("POS_TRANSACTION", "VOID"),
                new PermissionSeed("POS_TRANSACTION", "REFUND"),
                new PermissionSeed("POS_QUICK_ITEM", "VIEW"),
                new PermissionSeed("POS_QUICK_ITEM", "CREATE"),
                new PermissionSeed("POS_QUICK_ITEM", "UPDATE"),
                new PermissionSeed("POS_QUICK_ITEM", "DELETE"),
                new PermissionSeed("INVENTORY", "VIEW"),
                new PermissionSeed("INVENTORY", "ADJUST"),
                new PermissionSeed("PRODUCT_CATEGORY", "VIEW"),
                new PermissionSeed("WAREHOUSE", "VIEW"),
                new PermissionSeed("VENDOR", "VIEW"),
                new PermissionSeed("BRANCH", "VIEW"),
                new PermissionSeed("BRANCH", "CREATE"),
                new PermissionSeed("BRANCH", "UPDATE"),
                new PermissionSeed("BRANCH", "DELETE"),
                new PermissionSeed("DEPARTMENT", "VIEW"),
                new PermissionSeed("PERMISSION", "VIEW"),
                new PermissionSeed("ACCOUNTING", "VIEW"),
                new PermissionSeed("HR", "VIEW"),
                new PermissionSeed("MANUFACTURING", "VIEW"),
                new PermissionSeed("REPORTING", "VIEW")
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

    /**
     * Moves every superseded purchase-requisition code onto the {@code PR} codes that
     * replaced it.
     */
    private void renameLegacyPurchasePermissions() {
        for (Map.Entry<String, String[]> rename : LEGACY_PURCHASE_PERMISSION_CODES.entrySet()) {
            String legacyCode = rename.getKey();
            String[] target = rename.getValue();

            boolean legacyExists = permissionRepository.existsByPermissionCode(legacyCode);
            boolean targetExists = permissionRepository.existsByPermissionCode(target[0]);
            if (!legacyExists || targetExists) {
                continue;
            }

            int updated = entityManager.createQuery("""
                            UPDATE Permission p
                               SET p.permissionCode = :code,
                                   p.permissionName = :name,
                                   p.resourceCode = :resource,
                                   p.actionCode = :action,
                                   p.updatedAt = :now
                             WHERE p.permissionCode = :legacy
                            """)
                    .setParameter("code", target[0])
                    .setParameter("name", target[1] + " " + target[2])
                    .setParameter("resource", target[1])
                    .setParameter("action", target[2])
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("legacy", legacyCode)
                    .executeUpdate();

            log.info("Renamed legacy permission {} to {} ({} row(s), grants preserved)",
                    legacyCode, target[0], updated);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private record PermissionSeed(String resource, String action) {
    }
}
