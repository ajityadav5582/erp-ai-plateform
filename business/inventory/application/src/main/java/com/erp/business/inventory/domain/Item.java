package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import lombok.ToString;

import java.math.BigDecimal;

/**
 * An item / product in inventory with full support for Nepal IRD tax & billing compliance rules.
 */
@Entity
@Table(
        name = "items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_item_sku", columnNames = {"company_id", "sku"}),
                // Backstop for the service-level pre-check in ItemServiceImpl. That
                // check is a read-then-write and cannot see a concurrent transaction,
                // so without this index two simultaneous creates can both pass it and
                // commit the same barcode. Postgres treats NULLs as distinct, so items
                // that carry no barcode are unaffected.
                @UniqueConstraint(name = "uk_company_item_barcode", columnNames = {"company_id", "barcode"})
        },
        indexes = {
                @Index(name = "idx_items_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_items_category", columnList = "category_id"),
                @Index(name = "idx_items_uom", columnList = "uom_id"),
                @Index(name = "idx_items_hs_code", columnList = "hs_code"),
                @Index(name = "idx_items_taxability", columnList = "taxability_type"),
                @Index(name = "idx_items_status", columnList = "company_id, is_active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Item extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MAX_SKU_LENGTH = 100;
    private static final int MAX_BARCODE_LENGTH = 100;
    private static final int MAX_HS_CODE_LENGTH = 20;
    private static final int MAX_NAME_EN_LENGTH = 255;
    private static final int MAX_NAME_NP_LENGTH = 255;

    /**
     * Tax rates are stored as percentages, so anything above this is a data-entry
     * slip rather than a real rate.
     */
    private static final BigDecimal MAX_TAX_RATE = new BigDecimal("100.00");

    /**
     * Standard VAT rate under Section 18 of Nepal VAT Act 2052. Declared once here so
     * the column default, {@link #create} and {@link #applyChanges} cannot disagree.
     */
    public static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("13.00");

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    @Column(name = "barcode", length = 100)
    private String barcode;

    @Size(max = 20, message = "HS Code must not exceed 20 characters")
    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @NotBlank(message = "English item name is required")
    @Size(max = 255, message = "English item name must not exceed 255 characters")
    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Size(max = 255, message = "Nepali item name must not exceed 255 characters")
    @Column(name = "name_np", length = 255)
    private String nameNp;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_np", columnDefinition = "TEXT")
    private String descriptionNp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @NotNull(message = "Primary UOM is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    @ToString.Exclude
    private Unit uom;

    @NotNull(message = "Item type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    @Builder.Default
    private ItemType itemType = ItemType.GOODS;

    @NotNull(message = "Taxability type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "taxability_type", nullable = false, length = 20)
    @Builder.Default
    private TaxabilityType taxabilityType = TaxabilityType.TAXABLE;

    @NotNull(message = "VAT rate is required")
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = DEFAULT_VAT_RATE;

    @NotNull(message = "Excise type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "excise_type", nullable = false, length = 20)
    @Builder.Default
    private ExciseType exciseType = ExciseType.NONE;

    @Column(name = "excise_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal exciseRate = BigDecimal.ZERO;

    @Column(name = "excise_amount", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal exciseAmount = BigDecimal.ZERO;

    @NotNull(message = "Selling price is required")
    @Column(name = "selling_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal sellingPrice;

    @Column(name = "purchase_price", precision = 15, scale = 4)
    private BigDecimal purchasePrice;

    @Column(name = "mrp", precision = 15, scale = 4)
    private BigDecimal mrp;

    @Column(name = "is_vat_inclusive", nullable = false)
    @Builder.Default
    private Boolean isVatInclusive = false;

    @Column(name = "is_inventory_tracked", nullable = false)
    @Builder.Default
    private Boolean isInventoryTracked = true;

    @Column(name = "is_batch_tracked", nullable = false)
    @Builder.Default
    private Boolean isBatchTracked = false;

    @Column(name = "is_expiry_tracked", nullable = false)
    @Builder.Default
    private Boolean isExpiryTracked = false;

    @Column(name = "min_stock_level", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal minStockLevel = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "reorder_quantity", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal reorderQuantity = BigDecimal.ZERO;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        if (Boolean.TRUE.equals(this.isActive)) {
            throw new IllegalStateException("Item is already active");
        }
        this.isActive = true;
    }

    public void deactivate() {
        if (Boolean.FALSE.equals(this.isActive)) {
            throw new IllegalStateException("Item is already inactive");
        }
        this.isActive = false;
    }

    /**
     * A partial set of item changes.
     *
     * <p>Every field is nullable and a {@code null} field means "leave as is", which is
     * the same patch semantics {@code UpdateItemRequest} already uses. Passing the whole
     * group through one object keeps validation in the entity instead of spread across
     * two dozen setters in the service.
     *
     * <p>Uniqueness of {@link #sku} and {@link #barcode} is deliberately not checked
     * here: that needs a repository lookup and belongs to the service, which races with
     * the database unique constraints that are the real guarantee.
     */
    @Builder
    public record ItemChanges(
            String sku,
            String barcode,
            String hsCode,
            String nameEn,
            String nameNp,
            String descriptionEn,
            String descriptionNp,
            Category category,
            Unit uom,
            ItemType itemType,
            TaxabilityType taxabilityType,
            BigDecimal vatRate,
            ExciseType exciseType,
            BigDecimal exciseRate,
            BigDecimal exciseAmount,
            BigDecimal sellingPrice,
            BigDecimal purchasePrice,
            BigDecimal mrp,
            Boolean isVatInclusive,
            Boolean isInventoryTracked,
            Boolean isBatchTracked,
            Boolean isExpiryTracked,
            BigDecimal minStockLevel,
            BigDecimal reorderLevel,
            BigDecimal reorderQuantity) {
    }

    /**
     * Applies a partial change set, validating every supplied value before any field is
     * written.
     *
     * <p>Validating up front is what makes this safe to call on a managed entity: a
     * rejected change leaves the instance exactly as it was instead of half-mutated.
     */
    public void applyChanges(ItemChanges changes) {
        if (changes == null) {
            return;
        }

        if (changes.sku() != null) {
            validateSku(changes.sku());
        }
        if (changes.nameEn() != null) {
            validateNameEn(changes.nameEn());
        }
        validateMaxLength(changes.barcode(), MAX_BARCODE_LENGTH, "Barcode");
        validateMaxLength(changes.hsCode(), MAX_HS_CODE_LENGTH, "HS Code");
        validateMaxLength(changes.nameNp(), MAX_NAME_NP_LENGTH, "Nepali item name");
        requireNonNegative(changes.sellingPrice(), "Selling price");
        requireNonNegative(changes.purchasePrice(), "Purchase price");
        requireNonNegative(changes.mrp(), "MRP");
        requireNonNegative(changes.minStockLevel(), "Minimum stock level");
        requireNonNegative(changes.reorderLevel(), "Reorder level");
        requireNonNegative(changes.reorderQuantity(), "Reorder quantity");
        requireTaxRate(changes.vatRate(), "VAT rate");
        requireTaxRate(changes.exciseRate(), "Excise rate");
        requireNonNegative(changes.exciseAmount(), "Excise amount");

        // Tax and excise rules are checked against the state the item would end up in,
        // not against the fields that happened to be supplied. Checking only the supplied
        // fields would let a caller set a 13% VAT rate and then flip taxability to EXEMPT
        // in a second request, leaving a row that claims to be exempt but is taxed.
        //
        // effectiveVatRate resolves the derived cases first, so flipping taxability without
        // sending a rate is validated against the rate that will actually be stored.
        TaxabilityType effectiveTaxability = changes.taxabilityType() != null
                ? changes.taxabilityType()
                : this.taxabilityType;
        BigDecimal effectiveVatRate = effectiveVatRate(effectiveTaxability, changes.vatRate());
        validateTaxConsistency(effectiveTaxability, effectiveVatRate);
        validateExciseConsistency(
                changes.exciseType() != null ? changes.exciseType() : this.exciseType,
                changes.exciseRate() != null ? changes.exciseRate() : this.exciseRate,
                changes.exciseAmount() != null ? changes.exciseAmount() : this.exciseAmount);

        if (changes.sku() != null) {
            this.sku = changes.sku().trim().toUpperCase();
        }
        if (changes.barcode() != null) {
            this.barcode = trimToNull(changes.barcode());
        }
        if (changes.hsCode() != null) {
            this.hsCode = trimToNull(changes.hsCode());
        }
        if (changes.nameEn() != null) {
            this.nameEn = changes.nameEn().trim();
        }
        if (changes.nameNp() != null) {
            this.nameNp = trimToNull(changes.nameNp());
        }
        if (changes.descriptionEn() != null) {
            this.descriptionEn = trimToNull(changes.descriptionEn());
        }
        if (changes.descriptionNp() != null) {
            this.descriptionNp = trimToNull(changes.descriptionNp());
        }
        if (changes.category() != null) {
            this.category = changes.category();
        }
        if (changes.uom() != null) {
            this.uom = changes.uom();
        }
        if (changes.itemType() != null) {
            this.itemType = changes.itemType();
        }
        if (changes.taxabilityType() != null) {
            this.taxabilityType = changes.taxabilityType();
        }
        if (changes.vatRate() != null) {
            this.vatRate = changes.vatRate();
        } else if (changes.taxabilityType() != null) {
            // Only a taxability change justifies deriving a rate: flipping to
            // EXEMPT/ZERO_RATED must land on zero, and flipping back to TAXABLE must land
            // on the standard rate. A request that leaves taxability alone keeps the
            // stored rate untouched.
            this.vatRate = effectiveVatRate;
        }
        if (changes.exciseType() != null) {
            this.exciseType = changes.exciseType();
        }
        if (changes.exciseRate() != null) {
            this.exciseRate = changes.exciseRate();
        }
        if (changes.exciseAmount() != null) {
            this.exciseAmount = changes.exciseAmount();
        }
        if (changes.sellingPrice() != null) {
            this.sellingPrice = changes.sellingPrice();
        }
        if (changes.purchasePrice() != null) {
            this.purchasePrice = changes.purchasePrice();
        }
        if (changes.mrp() != null) {
            this.mrp = changes.mrp();
        }
        if (changes.isVatInclusive() != null) {
            this.isVatInclusive = changes.isVatInclusive();
        }
        if (changes.isInventoryTracked() != null) {
            this.isInventoryTracked = changes.isInventoryTracked();
        }
        if (changes.isBatchTracked() != null) {
            this.isBatchTracked = changes.isBatchTracked();
        }
        if (changes.isExpiryTracked() != null) {
            this.isExpiryTracked = changes.isExpiryTracked();
        }
        if (changes.minStockLevel() != null) {
            this.minStockLevel = changes.minStockLevel();
        }
        if (changes.reorderLevel() != null) {
            this.reorderLevel = changes.reorderLevel();
        }
        if (changes.reorderQuantity() != null) {
            this.reorderQuantity = changes.reorderQuantity();
        }
    }

    /**
     * Creates a new item.
     *
     * <p>{@code sku}, {@code nameEn}, {@code uom} and {@code sellingPrice} are passed
     * explicitly because a missing one must fail here rather than at flush time.
     * Everything else arrives in {@code changes} and is validated by
     * {@link #applyChanges(ItemChanges)}, so create and update cannot drift apart.
     */
    public static Item create(
            Long tenantId,
            Long companyId,
            String sku,
            String nameEn,
            Unit uom,
            BigDecimal sellingPrice,
            ItemChanges changes) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);
        validateSku(sku);
        validateNameEn(nameEn);
        if (uom == null) {
            throw new IllegalArgumentException("Primary UOM is required");
        }
        requireNonNegative(sellingPrice, "Selling price");
        if (sellingPrice == null) {
            throw new IllegalArgumentException("Selling price is required");
        }

        Item item = Item.builder()
                .sku(sku.trim().toUpperCase())
                .nameEn(nameEn.trim())
                .uom(uom)
                .sellingPrice(sellingPrice)
                .itemType(ItemType.GOODS)
                .taxabilityType(TaxabilityType.TAXABLE)
                .vatRate(DEFAULT_VAT_RATE)
                .exciseType(ExciseType.NONE)
                .isActive(true)
                .build();

        item.applyChanges(changes);
        item.assignScope(tenantId, companyId);
        return item;
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

    private static void validateSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Item SKU must not be blank");
        }
        if (sku.length() > MAX_SKU_LENGTH) {
            throw new IllegalArgumentException(
                    "Item SKU must not exceed " + MAX_SKU_LENGTH + " characters");
        }
    }

    private static void validateNameEn(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            throw new IllegalArgumentException("English item name must not be blank");
        }
        if (nameEn.length() > MAX_NAME_EN_LENGTH) {
            throw new IllegalArgumentException(
                    "English item name must not exceed " + MAX_NAME_EN_LENGTH + " characters");
        }
    }

    private static void validateMaxLength(String value, int max, String label) {
        if (value != null && !value.isBlank() && value.trim().length() > max) {
            throw new IllegalArgumentException(label + " must not exceed " + max + " characters");
        }
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
    }

    private static void requireTaxRate(BigDecimal value, String label) {
        if (value == null) {
            return;
        }
        requireNonNegative(value, label);
        if (value.compareTo(MAX_TAX_RATE) > 0) {
            throw new IllegalArgumentException(label + " must not exceed " + MAX_TAX_RATE + "%");
        }
    }

    /**
     * Resolves the VAT rate to store when taxability changed but no rate was supplied.
     *
     * <p>Returns {@code null} when no derivation applies, which the caller reads as
     * "keep the current rate".
     */
    private static BigDecimal effectiveVatRate(
            TaxabilityType taxabilityType,
            BigDecimal requestedVatRate) {
        if (requestedVatRate != null) {
            return requestedVatRate;
        }
        // A taxability change with no accompanying rate: EXEMPT and ZERO_RATED both mean
        // "no VAT", and returning to TAXABLE means the standard rate again.
        return taxabilityType == TaxabilityType.TAXABLE ? DEFAULT_VAT_RATE : BigDecimal.ZERO;
    }

    /**
     * A non-TAXABLE item must carry a zero VAT rate.
     *
     * <p>EXEMPT (Schedule 1) and ZERO_RATED (Section 10, exports) both mean VAT is not
     * charged, so a stored rate above zero would produce invoices that over-collect tax.
     */
    private static void validateTaxConsistency(TaxabilityType taxabilityType, BigDecimal vatRate) {
        if (taxabilityType == TaxabilityType.TAXABLE) {
            return;
        }
        if (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "VAT rate must be zero for a " + taxabilityType + " item");
        }
    }

    /**
     * Excise amounts must match the declared excise type.
     *
     * <p>PERCENTAGE needs a rate and no fixed amount; SPECIFIC_AMOUNT needs a fixed
     * amount; NONE needs neither. Enforcing this here keeps the invoice tax calculation
     * from having to guess which of the two fields is authoritative.
     */
    private static void validateExciseConsistency(
            ExciseType exciseType,
            BigDecimal exciseRate,
            BigDecimal exciseAmount) {
        if (exciseType == null || exciseType == ExciseType.NONE) {
            if (isPositive(exciseRate) || isPositive(exciseAmount)) {
                throw new IllegalArgumentException(
                        "Excise rate and amount must be zero when no excise applies");
            }
            return;
        }
        if (exciseType == ExciseType.PERCENTAGE) {
            if (!isPositive(exciseRate)) {
                throw new IllegalArgumentException(
                        "A positive excise rate is required for PERCENTAGE excise");
            }
            return;
        }
        if (!isPositive(exciseAmount)) {
            throw new IllegalArgumentException(
                    "A positive excise amount is required for SPECIFIC_AMOUNT excise");
        }
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String trimToNull(String value) {
        return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
    }
}
