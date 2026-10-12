package com.erp.platform.identity.infrastructure.initializer;

import com.erp.platform.identity.domain.DataScope;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Province;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleConstants;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.ProvinceRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.platform.identity.application.BsCalendarService;

/** Initializes platform-wide master data and global SYSTEM role definitions. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SYSTEM_ASSIGNED_BY = "system";

    /**
     * Superseded purchase-requisition code -> {code, resource, action} on the
     * {@code PR} resource that replaced it. Consumed by
     * {@link #renameLegacyPurchasePermissions()}.
     *
     * <p>Both generations are listed. {@code PURCHASE_*} was the original
     * module-level seeding and was renamed once onto {@code PURCHASE_REQUISITION_*};
     * that resource has now been replaced in turn by the operation-level {@code PR_*}
     * codes. A database is somewhere in one of those two states, or in the first,
     * so both have to land on the same target - otherwise upgrading from either
     * generation strips every role of its requisition grants instead of renaming
     * them.
     */
    private static final Map<String, String[]> LEGACY_PURCHASE_PERMISSION_CODES = Map.ofEntries(
            // First generation: module-level PURCHASE_*.
            Map.entry("PURCHASE_VIEW", new String[]{"PR_VIEW", "PR", "VIEW"}),
            Map.entry("PURCHASE_CREATE", new String[]{"PR_CREATE", "PR", "CREATE"}),
            Map.entry("PURCHASE_APPROVE", new String[]{"PR_APPROVE", "PR", "APPROVE"}),
            Map.entry("PURCHASE_ORDER", new String[]{"PR_CONVERT_TO_PO", "PR", "CONVERT_TO_PO"}),
            Map.entry("PURCHASE_CANCEL", new String[]{"PR_CANCEL", "PR", "CANCEL"}),
            Map.entry("PURCHASE_DELETE", new String[]{"PR_DELETE", "PR", "DELETE"}),
            Map.entry("PURCHASE_ADMIN", new String[]{"PR_ADMIN", "PR", "ADMIN"}),
            // Second generation: entity-level PURCHASE_REQUISITION_*. AWARD was
            // renamed CONVERT_TO_PO because the action it gates is the conversion
            // of an approved requisition into a purchase order, and "AWARD" read
            // as a generic grants-management verb from the point of view of an
            // administrator reading the Role screen.
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
    private final ProvinceRepository provinceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            BsCalendarService bsCalendarService,
            ProvinceRepository provinceRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.bsCalendarService = bsCalendarService;
        this.provinceRepository = provinceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureMasterFiscalYearsExist();
        bsCalendarService.seedBsCalendarRange(2070, 2090);
        bsCalendarService.relinkFiscalYears();
        ensureProvincesExist();

        List<Permission> permissions = ensureSupportedPermissionsExist();

        Role superAdminRole = ensureSystemRoleExists(
                RoleConstants.SUPER_ADMIN, "Super Administrator",
                "System super administrator with full platform access across all tenants");
        Role tenantAdminRole = ensureSystemRoleExists(
                RoleConstants.TENANT_ADMIN, "Tenant Administrator",
                "Platform-managed administrator role assigned to a tenant's first user");
        assignPermissions(superAdminRole, permissions);
        // The branch dialogs cascade province -> district -> local level, so the
        // tenant administrator needs the geography read permissions as well.
        // Without them the selects render empty even though the seed data exists.
        //
        // PR_VIEW / PR_CREATE / PR_EDIT / PR_SUBMIT / PR_APPROVE / PR_REJECT are
        // granted because the tenant administrator is, in a single-tenant
        // deployment, the person who raises requisitions and the person who signs
        // them off. Leaving them out makes every purchase-requisition screen
        // return 403 for the very role that administers the tenant.
        //
        // Granting PR_APPROVE / PR_REJECT here also lets that administrator decide
        // their own requisition when the approval policy names them as the assignee,
        // which is the ordinary arrangement in a small business. The step assignment
        // is the control for anyone who wants it tightened: point the step at a
        // different user or role and the administrator simply is not the assignee.
        //
        // PR_CONVERT_TO_PO, PR_CANCEL, PR_DELETE and PR_ADMIN are deliberately NOT
        // granted. Combining approve + convert-to-PO + delete on one role collapses
        // the buyer/approver separation this module exists to enforce, and would let
        // one person take a requisition from draft to supplier commitment unobserved.
        // Grant those to a dedicated role instead - which is exactly what the separate
        // PURCHASE_ORDER_* group in the Role screen is for.
        //
        // No PURCHASE_ORDER_* permission is granted either, for the same reason one
        // step further out: the buyer who commits spend to a supplier should not be
        // the approver who signs it off. Those rows exist and are selectable so those
        // roles can be built now, ahead of the purchase-order endpoints.
        //
        // This is a grant, not a role definition. It attaches existing permission
        // rows to the pre-existing platform-managed TENANT_ADMIN role; it creates
        // no procurement role of its own, and how any other role is composed is
        // left entirely to an administrator on the Role screen.
        // Tenant admin gets all permissions so the onboarding user has full
        // access to every module. Other users' menus are filtered by the
        // permissions attached to their assigned roles.
        assignPermissions(tenantAdminRole, permissions);

        // Procurement close-out roles. The three documents that close out a
        // purchase - goods receipt, supplier invoice, supplier payment - have
        // three different owners, which is why they are separate resources
        // rather than actions on one "procurement" permission (see
        // ProcurementPermissions in business:procurement:application). The
        // person who takes delivery is not the person who codes the invoice,
        // and neither is the person who releases the money.
        //
        // These are seeded as platform SYSTEM roles - the same kind as
        // SUPER_ADMIN and TENANT_ADMIN - because the initializer runs
        // platform-wide and does not know any tenant id, and a role with a
        // null tenant is the platform's way of offering a role that every
        // tenant's administrator can assign to their own users. Each is
        // granted only its own permission group, so a storekeeper can record
        // a delivery without being able to book the invoice or release the
        // payment.
        //
        // TENANT_ADMIN deliberately gets none of these (see the comment block
        // above): handing the tenant administrator every close-out permission
        // would collapse the buyer/approver/storekeeper/accounts/treasury
        // separation this module exists to enforce. In a single-tenant
        // deployment the administrator assigns the appropriate role to the
        // person doing each job; the roles exist here so that assignment is
        // a checkbox, not a build.
        ensureProcurementCloseOutRolesExist(permissions);
    }

    /**
     * Seeds the three platform roles that own the procurement close-out
     * documents, each granted only its own permission group.
     *
     * <p>Idempotent: an existing role with the same code and type is reused
     * rather than duplicated, and {@link #assignPermissions} is destructive
     * only for the role it is given, so re-running never strips a grant an
     * administrator added to another role. The three roles are independent -
     * granting one does not touch the others - so a tenant that has already
     * customised, say, its storekeeper role keeps that customisation's
     * permission set exactly as the seed defines it.
     *
     * @param permissions every supported permission, to resolve codes against
     */
    private void ensureProcurementCloseOutRolesExist(List<Permission> permissions) {
        seedCloseOutRole(
                "STOREKEEPER", "Storekeeper",
                "Records goods receipts against purchase orders",
                List.of("GOODS_RECEIPT_VIEW", "GOODS_RECEIPT_CREATE", "GOODS_RECEIPT_CANCEL"),
                permissions);
        seedCloseOutRole(
                "ACCOUNTS_CLERK", "Accounts Clerk",
                "Records supplier invoices against received goods",
                List.of("PURCHASE_INVOICE_VIEW", "PURCHASE_INVOICE_CREATE", "PURCHASE_INVOICE_CANCEL"),
                permissions);
        seedCloseOutRole(
                "TREASURY", "Treasury",
                "Releases supplier payments against booked invoices",
                List.of("SUPPLIER_PAYMENT_VIEW", "SUPPLIER_PAYMENT_CREATE", "SUPPLIER_PAYMENT_CANCEL"),
                permissions);
    }

    /**
     * Creates (or reuses) a platform close-out role and grants it exactly
     * the given permission codes.
     *
     * <p>The role is a SYSTEM role with a null tenant id, the same shape as
     * {@code SUPER_ADMIN} and {@code TENANT_ADMIN}: it is a platform-offered
     * role that an administrator assigns to users within a tenant. Granting
     * is done via {@link #assignPermissions}, which replaces only this role's
     * grants.
     *
     * @param roleCode      the stable role code, e.g. {@code STOREKEEPER}
     * @param roleName      the human-readable name
     * @param description   what the role is for
     * @param permissionCodes the permission codes this role holds
     * @param permissions   every supported permission, to resolve codes against
     */
    private void seedCloseOutRole(
            String roleCode, String roleName, String description,
            List<String> permissionCodes, List<Permission> permissions) {
        Role role = roleRepository.findByTenantIdAndRoleCodeAndRoleType(null, roleCode, RoleType.SYSTEM)
                .orElseGet(() -> roleRepository.save(
                        Role.createSystem(roleCode, roleName, description)));
        List<Permission> rolePermissions = permissions.stream()
                .filter(permission -> permissionCodes.contains(permission.getPermissionCode()))
                .toList();
        assignPermissions(role, rolePermissions);
        log.info("Seeded procurement close-out role {} with {} permission(s)",
                roleCode, rolePermissions.size());
    }

    private List<Permission> ensureSupportedPermissionsExist() {
        // Must run before the seeding loop below. The loop looks each row up by its
        // current code and inserts it only when absent, so renaming first is what
        // turns the superseded rows into the current requisition rows instead of
        // leaving the old ones behind as granted-but-dead duplicates in the Role
        // screen - a role holding PURCHASE_REQUISITION_VIEW would keep showing that
        // checkbox, stop matching any annotated method, and silently lose access.
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
                // Procurement splits into two resources, because a requisition is an
                // internal request for spend and an order is a commitment to a
                // supplier, and an administrator has to be able to grant one without
                // granting the other. This mirrors every other permission in the
                // table, where RESOURCE names the entity (BRANCH_VIEW, ROLE_UPDATE,
                // INVENTORY_ADJUST) rather than the module - and it is the value the
                // Role screen groups its checkbox list by.
                //
                // The requisition resource is PR, and it carries one action per
                // business operation rather than one action per verb. PR_CREATE,
                // PR_EDIT and PR_SUBMIT used to be a single CREATE, because they
                // share an owner's daily work; they do not share a control, and an
                // administrator asked to let somebody submit a draft must not thereby
                // hand them the ability to rewrite one. PR_APPROVE and PR_REJECT were
                // likewise a single APPROVE for the same reason: refusing spend is a
                // real decision with real consequences, and companies grant it
                // deliberately.
                //
                // Every code here is referenced by a @RequiresPermission annotation in
                // the procurement service, and a code with no row in this table can
                // never be granted to any role, so it can never reach an access token,
                // so the annotated method is closed to EVERYONE including SUPER_ADMIN.
                // PurchaseRequisitionPermissions and PurchaseOrderPermissions in
                // business:procurement:application are the two halves of this list and
                // must be kept in step with it by hand.
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

                // Purchase orders. The aggregate, its lines and its repository all
                // exist, but there is no purchase-order REST layer yet, so these rows
                // gate nothing today. They are seeded now so an administrator can
                // build the buyer / approver / storekeeper roles up front, and the
                // endpoints inherit them the day they land.
                new PermissionSeed("PURCHASE_ORDER", "VIEW"),
                new PermissionSeed("PURCHASE_ORDER", "CREATE"),
                new PermissionSeed("PURCHASE_ORDER", "APPROVE"),
                new PermissionSeed("PURCHASE_ORDER", "RECEIVE"),
                new PermissionSeed("PURCHASE_ORDER", "CANCEL"),
                new PermissionSeed("PURCHASE_ORDER", "DELETE"),

                // Goods receipt. A storekeeper records the delivery; the buyer who
                // placed the order and the approver who signed it should not be able to
                // mark goods as arrived, because receipt is what makes the order billable.
                new PermissionSeed("GOODS_RECEIPT", "VIEW"),
                new PermissionSeed("GOODS_RECEIPT", "CREATE"),
                new PermissionSeed("GOODS_RECEIPT", "CANCEL"),

                // Purchase invoice. Recording a payable is the accounts clerk's job and
                // is deliberately separate from receiving goods: the point of a
                // three-way match is that the two are done by different people.
                new PermissionSeed("PURCHASE_INVOICE", "VIEW"),
                new PermissionSeed("PURCHASE_INVOICE", "CREATE"),
                new PermissionSeed("PURCHASE_INVOICE", "CANCEL"),

                // Supplier payment. Releasing money is treasury's, and separate from both
                // coding the invoice and receiving the goods.
                new PermissionSeed("SUPPLIER_PAYMENT", "VIEW"),
                new PermissionSeed("SUPPLIER_PAYMENT", "CREATE"),
                new PermissionSeed("SUPPLIER_PAYMENT", "CANCEL"),

                new PermissionSeed("SALES", "VIEW"),
                new PermissionSeed("SALES", "CREATE"),
                new PermissionSeed("SALES", "APPROVE"),

                // Customer master. The sell-side counterpart of a supplier, and a
                // shared resource across the whole sales module, so it is a resource
                // of its own. Referenced by @RequiresPermission in
                // business:sales:application (CustomerPermissions) and must be kept
                // in step with it by hand.
                new PermissionSeed("CUSTOMER", "VIEW"),
                new PermissionSeed("CUSTOMER", "CREATE"),
                new PermissionSeed("CUSTOMER", "UPDATE"),
                new PermissionSeed("CUSTOMER", "DELETE"),

                // Sales order. The sell-side commitment to a customer. Confirming,
                // cancelling and closing are separate controls because issuing an
                // order to a customer, voiding one and settling one are different
                // decisions with different owners. Referenced by
                // @RequiresPermission in business:sales:application
                // (SalesOrderPermissions) and must be kept in step with it by hand.
                new PermissionSeed("SALES_ORDER", "VIEW"),
                new PermissionSeed("SALES_ORDER", "CREATE"),
                new PermissionSeed("SALES_ORDER", "EDIT"),
                new PermissionSeed("SALES_ORDER", "CONFIRM"),
                new PermissionSeed("SALES_ORDER", "CANCEL"),
                new PermissionSeed("SALES_ORDER", "CLOSE"),

                // Sales delivery (goods issue). Posting is what decrements stock and
                // advances the order, so it is a storekeeper's own control, separate
                // from raising the delivery and from cancelling a posted one.
                // Referenced by @RequiresPermission in business:sales:application
                // (SalesDeliveryPermissions) and must be kept in step with it by hand.
                new PermissionSeed("SALES_DELIVERY", "VIEW"),
                new PermissionSeed("SALES_DELIVERY", "CREATE"),
                new PermissionSeed("SALES_DELIVERY", "POST"),
                new PermissionSeed("SALES_DELIVERY", "CANCEL"),

                // Sales (tax) invoice. The IRD-relevant document. Issuing is the act
                // that creates the output-VAT liability, so it is its own control,
                // separate from raising a draft and from voiding an issued one.
                // Referenced by @RequiresPermission in business:sales:application
                // (SalesInvoicePermissions) and must be kept in step with it by hand.
                new PermissionSeed("SALES_INVOICE", "VIEW"),
                new PermissionSeed("SALES_INVOICE", "CREATE"),
                new PermissionSeed("SALES_INVOICE", "ISSUE"),
                new PermissionSeed("SALES_INVOICE", "CANCEL"),

                // Customer receipt. Recording money received is the cashier's act;
                // reversing one is a separate, deliberate control.
                new PermissionSeed("CUSTOMER_RECEIPT", "VIEW"),
                new PermissionSeed("CUSTOMER_RECEIPT", "CREATE"),
                new PermissionSeed("CUSTOMER_RECEIPT", "CANCEL"),

                // Sales return (credit note). Posting restocks goods and reverses
                // output VAT, so it is its own control.
                new PermissionSeed("SALES_RETURN", "VIEW"),
                new PermissionSeed("SALES_RETURN", "CREATE"),
                new PermissionSeed("SALES_RETURN", "POST"),
                new PermissionSeed("SALES_RETURN", "CANCEL"),

                new PermissionSeed("INVENTORY", "VIEW"),
                new PermissionSeed("INVENTORY", "ADJUST"),
                new PermissionSeed("PRODUCT", "VIEW"),
                new PermissionSeed("PRODUCT_CATEGORY", "VIEW"),
                new PermissionSeed("WAREHOUSE", "VIEW"),
                new PermissionSeed("VENDOR", "VIEW"),
                new PermissionSeed("BRANCH", "VIEW"),
                new PermissionSeed("BRANCH", "CREATE"),
                new PermissionSeed("BRANCH", "UPDATE"),
                new PermissionSeed("BRANCH", "DELETE"),
                new PermissionSeed("PROVINCE", "VIEW"),
                new PermissionSeed("DISTRICT", "VIEW"),
                new PermissionSeed("LOCAL_LEVEL", "VIEW"),
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
     *
     * <p>Procurement has been through two rounds of naming. It was originally seeded
     * under the module resource {@code PURCHASE} ({@code PURCHASE_VIEW}), which cost
     * the Role screen its ability to grant requisition and purchase-order access
     * independently; that was replaced by the entity resource
     * {@code PURCHASE_REQUISITION} ({@code PURCHASE_REQUISITION_VIEW}), which is now
     * replaced by the per-operation {@code PR} resource ({@code PR_VIEW}). A database
     * sits in whichever of those states it was created in, so all fourteen legacy
     * codes are mapped onto the current ten.
     *
     * <p>The action count grows from seven to ten rather than shrinking, because the
     * split is the point: {@code PURCHASE_REQUISITION_CREATE} alone cannot express
     * "may submit but may not rewrite", and {@code PURCHASE_REQUISITION_APPROVE}
     * alone cannot express "may refuse but may not authorise". Codes with no
     * predecessor - {@code PR_EDIT}, {@code PR_SUBMIT}, {@code PR_REJECT} - are
     * inserted fresh by the seeding loop and grant nothing until an administrator
     * assigns them on the Role screen, which is the correct default: they widen
     * nothing until deliberately widened.
     *
     * <p>The update is issued as a bulk JPQL statement rather than as save-after-load
     * for one specific reason: <strong>it keeps each row's primary key</strong>.
     * {@code role_permissions.permission_id} is a foreign key onto {@code
     * permissions.id}, so deleting the old row and inserting a new one would either
     * fail on that constraint or silently strip every role that already held the
     * permission. Renaming in place preserves the grants - {@code PURCHASE_VIEW}
     * granted to TENANT_ADMIN becomes {@code PR_VIEW} still granted to TENANT_ADMIN -
     * which is the only reason this is safe to run on a live database without an
     * outage.
     *
     * <p>Every target code is absent on first run and present afterwards, so this is
     * a no-op once the rename has happened.
     */
    private void renameLegacyPurchasePermissions() {
        for (Map.Entry<String, String[]> rename : LEGACY_PURCHASE_PERMISSION_CODES.entrySet()) {
            String legacyCode = rename.getKey();
            String[] target = rename.getValue();

            boolean legacyExists = permissionRepository.existsByPermissionCode(legacyCode);
            boolean targetExists = permissionRepository.existsByPermissionCode(target[0]);
            if (!legacyExists || targetExists) {
                // Nothing to rename, or the replacement already landed and this row is
                // a genuine leftover duplicate - left untouched rather than deleted,
                // because dropping a row that roles still reference is a data-loss
                // decision, not a seeding decision.
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

    /**
     * Seeds the seven provinces of Nepal.
     *
     * <p>Flyway is disabled for this service, so the geography tables created by
     * {@code ddl-auto: update} stay empty and the province / district / local
     * level dropdowns in the branch form render nothing. Seeding is idempotent:
     * an existing province code is skipped, so re-running never duplicates rows.
     *
     * <p>Districts and local levels are left to the environment that owns the
     * full administrative reference list; the branch form only needs provinces
     * to be present for the first cascading select to work.
     */
    private void ensureProvincesExist() {
        List<ProvinceSeed> provinces = List.of(
                new ProvinceSeed("P1", "Koshi", "कोशी"),
                new ProvinceSeed("P2", "Madhesh", "मधेश"),
                new ProvinceSeed("P3", "Bagmati", "बागमती"),
                new ProvinceSeed("P4", "Gandaki", "गण्डकी"),
                new ProvinceSeed("P5", "Lumbini", "लुम्बिनी"),
                new ProvinceSeed("P6", "Karnali", "कर्णाली"),
                new ProvinceSeed("P7", "Sudurpashchim", "सुदूरपश्चिम")
        );

        int created = 0;
        for (ProvinceSeed seed : provinces) {
            if (provinceRepository.existsByProvinceCode(seed.code())) {
                continue;
            }
            provinceRepository.save(Province.create(seed.code(), seed.name(), seed.nepaliName(), "NPL"));
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} province(s) missing from the geography master data", created);
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
                    role.getId(), permission.getId(), SYSTEM_ASSIGNED_BY, LocalDateTime.now(), DataScope.ALL.name());
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

    private record ProvinceSeed(String code, String name, String nepaliName) {
    }
}

