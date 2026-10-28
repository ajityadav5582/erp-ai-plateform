package com.erp.business.sales.register;

import com.erp.business.sales.register.dto.SalesRegisterResponse;

import java.time.LocalDate;

/**
 * Sales register use case: the VAT-201 figures for a period, net of credit notes.
 *
 * <p>Read-only and tenant/company-scoped by its leading parameters. Authorisation
 * is declared with {@code @RequiresPermission} on the implementation.
 */
public interface SalesRegisterService {

    SalesRegisterResponse getRegister(Long tenantId, Long companyId, LocalDate from, LocalDate to);
}
