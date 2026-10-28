package com.erp.business.pos.quickitem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Batch reorder payload: the new grid placement of several tiles in one call.
 *
 * <p>The terminal drags tiles around and persists the whole layout at once, so
 * a reorder is a single atomic write rather than one request per tile. Tiles
 * absent from the list keep their current placement.
 *
 * @param placements the new placements; at least one
 */
public record ReorderQuickItemsRequest(

        @NotEmpty(message = "At least one placement is required")
        @Valid
        List<Placement> placements
) {

    /**
     * One tile's new placement.
     *
     * @param id        the quick item being moved
     * @param gridRow   new grid row (0-based)
     * @param gridCol   new grid column (0-based)
     * @param sortOrder new ordering within the grid
     */
    public record Placement(

            @NotNull(message = "Quick item id is required")
            Long id,

            @Min(value = 0, message = "Grid row must not be negative")
            int gridRow,

            @Min(value = 0, message = "Grid column must not be negative")
            int gridCol,

            @Min(value = 0, message = "Sort order must not be negative")
            int sortOrder
    ) {
    }
}
