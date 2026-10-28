package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.OnboardingService;
import com.erp.platform.identity.application.dto.OnboardingRequest;
import com.erp.platform.identity.application.dto.OnboardingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for account registration.
 *
 * <p>The tenant context is provisioned internally. Registration does not
 * create a Company.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/identity/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Registers an account.
     *
     * <p>Creates the account and required internal SaaS records.
     *
     * @param request the onboarding request
     * @return 201 Created with the registration response
     */
    @PostMapping
    public ResponseEntity<OnboardingResponse> onboard(@Valid @RequestBody OnboardingRequest request) {
        OnboardingResponse response = onboardingService.onboard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
