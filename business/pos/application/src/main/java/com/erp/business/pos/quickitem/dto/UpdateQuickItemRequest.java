package com.erp.business.pos.quickitem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Update payload for a quick-item tile. The register scope is fixed at
 * creation — moving a tile between registers is a delete + create.
 *
 * <p>The item snapshot (code, name, UOM) is re-resolved server-side from the
 * catalogue, so a re-pointed tile never carries a stale snapshot.
 *
 * @param itemId       the inventory item the tile sells
 * @param defaultPrice optional price override; {@code null} always fetches the current price
 * @param gridRow      grid row (0-based)
 * @param gridCol      grid column (0-based)
 * @param displayColor CSS hex colour; {@code null} uses the theme default
 * @param displayIcon  icon name or class; {@code null}
 * @param sortOrder    ordering within the grid
 */
public record UpdateQuickItemRequest(

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
