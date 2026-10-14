package com.erp.business.pos.pricing;

import com.erp.business.pos.shared.security.PosScanPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * POS pricing preview endpoint.
 *
 * <p>Prices a cart server-side (unit prices resolved from the catalogue, never
 * from the request) and returns the header totals, so the terminal can show an
 * authoritative subtotal / VAT / total. Authorisation lives on the service via
 * {@code @RequiresPermission}.
 */
@RestController
@RequestMapping("/api/v1/pos/pricing")
@RequiredArgsConstructor
public class PosPricingController {

    private final PosPricingService pricingService;

    @PostMapping("/preview")
    @RequiresPermission(PosScanPermissions.POS_SCAN)
    public ResponseEntity<PricePreview> preview(@Valid @RequestBody List<PriceLineRequest> lines) {
        return ResponseEntity.ok(pricingService.preview(lines));
    }
}
