package com.erp.business.pos.poscompanyconfig;

import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigRequest;
import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigResponse;

/**
 * Application service for per-company POS configuration.
 */
public interface PosCompanyConfigService {

    PosCompanyConfigResponse get(Long tenantId, Long companyId);

    PosCompanyConfigResponse createOrUpdate(Long tenantId, Long companyId, PosCompanyConfigRequest request);

    /**
     * Returns true when the given payment method is enabled for the company.
     * A null/blank enabled list means "all methods enabled" (backward compatible).
     */
    boolean isPaymentMethodEnabled(Long tenantId, Long companyId, String paymentMethod);
}
