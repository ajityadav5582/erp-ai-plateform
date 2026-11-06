package com.erpai.integration.ird.adapter.in.web.dto;

import com.erpai.integration.ird.domain.IrdEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigureCompanyIrdRequest(
        @NotBlank String sellerPan,
        @NotBlank String apiUsername,
        @NotBlank String apiPassword,
        @NotNull IrdEnvironment environment
) {
}
