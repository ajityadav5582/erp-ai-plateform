package com.erp.business.pos.poscompanyconfig.dto;

import java.math.BigDecimal;

/**
 * Response body for a company's POS configuration.
 */
public record PosCompanyConfigResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String enabledPaymentMethods,
        boolean cashRounding,
        BigDecimal serviceChargePercent,
        String tipHandling,
        Long defaultRegisterId,
        Long defaultWarehouseId,
        Long priceListId,
        String restaurantSettings
) {
}
