package com.erp.business.sales.salesorder;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.infrastructure.persistence.CustomerRepository;
import com.erp.business.sales.salesorder.domain.SalesOrder;
import com.erp.business.sales.salesorder.domain.SalesOrderLine;
import com.erp.business.sales.salesorder.domain.SalesOrderStatus;
import com.erp.business.sales.salesorder.dto.CreateSalesOrderRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderDecisionRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderLineRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import com.erp.business.sales.salesorder.dto.UpdateSalesOrderRequest;
import com.erp.business.sales.salesorder.exception.SalesOrderNotFoundException;
import com.erp.business.sales.salesorder.infrastructure.persistence.SalesOrderRepository;
import com.erp.business.sales.salesorder.mapper.SalesOrderMapper;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.SalesOrderPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales order use-case implementation.
 *
 * <p>Authorisation is declared per method with {@code @RequiresPermission} and
 * enforced by the shared aspect before the body runs. Customer and item references
 * are resolved and their display fields frozen onto the order, so a later rename
 * never rewrites a confirmed order. The document number is allocated inside the
 * same transaction as the insert, keeping the series gap-free.
 */
@Service
@Transactional
public class SalesOrderServiceImpl implements SalesOrderService {

    private static final String DOCUMENT_TYPE = "SALES_ORDER";
    private static final String NUMBER_PREFIX = "SO-";

    private final SalesOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final SalesOrderMapper orderMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public SalesOrderServiceImpl(SalesOrderRepository orderRepository,
                                 CustomerRepository customerRepository,
                                 ItemService itemService,
                                 SalesOrderMapper orderMapper,
                                 DocumentNumberService documentNumberService,
                                 BusinessContextAccessor context) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.itemService = itemService;
        this.orderMapper = orderMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CREATE)
    public SalesOrderResponse createOrder(Long tenantId, Long companyId, CreateSalesOrderRequest request) {
        Customer customer = customerRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.customerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with ID: " + request.customerId()));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalStateException("Customer '" + customer.getName() + " (" + customer.getCode()
                    + ")' is inactive. Cannot create sales orders for an inactive customer.");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Sales order must contain at least one line item.");
        }

        for (SalesOrderLineRequest lineReq : request.lines()) {
            if (lineReq.quantity() == null || lineReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Line quantity must be greater than zero.");
            }
            if (lineReq.unitPrice() != null && lineReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must not be negative.");
            }
        }

        String periodKey = resolvePeriodKey(request.orderDate());
        String soNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        SalesOrder order = SalesOrder.draft(
                soNumber, request.orderDate(), customer.getId(), customer.getCode(),
                customer.getName(), customer.getPanNumber(), customer.getCurrency(),
                context.findFiscalYearId(), request.branchId(), request.warehouseId(),
                null, null, request.paymentTermsDays(), request.reference(), request.notes());
        // The draft factory sets the business fields but not the tenant/company
        // scope; addLine propagates getTenantId()/getCompanyId() onto each line,
        // so the header must be scoped first or the first addLine throws
        // "Cannot assign a null tenant scope".
        order.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (SalesOrderLineRequest lineRequest : request.lines()) {
            order.addLine(buildLine(lineNumber++, lineRequest));
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesOrderResponse updateOrder(Long tenantId, Long companyId, Long id, UpdateSalesOrderRequest request) {
        SalesOrder order = loadOrder(tenantId, companyId, id);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new IllegalStateException("Sales Order " + order.getSoNumber()
                    + " is in " + order.getStatus() + " status and cannot be modified. Only DRAFT orders can be edited.");
        }

        Customer customer = customerRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.customerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with ID: " + request.customerId()));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalStateException("Customer '" + customer.getName() + " (" + customer.getCode()
                    + ")' is inactive. Cannot assign an inactive customer.");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Sales order must contain at least one line item.");
        }

        for (SalesOrderLineRequest lineReq : request.lines()) {
            if (lineReq.quantity() == null || lineReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Line quantity must be greater than zero.");
            }
            if (lineReq.unitPrice() != null && lineReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must not be negative.");
            }
        }

        order.updateDraft(
                request.orderDate(), request.expectedDeliveryDate(),
                request.branchId(), request.warehouseId(), null, null,
                request.paymentTermsDays(), request.reference(), request.notes());

        order.updateCustomer(
                customer.getId(), customer.getCode(), customer.getName(),
                customer.getPanNumber(), customer.getCurrency());

        int lineNumber = 1;
        List<SalesOrderLine> newLines = new ArrayList<>();
        for (SalesOrderLineRequest lineRequest : request.lines()) {
            newLines.add(buildLine(lineNumber++, lineRequest));
        }
        order.replaceLines(newLines);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public SalesOrderResponse getOrderById(Long tenantId, Long companyId, Long id) {
        return orderMapper.toResponse(loadOrder(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public SalesOrderResponse getOrderByNumber(Long tenantId, Long companyId, String soNumber) {
        SalesOrder order = orderRepository.findByTenantIdAndCompanyIdAndSoNumber(tenantId, companyId, soNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(
                        "Sales order not found with number: " + soNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesOrderResponse> listOrders(Long tenantId, Long companyId, Pageable pageable) {
        return orderRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesOrderResponse> listOrdersByStatus(Long tenantId, Long companyId,
                                                       SalesOrderStatus status, Pageable pageable) {
        return orderRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesOrderResponse> listOrdersByCustomer(Long tenantId, Long companyId,
                                                         Long customerId, Pageable pageable) {
        return orderRepository.findByTenantIdAndCompanyIdAndCustomerId(tenantId, companyId, customerId, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesOrderResponse> searchOrders(Long tenantId, Long companyId, String term, Pageable pageable) {
        return orderRepository.searchByNumber(tenantId, companyId, term, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public List<SalesOrderResponse> listOpenOrders(Long tenantId, Long companyId) {
        return orderRepository.findByTenantIdAndCompanyIdAndStatusInOrderByIdDesc(
                        tenantId, companyId,
                        List.of(SalesOrderStatus.CONFIRMED, SalesOrderStatus.PARTIALLY_DELIVERED))
                .stream().map(orderMapper::toResponse).toList();
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CONFIRM)
    public SalesOrderResponse confirmOrder(Long tenantId, Long companyId, Long id) {
        SalesOrder order = loadOrder(tenantId, companyId, id);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new IllegalStateException("Sales Order " + order.getSoNumber()
                    + " cannot be confirmed because it is already in " + order.getStatus() + " status.");
        }

        Customer customer = customerRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, order.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + order.getCustomerId()));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalStateException("Cannot confirm Sales Order " + order.getSoNumber()
                    + ": Customer '" + customer.getName() + " (" + customer.getCode() + ")' is inactive.");
        }

        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot confirm Sales Order " + order.getSoNumber()
                    + ": The order contains no line items.");
        }

        order.confirm(currentUser());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CANCEL)
    public SalesOrderResponse cancelOrder(Long tenantId, Long companyId, Long id,
                                          SalesOrderDecisionRequest request) {
        SalesOrder order = loadOrder(tenantId, companyId, id);

        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new IllegalStateException("Sales Order " + order.getSoNumber() + " is already cancelled.");
        }
        if (order.getStatus() == SalesOrderStatus.CLOSED) {
            throw new IllegalStateException("Sales Order " + order.getSoNumber() + " is closed and cannot be cancelled.");
        }
        if (order.getDeliveredAmount() != null && order.getDeliveredAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot cancel Sales Order " + order.getSoNumber()
                    + ": Goods have already been delivered (Delivered value: NPR " + order.getDeliveredAmount()
                    + "). You must cancel the associated delivery first.");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("A cancellation reason is required to cancel Sales Order " + order.getSoNumber() + ".");
        }

        order.cancel(currentUser(), request.reason());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CLOSE)
    public SalesOrderResponse closeOrder(Long tenantId, Long companyId, Long id) {
        SalesOrder order = loadOrder(tenantId, companyId, id);
        order.close(currentUser());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesOrderResponse recordDelivery(Long tenantId, Long companyId, Long orderId,
                                             int lineNumber, BigDecimal quantity) {
        SalesOrder order = loadOrder(tenantId, companyId, orderId);
        order.deliverAgainstLine(lineNumber, quantity);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesOrderResponse reverseDelivery(Long tenantId, Long companyId, Long orderId,
                                              int lineNumber, BigDecimal quantity) {
        SalesOrder order = loadOrder(tenantId, companyId, orderId);
        order.reverseDeliveryAgainstLine(lineNumber, quantity);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SalesOrder loadOrder(Long tenantId, Long companyId, Long id) {
        return orderRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new SalesOrderNotFoundException("Sales order not found with ID: " + id));
    }

    /**
     * Resolves the item and freezes its display fields onto a new order line.
     * The item must exist and be active in the caller's scope.
     */
    private SalesOrderLine buildLine(int lineNumber, SalesOrderLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));
        if (Boolean.FALSE.equals(item.getIsActive())) {
            throw new IllegalArgumentException("Item " + item.getSku() + " is not active");
        }
        return SalesOrderLine.create(
                lineNumber, item.getId(), item.getSku(), item.getName(),
                request.uomCode() != null ? request.uomCode() : item.getUnitCode(),
                request.quantity(), request.unitPrice(),
                request.discountPercentage(), request.taxRate(), request.description());
    }

    /**
     * The numbering period. Uses the selected fiscal year when the caller has one,
     * otherwise the order's calendar year, so the series is always scoped and never
     * collides across years.
     */
    private String resolvePeriodKey(java.time.LocalDate orderDate) {
        Long fiscalYearId = context.findFiscalYearId();
        return fiscalYearId != null ? "FY" + fiscalYearId : "CY" + orderDate.getYear();
    }

    private String currentUser() {
        return context.getUserId();
    }
}
