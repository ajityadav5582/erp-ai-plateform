package com.erp.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Main application class for the API Gateway Service.
 *
 * <p>This service provides a unified API entry point for the ERP AI Platform.
 * It routes requests to downstream services (identity, inventory, tenant, etc.)
 * and exposes a single port to the frontend. Uses Spring Cloud Gateway for
 * reactive proxying, request/response transformation, and cross-cutting
 * concerns (CORS, rate limiting, tenant/header propagation).
 *
 * @since 1.0.0
 */
@SpringBootApplication
@Import({
        com.erp.platform.gateway.config.GatewayConfigurationProperties.class,
        com.erp.platform.gateway.infrastructure.propagation.RequestContextPropagator.class
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
