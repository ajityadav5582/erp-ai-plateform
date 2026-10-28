package com.erp.business.pos.scan;

import com.erp.business.inventory.interfaces.item.ItemService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link BarcodeResolver}: tries the code as a barcode first, then as a
 * SKU / product code. A single catalogue lookup per attempt, so a scan resolves
 * in one round-trip in the common case.
 */
@Component
public class InventoryBarcodeResolver implements BarcodeResolver {

    private final ItemService itemService;

    public InventoryBarcodeResolver(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public Optional<ItemService.ItemResponse> resolve(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String trimmed = code.trim();
        return itemService.findByBarcode(trimmed)
                .or(() -> itemService.findBySku(trimmed));
    }
}
