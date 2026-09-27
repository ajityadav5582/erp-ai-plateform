package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.FiscalYearType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyFiscalYearRequest(
        @NotNull Short masterFiscalYearId,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String startDateBs,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String endDateBs,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String startDateAd,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String endDateAd,
        @NotNull FiscalYearType calendarType
) {}
