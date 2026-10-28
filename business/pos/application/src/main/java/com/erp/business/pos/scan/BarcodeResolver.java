package com.erp.business.pos.scan;

import com.erp.business.inventory.interfaces.item.ItemService;

import java.util.Optional;

/**
 * Strategy for turning a scanned string into an item.
 *
 * <p>An interface rather than a hard-coded lookup so new symbologies or code
 * schemes (for example price-embedded or weighted barcodes) can be added as a
 * new resolver without touching the scan flow.
 */
public interface BarcodeResolver {

    /**
     * Resolves a scanned code to an item, or empty when nothing matches.
     *
     * @param code the raw scanned string (barcode, SKU or product code)
     */
    Optional<ItemService.ItemResponse> resolve(String code);
}
