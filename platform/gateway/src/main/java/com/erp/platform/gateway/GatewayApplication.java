package com.erp.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Main application class for the API Gateway Service.
 *
 * <p>This service provides a unified API entry point for the ERP AI Platform.
 * It routes requests to downstream services (identity, inventory, procurement,
 * etc.) and exposes a single port to the frontend. Uses Spring Cloud Gateway for
 * reactive proxying, request/response transformation, and cross-cutting concerns
 * (CORS, rate limiting, tenant/header propagation).
 *
 * <p><strong>CORS is defined here and nowhere else.</strong> It is declared once,
 * under {@code spring.cloud.gateway.global-cors}, and every downstream service
 * disables CORS in its own Spring Security so that only one hop ever emits
 * {@code Access-Control-Allow-*}. Two hops emitting the header is how a browser
 * ends up refusing a response the server considers a success.
 *
 * <p>There is deliberately no {@code @ConfigurationProperties} class for the
 * route list. One existed, bound {@code erp.gateway.routes}, and nothing read it
 * - which meant the deployed gateway could serve a route list that had silently
 * diverged from the one written in the config file, with no symptom other than
 * requests arriving as an unexplained 403 that the browser reported as a CORS
 * failure. {@code GatewayRouteCoverageTest} now derives the required routes from
 * {@code docker-compose.yml} instead, so there is no second list to keep in sync
 * and a missing route fails the build rather than a browser session.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@Import({
        com.erp.platform.gateway.infrastructure.propagation.RequestContextPropagator.class
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
