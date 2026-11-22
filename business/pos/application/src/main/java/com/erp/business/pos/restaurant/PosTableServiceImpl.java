package com.erp.business.pos.restaurant;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.business.pos.restaurant.dto.CreatePosTableRequest;
import com.erp.business.pos.restaurant.dto.TableBillSummaryResponse;
import com.erp.business.pos.restaurant.dto.UpdatePosTableRequest;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderLineRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.PosTableRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class PosTableServiceImpl implements PosTableService {

    private final PosTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final KitchenOrderLineRepository lineRepository;
    private final ItemService itemService;

    @Override
    @Transactional(readOnly = true)
    public List<PosTable> listTables(Long tenantId, Long companyId) {
        return tableRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Override
    public PosTable occupy(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        if (table.getStatus() != PosTable.Status.FREE) {
            throw new IllegalStateException("Table " + table.getTableNumber() + " is not free");
        }
        TableSession session = TableSession.builder().tableId(tableId).build();
        session.assignScope(tenantId, companyId);
        TableSession saved = sessionRepository.save(session);
        table.occupy(saved.getId());
        return tableRepository.save(table);
    }

    @Override
    public void free(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        table.free();
        tableRepository.save(table);
    }

    @Override
    public TableSession openSession(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        TableSession session = TableSession.builder().tableId(tableId).build();
        session.assignScope(tenantId, companyId);
        TableSession saved = sessionRepository.save(session);
        table.occupy(saved.getId());
        tableRepository.save(table);
        return saved;
    }

    @Override
    public PosTable markOrdered(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        table.markOrdered();
        return tableRepository.save(table);
    }

    @Override
    public PosTable markCleaning(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        table.markCleaning();
        return tableRepository.save(table);
    }

    @Override
    public PosTable createTable(Long tenantId, Long companyId, CreatePosTableRequest request) {
        String trimmedNumber = request.tableNumber().trim().toUpperCase();
        if (tableRepository.existsByTenantIdAndCompanyIdAndTableNumber(tenantId, companyId, trimmedNumber)) {
            throw new IllegalArgumentException("Table number already exists: " + trimmedNumber);
        }

        PosTable table = PosTable.builder()
                .tableNumber(trimmedNumber)
                .capacity(request.capacity())
                .section(request.section() != null && !request.section().isBlank() ? request.section().trim() : "Main Floor")
                .status(PosTable.Status.FREE)
                .build();
        table.assignScope(tenantId, companyId);
        return tableRepository.save(table);
    }

    @Override
    public PosTable updateTable(Long tenantId, Long companyId, Long tableId, UpdatePosTableRequest request) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);

        if (request.tableNumber() != null && !request.tableNumber().isBlank()) {
            String trimmedNumber = request.tableNumber().trim().toUpperCase();
            if (tableRepository.existsByTenantIdAndCompanyIdAndTableNumberAndIdNot(tenantId, companyId, trimmedNumber, tableId)) {
                throw new IllegalArgumentException("Table number already exists: " + trimmedNumber);
            }
        }

        table.update(request.tableNumber(), request.capacity(), request.section());
        return tableRepository.save(table);
    }

    @Override
    public void deleteTable(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);
        if (table.getStatus() != PosTable.Status.FREE) {
            throw new IllegalStateException("Cannot delete table " + table.getTableNumber() + " because it is currently " + table.getStatus());
        }
        tableRepository.delete(table);
    }

    @Override
    public List<PosTable> bootstrapDefaultTables(Long tenantId, Long companyId) {
        long count = tableRepository.countByTenantIdAndCompanyId(tenantId, companyId);
        if (count > 0) {
            return listTables(tenantId, companyId);
        }

        List<PosTable> defaults = List.of(
                // Main Hall Tables
                createTableEntity(tenantId, companyId, "T-1", 2, "Main Floor"),
                createTableEntity(tenantId, companyId, "T-2", 4, "Main Floor"),
                createTableEntity(tenantId, companyId, "T-3", 4, "Main Floor"),
                createTableEntity(tenantId, companyId, "T-4", 4, "Main Floor"),
                createTableEntity(tenantId, companyId, "T-5", 6, "Main Floor"),
                createTableEntity(tenantId, companyId, "T-6", 6, "Main Floor"),
                // Balcony / Rooftop / Garden
                createTableEntity(tenantId, companyId, "G-1", 4, "Garden & Balcony"),
                createTableEntity(tenantId, companyId, "G-2", 4, "Garden & Balcony"),
                createTableEntity(tenantId, companyId, "G-3", 6, "Garden & Balcony"),
                // VIP Cabin
                createTableEntity(tenantId, companyId, "VIP-1", 8, "VIP Cabin")
        );

        return tableRepository.saveAll(defaults);
    }

    private PosTable createTableEntity(Long tenantId, Long companyId, String number, int capacity, String section) {
        PosTable table = PosTable.builder()
                .tableNumber(number)
                .capacity(capacity)
                .section(section)
                .status(PosTable.Status.FREE)
                .build();
        table.assignScope(tenantId, companyId);
        return table;
    }

    @Override
    @Transactional(readOnly = true)
    public TableBillSummaryResponse getTableCurrentBill(Long tenantId, Long companyId, Long tableId) {
        PosTable table = findTableOrThrow(tenantId, companyId, tableId);

        List<KitchenOrder> activeOrders = kitchenOrderRepository
                .findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                        tenantId, companyId, tableId, KitchenOrder.Status.CANCELLED);

        if (activeOrders.isEmpty()) {
            return new TableBillSummaryResponse(
                    table.getId(),
                    table.getTableNumber(),
                    table.getSection(),
                    table.getCurrentSessionId(),
                    null,
                    null,
                    0,
                    List.of(),
                    BigDecimal.ZERO
            );
        }

        String waiter = activeOrders.stream()
                .map(KitchenOrder::getWaiter)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Instant openedAt = activeOrders.stream()
                .map(KitchenOrder::getFiredAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);

        // Aggregate order lines by itemId + modifiers + modifierPrice
        Map<String, AccumulatedLine> grouped = new LinkedHashMap<>();

        for (KitchenOrder order : activeOrders) {
            List<KitchenOrderLine> lines = lineRepository
                    .findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, order.getId());

            for (KitchenOrderLine line : lines) {
                String groupKey = line.getItemId() + "|" +
                        (line.getModifiers() != null ? line.getModifiers().trim() : "") + "|" +
                        (line.getModifierPrice() != null ? line.getModifierPrice() : BigDecimal.ZERO);

                grouped.compute(groupKey, (key, acc) -> {
                    if (acc == null) {
                        return new AccumulatedLine(
                                line.getItemId(),
                                line.getItemCode(),
                                line.getItemName(),
                                line.getQuantity(),
                                line.getNotes() != null ? new LinkedHashSet<>(List.of(line.getNotes())) : new LinkedHashSet<>(),
                                line.getStation(),
                                line.getModifiers(),
                                line.getModifierPrice()
                        );
                    } else {
                        acc.quantity = acc.quantity.add(line.getQuantity());
                        if (line.getNotes() != null && !line.getNotes().isBlank()) {
                            acc.notes.add(line.getNotes());
                        }
                        return acc;
                    }
                });
            }
        }

        List<TableBillSummaryResponse.BillLineItem> billLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, ItemService.ItemResponse> itemCache = new HashMap<>();

        for (AccumulatedLine acc : grouped.values()) {
            ItemService.ItemResponse item = itemCache.computeIfAbsent(acc.itemId, id -> itemService.findById(id).orElse(null));
            BigDecimal basePrice = item != null && item.getSellingPrice() != null
                    ? item.getSellingPrice()
                    : BigDecimal.ZERO;
            BigDecimal unitPrice = basePrice.add(acc.modifierPrice).setScale(2, RoundingMode.HALF_UP);
            String uomCode = item != null && item.getUnitCode() != null ? item.getUnitCode() : "PCS";

            BigDecimal lineAmount = acc.quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineAmount);

            String aggregatedNotes = String.join("; ", acc.notes);

            billLines.add(new TableBillSummaryResponse.BillLineItem(
                    acc.itemId,
                    acc.itemCode,
                    acc.itemName,
                    uomCode,
                    acc.quantity,
                    unitPrice,
                    lineAmount,
                    aggregatedNotes.isEmpty() ? null : aggregatedNotes,
                    acc.station,
                    acc.modifiers,
                    acc.modifierPrice
            ));
        }

        BigDecimal scaledSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal scPercent = new BigDecimal("10.00");
        BigDecimal scAmount = scaledSubtotal.multiply(scPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal taxableBase = scaledSubtotal.add(scAmount);
        BigDecimal vatRate = new BigDecimal("13.00");
        BigDecimal vatAmount = taxableBase.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableBase.add(vatAmount);

        return new TableBillSummaryResponse(
                table.getId(),
                table.getTableNumber(),
                table.getSection(),
                table.getCurrentSessionId(),
                waiter,
                openedAt,
                activeOrders.size(),
                billLines,
                scaledSubtotal,
                scPercent,
                scAmount,
                vatRate,
                vatAmount,
                totalAmount
        );
    }

    private static class AccumulatedLine {
        final Long itemId;
        final String itemCode;
        final String itemName;
        BigDecimal quantity;
        final Set<String> notes;
        final String station;
        final String modifiers;
        final BigDecimal modifierPrice;

        AccumulatedLine(Long itemId, String itemCode, String itemName, BigDecimal quantity, Set<String> notes,
                        String station, String modifiers, BigDecimal modifierPrice) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.quantity = quantity;
            this.notes = notes;
            this.station = station != null ? station : "KITCHEN";
            this.modifiers = modifiers;
            this.modifierPrice = modifierPrice != null ? modifierPrice : BigDecimal.ZERO;
        }
    }

    @Override
    public void transferTable(Long tenantId, Long companyId, Long fromTableId, Long toTableId) {
        if (fromTableId.equals(toTableId)) {
            throw new IllegalArgumentException("Source and destination table cannot be the same");
        }

        PosTable fromTable = findTableOrThrow(tenantId, companyId, fromTableId);
        PosTable toTable = findTableOrThrow(tenantId, companyId, toTableId);

        if (fromTable.getStatus() == PosTable.Status.FREE) {
            throw new IllegalStateException("Cannot transfer from free table " + fromTable.getTableNumber());
        }
        if (toTable.getStatus() != PosTable.Status.FREE) {
            throw new IllegalStateException("Target table " + toTable.getTableNumber() + " is currently " + toTable.getStatus() + ". Only free tables can receive transfers.");
        }

        Long sessionId = fromTable.getCurrentSessionId();
        toTable.occupy(sessionId);
        toTable.setStatus(fromTable.getStatus());
        fromTable.free();

        tableRepository.save(fromTable);
        tableRepository.save(toTable);

        // Move active unbilled kitchen orders
        List<KitchenOrder> orders = kitchenOrderRepository
                .findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                        tenantId, companyId, fromTableId, KitchenOrder.Status.CANCELLED);

        for (KitchenOrder order : orders) {
            order.transferToTable(toTableId, sessionId);
            kitchenOrderRepository.save(order);
        }
    }

    @Override
    public void mergeTables(Long tenantId, Long companyId, Long fromTableId, Long intoTableId) {
        if (fromTableId.equals(intoTableId)) {
            throw new IllegalArgumentException("Cannot merge a table into itself");
        }

        PosTable fromTable = findTableOrThrow(tenantId, companyId, fromTableId);
        PosTable intoTable = findTableOrThrow(tenantId, companyId, intoTableId);

        if (fromTable.getStatus() == PosTable.Status.FREE) {
            throw new IllegalStateException("Source table " + fromTable.getTableNumber() + " has no active order to merge");
        }
        if (intoTable.getStatus() == PosTable.Status.FREE) {
            throw new IllegalStateException("Target table " + intoTable.getTableNumber() + " is free. Use transfer instead of merge.");
        }

        Long intoSessionId = intoTable.getCurrentSessionId();
        if (intoSessionId == null) {
            TableSession newSession = TableSession.builder().tableId(intoTableId).build();
            newSession.assignScope(tenantId, companyId);
            intoSessionId = sessionRepository.save(newSession).getId();
            intoTable.occupy(intoSessionId);
        }

        Long fromSessionId = fromTable.getCurrentSessionId();

        // Reassign all active orders to the target table and session
        List<KitchenOrder> orders = kitchenOrderRepository
                .findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                        tenantId, companyId, fromTableId, KitchenOrder.Status.CANCELLED);

        for (KitchenOrder order : orders) {
            order.transferToTable(intoTableId, intoSessionId);
            kitchenOrderRepository.save(order);
        }

        if (fromSessionId != null) {
            final Long targetSession = intoSessionId;
            sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, fromSessionId)
                    .ifPresent(s -> {
                        s.mergeInto(targetSession);
                        sessionRepository.save(s);
                    });
        }

        fromTable.free();
        intoTable.markOrdered();

        tableRepository.save(fromTable);
        tableRepository.save(intoTable);
    }

    @Override
    public void closeTableSessionOnCheckout(Long tenantId, Long companyId, Long tableId, Long posTransactionId) {
        if (tableId == null) return;

        tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId).ifPresent(table -> {
            // 1. Link and complete all active kitchen orders
            List<KitchenOrder> orders = kitchenOrderRepository
                    .findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                            tenantId, companyId, tableId, KitchenOrder.Status.CANCELLED);

            for (KitchenOrder order : orders) {
                order.linkTransaction(posTransactionId);
                kitchenOrderRepository.save(order);
            }

            // 2. Close table session
            if (table.getCurrentSessionId() != null) {
                sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, table.getCurrentSessionId())
                        .ifPresent(s -> {
                            s.bill(posTransactionId);
                            s.close();
                            sessionRepository.save(s);
                        });
            }

            // 3. Set table to CLEANING status so staff knows to wipe the table before new seating
            table.markCleaning();
            tableRepository.save(table);
        });
    }

    @Override
    public void closeTakeawayOrderOnCheckout(Long tenantId, Long companyId, String tokenNumber, Long posTransactionId) {
        if (tokenNumber == null || tokenNumber.isBlank()) return;
        List<KitchenOrder> orders = kitchenOrderRepository
                .findByTenantIdAndCompanyIdAndTokenNumberAndPosTransactionIdIsNullAndStatusNot(
                        tenantId, companyId, tokenNumber.trim(), KitchenOrder.Status.CANCELLED);
        for (KitchenOrder order : orders) {
            order.linkTransaction(posTransactionId);
            kitchenOrderRepository.save(order);
        }
    }

    private PosTable findTableOrThrow(Long tenantId, Long companyId, Long tableId) {
        return tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found with ID: " + tableId));
    }
}
