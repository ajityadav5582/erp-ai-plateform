package com.erp.business.pos.quickitem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create payload for a quick-item tile.
 *
 * <p>The item snapshot (code, name, UOM) is resolved server-side from the
 * Inventory catalogue — the caller supplies only the item id, so a tile can
 * never carry a stale or hand-edited snapshot.
 *
 * @param registerId   register the tile belongs to; {@code null} shares the
 *                     tile across all registers in the company
 * @param itemId       the inventory item the tile sells
 * @param defaultPrice optional price override shown on the tile; {@code null}
 *                     always fetches the current catalogue price
 * @param gridRow      grid row (0-based)
 * @param gridCol      grid column (0-based)
 * @param displayColor CSS hex colour (e.g. {@code "#3b82f6"}); {@code null} uses the theme default
 * @param displayIcon  icon name or class; {@code null} uses the item image or a generic icon
 * @param sortOrder    ordering within the grid
 */
public record CreateQuickItemRequest(

        Long registerId,

        @NotNull(message = "Item is required")
        Long itemId,

        @DecimalMin(value = "0.0000", message = "Default price must not be negative")
        BigDecimal defaultPrice,

        @Min(value = 0, message = "Grid row must not be negative")
        int gridRow,

        @Min(value = 0, message = "Grid column must not be negative")
        int gridCol,

        @Size(max = 20, message = "Display colour must not exceed 20 characters")
        String displayColor,

        @Size(max = 100, message = "Display icon must not exceed 100 characters")
        String displayIcon,

        @Min(value = 0, message = "Sort order must not be negative")
        int sortOrder
) {
}
