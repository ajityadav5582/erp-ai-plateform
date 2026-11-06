package com.erp.business.pos.capability;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Backend mirror of {@code frontend/src/config/pos-capabilities.ts}.
 * Derives POS capability flags from the company's business type string.
 * Shared cart/checkout/tax/payment behaviour stays common; only these flags vary.
 */
@Service
public class PosCapabilityService {

    private static final Map<String, PosCapabilities> BY_TYPE = Map.ofEntries(
            Map.entry("RESTAURANT", new PosCapabilities(false, true, true, true, false, true)),
            Map.entry("CAFE", new PosCapabilities(false, true, true, true, false, true)),
            Map.entry("HOTEL", new PosCapabilities(false, true, true, true, false, true)),
            Map.entry("SWEETS_RETAIL", new PosCapabilities(true, true, false, true, true, false)),
            Map.entry("GENERAL_RETAIL", new PosCapabilities(true, true, false, true, false, false)),
            Map.entry("GROCERY", new PosCapabilities(true, true, false, true, true, false)),
            Map.entry("HARDWARE", new PosCapabilities(true, true, false, true, false, false)),
            Map.entry("SANITARY", new PosCapabilities(true, true, false, true, false, false)),
            Map.entry("PHARMACY", new PosCapabilities(true, true, false, true, true, false)),
            Map.entry("CLOTHING", new PosCapabilities(true, true, false, true, false, false)),
            Map.entry("SERVICES", new PosCapabilities(false, true, true, false, false, true))
    );

    public PosCapabilities getCapabilities(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return PosCapabilities.DEFAULT;
        }
        return BY_TYPE.getOrDefault(businessType.toUpperCase(), PosCapabilities.DEFAULT);
    }

    public boolean isRestaurantFamily(String businessType) {
        PosCapabilities c = getCapabilities(businessType);
        return c.orderTypes() && c.serviceSelection();
    }
}
