package com.erp.business.pos.quickitem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchLayoutRequest(
        @NotBlank String tabName,
        @Min(1) int pageNumber,
        Long registerId,
        @NotEmpty @Valid List<SlotPlacement> slots
) {
    public record SlotPlacement(
            @NotNull Long id,
            @Min(0) @Max(2) int gridRow,
            @Min(0) @Max(5) int gridCol
    ) {
    }
}
