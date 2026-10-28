package com.erp.business.procurement.purchaseorder;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderDecisionRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;
import com.erp.business.procurement.purchaseorder.infrastructure.persistence.PurchaseOrderRepository;
import com.erp.business.procurement.shared.security.PurchaseOrderPermissions;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purchase order lifecycle transitions.
 *
 * <p>Deliberately thin. The rules live in {@code PurchaseOrderStatus.canTransitionTo}
 * and {@code PurchaseOrder.transitionTo}; this loads the aggregate under a write lock,
 * asks it to move, and saves it. Any state check written here as well would be a second
 * source of truth that eventually disagrees with the first.
 *
 * <p><strong>Every load takes a pessimistic lock.</strong> Two approvers pressing the
 * button at once would both read SUBMITTED and both stamp it; the lock makes the second
 * one wait, read APPROVED, and be refused by the aggregate with a message naming the
 * state. Optimistic locking alone would do the same job but surface as a 409 from the
 * persistence layer rather than as a readable refusal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderLifecycleServiceImpl implements PurchaseOrderLifecycleService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderAssembler assembler;

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.CREATE)
    public PurchaseOrderResponse submit(Long tenantId, Long companyId, Long id, String actor) {
        return move(tenantId, companyId, id, PurchaseOrderStatus.SUBMITTED, null, actor);
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.APPROVE)
    public PurchaseOrderResponse approve(Long tenantId, Long companyId, Long id, String actor) {
        return move(tenantId, companyId, id, PurchaseOrderStatus.APPROVED, null, actor);
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.APPROVE)
    public PurchaseOrderResponse reject(Long tenantId, Long companyId, Long id,
                                        PurchaseOrderDecisionRequest request, String actor) {
        return move(tenantId, companyId, id, PurchaseOrderStatus.REJECTED, request, actor);
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.CANCEL)
    public PurchaseOrderResponse cancel(Long tenantId, Long companyId, Long id,
                                        PurchaseOrderDecisionRequest request, String actor) {
        return move(tenantId, companyId, id, PurchaseOrderStatus.CANCELLED, request, actor);
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.APPROVE)
    public PurchaseOrderResponse close(Long tenantId, Long companyId, Long id, String actor) {
        return move(tenantId, companyId, id, PurchaseOrderStatus.CLOSED, null, actor);
    }

    /**
     * Loads the order, asks the aggregate to transition, and persists it.
     *
     * <p>The {@link IllegalStateException} the aggregate raises for an illegal move is
     * translated into a {@link ValidationException} so the caller sees a 400 naming the
     * current state, rather than a 500 from a lifecycle rule that is working correctly.
     */
    private PurchaseOrderResponse move(Long tenantId, Long companyId, Long id,
                                       PurchaseOrderStatus target,
                                       PurchaseOrderDecisionRequest request, String actor) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        PurchaseOrder order = orderRepository
                .findByIdForUpdate(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order " + id + " was not found"));

        try {
            order.transitionTo(target, actor, request == null ? null : request.reason());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }

        PurchaseOrder saved = orderRepository.saveAndFlush(order);
        return assembler.toResponse(saved);
    }
}
