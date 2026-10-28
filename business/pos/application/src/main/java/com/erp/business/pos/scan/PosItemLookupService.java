package com.erp.business.pos.scan;

import com.erp.business.pos.scan.dto.ScannedItemResponse;

/**
 * Resolves a scanned code into a priced item for the POS terminal.
 *
 * <p>Tenant and company are scoped by the authenticated request context on the
 * Inventory side. Authorisation is declared with {@code @RequiresPermission} on
 * the implementation and enforced by the shared aspect before the body runs.
 */
public interface PosItemLookupService {

    /**
     * Resolves a scanned barcode / SKU / product code to a priced item.
     *
     * @param code the raw scanned string
     * @throws com.erp.business.pos.scan.exception.ItemNotScannableException if nothing matches
     */
    ScannedItemResponse resolve(String code);
}
