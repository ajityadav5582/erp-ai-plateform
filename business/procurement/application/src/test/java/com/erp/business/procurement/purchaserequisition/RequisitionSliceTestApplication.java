package com.erp.business.procurement.purchaserequisition;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-only Spring Boot configuration for requisition repository (JPA slice) tests.
 *
 * <p>It lives in this package on purpose. {@code @DataJpaTest} discovers the
 * nearest {@code @SpringBootConfiguration} at or above the test class, so this
 * one is picked instead of the real {@code ProcurementApplication}. That matters
 * because the real application component-scans {@code com.erp.business.procurement.config},
 * whose {@code SecurityConfig} ({@code @EnableWebSecurity}) needs
 * {@code PlatformJwtAuthenticationFilter} and an {@code HttpSecurity} bean -
 * neither of which exists off the web, so the full context cannot start in a
 * non-web slice. Scanning only the requisition persistence package here keeps
 * the security (and service) wiring out of the context entirely.
 *
 * <p>Entities and repositories are named explicitly rather than relying on the
 * application's package scan, so the slice is self-contained and does not depend
 * on where this class happens to sit.
 */
@SpringBootApplication(
        scanBasePackages = "com.erp.business.procurement.purchaserequisition.infrastructure.persistence",
        exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
@EntityScan(basePackages = "com.erp.business.procurement.purchaserequisition.domain")
@EnableJpaRepositories(basePackages = "com.erp.business.procurement.purchaserequisition.infrastructure.persistence")
public class RequisitionSliceTestApplication {
}
