package com.erpai.integration.ird.port.in;

import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.domain.IrdEnvironment;
import com.erpai.integration.ird.domain.SellerPan;

public interface ConfigureCompanyIrdUseCase {
    CompanyIrdConfig configure(Long tenantId, Long companyId, SellerPan sellerPan, String apiUsername,
                               String apiPassword, IrdEnvironment environment);
    CompanyIrdConfig update(Long tenantId, Long companyId, SellerPan sellerPan, String apiUsername,
                            String apiPassword, IrdEnvironment environment);
}
