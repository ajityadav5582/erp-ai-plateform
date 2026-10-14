package com.erp.business.sales.salesorder;

import com.erp.business.sales.salesorder.domain.SalesOrderStatus;
import com.erp.business.sales.salesorder.dto.CreateSalesOrderRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderDecisionRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import com.erp.business.sales.salesorder.dto.UpdateSalesOrderRequest;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Sales order endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private static final String BASE_PATH = "/api/v1/sales/sales-orders";

    private final SalesOrderService salesOrderService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SalesOrderResponse> createOrder(
            @Valid @RequestBody CreateSalesOrderRequest request,
            UriComponentsBuilder uriBuilder) {

        SalesOrderResponse response = salesOrderService.createOrder(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesOrderRequest request) {
        return ResponseEntity.ok(salesOrderService.updateOrder(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @GetMapping
    public ResponseEntity<Page<SalesOrderResponse>> listOrders(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesOrderService.listOrders(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SalesOrderResponse>> searchOrders(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesOrderService.searchOrders(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<SalesOrderResponse>> listOrdersByStatus(
            @RequestParam("status") SalesOrderStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesOrderService.listOrdersByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-customer")
    public ResponseEntity<Page<SalesOrderResponse>> listOrdersByCustomer(
            @RequestParam("customerId") Long customerId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesOrderService.listOrdersByCustomer(
                context.getTenantId(), context.getCompanyId(), customerId, pageable));
    }

    @GetMapping("/open")
    public ResponseEntity<List<SalesOrderResponse>> listOpenOrders() {
        return ResponseEntity.ok(salesOrderService.listOpenOrders(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-number")
    public ResponseEntity<SalesOrderResponse> getOrderByNumber(@RequestParam("soNumber") String soNumber) {
        return ResponseEntity.ok(salesOrderService.getOrderByNumber(
                context.getTenantId(), context.getCompanyId(), soNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.getOrderById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SalesOrderResponse> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.confirmOrder(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesOrderResponse> cancelOrder(
            @PathVariable Long id, @Valid @RequestBody SalesOrderDecisionRequest request) {
        return ResponseEntity.ok(salesOrderService.cancelOrder(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<SalesOrderResponse> closeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.closeOrder(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
