package com.erp.business.sales.delivery;

import com.erp.business.sales.delivery.domain.DeliveryStatus;
import com.erp.business.sales.delivery.dto.CreateDeliveryRequest;
import com.erp.business.sales.delivery.dto.DeliveryDecisionRequest;
import com.erp.business.sales.delivery.dto.SalesDeliveryResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sales delivery endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/deliveries")
@RequiredArgsConstructor
public class SalesDeliveryController {

    private static final String BASE_PATH = "/api/v1/sales/deliveries";

    private final SalesDeliveryService salesDeliveryService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SalesDeliveryResponse> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request,
            UriComponentsBuilder uriBuilder) {

        SalesDeliveryResponse response = salesDeliveryService.createDelivery(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SalesDeliveryResponse>> listDeliveries(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesDeliveryService.listDeliveries(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SalesDeliveryResponse>> searchDeliveries(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesDeliveryService.searchDeliveries(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<SalesDeliveryResponse>> listDeliveriesByStatus(
            @RequestParam("status") DeliveryStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesDeliveryService.listDeliveriesByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-order")
    public ResponseEntity<Page<SalesDeliveryResponse>> listDeliveriesByOrder(
            @RequestParam("salesOrderId") Long salesOrderId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesDeliveryService.listDeliveriesByOrder(
                context.getTenantId(), context.getCompanyId(), salesOrderId, pageable));
    }

    @GetMapping("/by-number")
    public ResponseEntity<SalesDeliveryResponse> getDeliveryByNumber(
            @RequestParam("deliveryNumber") String deliveryNumber) {
        return ResponseEntity.ok(salesDeliveryService.getDeliveryByNumber(
                context.getTenantId(), context.getCompanyId(), deliveryNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesDeliveryResponse> getDeliveryById(@PathVariable Long id) {
        return ResponseEntity.ok(salesDeliveryService.getDeliveryById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<SalesDeliveryResponse> postDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(salesDeliveryService.postDelivery(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesDeliveryResponse> cancelDelivery(
            @PathVariable Long id, @Valid @RequestBody DeliveryDecisionRequest request) {
        return ResponseEntity.ok(salesDeliveryService.cancelDelivery(
                context.getTenantId(), context.getCompanyId(), id, request));
    }
}
