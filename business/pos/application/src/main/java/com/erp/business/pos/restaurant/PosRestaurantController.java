package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/restaurant")
@RequiredArgsConstructor
public class PosRestaurantController {

    private final PosTableService posTableService;
    private final KitchenOrderService kitchenOrderService;
    private final BusinessContextAccessor context;

    @GetMapping("/tables")
    public ResponseEntity<List<PosTable>> listTables() {
        return ResponseEntity.ok(posTableService.listTables(context.getTenantId(), context.getCompanyId()));
    }

    @PostMapping("/tables/{tableId}/occupy")
    public ResponseEntity<PosTable> occupy(@PathVariable Long tableId) {
        return ResponseEntity.ok(posTableService.occupy(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{tableId}/free")
    public ResponseEntity<Void> free(@PathVariable Long tableId) {
        posTableService.free(context.getTenantId(), context.getCompanyId(), tableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tables/{tableId}/ordered")
    public ResponseEntity<PosTable> markOrdered(@PathVariable Long tableId) {
        return ResponseEntity.ok(posTableService.markOrdered(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{tableId}/cleaning")
    public ResponseEntity<PosTable> markCleaning(@PathVariable Long tableId) {
        return ResponseEntity.ok(posTableService.markCleaning(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{tableId}/sessions")
    public ResponseEntity<TableSession> openSession(@PathVariable Long tableId) {
        return ResponseEntity.ok(posTableService.openSession(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{tableId}/kitchen-orders")
    public ResponseEntity<KitchenOrder> fireKitchenOrder(@PathVariable Long tableId,
                                                         @RequestParam(required = false) String waiter,
                                                         @RequestBody List<KitchenOrderLine> lines) {
        return ResponseEntity.ok(kitchenOrderService.fire(context.getTenantId(), context.getCompanyId(), tableId, null, waiter, lines));
    }

    @GetMapping("/kitchen-orders")
    public ResponseEntity<List<KitchenOrder>> listKitchenOrders() {
        return ResponseEntity.ok(kitchenOrderService.listAll(context.getTenantId(), context.getCompanyId()));
    }

    @PostMapping("/kitchen-orders")
    public ResponseEntity<KitchenOrder> fireGeneralKitchenOrder(@RequestParam(required = false) Long tableId,
                                                               @RequestParam(required = false) String waiter,
                                                               @RequestBody List<KitchenOrderLine> lines) {
        return ResponseEntity.ok(kitchenOrderService.fire(context.getTenantId(), context.getCompanyId(), tableId, null, waiter, lines));
    }

    @GetMapping("/kitchen-orders/{orderId}/lines")
    public ResponseEntity<List<KitchenOrderLine>> listKitchenOrderLines(@PathVariable Long orderId) {
        return ResponseEntity.ok(kitchenOrderService.listLines(context.getTenantId(), context.getCompanyId(), orderId));
    }

    @PatchMapping("/kitchen-orders/{orderId}/status")
    public ResponseEntity<KitchenOrder> updateKitchenOrderStatus(@PathVariable Long orderId, @RequestParam KitchenOrder.Status status) {
        return ResponseEntity.ok(kitchenOrderService.updateStatus(context.getTenantId(), context.getCompanyId(), orderId, status));
    }

    @GetMapping("/tables/{tableId}/kitchen-orders")
    public ResponseEntity<List<KitchenOrder>> listKitchenOrders(@PathVariable Long tableId) {
        return ResponseEntity.ok(kitchenOrderService.listByTable(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{fromTableId}/merge/{intoTableId}")
    public ResponseEntity<Void> mergeTables(@PathVariable Long fromTableId, @PathVariable Long intoTableId) {
        posTableService.free(context.getTenantId(), context.getCompanyId(), fromTableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tables/{tableId}/split")
    public ResponseEntity<Void> splitTable(@PathVariable Long tableId) {
        return ResponseEntity.noContent().build();
    }
}
