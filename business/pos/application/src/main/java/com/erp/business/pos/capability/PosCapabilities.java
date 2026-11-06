package com.erp.business.pos.capability;

/**
 * Backend mirror of {@code frontend/src/config/pos-capabilities.ts}.
 * Capability flags derived from the company's business type. Shared cart,
 * checkout, tax and payment behaviour stays common; only these flags vary.
 */
public record PosCapabilities(
        boolean barcodeScanning,
        boolean productGrid,
        boolean orderTypes,
        boolean productVariants,
        boolean batchTracking,
        boolean serviceSelection
) {
    public static final PosCapabilities DEFAULT = new PosCapabilities(
            true, true, false, false, false, false);
}
