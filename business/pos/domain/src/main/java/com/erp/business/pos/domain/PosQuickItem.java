package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A configured quick-access item tile on the POS terminal grid.
 *
 * <p>Quick items provide one-tap access to frequently sold products. They are
 * optionally scoped to a specific register ({@link #registerId} nullable — a
 * null register means the tile is shared across all registers in the company).
 *
 * <p>Grid position is defined by {@link #gridRow} and {@link #gridCol}.
 * The UI reads these to place tiles in the correct position. Duplicate positions
 * are allowed (the UI resolves conflicts by sort order).
 *
 * @since 1.0.0
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_quick_items",
        indexes = {
                @Index(name = "idx_pos_quick_items_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_quick_items_register",
                        columnList = "tenant_id, company_id, register_id, sort_order"),
                @Index(name = "idx_pos_quick_items_item",
                        columnList = "tenant_id, company_id, item_id"),
                @Index(name = "idx_pos_quick_items_tab_page",
                        columnList = "tenant_id, company_id, tab_name, page_number, is_active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosQuickItem extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Optional register this tile belongs to.
     * {@code null} means the tile is shared across all registers in the company.
     */
    @Column(name = "register_id")
    private Long registerId;

    // -----------------------------------------------------------------------
    // Item reference (nullable for folder tiles)
    // -----------------------------------------------------------------------

    @Column(name = "item_id")
    private Long itemId;

    @Size(max = 100)
    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Size(max = 255)
    @Column(name = "item_name", length = 255)
    private String itemName;

    @Size(max = 32)
    @Column(name = "uom_code", length = 32)
    private String uomCode;

    /**
     * Override price shown on the tile. When null, the current price is fetched
     * from the inventory service when the item is added to the cart.
     */
    @Column(name = "default_price", precision = 19, scale = 4)
    private BigDecimal defaultPrice;

    // -----------------------------------------------------------------------
    // Grid position
    // -----------------------------------------------------------------------

    @Column(name = "grid_row", nullable = false)
    @Builder.Default
    private int gridRow = 0;

    @Column(name = "grid_col", nullable = false)
    @Builder.Default
    private int gridCol = 0;

    /** CSS hex color for the button (e.g. "#3b82f6"). Null = use theme default. */
    @Size(max = 20)
    @Column(name = "display_color", length = 20)
    private String displayColor;

    /** Icon name or class for the button. Null = use item image or generic icon. */
    @Size(max = 100)
    @Column(name = "display_icon", length = 100)
    private String displayIcon;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false,
            columnDefinition = "boolean default true")
    @Builder.Default
    private boolean isActive = true;

    // -----------------------------------------------------------------------
    // New UI features
    // -----------------------------------------------------------------------

    @Column(name = "tab_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100) DEFAULT 'All Items'")
    @Builder.Default
    private String tabName = "All Items";

    @Column(name = "page_number", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    @Builder.Default
    private int pageNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PRODUCT'")
    @Builder.Default
    private QuickItemType itemType = QuickItemType.PRODUCT;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_tax_inclusive", nullable = false,
            columnDefinition = "boolean default true")
    @Builder.Default
    private boolean isTaxInclusive = true;

    @Size(max = 16)
    @Column(name = "hotkey", length = 16)
    private String hotkey;

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Creates a new quick-item tile. Scope must be assigned before persisting.
     */
    public static PosQuickItem create(
            Long registerId, Long itemId, String itemCode, String itemName, String uomCode,
            BigDecimal defaultPrice, int gridRow, int gridCol,
            String displayColor, String displayIcon, int sortOrder) {

        return PosQuickItem.builder()
                .registerId(registerId)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .defaultPrice(defaultPrice)
                .gridRow(gridRow)
                .gridCol(gridCol)
                .displayColor(displayColor)
                .displayIcon(displayIcon)
                .sortOrder(sortOrder)
                .isActive(true)
                .build();
    }

    /**
     * Updates the tile's item reference, presentation and placement.
     */
    public void updateDetails(Long itemId, String itemCode, String itemName, String uomCode,
                              BigDecimal defaultPrice, int gridRow, int gridCol,
                              String displayColor, String displayIcon, int sortOrder) {
        this.itemId = itemId;
        if (itemCode != null) this.itemCode = itemCode.trim();
        if (itemName != null) this.itemName = itemName.trim();
        if (uomCode != null) this.uomCode = uomCode.trim();
        this.defaultPrice = defaultPrice;
        this.gridRow = gridRow;
        this.gridCol = gridCol;
        this.displayColor = displayColor;
        this.displayIcon = displayIcon;
        this.sortOrder = sortOrder;
    }

    /**
     * Moves the tile to a new grid position.
     */
    public void reposition(int gridRow, int gridCol, int sortOrder) {
        this.gridRow = gridRow;
        this.gridCol = gridCol;
        this.sortOrder = sortOrder;
    }

    /** Deactivates this quick item tile without deleting it. */
    public void deactivate() {
        this.isActive = false;
    }

    /** Re-activates a deactivated quick item tile. */
    public void activate() {
        this.isActive = true;
    }

    /** Assigns the tab and page this tile belongs to. */
    public void assignTab(String tabName, int pageNumber) {
        this.tabName = (tabName == null || tabName.isBlank()) ? "All Items" : tabName.trim();
        this.pageNumber = pageNumber < 1 ? 1 : pageNumber;
    }
}
