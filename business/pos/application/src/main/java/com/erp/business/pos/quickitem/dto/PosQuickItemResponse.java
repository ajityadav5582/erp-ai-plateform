package com.erp.business.pos.quickitem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model for a quick-item tile: the frozen item snapshot, the optional
 * price override and the grid placement the terminal renders.
 */
public record PosQuickItemResponse(

        Long id,

        Long tenantId,

        Long companyId,

        /** {@code null} when the tile is shared across all registers in the company. */
        Long registerId,

        Long itemId,

        String itemCode,

        String itemName,

        String uomCode,

        /** Price override shown on the tile; {@code null} fetches the current price. */
        BigDecimal defaultPrice,

        int gridRow,

        int gridCol,

        String displayColor,

        String displayIcon,

        int sortOrder,

        boolean isActive,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        Integer version
) {
}
