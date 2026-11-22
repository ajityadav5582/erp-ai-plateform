package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.dto.KitchenTicketResponse;
import com.erp.business.pos.restaurant.dto.RestaurantModifierPresetDto;

import java.util.List;

public interface KitchenOrderService {
    KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter, List<KitchenOrderLine> lines);
    KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter,
                       String orderType, String tokenNumber, String deliveryPartner, String customerPhone,
                       List<KitchenOrderLine> lines);
    KitchenOrder updateStatus(Long tenantId, Long companyId, Long kitchenOrderId, KitchenOrder.Status status);
    List<KitchenOrder> listByTable(Long tenantId, Long companyId, Long tableId);
    List<KitchenOrder> listAll(Long tenantId, Long companyId);
    List<KitchenOrder> listAll(Long tenantId, Long companyId, String station);
    List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId);
    List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId, String station);
    KitchenTicketResponse getKitchenTicket(Long tenantId, Long companyId, Long orderId);
    List<RestaurantModifierPresetDto> getModifierPresets(Long tenantId, Long companyId);
}
