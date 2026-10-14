package com.erp.business.sales.delivery;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.sales.delivery.domain.DeliveryStatus;
import com.erp.business.sales.delivery.domain.SalesDelivery;
import com.erp.business.sales.delivery.domain.SalesDeliveryLine;
import com.erp.business.sales.delivery.dto.CreateDeliveryRequest;
import com.erp.business.sales.delivery.dto.DeliveryDecisionRequest;
import com.erp.business.sales.delivery.dto.DeliveryLineRequest;
import com.erp.business.sales.delivery.dto.SalesDeliveryResponse;
import com.erp.business.sales.delivery.exception.SalesDeliveryNotFoundException;
import com.erp.business.sales.delivery.infrastructure.persistence.SalesDeliveryRepository;
import com.erp.business.sales.delivery.mapper.SalesDeliveryMapper;
import com.erp.business.sales.salesorder.SalesOrderService;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.SalesDeliveryPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Sales delivery use-case implementation.
 *
 * <p>Posting a delivery is the coordination point of the sell side: it issues the
 * outbound stock in Inventory and advances the sales order's delivery progress, all
 * inside one transaction, so a partial failure leaves neither a half-issued delivery
 * nor a stock movement without its order update. Cancelling reverses both. The
 * aggregate owns its own status; this service owns the cross-aggregate wiring.
 */
@Service
@Transactional
public class SalesDeliveryServiceImpl implements SalesDeliveryService {

    private static final String DOCUMENT_TYPE = "SALES_DELIVERY";
    private static final String NUMBER_PREFIX = "DEL-";
    private static final String STOCK_REFERENCE_TYPE = "SALES_DELIVERY";

    private final SalesDeliveryRepository deliveryRepository;
    private final SalesOrderService salesOrderService;
    private final ItemService itemService;
    private final StockMovementService stockMovementService;
    private final SalesDeliveryMapper deliveryMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public SalesDeliveryServiceImpl(SalesDeliveryRepository deliveryRepository,
                                    SalesOrderService salesOrderService,
                                    ItemService itemService,
                                    StockMovementService stockMovementService,
                                    SalesDeliveryMapper deliveryMapper,
                                    DocumentNumberService documentNumberService,
                                    BusinessContextAccessor context) {
        this.deliveryRepository = deliveryRepository;
        this.salesOrderService = salesOrderService;
        this.itemService = itemService;
        this.stockMovementService = stockMovementService;
        this.deliveryMapper = deliveryMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_CREATE)
    public SalesDeliveryResponse createDelivery(Long tenantId, Long companyId, CreateDeliveryRequest request) {
        SalesOrderResponse order = salesOrderService.getOrderById(tenantId, companyId, request.salesOrderId());

        if (order.status() != com.erp.business.sales.salesorder.domain.SalesOrderStatus.CONFIRMED
                && order.status() != com.erp.business.sales.salesorder.domain.SalesOrderStatus.PARTIALLY_DELIVERED) {
            throw new IllegalStateException("Cannot deliver goods against Sales Order " + order.soNumber()
                    + ": Order is in " + order.status() + " status. Sales orders must be Confirmed before dispatching goods.");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Delivery must contain at least one line item.");
        }

        java.util.Map<Integer, com.erp.business.sales.salesorder.dto.SalesOrderLineResponse> orderLinesByNumber =
                order.lines().stream().collect(java.util.stream.Collectors.toMap(
                        com.erp.business.sales.salesorder.dto.SalesOrderLineResponse::lineNumber, l -> l));

        for (DeliveryLineRequest lineRequest : request.lines()) {
            com.erp.business.sales.salesorder.dto.SalesOrderLineResponse orderLine =
                    orderLinesByNumber.get(lineRequest.salesOrderLineNumber());
            if (orderLine == null) {
                throw new IllegalArgumentException("Line " + lineRequest.salesOrderLineNumber()
                        + " does not exist on Sales Order " + order.soNumber() + ".");
            }
            if (lineRequest.quantity() == null || lineRequest.quantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Delivery quantity must be greater than zero for item "
                        + orderLine.itemCode() + " (" + orderLine.itemName() + ").");
            }
            if (lineRequest.quantity().compareTo(orderLine.outstandingQuantity()) > 0) {
                throw new IllegalStateException("Delivery quantity (" + lineRequest.quantity()
                        + ") exceeds the remaining outstanding quantity (" + orderLine.outstandingQuantity()
                        + ") for item " + orderLine.itemCode() + " (" + orderLine.itemName()
                        + ") on Sales Order " + order.soNumber() + ".");
            }
        }

        String periodKey = context.findFiscalYearId() != null
                ? "FY" + context.findFiscalYearId() : "CY" + request.deliveryDate().getYear();
        String deliveryNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        SalesDelivery delivery = SalesDelivery.draft(
                deliveryNumber, order.id(), order.soNumber(),
                order.customerId(), order.customerCode(), order.customerName(),
                request.warehouseId(), order.warehouseCode(), order.warehouseName(),
                request.deliveryDate(), order.fiscalYearId(), order.branchId(), request.notes());
        delivery.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (DeliveryLineRequest lineRequest : request.lines()) {
            delivery.addLine(buildLine(lineNumber++, lineRequest));
        }

        return deliveryMapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public SalesDeliveryResponse getDeliveryById(Long tenantId, Long companyId, Long id) {
        return deliveryMapper.toResponse(loadDelivery(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public SalesDeliveryResponse getDeliveryByNumber(Long tenantId, Long companyId, String deliveryNumber) {
        SalesDelivery delivery = deliveryRepository
                .findByTenantIdAndCompanyIdAndDeliveryNumber(tenantId, companyId, deliveryNumber)
                .orElseThrow(() -> new SalesDeliveryNotFoundException(
                        "Sales delivery not found with number: " + deliveryNumber));
        return deliveryMapper.toResponse(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public Page<SalesDeliveryResponse> listDeliveries(Long tenantId, Long companyId, Pageable pageable) {
        return deliveryRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(deliveryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public Page<SalesDeliveryResponse> listDeliveriesByStatus(Long tenantId, Long companyId,
                                                              DeliveryStatus status, Pageable pageable) {
        return deliveryRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(deliveryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public Page<SalesDeliveryResponse> listDeliveriesByOrder(Long tenantId, Long companyId,
                                                             Long salesOrderId, Pageable pageable) {
        return deliveryRepository.findByTenantIdAndCompanyIdAndSalesOrderId(tenantId, companyId, salesOrderId, pageable)
                .map(deliveryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_VIEW)
    public Page<SalesDeliveryResponse> searchDeliveries(Long tenantId, Long companyId, String term, Pageable pageable) {
        return deliveryRepository.searchByNumber(tenantId, companyId, term, pageable)
                .map(deliveryMapper::toResponse);
    }

    @Override
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_POST)
    public SalesDeliveryResponse postDelivery(Long tenantId, Long companyId, Long id) {
        SalesDelivery delivery = loadDelivery(tenantId, companyId, id);
        delivery.post(currentUser());
        SalesDelivery saved = deliveryRepository.save(delivery);

        // Issue the outbound stock, then advance the order's delivery progress.
        stockMovementService.postIssue(buildStockRequest(saved));
        for (SalesDeliveryLine line : saved.getLines()) {
            salesOrderService.recordDelivery(tenantId, companyId, saved.getSalesOrderId(),
                    line.getSalesOrderLineNumber(), line.getQuantity());
        }
        return deliveryMapper.toResponse(saved);
    }

    @Override
    @RequiresPermission(SalesDeliveryPermissions.SALES_DELIVERY_CANCEL)
    public SalesDeliveryResponse cancelDelivery(Long tenantId, Long companyId, Long id,
                                                DeliveryDecisionRequest request) {
        SalesDelivery delivery = loadDelivery(tenantId, companyId, id);
        boolean wasPosted = delivery.getStatus() == DeliveryStatus.POSTED;
        delivery.cancel(currentUser(), request.reason());
        SalesDelivery saved = deliveryRepository.save(delivery);

        // Reverse the outbound stock, then roll back the order's delivery progress only if it was posted.
        if (wasPosted) {
            stockMovementService.reverseIssue(buildStockRequest(saved));
            for (SalesDeliveryLine line : saved.getLines()) {
                salesOrderService.reverseDelivery(tenantId, companyId, saved.getSalesOrderId(),
                        line.getSalesOrderLineNumber(), line.getQuantity());
            }
        }
        return deliveryMapper.toResponse(saved);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SalesDelivery loadDelivery(Long tenantId, Long companyId, Long id) {
        return deliveryRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new SalesDeliveryNotFoundException(
                        "Sales delivery not found with ID: " + id));
    }

    private SalesDeliveryLine buildLine(int lineNumber, DeliveryLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));
        return SalesDeliveryLine.create(
                lineNumber, request.salesOrderLineNumber(), item.getId(), item.getSku(),
                item.getName(), request.uomCode(), request.quantity(), request.description());
    }

    /**
     * Builds the idempotent outbound stock request for this delivery. The
     * {@code (referenceType, referenceId)} pair is the idempotency key, so a retry
     * after a timeout returns the movements already posted rather than duplicating.
     */
    private StockReceiptRequest buildStockRequest(SalesDelivery delivery) {
        List<StockMovementLineRequest> stockLines = delivery.getLines().stream()
                .map(line -> new StockMovementLineRequest(
                        delivery.getWarehouseId(), line.getItemId(), line.getUomCode(), line.getQuantity()))
                .toList();
        return new StockReceiptRequest(
                STOCK_REFERENCE_TYPE, delivery.getId(), delivery.getDeliveryNumber(),
                Instant.now(), currentUser(), null, stockLines);
    }

    private String currentUser() {
        return context.getUserId();
    }
}
