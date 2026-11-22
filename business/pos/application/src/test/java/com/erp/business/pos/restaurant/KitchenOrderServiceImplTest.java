package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.dto.KitchenTicketResponse;
import com.erp.business.pos.restaurant.dto.RestaurantModifierPresetDto;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KitchenOrderServiceImplTest {

    @Mock private KitchenOrderRepository repository;
    @Mock private KitchenOrderLineRepository lineRepository;
    @Mock private PosTableRepository tableRepository;
    @Mock private TableSessionRepository tableSessionRepository;

    private KitchenOrderServiceImpl service;

    private final Long tenantId = 1L;
    private final Long companyId = 100L;

    @BeforeEach
    void setUp() {
        service = new KitchenOrderServiceImpl(repository, lineRepository, tableRepository, tableSessionRepository);
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> {
            KitchenOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(101L);
            }
            return o;
        });
        when(lineRepository.save(any(KitchenOrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("fire should auto-route items to BAR, TANDOOR, DESSERT, and KITCHEN stations")
    void testFireAutoRoutesStations() {
        PosTable table = PosTable.builder().tableNumber("T-1").section("Main Floor").status(PosTable.Status.OCCUPIED).build();
        table.occupy(500L);
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(table));

        KitchenOrderLine teaLine = KitchenOrderLine.builder()
                .itemId(1L).itemName("Nepali Milk Tea").quantity(BigDecimal.ONE).build();
        KitchenOrderLine sekuwaLine = KitchenOrderLine.builder()
                .itemId(2L).itemName("Chicken Sekuwa").quantity(BigDecimal.ONE).build();
        KitchenOrderLine kheerLine = KitchenOrderLine.builder()
                .itemId(3L).itemName("Nepali Rice Kheer (Sweet)").quantity(BigDecimal.ONE).build();
        KitchenOrderLine momoLine = KitchenOrderLine.builder()
                .itemId(4L).itemName("Buff Steam Momo").quantity(BigDecimal.valueOf(2))
                .modifiers("Extra Spicy, Add Cheese (+Rs. 50)")
                .modifierPrice(new BigDecimal("50.00"))
                .build();

        KitchenOrder saved = service.fire(tenantId, companyId, 1L, null, "Hari", List.of(teaLine, sekuwaLine, kheerLine, momoLine));

        assertNotNull(saved);
        assertEquals(500L, saved.getSessionId());
        assertEquals("DINE_IN", saved.getOrderType());
        assertEquals("KOT-0101", saved.getKotNumber());

        // Verify line routing with ArgumentCaptor
        org.mockito.ArgumentCaptor<KitchenOrderLine> captor = org.mockito.ArgumentCaptor.forClass(KitchenOrderLine.class);
        verify(lineRepository, times(4)).save(captor.capture());
        List<KitchenOrderLine> savedLines = captor.getAllValues();

        KitchenOrderLine savedTea = savedLines.stream().filter(l -> "Nepali Milk Tea".equals(l.getItemName())).findFirst().orElseThrow();
        assertEquals("BAR", savedTea.getStation());

        KitchenOrderLine savedSekuwa = savedLines.stream().filter(l -> "Chicken Sekuwa".equals(l.getItemName())).findFirst().orElseThrow();
        assertEquals("TANDOOR", savedSekuwa.getStation());

        KitchenOrderLine savedKheer = savedLines.stream().filter(l -> "Nepali Rice Kheer (Sweet)".equals(l.getItemName())).findFirst().orElseThrow();
        assertEquals("DESSERT", savedKheer.getStation());

        KitchenOrderLine savedMomo = savedLines.stream().filter(l -> "Buff Steam Momo".equals(l.getItemName())).findFirst().orElseThrow();
        assertEquals("KITCHEN", savedMomo.getStation());
        assertEquals(new BigDecimal("50.00"), savedMomo.getModifierPrice());
        assertEquals("Extra Spicy, Add Cheese (+Rs. 50)", savedMomo.getModifiers());
    }

    @Test
    @DisplayName("fire with TAKEAWAY should assign token number and customer phone")
    void testFireTakeawayAssignsToken() {
        KitchenOrderLine line = KitchenOrderLine.builder()
                .itemId(10L).itemName("Samosa").quantity(BigDecimal.valueOf(4)).build();

        KitchenOrder saved = service.fire(tenantId, companyId, null, null, "Counter-1",
                "TAKEAWAY", "TK-088", null, "9841112233", List.of(line));

        assertNotNull(saved);
        assertEquals("TAKEAWAY", saved.getOrderType());
        assertEquals("TK-088", saved.getTokenNumber());
        assertEquals("9841112233", saved.getCustomerPhone());
        assertNull(saved.getTableId());
        assertEquals("KOT-0101", saved.getKotNumber());
    }

    @Test
    @DisplayName("fire with DELIVERY should assign platform and rider details")
    void testFireDeliveryPlatform() {
        KitchenOrderLine line = KitchenOrderLine.builder()
                .itemId(11L).itemName("Chicken Biryani").quantity(BigDecimal.valueOf(2)).build();

        KitchenOrder saved = service.fire(tenantId, companyId, null, null, "Online-Dispatcher",
                "DELIVERY", null, "Pathao Food", "9812999888", List.of(line));

        assertNotNull(saved);
        assertEquals("DELIVERY", saved.getOrderType());
        assertEquals("Pathao Food", saved.getDeliveryPartner());
        assertEquals("9812999888", saved.getCustomerPhone());
        assertEquals("TK-101", saved.getTokenNumber()); // Auto-generated token from id 101
    }

    @Test
    @DisplayName("updateStatus should update KOT status through state transitions")
    void testUpdateStatus() {
        KitchenOrder order = KitchenOrder.builder().status(KitchenOrder.Status.PENDING).build();
        order.setId(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(order));

        service.updateStatus(tenantId, companyId, 101L, KitchenOrder.Status.PREPARING);
        assertEquals(KitchenOrder.Status.PREPARING, order.getStatus());

        service.updateStatus(tenantId, companyId, 101L, KitchenOrder.Status.READY);
        assertEquals(KitchenOrder.Status.READY, order.getStatus());

        service.updateStatus(tenantId, companyId, 101L, KitchenOrder.Status.SERVED);
        assertEquals(KitchenOrder.Status.SERVED, order.getStatus());
    }

    @Test
    @DisplayName("listLines by station should return filtered lines")
    void testListLinesByStation() {
        KitchenOrderLine barLine = KitchenOrderLine.builder().kitchenOrderId(101L).itemName("Beer").station("BAR").build();
        KitchenOrderLine kitchenLine = KitchenOrderLine.builder().kitchenOrderId(101L).itemName("Chowmein").station("KITCHEN").build();

        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderIdAndStation(tenantId, companyId, 101L, "BAR"))
                .thenReturn(List.of(barLine));
        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, 101L))
                .thenReturn(List.of(barLine, kitchenLine));

        List<KitchenOrderLine> barLines = service.listLines(tenantId, companyId, 101L, "BAR");
        assertEquals(1, barLines.size());
        assertEquals("BAR", barLines.get(0).getStation());

        List<KitchenOrderLine> allLines = service.listLines(tenantId, companyId, 101L, "ALL");
        assertEquals(2, allLines.size());
    }

    @Test
    @DisplayName("getKitchenTicket should return detailed ticket response with delivery & token details")
    void testGetKitchenTicket() {
        KitchenOrder order = KitchenOrder.builder()
                .tableId(1L)
                .waiter("Suresh")
                .orderType("DELIVERY")
                .notes("Pack chutney separately")
                .build();
        order.assignScope(tenantId, companyId);
        order.setId(101L);
        order.assignKotNumber("KOT-0101");
        order.assignDeliveryDetails("Foodmandu", "9800000000", "TK-055");

        PosTable table = PosTable.builder().tableNumber("T-5").section("Rooftop").build();
        when(repository.findById(101L)).thenReturn(Optional.of(order));
        when(tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, 1L)).thenReturn(Optional.of(table));

        KitchenOrderLine line = KitchenOrderLine.builder()
                .itemId(10L)
                .itemCode("MOMO-01")
                .itemName("Chicken Momo")
                .quantity(BigDecimal.valueOf(2))
                .station("KITCHEN")
                .modifiers("Steam, Non-Spicy")
                .modifierPrice(BigDecimal.ZERO)
                .course(1)
                .notes("No coriander")
                .build();
        when(lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, 101L))
                .thenReturn(List.of(line));

        KitchenTicketResponse ticket = service.getKitchenTicket(tenantId, companyId, 101L);

        assertNotNull(ticket);
        assertEquals(101L, ticket.orderId());
        assertEquals("KOT-0101", ticket.kotNumber());
        assertEquals("T-5", ticket.tableNumber());
        assertEquals("Rooftop", ticket.section());
        assertEquals("Suresh", ticket.waiter());
        assertEquals("DELIVERY", ticket.orderType());
        assertEquals("TK-055", ticket.tokenNumber());
        assertEquals("Foodmandu", ticket.deliveryPartner());
        assertEquals("9800000000", ticket.customerPhone());
        assertEquals("Pack chutney separately", ticket.notes());
        assertEquals(1, ticket.lines().size());
        assertEquals("Chicken Momo", ticket.lines().get(0).itemName());
    }

    @Test
    @DisplayName("getModifierPresets should return standard restaurant presets")
    void testModifierPresets() {
        List<RestaurantModifierPresetDto> presets = service.getModifierPresets(tenantId, companyId);
        assertNotNull(presets);
        assertTrue(presets.stream().anyMatch(p -> "Spice Level".equals(p.groupName())));
        assertTrue(presets.stream().anyMatch(p -> "Preparation Style".equals(p.groupName())));
        assertTrue(presets.stream().anyMatch(p -> "Drink & Ice".equals(p.groupName())));
        assertTrue(presets.stream().anyMatch(p -> "Add-ons & Extras".equals(p.groupName())));
    }
}
