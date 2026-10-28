package com.erp.business.pos.kitchen;

import com.erp.business.pos.kitchen.dto.PosKitchenTicketResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/kitchen-tickets")
@RequiredArgsConstructor
public class PosKitchenTicketController {

    private final PosKitchenTicketService service;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PosKitchenTicketResponse> create(@RequestParam Long posTransactionId) {
        return ResponseEntity.ok(service.createTicket(
                context.getTenantId(), context.getCompanyId(), posTransactionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosKitchenTicketResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTicketById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping("/by-transaction/{posTransactionId}")
    public ResponseEntity<List<PosKitchenTicketResponse>> byTransaction(@PathVariable Long posTransactionId) {
        return ResponseEntity.ok(service.listByTransaction(
                context.getTenantId(), context.getCompanyId(), posTransactionId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<PosKitchenTicketResponse>> byStatus(@RequestParam String status) {
        return ResponseEntity.ok(service.listByStatus(
                context.getTenantId(), context.getCompanyId(), status));
    }

    @PostMapping("/{id}/preparing")
    public ResponseEntity<PosKitchenTicketResponse> preparing(@PathVariable Long id) {
        return ResponseEntity.ok(service.markPreparing(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<PosKitchenTicketResponse> ready(@PathVariable Long id) {
        return ResponseEntity.ok(service.markReady(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/served")
    public ResponseEntity<PosKitchenTicketResponse> served(@PathVariable Long id) {
        return ResponseEntity.ok(service.markServed(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PosKitchenTicketResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
