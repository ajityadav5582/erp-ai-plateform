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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosTableServiceImplTest {

    @Mock private PosTableRepository tableRepository;
    @Mock private TableSessionRepository sessionRepository;
    @Mock private KitchenOrderRepository kitchenOrderRepository;
    @Mock private KitchenOrderLineRepository lineRepository;
    @Mock private ItemService itemService;

    private PosTableServiceImpl service;

    private final Long tenantId = 1L;
    private final Long companyId = 100L;

    @BeforeEach
    void setUp() {
        service = new PosTableServiceImpl(tableRepository, sessionRepository, kitchenOrderRepository, lineRepository, itemService);
        when(tableRepository.save(any(PosTable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kitchenOrderRepository.save(any(KitchenOrder.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("createTable should normalize table number to upper case and save")
    void testCreateTableSuccess() {
        CreatePosTableRequest request = new CreatePosTableRequest(" t-10 ", 4, "Rooftop");
        when(tableRepository.existsByTenantIdAndCompanyIdAndTableNumber(tenantId, companyId, "T-10")).thenReturn(false);

        PosTable created = service.createTable(tenantId, companyId, request);

        assertNotNull(created);
        assertEquals("T-10", created.getTableNumber());
        assertEquals(4, created.getCapacity());
        assertEquals("Rooftop", created.getSection());
        assertEquals(PosTable.Status.FREE, created.getStatus());
        verify(tableRepository).save(any(PosTable.class));
    }

    @Test
    @DisplayName("createTable should throw exception when table number already exists")
    void testCreateTableDuplicateFails() {
        CreatePosTableRequest request = new CreatePosTableRequest("T-1", 4, "Main Floor");
        when(tableRepository.existsByTenantIdAndCompanyIdAndTableNumber(tenantId, companyId, "T-1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createTable(tenantId, companyId, request));
    }

    @Test
    @DisplayName("updateTable should update capacity and section")
    void testUpdateTableSuccess() {
        PosTable table = PosTable.builder().tableNumber("T-1").capacity(2).section("Main Floor").status(PosTable.Status.FREE).build();
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(table));

        UpdatePosTableRequest request = new UpdatePosTableRequest("T-1-NEW", 6, "Patio");
        when(tableRepository.existsByTenantIdAndCompanyIdAndTableNumberAndIdNot(tenantId, companyId, "T-1-NEW", 1L)).thenReturn(false);

        PosTable updated = service.updateTable(tenantId, companyId, 1L, request);

        assertEquals("T-1-NEW", updated.getTableNumber());
        assertEquals(6, updated.getCapacity());
        assertEquals("Patio", updated.getSection());
    }

    @Test
    @DisplayName("deleteTable should allow deleting free table but reject occupied table")
    void testDeleteTableLifecycle() {
        PosTable freeTable = PosTable.builder().tableNumber("T-1").capacity(2).status(PosTable.Status.FREE).build();
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(freeTable));

        assertDoesNotThrow(() -> service.deleteTable(tenantId, companyId, 1L));
        verify(tableRepository).delete(freeTable);

        PosTable occupiedTable = PosTable.builder().tableNumber("T-2").capacity(4).status(PosTable.Status.OCCUPIED).build();
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 2L)).thenReturn(Optional.of(occupiedTable));

        assertThrows(IllegalStateException.class, () -> service.deleteTable(tenantId, companyId, 2L));
    }

    @Test
    @DisplayName("bootstrapDefaultTables should seed 10 default tables when table count is zero")
    void testBootstrapDefaultTables() {
        when(tableRepository.countByTenantIdAndCompanyId(tenantId, companyId)).thenReturn(0L);
        when(tableRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<PosTable> tables = service.bootstrapDefaultTables(tenantId, companyId);

        assertEquals(10, tables.size());
        assertTrue(tables.stream().anyMatch(t -> t.getTableNumber().equals("T-1")));
        assertTrue(tables.stream().anyMatch(t -> t.getTableNumber().equals("G-1")));
        assertTrue(tables.stream().anyMatch(t -> t.getTableNumber().equals("VIP-1")));
    }

    @Test
    @DisplayName("occupy should start table session and change status to OCCUPIED")
    void testOccupyTable() {
        PosTable table = PosTable.builder().tableNumber("T-1").capacity(4).status(PosTable.Status.FREE).build();
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(table));
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(inv -> {
            TableSession s = inv.getArgument(0);
            return TableSession.builder().tableId(s.getTableId()).status(TableSession.Status.OPEN).build();
        });

        PosTable occupied = service.occupy(tenantId, companyId, 1L);

        assertEquals(PosTable.Status.OCCUPIED, occupied.getStatus());
        verify(sessionRepository).save(any(TableSession.class));
    }

    @Test
    @DisplayName("getTableCurrentBill should aggregate multiple KOT lines and compute subtotal")
    void testGetTableCurrentBill() {
        PosTable table = PosTable.builder()
                .tableNumber("T-5")
                .section("Main Floor")
                .status(PosTable.Status.ORDERED)
                .build();
        table.setId(5L);
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 5L)).thenReturn(Optional.of(table));

        KitchenOrder ko1 = KitchenOrder.builder()
                .tableId(5L)
                .waiter("Ram")
                .firedAt(Instant.now().minusSeconds(1800))
                .status(KitchenOrder.Status.SERVED)
                .build();
        ko1.setId(101L);

        KitchenOrder ko2 = KitchenOrder.builder()
                .tableId(5L)
                .waiter("Ram")
                .firedAt(Instant.now().minusSeconds(600))
                .status(KitchenOrder.Status.PREPARING)
                .build();
        ko2.setId(102L);

        when(kitchenOrderRepository.findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                tenantId, companyId, 5L, KitchenOrder.Status.CANCELLED))
                .thenReturn(List.of(ko1, ko2));

        // Line items for ko1: 2x Buff Momo (item 10), 2x Milk Tea (item 20)
        KitchenOrderLine line1 = KitchenOrderLine.builder()
                .kitchenOrderId(101L)
                .itemId(10L)
                .itemCode("MOMO-01")
                .itemName("Buff Steam Momo")
                .quantity(new BigDecimal("2"))
                .notes("Extra spicy chutney")
                .build();
        KitchenOrderLine line2 = KitchenOrderLine.builder()
                .kitchenOrderId(101L)
                .itemId(20L)
                .itemCode("TEA-01")
                .itemName("Nepali Masala Tea")
                .quantity(new BigDecimal("2"))
                .build();
        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, 101L))
                .thenReturn(List.of(line1, line2));

        // Line items for ko2: 1x Buff Momo (item 10) - duplicate item, 1x Coke (item 30)
        KitchenOrderLine line3 = KitchenOrderLine.builder()
                .kitchenOrderId(102L)
                .itemId(10L)
                .itemCode("MOMO-01")
                .itemName("Buff Steam Momo")
                .quantity(new BigDecimal("1"))
                .notes("Less spicy")
                .build();
        KitchenOrderLine line4 = KitchenOrderLine.builder()
                .kitchenOrderId(102L)
                .itemId(30L)
                .itemCode("COKE-01")
                .itemName("Coca Cola 250ml")
                .quantity(new BigDecimal("1"))
                .build();
        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, 102L))
                .thenReturn(List.of(line3, line4));

        // Mock ItemService prices
        ItemService.ItemResponse momoItem = mock(ItemService.ItemResponse.class);
        when(momoItem.getId()).thenReturn(10L);
        when(momoItem.getName()).thenReturn("Buff Steam Momo");
        when(momoItem.getSellingPrice()).thenReturn(new BigDecimal("150.00"));
        when(momoItem.getUnitCode()).thenReturn("PLT");

        ItemService.ItemResponse teaItem = mock(ItemService.ItemResponse.class);
        when(teaItem.getId()).thenReturn(20L);
        when(teaItem.getName()).thenReturn("Nepali Masala Tea");
        when(teaItem.getSellingPrice()).thenReturn(new BigDecimal("40.00"));
        when(teaItem.getUnitCode()).thenReturn("CUP");

        ItemService.ItemResponse cokeItem = mock(ItemService.ItemResponse.class);
        when(cokeItem.getId()).thenReturn(30L);
        when(cokeItem.getName()).thenReturn("Coca Cola 250ml");
        when(cokeItem.getSellingPrice()).thenReturn(new BigDecimal("70.00"));
        when(cokeItem.getUnitCode()).thenReturn("BOT");

        when(itemService.findById(10L)).thenReturn(Optional.of(momoItem));
        when(itemService.findById(20L)).thenReturn(Optional.of(teaItem));
        when(itemService.findById(30L)).thenReturn(Optional.of(cokeItem));

        TableBillSummaryResponse bill = service.getTableCurrentBill(tenantId, companyId, 5L);

        assertNotNull(bill);
        assertEquals(5L, bill.tableId());
        assertEquals("T-5", bill.tableNumber());
        assertEquals(2, bill.kotCount());
        assertEquals("Ram", bill.waiter());
        assertEquals(3, bill.lines().size());

        // Buff Momo: 2 + 1 = 3 qty @ 150 = 450.00
        TableBillSummaryResponse.BillLineItem momoLine = bill.lines().stream().filter(l -> l.itemId().equals(10L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("3"), momoLine.quantity());
        assertEquals(new BigDecimal("150.00"), momoLine.unitPrice());
        assertEquals(new BigDecimal("450.00"), momoLine.lineAmount());
        assertTrue(momoLine.notes().contains("Extra spicy chutney"));
        assertTrue(momoLine.notes().contains("Less spicy"));

        // Masala Tea: 2 qty @ 40 = 80.00
        TableBillSummaryResponse.BillLineItem teaLine = bill.lines().stream().filter(l -> l.itemId().equals(20L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("2"), teaLine.quantity());
        assertEquals(new BigDecimal("80.00"), teaLine.lineAmount());

        // Coke: 1 qty @ 70 = 70.00
        TableBillSummaryResponse.BillLineItem cokeLine = bill.lines().stream().filter(l -> l.itemId().equals(30L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1"), cokeLine.quantity());
        assertEquals(new BigDecimal("70.00"), cokeLine.lineAmount());

        // Subtotal: 450 + 80 + 70 = 600.00
        assertEquals(new BigDecimal("600.00"), bill.subtotal());
        assertEquals(new BigDecimal("10.00"), bill.serviceChargePercent());
        assertEquals(new BigDecimal("60.00"), bill.serviceChargeAmount());
        assertEquals(new BigDecimal("13.00"), bill.vatRate());
        assertEquals(new BigDecimal("85.80"), bill.vatAmount());
        assertEquals(new BigDecimal("745.80"), bill.totalAmount());
    }

    @Test
    @DisplayName("getTableCurrentBill should include modifier price in unit price and line subtotal")
    void testGetTableCurrentBillWithModifiersAndAddonPricing() {
        PosTable table = PosTable.builder().tableNumber("T-8").section("VIP Cabin").status(PosTable.Status.ORDERED).build();
        table.setId(8L);
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 8L)).thenReturn(Optional.of(table));

        KitchenOrder ko = KitchenOrder.builder().tableId(8L).status(KitchenOrder.Status.PREPARING).build();
        ko.setId(201L);

        when(kitchenOrderRepository.findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                tenantId, companyId, 8L, KitchenOrder.Status.CANCELLED)).thenReturn(List.of(ko));

        KitchenOrderLine lineWithAddon = KitchenOrderLine.builder()
                .kitchenOrderId(201L)
                .itemId(10L)
                .itemCode("MOMO-01")
                .itemName("Buff Steam Momo")
                .quantity(new BigDecimal("2"))
                .station("KITCHEN")
                .modifiers("Steamed, Extra Spicy, Add Cheese (+Rs. 50)")
                .modifierPrice(new BigDecimal("50.00"))
                .build();

        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, 201L))
                .thenReturn(List.of(lineWithAddon));

        ItemService.ItemResponse momoItem = mock(ItemService.ItemResponse.class);
        when(momoItem.getId()).thenReturn(10L);
        when(momoItem.getName()).thenReturn("Buff Steam Momo");
        when(momoItem.getSellingPrice()).thenReturn(new BigDecimal("150.00"));
        when(momoItem.getUnitCode()).thenReturn("PLT");

        when(itemService.findById(10L)).thenReturn(Optional.of(momoItem));

        TableBillSummaryResponse bill = service.getTableCurrentBill(tenantId, companyId, 8L);

        assertNotNull(bill);
        assertEquals(1, bill.lines().size());
        TableBillSummaryResponse.BillLineItem line = bill.lines().get(0);
        assertEquals(new BigDecimal("200.00"), line.unitPrice());
        assertEquals(new BigDecimal("400.00"), line.lineAmount());
        assertEquals(new BigDecimal("400.00"), bill.subtotal());
        assertEquals(new BigDecimal("40.00"), bill.serviceChargeAmount());
        assertEquals(new BigDecimal("57.20"), bill.vatAmount());
        assertEquals(new BigDecimal("497.20"), bill.totalAmount());
        assertEquals("Steamed, Extra Spicy, Add Cheese (+Rs. 50)", line.modifiers());
        assertEquals("KITCHEN", line.station());
    }

    @Test
    @DisplayName("transferTable should move unbilled KOTs to target table and free source table")
    void testTransferTable() {
        PosTable fromTable = PosTable.builder().tableNumber("T-1").capacity(2).status(PosTable.Status.FREE).build();
        fromTable.occupy(501L);
        fromTable.markOrdered();
        PosTable toTable = PosTable.builder().tableNumber("T-2").capacity(4).status(PosTable.Status.FREE).build();

        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 2L)).thenReturn(Optional.of(toTable));

        KitchenOrder ko = KitchenOrder.builder().tableId(1L).sessionId(501L).status(KitchenOrder.Status.PREPARING).build();
        when(kitchenOrderRepository.findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                tenantId, companyId, 1L, KitchenOrder.Status.CANCELLED)).thenReturn(List.of(ko));

        service.transferTable(tenantId, companyId, 1L, 2L);

        assertEquals(PosTable.Status.FREE, fromTable.getStatus());
        assertEquals(PosTable.Status.ORDERED, toTable.getStatus());
        assertEquals(501L, toTable.getCurrentSessionId());
        assertEquals(2L, ko.getTableId());
        verify(kitchenOrderRepository).save(ko);
    }

    @Test
    @DisplayName("mergeTables should move active orders from source to target and close source session")
    void testMergeTables() {
        PosTable fromTable = PosTable.builder().tableNumber("T-1").capacity(2).status(PosTable.Status.ORDERED).build();
        fromTable.occupy(501L);
        PosTable intoTable = PosTable.builder().tableNumber("T-2").capacity(4).status(PosTable.Status.ORDERED).build();
        intoTable.occupy(502L);

        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 2L)).thenReturn(Optional.of(intoTable));

        TableSession fromSession = TableSession.builder().tableId(1L).status(TableSession.Status.OPEN).build();
        when(sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 501L)).thenReturn(Optional.of(fromSession));

        KitchenOrder koFrom = KitchenOrder.builder().tableId(1L).sessionId(501L).status(KitchenOrder.Status.SERVED).build();
        when(kitchenOrderRepository.findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                tenantId, companyId, 1L, KitchenOrder.Status.CANCELLED)).thenReturn(List.of(koFrom));

        service.mergeTables(tenantId, companyId, 1L, 2L);

        assertEquals(PosTable.Status.FREE, fromTable.getStatus());
        assertEquals(PosTable.Status.ORDERED, intoTable.getStatus());
        assertEquals(2L, koFrom.getTableId());
        assertEquals(502L, koFrom.getSessionId());
        assertEquals(TableSession.Status.MERGED, fromSession.getStatus());
        assertEquals(502L, fromSession.getMergedIntoSessionId());
    }

    @Test
    @DisplayName("closeTableSessionOnCheckout should link transaction, bill session and set table to CLEANING")
    void testCloseTableSessionOnCheckout() {
        PosTable table = PosTable.builder().tableNumber("T-3").capacity(4).status(PosTable.Status.ORDERED).build();
        table.occupy(601L);

        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 3L)).thenReturn(Optional.of(table));

        KitchenOrder ko = KitchenOrder.builder().tableId(3L).sessionId(601L).status(KitchenOrder.Status.SERVED).build();
        when(kitchenOrderRepository.findByTenantIdAndCompanyIdAndTableIdAndPosTransactionIdIsNullAndStatusNot(
                tenantId, companyId, 3L, KitchenOrder.Status.CANCELLED)).thenReturn(List.of(ko));

        TableSession session = TableSession.builder().tableId(3L).status(TableSession.Status.OPEN).build();
        when(sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 601L)).thenReturn(Optional.of(session));

        service.closeTableSessionOnCheckout(tenantId, companyId, 3L, 999L);

        assertEquals(999L, ko.getPosTransactionId());
        assertEquals(KitchenOrder.Status.SERVED, ko.getStatus());
        assertEquals(TableSession.Status.CLOSED, session.getStatus());
        assertEquals(999L, session.getPosTransactionId());
        assertEquals(PosTable.Status.CLEANING, table.getStatus());

        verify(kitchenOrderRepository).save(ko);
        verify(sessionRepository).save(session);
        verify(tableRepository).save(table);
    }
}
