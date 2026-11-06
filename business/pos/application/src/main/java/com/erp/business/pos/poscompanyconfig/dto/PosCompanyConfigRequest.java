package com.erp.business.pos.poscompanyconfig.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for creating/updating a company's POS configuration.
 */
public record PosCompanyConfigRequest(
        @Size(max = 500) String enabledPaymentMethods,
        Boolean cashRounding,
        BigDecimal serviceChargePercent,
        @Size(max = 20) String tipHandling,
        Long defaultRegisterId,
        Long defaultWarehouseId,
        Long priceListId,
        String restaurantSettings
) {
}
