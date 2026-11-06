package com.erpai.integration.ird.port.in;

public interface VerifyIrdConnectionUseCase {
    boolean verify(Long tenantId, Long companyId);
}
