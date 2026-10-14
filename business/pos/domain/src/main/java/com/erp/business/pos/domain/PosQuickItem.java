package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(
        name = "pos_quick_items",
        indexes = {
                @Index(name = "idx_pos_quick_items_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_quick_items_register",
                        columnList = "tenant_id, company_id, register_id, sort_order"),
                @Index(name = "idx_pos_quick_items_item",
                        columnList = "tenant_id, company_id, item_id")
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
    // Item reference
    // -----------------------------------------------------------------------

    @NotNull(message = "Item id is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotBlank(message = "Item code snapshot is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name snapshot is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "Unit of measure code is required")
    @Column(name = "uom_code", nullable = false, length = 32)
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
     *
     * <p>The item snapshot fields are refreshed from the catalogue by the
     * caller, so a re-pointed tile never carries a stale code or name. The
     * register scope is fixed at creation — moving a tile between registers is
     * a delete + create, keeping each register's grid history coherent.
     *
     * @param itemId       new item id (required)
     * @param itemCode     new item code snapshot
     * @param itemName     new item name snapshot
     * @param uomCode      new unit-of-measure code snapshot
     * @param defaultPrice override price shown on the tile, or {@code null} to
     *                     always fetch the current price
     * @param gridRow      grid row (0-based)
     * @param gridCol      grid column (0-based)
     * @param displayColor CSS hex colour, or {@code null} for the theme default
     * @param displayIcon  icon name, or {@code null}
     * @param sortOrder    ordering within the grid
     */
    public void updateDetails(Long itemId, String itemCode, String itemName, String uomCode,
                              BigDecimal defaultPrice, int gridRow, int gridCol,
                              String displayColor, String displayIcon, int sortOrder) {
        Objects.requireNonNull(itemId, "Item id is required");
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Item code is required");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (uomCode == null || uomCode.isBlank()) {
            throw new IllegalArgumentException("Unit of measure code is required");
        }
        this.itemId = itemId;
        this.itemCode = itemCode.trim();
        this.itemName = itemName.trim();
        this.uomCode = uomCode.trim();
        this.defaultPrice = defaultPrice;
        this.gridRow = gridRow;
        this.gridCol = gridCol;
        this.displayColor = displayColor;
        this.displayIcon = displayIcon;
        this.sortOrder = sortOrder;
    }

    /**
     * Moves the tile to a new grid position. Called by the reorder use case,
     * which persists a whole dragged layout in one batch.
     *
     * @param gridRow   new grid row (0-based)
     * @param gridCol   new grid column (0-based)
     * @param sortOrder new ordering within the grid
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
}
