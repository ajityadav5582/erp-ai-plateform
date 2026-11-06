package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;

import java.util.List;

public interface KitchenOrderService {
    KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter, List<KitchenOrderLine> lines);
    KitchenOrder updateStatus(Long tenantId, Long companyId, Long kitchenOrderId, KitchenOrder.Status status);
    List<KitchenOrder> listByTable(Long tenantId, Long companyId, Long tableId);
    List<KitchenOrder> listAll(Long tenantId, Long companyId);
    List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId);
}
