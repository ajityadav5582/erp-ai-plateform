package com.erp.business.pos.capability;

import org.springframework.stereotype.Component;

/**
 * Reusable guard for business-type-gated POS features (e.g. restaurant
 * table/kitchen endpoints). Throws {@link IllegalStateException} when the
 * company's capabilities do not include the requested feature.
 *
 * <p>Apply in service methods once restaurant endpoints are introduced
 * (Phase 4). Kept as a small, testable component rather than an aspect to
 * avoid AOP complexity.
 */
@Component
public class PosCapabilityGuard {

    private final PosCapabilityService capabilityService;

    public PosCapabilityGuard(PosCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    public void requireRestaurantFamily(String businessType) {
        if (!capabilityService.isRestaurantFamily(businessType)) {
            throw new IllegalStateException(
                    "Operation requires a restaurant-family business type, got: " + businessType);
        }
    }

    public void require(PosCapabilities caps, java.util.function.Predicate<PosCapabilities> predicate, String feature) {
        if (!predicate.test(caps)) {
            throw new IllegalStateException("Operation requires capability: " + feature);
        }
    }
}
