package com.erp.business.pos.restaurant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdatePosTableRequest(
        @Size(max = 20, message = "Table number must not exceed 20 characters")
        String tableNumber,

        @Min(value = 1, message = "Seating capacity must be at least 1")
        Integer capacity,

        @Size(max = 50, message = "Section must not exceed 50 characters")
        String section
) {
}

