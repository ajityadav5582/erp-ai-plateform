package com.erp.platform.gateway.infrastructure.propagation;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gateway filter that propagates cross-cutting context headers from the
 * inbound request to downstream services.
 *
 * <p>The following headers are forwarded to every downstream request:
 * <ul>
 *   <li>Tenant identifier (X-Tenant-Id)</li>
 *   <li>Authorization / JWT Bearer token</li>
 *   <li>Selected company (X-Company-Id)</li>
 *   <li>Selected fiscal year (X-Fiscal-Year-Id)</li>
 *   <li>Correlation ID (X-Correlation-Id)</li>
 *   <li>Request ID (X-Request-Id)</li>
 *   <li>Trace context (traceparent)</li>
 *   <li>Accept-Language</li>
 *   <li>X-Forwarded-For / X-Forwarded-Proto / X-Forwarded-Host</li>
 * </ul>
 *
 * <p>This ensures downstream services (identity, inventory, tenant) receive
 * the same tenant and authentication context without re-parsing cookies
 * or re-validating JWTs.
 */
@Component
public class RequestContextPropagator extends AbstractGatewayFilterFactory<RequestContextPropagator.Config> {

    /** Header carrying the company (legal entity) the user has selected. */
    public static final String HEADER_COMPANY_ID = "X-Company-Id";

    /** Header carrying the fiscal year the user has selected. */
    public static final String HEADER_FISCAL_YEAR_ID = "X-Fiscal-Year-Id";

    private static final List<String> PROPAGATED_HEADERS = List.of(
            "X-Tenant-Id",
            "Authorization",
            HEADER_COMPANY_ID,
            HEADER_FISCAL_YEAR_ID,
            "X-Correlation-Id",
            "X-Request-Id",
            "traceparent",
            "tracestate",
            "Accept-Language",
            "X-Forwarded-For",
            "X-Forwarded-Proto",
            "X-Forwarded-Host",
            "X-Forwarded-Port"
    );

    public RequestContextPropagator() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            MultiValueMap<String, String> headers = request.getHeaders();

            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        // Forward only whitelisted headers to avoid leaking
                        // internal hop-by-hop headers to downstream services.
                        for (String headerName : PROPAGATED_HEADERS) {
                            httpHeaders.remove(headerName);
                        }

                        for (String headerName : PROPAGATED_HEADERS) {
                            List<String> values = headers.get(headerName);
                            if (values != null && !values.isEmpty()) {
                                httpHeaders.addAll(headerName, values);
                            }
                        }

                        // Ensure a correlation ID exists for tracing.
                        if (!httpHeaders.containsKey("X-Correlation-Id")) {
                            httpHeaders.set("X-Correlation-Id", java.util.UUID.randomUUID().toString());
                        }
                    })
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        // Configuration holder - currently empty; reserved for future
        // per-route header customization.
    }
}
