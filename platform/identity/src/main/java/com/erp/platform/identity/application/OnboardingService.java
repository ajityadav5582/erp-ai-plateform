package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.OnboardingRequest;
import com.erp.platform.identity.application.dto.OnboardingResponse;

/**
 * Service interface for tenant onboarding.
 *
 * <p>Handles account registration and internal SaaS tenant provisioning:
 * <ul>
 *   <li>Creates an internal tenant record</li>
 *   <li>Creates the user account and applies existing role templates</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface OnboardingService {

    /**
     * Registers an account and provisions its internal tenant context.
     *
     * <p>No Company is created by this method.
     *
     * @param request the account registration details
     * @return the onboarding response
     */
    OnboardingResponse onboard(OnboardingRequest request);
}
