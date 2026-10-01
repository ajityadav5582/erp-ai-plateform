package com.erp.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the API Gateway's global CORS configuration binds correctly
 * from {@code application.yml} / {@code application-local.yml}.
 *
 * <p>Spring Cloud Gateway 4.x binds global CORS through
 * {@link GlobalCorsProperties#getCorsConfigurations()} (a
 * {@code Map<String, CorsConfiguration>} keyed by path pattern). The map key
 * must use bracket notation (e.g. {@code '[/**]'}) because Spring's relaxed
 * binding treats {@code /} as a path separator.
 */
@org.springframework.boot.test.context.SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=optional:classpath:application.yml",
        "spring.profiles.active=local"
})
class GatewayCorsConfigurationTest {

    @Autowired
    private GlobalCorsProperties globalCorsProperties;

    @Test
    void globalCorsConfigurationIsBoundForAllPaths() {
        CorsConfiguration cors = globalCorsProperties.getCorsConfigurations().get("/**");
        assertThat(cors).as("global CORS config for /** must be bound").isNotNull();

        assertThat(cors.getAllowedOriginPatterns())
                .as("allowed-origin-patterns must include the frontend origin")
                .contains("http://localhost:3000", "http://127.0.0.1:3000");

        assertThat(cors.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        assertThat(cors.getAllowedHeaders()).containsExactly("*");

        assertThat(cors.getExposedHeaders())
                .contains("Authorization", "Location", "X-Tenant-ID",
                        "X-Correlation-Id", "X-Request-Id");

        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }
}
