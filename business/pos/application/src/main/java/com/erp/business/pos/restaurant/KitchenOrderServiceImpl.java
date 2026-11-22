package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.business.pos.restaurant.dto.KitchenTicketResponse;
import com.erp.business.pos.restaurant.dto.RestaurantModifierPresetDto;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderLineRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.PosTableRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final KitchenOrderRepository repository;
    private final KitchenOrderLineRepository lineRepository;
    private final PosTableRepository tableRepository;
    private final TableSessionRepository tableSessionRepository;

    @Override
    public KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter, List<KitchenOrderLine> lines) {
        return fire(tenantId, companyId, tableId, sessionId, waiter, tableId != null ? "DINE_IN" : "TAKEAWAY", null, null, null, lines);
    }

    @Override
    public KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter,
                             String orderType, String tokenNumber, String deliveryPartner, String customerPhone,
                             List<KitchenOrderLine> lines) {
        Long effectiveSessionId = sessionId;
        String effectiveOrderType = (orderType != null && !orderType.isBlank())
                ? orderType.trim().toUpperCase()
                : (tableId != null ? "DINE_IN" : "TAKEAWAY");

        if (tableId != null && effectiveSessionId == null) {
            PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId).orElse(null);
            if (table != null) {
                if (table.getCurrentSessionId() != null) {
                    effectiveSessionId = table.getCurrentSessionId();
                } else {
                    TableSession newSession = TableSession.builder().tableId(tableId).build();
                    newSession.assignScope(tenantId, companyId);
                    TableSession savedSession = tableSessionRepository.save(newSession);
                    effectiveSessionId = savedSession.getId();
                    table.occupy(effectiveSessionId);
                    table.markOrdered();
                    tableRepository.save(table);
                }
            }
        }

        KitchenOrder order = KitchenOrder.builder()
                .tableId(tableId)
                .sessionId(effectiveSessionId)
                .waiter(waiter)
                .orderType(effectiveOrderType)
                .status(KitchenOrder.Status.PENDING)
                .firedAt(Instant.now())
                .build();
        order.assignScope(tenantId, companyId);
        KitchenOrder saved = repository.save(order);

        // Assign formatted KOT number
        saved.assignKotNumber("KOT-" + String.format("%04d", saved.getId()));

        // Assign token number & delivery details
        long tokenSeq = (saved.getId() % 1000 == 0) ? 1000 : (saved.getId() % 1000);
        String effectiveToken = (tokenNumber != null && !tokenNumber.isBlank())
                ? tokenNumber
                : String.format("TK-%03d", tokenSeq);

        if ("TAKEAWAY".equalsIgnoreCase(effectiveOrderType)) {
            saved.assignTakeawayDetails(effectiveToken, customerPhone);
        } else if ("DELIVERY".equalsIgnoreCase(effectiveOrderType)) {
            saved.assignDeliveryDetails(deliveryPartner != null && !deliveryPartner.isBlank() ? deliveryPartner : "In-House Rider", customerPhone, effectiveToken);
        }

        saved = repository.save(saved);

        if (lines != null) {
            for (KitchenOrderLine line : lines) {
                String station = resolveStation(line);
                KitchenOrderLine preparedLine = KitchenOrderLine.builder()
                        .kitchenOrderId(saved.getId())
                        .itemId(line.getItemId())
                        .itemCode(line.getItemCode())
                        .itemName(line.getItemName())
                        .quantity(line.getQuantity())
                        .notes(line.getNotes())
                        .course(line.getCourse() > 0 ? line.getCourse() : 1)
                        .station(station)
                        .modifiers(line.getModifiers())
                        .modifierPrice(line.getModifierPrice() != null ? line.getModifierPrice() : BigDecimal.ZERO)
                        .build();
                preparedLine.assignScope(tenantId, companyId);
                lineRepository.save(preparedLine);
            }
        }
        return saved;
    }

    private String resolveStation(KitchenOrderLine line) {
        if (line.getStation() != null && !line.getStation().isBlank()
                && !"KITCHEN".equalsIgnoreCase(line.getStation())
                && !"ALL".equalsIgnoreCase(line.getStation())) {
            return line.getStation().trim().toUpperCase();
        }
        String name = (line.getItemName() != null ? line.getItemName() : "").toLowerCase();
        if (name.contains("beverage") || name.contains("beer") || name.contains("wine") ||
                name.contains("coke") || name.contains("fanta") || name.contains("sprite") ||
                name.contains("juice") || name.contains("coffee") || name.contains("mojito") ||
                name.contains("cocktail") || name.contains("mocktail") || name.contains("lassi") ||
                name.contains("shake") || name.contains("smoothie") ||
                matchesWord(name, "tea") || matchesWord(name, "chai") || matchesWord(name, "water") || matchesWord(name, "drink")) {
            return "BAR";
        }
        if (name.contains("sekuwa") || name.contains("tandoor") || name.contains("naan") ||
                name.contains("roti") || name.contains("tikka") || name.contains("kebab") ||
                name.contains("grill")) {
            return "TANDOOR";
        }
        if (name.contains("ice cream") || name.contains("dessert") || name.contains("sweet") ||
                name.contains("gulab") || name.contains("rasgulla") || name.contains("pastry") ||
                name.contains("cake") || name.contains("kheer") || name.contains("jalebi")) {
            return "DESSERT";
        }
        return "KITCHEN";
    }

    private boolean matchesWord(String text, String word) {
        return text.matches("(?i).*\\b" + Pattern.quote(word) + "\\b.*");
    }

    @Override
    public KitchenOrder updateStatus(Long tenantId, Long companyId, Long kitchenOrderId, KitchenOrder.Status status) {
        KitchenOrder order = repository.findById(kitchenOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Kitchen order not found: " + kitchenOrderId));
        switch (status) {
            case PREPARING -> order.markPreparing();
            case READY -> order.markReady();
            case SERVED -> order.markServed();
            case CANCELLED -> order.cancel();
            default -> {}
        }
        return repository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrder> listByTable(Long tenantId, Long companyId, Long tableId) {
        return repository.findByTenantIdAndCompanyIdAndTableId(tenantId, companyId, tableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrder> listAll(Long tenantId, Long companyId) {
        return repository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrder> listAll(Long tenantId, Long companyId, String station) {
        if (station == null || station.isBlank() || "ALL".equalsIgnoreCase(station)) {
            return listAll(tenantId, companyId);
        }
        String normalizedStation = station.trim().toUpperCase();
        List<KitchenOrderLine> stationLines = lineRepository.findByTenantIdAndCompanyIdAndStation(tenantId, companyId, normalizedStation);
        Set<Long> orderIds = stationLines.stream().map(KitchenOrderLine::getKitchenOrderId).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndCompanyId(tenantId, companyId).stream()
                .filter(o -> orderIds.contains(o.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId) {
        return lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, kitchenOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId, String station) {
        if (station == null || station.isBlank() || "ALL".equalsIgnoreCase(station)) {
            return listLines(tenantId, companyId, kitchenOrderId);
        }
        return lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderIdAndStation(
                tenantId, companyId, kitchenOrderId, station.trim().toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenTicketResponse getKitchenTicket(Long tenantId, Long companyId, Long orderId) {
        KitchenOrder order = repository.findById(orderId)
                .filter(o -> tenantId.equals(o.getTenantId()) && companyId.equals(o.getCompanyId()))
                .orElseThrow(() -> new IllegalArgumentException("Kitchen order not found: " + orderId));

        PosTable table = order.getTableId() != null
                ? tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, order.getTableId()).orElse(null)
                : null;

        List<KitchenOrderLine> lines = lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, orderId);
        List<KitchenTicketResponse.TicketLine> ticketLines = lines.stream()
                .map(l -> new KitchenTicketResponse.TicketLine(
                        l.getItemId(),
                        l.getItemCode(),
                        l.getItemName(),
                        l.getQuantity(),
                        l.getStation(),
                        l.getModifiers(),
                        l.getModifierPrice(),
                        l.getNotes(),
                        l.getCourse()
                ))
                .toList();

        return new KitchenTicketResponse(
                order.getId(),
                order.getKotNumber() != null ? order.getKotNumber() : "KOT-" + order.getId(),
                order.getTableId(),
                table != null ? table.getTableNumber() : null,
                table != null ? table.getSection() : null,
                order.getWaiter(),
                order.getOrderType() != null ? order.getOrderType() : "DINE_IN",
                order.getTokenNumber(),
                order.getDeliveryPartner(),
                order.getCustomerPhone(),
                order.getFiredAt(),
                order.getStatus(),
                order.getCourse(),
                order.getNotes(),
                ticketLines
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantModifierPresetDto> getModifierPresets(Long tenantId, Long companyId) {
        return List.of(
                new RestaurantModifierPresetDto(
                        "Spice Level",
                        false,
                        List.of(
                                new RestaurantModifierPresetDto.Option("Non-Spicy", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Mild", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Medium", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Spicy", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Extra Spicy 🔥", BigDecimal.ZERO)
                        )
                ),
                new RestaurantModifierPresetDto(
                        "Preparation Style",
                        false,
                        List.of(
                                new RestaurantModifierPresetDto.Option("Steamed", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Fried", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Kothey", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Jhol", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Sadheko", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Tandoori", BigDecimal.ZERO)
                        )
                ),
                new RestaurantModifierPresetDto(
                        "Drink & Ice",
                        false,
                        List.of(
                                new RestaurantModifierPresetDto.Option("Normal", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Less Ice", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("No Ice", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Hot", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Chilled", BigDecimal.ZERO)
                        )
                ),
                new RestaurantModifierPresetDto(
                        "Sweetness",
                        false,
                        List.of(
                                new RestaurantModifierPresetDto.Option("Normal Sugar", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Less Sugar", BigDecimal.ZERO),
                                new RestaurantModifierPresetDto.Option("Sugar Free", BigDecimal.ZERO)
                        )
                ),
                new RestaurantModifierPresetDto(
                        "Add-ons & Extras",
                        true,
                        List.of(
                                new RestaurantModifierPresetDto.Option("Add Cheese", new BigDecimal("50.00")),
                                new RestaurantModifierPresetDto.Option("Add Egg", new BigDecimal("30.00")),
                                new RestaurantModifierPresetDto.Option("Extra Chutney", new BigDecimal("20.00")),
                                new RestaurantModifierPresetDto.Option("Extra Gravy", new BigDecimal("30.00")),
                                new RestaurantModifierPresetDto.Option("Add Ice Cream", new BigDecimal("50.00")),
                                new RestaurantModifierPresetDto.Option("Extra Butter", new BigDecimal("40.00"))
                        )
                )
        );
    }
}
