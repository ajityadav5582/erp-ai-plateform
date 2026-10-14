package com.erp.business.pos.config;

import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Secondary adapter that satisfies the stock-movement port over HTTP by calling
 * Inventory's stock-movement endpoint.
 *
 * <p>The caller's bearer token and company header are forwarded, so Inventory
 * authorises and scopes the post exactly as it would a direct call - the tenant
 * comes from the token, the company from the header. This is the microservice
 * wiring of the port; a modular-monolith deployment can replace it with an
 * in-process adapter against the same interface.
 *
 * <p>Checkout posts an issue (goods leaving for a customer) and a void posts the
 * matching issue-reversal. Both share the {@code (referenceType, referenceId)}
 * idempotency key, so a retried checkout after a timeout is recognised by
 * Inventory instead of deducting stock twice.
 */
class HttpStockMovementService implements StockMovementService {

    private static final String RECEIPTS_PATH = "/api/v1/inventory/stock-movements/receipts";
    private static final String REVERSALS_PATH = "/api/v1/inventory/stock-movements/reversals";
    private static final String ISSUES_PATH = "/api/v1/inventory/stock-movements/issues";
    private static final String ISSUE_REVERSALS_PATH = "/api/v1/inventory/stock-movements/issue-reversals";

    private final RestClient restClient;

    HttpStockMovementService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public StockMovementBatchResponse postReceipt(StockReceiptRequest request) {
        return post(RECEIPTS_PATH, request);
    }

    @Override
    public StockMovementBatchResponse reverseReceipt(StockReceiptRequest request) {
        return post(REVERSALS_PATH, request);
    }

    @Override
    public StockMovementBatchResponse postIssue(StockReceiptRequest request) {
        return post(ISSUES_PATH, request);
    }

    @Override
    public StockMovementBatchResponse reverseIssue(StockReceiptRequest request) {
        return post(ISSUE_REVERSALS_PATH, request);
    }

    private StockMovementBatchResponse post(String path, StockReceiptRequest request) {
        return restClient.post()
                .uri(path)
                .headers(this::forwardRequestContext)
                .body(request)
                .retrieve()
                .body(StockMovementBatchResponse.class);
    }

    /**
     * Copies the incoming request's auth and scope headers onto the outbound
     * call. Both must travel with it: Inventory reads the tenant from the token
     * and the company from the header, and a post without them is refused.
     */
    private void forwardRequestContext(HttpHeaders headers) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        copyHeader(request, headers, "Authorization");
        copyHeader(request, headers, "X-Company-Id");
        copyHeader(request, headers, "X-Fiscal-Year-Id");
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }
}
