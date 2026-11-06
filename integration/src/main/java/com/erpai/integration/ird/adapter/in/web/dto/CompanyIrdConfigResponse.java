package com.erpai.integration.ird.adapter.in.web.dto;

import com.erpai.integration.ird.domain.IrdEnvironment;
import com.erpai.integration.ird.domain.IrdVerificationStatus;
import java.time.Instant;

public record CompanyIrdConfigResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String sellerPan,
        String apiUsername,
        IrdEnvironment environment,
        boolean cbmsActive,
        IrdVerificationStatus verificationStatus,
        Instant lastVerifiedAt,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
