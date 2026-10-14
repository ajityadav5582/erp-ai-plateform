package com.erp.business.pos.scan;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.pos.pricing.PosPricingService;
import com.erp.business.pos.pricing.ResolvedPrice;
import com.erp.business.pos.scan.dto.ScannedItemResponse;
import com.erp.business.pos.scan.exception.ItemNotScannableException;
import com.erp.business.pos.shared.security.PosScanPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a scanned code to a priced item in a single catalogue lookup, then
 * attaches the resolved price and VAT so the terminal can add the line and show
 * totals without a second round-trip.
 */
@Service
@Transactional(readOnly = true)
public class PosItemLookupServiceImpl implements PosItemLookupService {

    private final BarcodeResolver barcodeResolver;
    private final PosPricingService pricingService;

    public PosItemLookupServiceImpl(BarcodeResolver barcodeResolver, PosPricingService pricingService) {
        this.barcodeResolver = barcodeResolver;
        this.pricingService = pricingService;
    }

    @Override
    @RequiresPermission(PosScanPermissions.POS_SCAN)
    public ScannedItemResponse resolve(String code) {
        ItemService.ItemResponse item = barcodeResolver.resolve(code)
                .orElseThrow(() -> new ItemNotScannableException(
                        "No item found for code '" + code + "'"));

        ResolvedPrice price = pricingService.resolvePrice(item);

        return new ScannedItemResponse(
                item.getId(),
                item.getSku(),
                item.getBarcode(),
                item.getName(),
                item.getUnitCode(),
                price.unitPrice(),
                price.vatType(),
                price.vatRate(),
                item.getAvailableStock(),
                item.getIsStockable()
        );
    }
}
