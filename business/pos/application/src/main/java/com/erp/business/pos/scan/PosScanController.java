package com.erp.business.pos.scan;

import com.erp.business.pos.scan.dto.ScannedItemResponse;
import com.erp.business.pos.shared.security.PosScanPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * POS scan endpoint.
 *
 * <p>Resolves a scanned barcode / SKU / product code to a priced item in one
 * call. Authorisation lives on the service via {@code @RequiresPermission}.
 */
@RestController
@RequestMapping("/api/v1/pos/scan")
@RequiredArgsConstructor
public class PosScanController {

    private final PosItemLookupService itemLookupService;

    @GetMapping
    @RequiresPermission(PosScanPermissions.POS_SCAN)
    public ResponseEntity<ScannedItemResponse> scan(@RequestParam("code") String code) {
        return ResponseEntity.ok(itemLookupService.resolve(code));
    }
}
