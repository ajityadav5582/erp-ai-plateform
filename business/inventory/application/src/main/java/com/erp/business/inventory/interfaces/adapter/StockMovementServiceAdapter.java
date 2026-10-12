package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import com.erp.business.inventory.stock.StockMovementBatchResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Driving adapter that implements the public stock-movement port by delegating
 * to the internal use-case service, resolving tenant and company from the
 * current request context.
 *
 * <p>This is the in-process wiring of the port. When Inventory is consumed over
 * the wire, the caller supplies its own adapter (an HTTP client) against the
 * very same port - the port is the contract, this class is one implementation.
 */
@Component
public class StockMovementServiceAdapter implements StockMovementService {

    private final com.erp.business.inventory.stock.StockMovementService internalStockMovementService;
    private final BusinessContextAccessor contextAccessor;

    public StockMovementServiceAdapter(
            com.erp.business.inventory.stock.StockMovementService internalStockMovementService,
            BusinessContextAccessor contextAccessor) {
        this.internalStockMovementService = internalStockMovementService;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public StockMovementBatchResponse postReceipt(StockReceiptRequest request) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        StockMovementBatchResult result =
                internalStockMovementService.postReceiptBatch(tenantId, companyId, request);
        return toResponse(request, result);
    }

    @Override
    public StockMovementBatchResponse reverseReceipt(StockReceiptRequest request) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        StockMovementBatchResult result =
                internalStockMovementService.reverseReceiptBatch(tenantId, companyId, request);
        return toResponse(request, result);
    }

    @Override
    public StockMovementBatchResponse postIssue(StockReceiptRequest request) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        StockMovementBatchResult result =
                internalStockMovementService.postIssueBatch(tenantId, companyId, request);
        return toResponse(request, result);
    }

    @Override
    public StockMovementBatchResponse reverseIssue(StockReceiptRequest request) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        StockMovementBatchResult result =
                internalStockMovementService.reverseIssueBatch(tenantId, companyId, request);
        return toResponse(request, result);
    }

    private StockMovementBatchResponse toResponse(StockReceiptRequest request, StockMovementBatchResult result) {
        List<Long> ids = result.movements().stream().map(StockMovement::getId).toList();
        return new StockMovementBatchResponse(
                request.referenceType(),
                request.referenceId(),
                result.postedCount(),
                result.alreadyExisted(),
                ids);
    }
}
