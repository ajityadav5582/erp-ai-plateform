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
 * A unit of measure that inventory quantities can be expressed in.
 *
 * <p>A unit is a small, low-churn master record owned by exactly one company
 * inside one tenant. It deliberately carries no relationship to products: it is
 * the vocabulary that quantities are later written in, not a packaging rule.
 *
 * <p><strong>Why the fields look the way they do.</strong>
 * <ul>
 *   <li>{@code code} is the operational key users type ({@code KG}, {@code PCS}).
 *       It is normalised to upper case and made unique per company, so the same
 *       short code always means the same thing inside a company while two
 *       companies stay free to define their own set.</li>
 *   <li>{@code dimension} is the physical quantity being measured. It is what
 *       makes an illegal operation &mdash; adding kilograms to litres &mdash;
 *       detectable at the domain layer rather than only in a report.</li>
 *   <li>{@code decimalScale} is how many decimal places quantities in this unit
 *       may carry. Kilograms need two, pieces need zero. Storing it on the unit
 *       means the rounding rule travels with the vocabulary instead of being
 *       hard-coded at every call site.</li>
 * </ul>
 */
@Entity
@Table(
        name = "units",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_unit_code",
                        columnNames = {"company_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_units_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_units_dimension", columnList = "tenant_id, company_id, dimension")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Unit extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /** Upper bound on {@link #decimalScale}; keeps rounding within BigDecimal's useful range. */
    public static final int MAX_DECIMAL_SCALE = 6;

    /**
     * Decimal places applied when a caller does not specify one.
     *
     * <p>Zero, because counts and packaging pieces are whole numbers and are the
     * most common units by row count. A fractional default would silently permit
     * quantities like 2.5 boxes.
     */
    public static final int DEFAULT_DECIMAL_SCALE = 0;

    @NotBlank(message = "Unit code is required")
    @Size(max = 32, message = "Unit code must not exceed 32 characters")
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @NotBlank(message = "Unit name is required")
    @Size(max = 100, message = "Unit name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "Unit dimension is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 32)
    private UnitDimension dimension;

    @Size(max = 16, message = "Unit symbol must not exceed 16 characters")
    @Column(name = "symbol", length = 16)
    private String symbol;

    /**
     * Number of decimal places a quantity in this unit may carry.
     *
     * <p>Defaults to 0 because the most common units by row count are counts and
     * packaging pieces, which are whole numbers.
     */
    @Column(name = "decimal_scale", nullable = false)
    @Builder.Default
    private Integer decimalScale = 0;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Creates a new active unit and assigns its tenant/company scope.
     *
     * <p>Scope is assigned here rather than by the caller so that no code path can
     * construct a unit without it; {@link TenantScopedEntity} rejects an unscoped
     * row at flush time.
     *
     * @throws IllegalArgumentException if any field fails domain validation
     * @throws IllegalStateException    if either scope id is missing
     */
    public static Unit create(Long tenantId, Long companyId, String name, String code,
                              UnitDimension dimension, String symbol, Integer decimalScale) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);
        validateName(name);
        validateCode(code);
        validateDimension(dimension);
        // Resolve the default first, then validate. Validating before defaulting
        // would reject the very null this branch exists to handle.
        int scale = decimalScale == null ? DEFAULT_DECIMAL_SCALE : decimalScale;
        validateDecimalScale(scale);

        Unit unit = Unit.builder()
                .name(name.trim())
                .code(code.trim().toUpperCase())
                .dimension(dimension)
                .symbol(trimToNull(symbol))
                .decimalScale(scale)
                .isActive(true)
                .build();

        unit.assignScope(tenantId, companyId);
        return unit;
    }

    /**
     * Applies a partial update.
     *
     * <p>Required fields ({@code name}, {@code code}, {@code dimension}) are only
     * touched when non-null, so a PATCH that omits them leaves them alone.
     * {@code symbol} and {@code decimalScale} are assigned unconditionally: null
     * there means "clear it", which is a legitimate instruction from a client
     * editing the form.
     *
     * @throws IllegalArgumentException if a supplied field fails domain validation
     */
    public void update(String name, String code, UnitDimension dimension, String symbol, Integer decimalScale) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (code != null) {
            validateCode(code);
            this.code = code.trim().toUpperCase();
        }
        if (dimension != null) {
            validateDimension(dimension);
            this.dimension = dimension;
        }
        if (decimalScale != null) {
            validateDecimalScale(decimalScale);
        }
        this.symbol = trimToNull(symbol);
        if (decimalScale != null) {
            this.decimalScale = decimalScale;
        }
    }

    /**
     * Marks the unit active.
     *
     * <p>Fails loudly instead of silently no-oping. A unit is a low-row-count
     * master record that is never touched concurrently by background jobs, so a
     * redundant call is a client bug worth surfacing rather than absorbing.
     *
     * @throws IllegalStateException if the unit is already active
     */
    public void activate() {
        if (Boolean.TRUE.equals(this.isActive)) {
            throw new IllegalStateException("Unit is already active");
        }
        this.isActive = true;
    }

    /**
     * Marks the unit inactive.
     *
     * @throws IllegalStateException if the unit is already inactive
     */
    public void deactivate() {
        if (Boolean.FALSE.equals(this.isActive)) {
            throw new IllegalStateException("Unit is already inactive");
        }
        this.isActive = false;
    }

    /** Whether a quantity expressed in this unit may carry decimal places. */
    public boolean isFractional() {
        return decimalScale != null && decimalScale > 0;
    }

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

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Unit name must not be blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Unit name must not exceed 100 characters");
        }
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Unit code must not be blank");
        }
        if (code.length() > 32) {
            throw new IllegalArgumentException("Unit code must not exceed 32 characters");
        }
    }

    private static void validateDimension(UnitDimension dimension) {
        if (dimension == null) {
            throw new IllegalArgumentException("Unit dimension is required");
        }
    }

    private static void validateDecimalScale(Integer decimalScale) {
        if (decimalScale == null) {
            throw new IllegalArgumentException("Unit decimal scale is required");
        }
        if (decimalScale < 0) {
            throw new IllegalArgumentException("Unit decimal scale cannot be negative");
        }
        if (decimalScale > MAX_DECIMAL_SCALE) {
            throw new IllegalArgumentException(
                    "Unit decimal scale must not exceed " + MAX_DECIMAL_SCALE + " digits");
        }
    }

    private static String trimToNull(String value) {
        return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
    }
}
