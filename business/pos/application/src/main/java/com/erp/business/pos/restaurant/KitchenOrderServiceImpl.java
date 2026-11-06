package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderLineRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.KitchenOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final KitchenOrderRepository repository;
    private final KitchenOrderLineRepository lineRepository;

    public KitchenOrderServiceImpl(KitchenOrderRepository repository, KitchenOrderLineRepository lineRepository) {
        this.repository = repository;
        this.lineRepository = lineRepository;
    }

    @Override
    public KitchenOrder fire(Long tenantId, Long companyId, Long tableId, Long sessionId, String waiter, List<KitchenOrderLine> lines) {
        KitchenOrder order = KitchenOrder.builder()
                .tableId(tableId)
                .sessionId(sessionId)
                .waiter(waiter)
                .status(KitchenOrder.Status.PENDING)
                .build();
        order.assignScope(tenantId, companyId);
        KitchenOrder saved = repository.save(order);
        if (lines != null) {
            for (KitchenOrderLine line : lines) {
                line.assignScope(tenantId, companyId);
                line.assignKitchenOrder(saved.getId());
                lineRepository.save(line);
            }
        }
        return saved;
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
    public List<KitchenOrderLine> listLines(Long tenantId, Long companyId, Long kitchenOrderId) {
        return lineRepository.findByTenantIdAndCompanyIdAndKitchenOrderId(tenantId, companyId, kitchenOrderId);
    }
}
