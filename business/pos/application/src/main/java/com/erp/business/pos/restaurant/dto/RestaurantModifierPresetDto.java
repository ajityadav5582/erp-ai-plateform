package com.erp.business.pos.restaurant.dto;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantModifierPresetDto(
        String groupName,
        boolean multiple,
        List<Option> options
) {
    public record Option(
            String name,
            BigDecimal priceDelta
    ) {}
}

