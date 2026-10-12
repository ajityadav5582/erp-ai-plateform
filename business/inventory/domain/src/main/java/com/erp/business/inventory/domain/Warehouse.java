package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A physical location where stock is held.
 *
 * <p><strong>A warehouse is not a branch.</strong> A {@code Branch} is an administrative
 * unit owned by the identity service: it carries the address, the phone number and the
 * approval chain. A warehouse is the thing stock physically sits in. The two answer
 * different questions and have different cardinality - a company with one branch can
 * have three warehouses, and one warehouse can serve several branches - which is why
 * stock is keyed on {@code warehouse_id} and never on {@code branch_id}.
 *
 * <p><strong>Every company gets exactly one default.</strong> A new company has no
 * warehouses, but the goods receipt flow has to put incoming stock somewhere, and
 * forcing the user to create a warehouse before their first purchase order can be
 * received is a pointless dead end. {@link #defaultFor(Long, Long, Long)} mints that
 * warehouse on demand.
 *
 * <p><strong>"Exactly one" is enforced by the database, not by this class.</strong> The
 * interesting column here is {@link #defaultMarker}. A plain {@code is_default BOOLEAN}
 * cannot be made unique: {@code UNIQUE (tenant_id, company_id, is_default)} would allow
 * only one {@code false} row per company, which breaks as soon as a company has a
 * second warehouse. The marker is therefore nullable - it holds {@code 1} on the
 * default warehouse and {@code NULL} on every other one - and SQL treats NULLs as
 * distinct inside a unique constraint, so many non-default rows coexist while a second
 * default is rejected outright. {@link #isDefault} remains a plain boolean for callers
 * to read and for queries to filter on.
 *
 * <p>This also means the guarantee holds under {@code spring.jpa.hibernate.ddl-auto=update},
 * which the inventory service runs today: a nullable column plus a plain
 * {@link UniqueConstraint} is something Hibernate can emit. A partial index
 * ({@code WHERE is_default}) would have been the more direct spelling but requires
 * hand-written DDL, and this module has no Flyway migrations yet.
 */
@Entity
@Table(
        name = "warehouses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouses_tenant_company_code",
                        columnNames = {"tenant_id", "company_id", "code"}
                ),
                // The "at most one default per company" guarantee. See the class comment.
                @UniqueConstraint(
                        name = "uk_warehouses_tenant_company_default",
                        columnNames = {"tenant_id", "company_id", "default_marker"}
                )
        },
        indexes = {
                @Index(name = "idx_warehouses_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_warehouses_branch",
                        columnList = "tenant_id, company_id, branch_id"),
                @Index(name = "idx_warehouses_default",
                        columnList = "tenant_id, company_id, is_default")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Warehouse extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The reserved code of an auto-provisioned default warehouse.
     *
     * <p>Reserved rather than generated so that provisioning the same company twice
     * collides on {@code uk_warehouses_tenant_company_code} instead of silently
     * producing {@code MAIN-000002}. That collision is what makes
     * {@link #defaultFor(Long, Long, Long)} safe to call concurrently.
     */
    public static final String DEFAULT_CODE = "MAIN";

    /** The value {@link #defaultMarker} holds on a default warehouse. */
    private static final Integer DEFAULT_MARKER_VALUE = 1;

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 64, message = "Warehouse code must not exceed 64 characters")
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull(message = "Warehouse type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private WarehouseType type;

    /**
     * The administrative branch this warehouse sits under, or {@code null} when it
     * serves the whole company.
     *
     * <p>No foreign key, deliberately: {@code Branch} is mastered by the identity
     * service, and a database-level reference across a service boundary forces both
     * services to migrate in lockstep. The id is validated in the service layer
     * instead, matching how {@code supplier_id} and {@code item_id} are handled.
     */
    @Column(name = "branch_id")
    private Long branchId;

    @Size(max = 500, message = "Warehouse address must not exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    @Size(max = 32, message = "Phone must not exceed 32 characters")
    @Column(name = "phone", length = 32)
    private String phone;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** True when this is the company's fallback warehouse. Read-only to callers. */
    @NotNull(message = "Default status is required")
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Backs the single-default-per-company constraint. {@code 1} when this warehouse
     * is the default, {@code null} otherwise. Never set directly.
     */
    @Column(name = "default_marker")
    private Integer defaultMarker;

    // ==================== Domain methods ====================

    /**
     * Creates the company's default warehouse.
     *
     * <p>Idempotent by collision rather than by lookup: two concurrent callers both
     * build this row, one INSERT wins, and the loser's unique-constraint violation
     * tells it to re-read. The reserved {@link #DEFAULT_CODE} is what makes that
     * work - with a generated code both rows would succeed and the company would end
     * up with two defaults.
     *
     * @param tenantId  the owning tenant
     * @param companyId the company that has no warehouse yet
     * @param branchId  the branch to hang it under, or {@code null} for company-wide
     * @return an unsaved default warehouse
     */
    public static Warehouse defaultFor(Long tenantId, Long companyId, Long branchId) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);

        Warehouse warehouse = Warehouse.builder()
                .code(DEFAULT_CODE)
                .name("Main Warehouse")
                .type(WarehouseType.GENERAL)
                .branchId(branchId)
                .isActive(true)
                .isDefault(true)
                .defaultMarker(DEFAULT_MARKER_VALUE)
                .build();

        warehouse.assignScope(tenantId, companyId);
        return warehouse;
    }

    /**
     * Creates an explicitly requested warehouse. Never the default: a caller creating
     * a second warehouse is adding to the network, not choosing its fallback.
     *
     * @throws IllegalArgumentException if the tenant, company, code or name is invalid
     */
    public static Warehouse create(Long tenantId, Long companyId, String code, String name,
                                   WarehouseType type, Long branchId, String address,
                                   String phone) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);
        validateCode(code);
        validateName(name);
        if (type == null) {
            throw new IllegalArgumentException("Warehouse type is required");
        }

        Warehouse warehouse = Warehouse.builder()
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .type(type)
                .branchId(branchId)
                .address(trimToNull(address))
                .phone(trimToNull(phone))
                .isActive(true)
                .isDefault(false)
                .defaultMarker(null)
                .build();

        warehouse.assignScope(tenantId, companyId);
        return warehouse;
    }

    /**
     * Applies a partial update. Every argument is optional; {@code null} and blank
     * leave the current value untouched, so a caller cannot blank out a required
     * field by omitting it.
     *
     * <p>{@code code} and {@code type} are deliberately not editable once stock has
     * been posted against the warehouse. That check lives in the service, which is
     * the only place that can see the stock ledger.
     */
    public void updateDetails(String code, String name, WarehouseType type, Long branchId,
                              String address, String phone) {
        if (code != null && !code.isBlank()) {
            validateCode(code);
            this.code = code.trim().toUpperCase();
        }
        if (name != null && !name.isBlank()) {
            validateName(name);
            this.name = name.trim();
        }
        if (type != null) {
            this.type = type;
        }
        if (branchId != null) {
            this.branchId = branchId;
        }
        if (address != null) {
            this.address = trimToNull(address);
        }
        if (phone != null) {
            this.phone = trimToNull(phone);
        }
    }

    /**
     * Makes this the company's default.
     *
     * <p>Refuses an inactive warehouse. A fallback that goods cannot be received into
     * is worse than no fallback at all, because the failure surfaces as a stock
     * posting error on the first delivery rather than as a configuration mistake.
     *
     * @throws IllegalStateException if this warehouse is inactive
     */
    public void markAsDefault() {
        if (!Boolean.TRUE.equals(this.isActive)) {
            throw new IllegalStateException(
                    "Warehouse " + this.code + " is inactive and cannot be the default");
        }
        this.isDefault = true;
        this.defaultMarker = DEFAULT_MARKER_VALUE;
    }

    /** Drops this warehouse's default status. The caller must promote a replacement. */
    public void clearDefault() {
        this.isDefault = false;
        this.defaultMarker = null;
    }

    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivates this warehouse.
     *
     * <p>Refuses to deactivate the company's default: the whole point of the default
     * is that something always exists to receive into, so it has to be replaced by
     * another warehouse before it can be switched off.
     *
     * @throws IllegalStateException if this warehouse is currently the default
     */
    public void deactivate() {
        if (Boolean.TRUE.equals(this.isDefault)) {
            throw new IllegalStateException(
                    "Warehouse " + this.code + " is the company default and cannot be "
                            + "deactivated; make another warehouse the default first");
        }
        this.isActive = false;
    }

    // ==================== Validation ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Warehouse code must not be blank");
        }
        if (code.length() > 64) {
            throw new IllegalArgumentException("Warehouse code must not exceed 64 characters");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Warehouse name must not be blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Warehouse name must not exceed 255 characters");
        }
    }

    private static String trimToNull(String value) {
        return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
    }
}
