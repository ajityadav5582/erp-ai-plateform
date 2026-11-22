package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.business.pos.restaurant.dto.CreatePosTableRequest;
import com.erp.business.pos.restaurant.dto.KitchenTicketResponse;
import com.erp.business.pos.restaurant.dto.RestaurantModifierPresetDto;
import com.erp.business.pos.restaurant.dto.TableBillSummaryResponse;
import com.erp.business.pos.restaurant.dto.UpdatePosTableRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping({"/tables", "/tables/"})
    public ResponseEntity<List<PosTable>> listTables() {
        return ResponseEntity.ok(posTableService.listTables(context.getTenantId(), context.getCompanyId()));
    }

    @PostMapping({"/tables", "/tables/"})
    public ResponseEntity<PosTable> createTable(@Valid @RequestBody CreatePosTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(posTableService.createTable(context.getTenantId(), context.getCompanyId(), request));
    }

    @PutMapping("/tables/{tableId}")
    public ResponseEntity<PosTable> updateTable(@PathVariable Long tableId,
                                                @Valid @RequestBody UpdatePosTableRequest request) {
        return ResponseEntity.ok(posTableService.updateTable(context.getTenantId(), context.getCompanyId(), tableId, request));
    }

    @DeleteMapping("/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long tableId) {
        posTableService.deleteTable(context.getTenantId(), context.getCompanyId(), tableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/tables/bootstrap", "/tables/bootstrap/"})
    public ResponseEntity<List<PosTable>> bootstrapDefaultTables() {
        return ResponseEntity.ok(posTableService.bootstrapDefaultTables(context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/tables/{tableId}/current-bill")
    public ResponseEntity<TableBillSummaryResponse> getTableCurrentBill(@PathVariable Long tableId) {
        return ResponseEntity.ok(posTableService.getTableCurrentBill(context.getTenantId(), context.getCompanyId(), tableId));
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

    @GetMapping({"/kitchen-orders", "/kitchen-orders/"})
    public ResponseEntity<List<KitchenOrder>> listKitchenOrders(@RequestParam(required = false) String station) {
        return ResponseEntity.ok(kitchenOrderService.listAll(context.getTenantId(), context.getCompanyId(), station));
    }

    @PostMapping({"/kitchen-orders", "/kitchen-orders/"})
    public ResponseEntity<KitchenOrder> fireGeneralKitchenOrder(@RequestParam(required = false) Long tableId,
                                                               @RequestParam(required = false) String waiter,
                                                               @RequestParam(required = false) String orderType,
                                                               @RequestParam(required = false) String tokenNumber,
                                                               @RequestParam(required = false) String deliveryPartner,
                                                               @RequestParam(required = false) String customerPhone,
                                                               @RequestBody List<KitchenOrderLine> lines) {
        return ResponseEntity.ok(kitchenOrderService.fire(
                context.getTenantId(), context.getCompanyId(), tableId, null, waiter,
                orderType, tokenNumber, deliveryPartner, customerPhone, lines));
    }

    @GetMapping("/kitchen-orders/{orderId}/lines")
    public ResponseEntity<List<KitchenOrderLine>> listKitchenOrderLines(@PathVariable Long orderId,
                                                                       @RequestParam(required = false) String station) {
        return ResponseEntity.ok(kitchenOrderService.listLines(context.getTenantId(), context.getCompanyId(), orderId, station));
    }

    @GetMapping("/kitchen-orders/{orderId}/ticket")
    public ResponseEntity<KitchenTicketResponse> getKitchenTicket(@PathVariable Long orderId) {
        return ResponseEntity.ok(kitchenOrderService.getKitchenTicket(context.getTenantId(), context.getCompanyId(), orderId));
    }

    @GetMapping({"/modifier-presets", "/modifier-presets/"})
    public ResponseEntity<List<RestaurantModifierPresetDto>> getModifierPresets() {
        return ResponseEntity.ok(kitchenOrderService.getModifierPresets(context.getTenantId(), context.getCompanyId()));
    }

    @PatchMapping("/kitchen-orders/{orderId}/status")
    public ResponseEntity<KitchenOrder> updateKitchenOrderStatus(@PathVariable Long orderId, @RequestParam KitchenOrder.Status status) {
        return ResponseEntity.ok(kitchenOrderService.updateStatus(context.getTenantId(), context.getCompanyId(), orderId, status));
    }

    @GetMapping("/tables/{tableId}/kitchen-orders")
    public ResponseEntity<List<KitchenOrder>> listKitchenOrders(@PathVariable Long tableId) {
        return ResponseEntity.ok(kitchenOrderService.listByTable(context.getTenantId(), context.getCompanyId(), tableId));
    }

    @PostMapping("/tables/{fromTableId}/transfer/{toTableId}")
    public ResponseEntity<Void> transferTable(@PathVariable Long fromTableId, @PathVariable Long toTableId) {
        posTableService.transferTable(context.getTenantId(), context.getCompanyId(), fromTableId, toTableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tables/{fromTableId}/merge/{intoTableId}")
    public ResponseEntity<Void> mergeTables(@PathVariable Long fromTableId, @PathVariable Long intoTableId) {
        posTableService.mergeTables(context.getTenantId(), context.getCompanyId(), fromTableId, intoTableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tables/{tableId}/split")
    public ResponseEntity<Void> splitTable(@PathVariable Long tableId) {
        return ResponseEntity.noContent().build();
    }
}
