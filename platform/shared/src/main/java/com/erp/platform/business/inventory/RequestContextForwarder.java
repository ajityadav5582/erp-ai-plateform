package com.erp.platform.business.inventory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captures the current request's forwarding headers (Authorization, company,
 * fiscal-year) into {@link InventoryRequestContext} so async workers can forward
 * them to Inventory. Shared across all business modules.
 */
public final class RequestContextForwarder {

    private RequestContextForwarder() {
    }

    public static Map<String, String> capture() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return headers;
        }
        HttpServletRequest request = attrs.getRequest();
        copy(request, headers, "Authorization");
        copy(request, headers, "X-Company-Id");
        copy(request, headers, "X-Fiscal-Year-Id");
        return headers;
    }

    public static void captureIntoContext() {
        InventoryRequestContext.set(capture());
    }

    private static void copy(HttpServletRequest request, Map<String, String> headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.put(name, value);
        }
    }
}
