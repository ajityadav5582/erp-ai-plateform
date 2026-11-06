package com.erp.business.pos.quickitem.dto;

import com.erp.business.pos.domain.QuickItemType;

import java.math.BigDecimal;

/**
 * Resolved quick-item tile for the terminal grid, after applying register-specific
 * overrides over company-wide defaults and enriching with catalogue pricing.
 */
public record PosQuickItemResolvedDto(
        int gridRow,
        int gridCol,
        String scope,            // "COMPANY" or "REGISTER"
        Long quickItemId,
        QuickItemType itemType,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal effectivePrice,
        BigDecimal vatRate,
        boolean taxInclusive,
        String hotkey,
        String displayColor,
        String displayIcon,
        Long parentId
) {
}
