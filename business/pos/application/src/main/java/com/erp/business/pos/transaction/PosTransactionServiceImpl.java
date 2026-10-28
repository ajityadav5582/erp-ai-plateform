package com.erp.business.pos.transaction;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.platform.data.tx.TransactionCallbacks;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.pos.domain.PosRegister;
import com.erp.business.pos.domain.PosSession;
import com.erp.business.pos.domain.PosSessionStatus;
import com.erp.business.pos.domain.PosCheckoutIdempotency;
import com.erp.business.pos.domain.PosKitchenTicket;
import com.erp.business.pos.domain.PosSalesIntegrationJob;
import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.pos.domain.PosTransactionStatus;
import com.erp.business.pos.pricing.PosPricingService;
import com.erp.business.pos.pricing.PricedLine;
import com.erp.business.pos.register.exception.PosRegisterNotFoundException;
import com.erp.business.pos.register.infrastructure.persistence.PosRegisterRepository;
import com.erp.business.pos.session.exception.PosSessionNotFoundException;
import com.erp.business.pos.session.infrastructure.persistence.PosSessionRepository;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.pos.shared.numbering.DocumentNumberService;
import com.erp.business.pos.shared.security.PosTransactionPermissions;
import com.erp.business.pos.transaction.dto.CheckoutLineRequest;
import com.erp.business.pos.transaction.dto.CheckoutRequest;
import com.erp.business.pos.transaction.dto.ParkTransactionRequest;
import com.erp.business.pos.transaction.dto.PosReceiptResponse;
import com.erp.business.pos.transaction.dto.PosTransactionResponse;
import com.erp.business.pos.transaction.dto.UpdateLineRequest;
import com.erp.business.pos.transaction.dto.VoidTransactionRequest;
import com.erp.business.pos.transaction.exception.PosTransactionNotFoundException;
import com.erp.business.pos.integration.PosSalesIntegrationJobRepository;
import com.erp.business.pos.kitchen.PosKitchenTicketRepository;
import com.erp.business.pos.transaction.infrastructure.persistence.PosCheckoutIdempotencyRepository;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import com.erp.business.pos.transaction.mapper.PosTransactionMapper;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * POS checkout use-case implementation.
 *
 * <p><strong>Checkout is the coordination point of the sell side.</strong> One
 * atomic call re-prices every line server-side, allocates the gap-free document
 * number, issues the outbound stock in Inventory and advances the session's
 * running totals — all inside a single {@code @Transactional} method, so a
 * partial failure leaves neither a half-rung sale nor a stock movement without
 * its transaction. Voiding reverses both, mirroring
 * {@code SalesDeliveryServiceImpl}.
 *
 * <p><strong>Server-authoritative pricing.</strong> No unit price is ever read
 * from the request: each line is re-resolved from the Inventory catalogue
 * through {@link PosPricingService} and computed by the domain, so a tampered
 * client cannot write its own price and a price that drifted between
 * add-to-cart and pay is silently corrected.
 *
 * <p><strong>Numbers are allocated on completion only.</strong> A parked or
 * abandoned cart never consumes a number from the IRD-compliant sequential
 * series.
 */
@Service
@Transactional
public class PosTransactionServiceImpl implements PosTransactionService {

    private static final String DOCUMENT_TYPE = "POS_TRANSACTION";
    private static final String NUMBER_PREFIX = "POS-";
    private static final String STOCK_REFERENCE_TYPE = "POS_TRANSACTION";

    private final PosTransactionRepository transactionRepository;
    private final PosCheckoutIdempotencyRepository idempotencyRepository;
    private final PosSalesIntegrationJobRepository integrationJobRepository;
    private final PosKitchenTicketRepository kitchenTicketRepository;
    private final PosSessionRepository sessionRepository;
    private final PosRegisterRepository registerRepository;
    private final PosPricingService pricingService;
    private final ItemService itemService;
    private final StockMovementService stockMovementService;
    private final PosTransactionMapper transactionMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public PosTransactionServiceImpl(PosTransactionRepository transactionRepository,
                                     PosCheckoutIdempotencyRepository idempotencyRepository,
                                     PosSalesIntegrationJobRepository integrationJobRepository,
                                     PosKitchenTicketRepository kitchenTicketRepository,
                                     PosSessionRepository sessionRepository,
                                     PosRegisterRepository registerRepository,
                                     PosPricingService pricingService,
                                     ItemService itemService,
                                     StockMovementService stockMovementService,
                                     PosTransactionMapper transactionMapper,
                                     DocumentNumberService documentNumberService,
                                     BusinessContextAccessor context) {
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.integrationJobRepository = integrationJobRepository;
        this.kitchenTicketRepository = kitchenTicketRepository;
        this.sessionRepository = sessionRepository;
        this.registerRepository = registerRepository;
        this.pricingService = pricingService;
        this.itemService = itemService;
        this.stockMovementService = stockMovementService;
        this.transactionMapper = transactionMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    // -----------------------------------------------------------------------
    // Checkout
    // -----------------------------------------------------------------------

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_CHECKOUT)
    public PosTransactionResponse checkout(Long tenantId, Long companyId, String idempotencyKey, CheckoutRequest request) {
        String userId = context.getUserId();

        // Idempotency: if a key is supplied, look for a prior attempt scoped to
        // this tenant/company/cashier. A matching payload returns the original
        // result; a differing payload is rejected.
        PosCheckoutIdempotency idempotencyRecord = null;
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = hashRequest(request);
            java.util.Optional<PosCheckoutIdempotency> existing =
                    idempotencyRepository.findByTenantIdAndCompanyIdAndUserIdAndIdempotencyKey(
                            tenantId, companyId, userId, idempotencyKey);
            if (existing.isPresent()) {
                PosCheckoutIdempotency prior = existing.get();
                if (!requestHash.equals(prior.getRequestHash())) {
                    throw new IllegalArgumentException(
                            "Idempotency key reused with a different checkout payload");
                }
                if (prior.getTransactionId() != null) {
                    return transactionMapper.toResponse(
                            loadTransaction(tenantId, companyId, prior.getTransactionId()));
                }
                throw new IllegalStateException("Checkout with this idempotency key is already in progress");
            }
            idempotencyRecord = PosCheckoutIdempotency.builder()
                    .userId(userId)
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status("IN_PROGRESS")
                    .build();
            idempotencyRecord.assignScope(tenantId, companyId);
            try {
                idempotencyRepository.saveAndFlush(idempotencyRecord);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Concurrent request won the race; reconcile with its result.
                PosCheckoutIdempotency concurrent = idempotencyRepository
                        .findByTenantIdAndCompanyIdAndUserIdAndIdempotencyKey(
                                tenantId, companyId, userId, idempotencyKey)
                        .orElseThrow(() -> ex);
                if (!requestHash.equals(concurrent.getRequestHash())) {
                    throw new IllegalArgumentException(
                            "Idempotency key reused with a different checkout payload");
                }
                if (concurrent.getTransactionId() != null) {
                    return transactionMapper.toResponse(
                            loadTransaction(tenantId, companyId, concurrent.getTransactionId()));
                }
                throw new IllegalStateException("Checkout with this idempotency key is already in progress");
            }
        }

        PosSession session = loadSession(tenantId, companyId, request.sessionId());
        PosRegister register = loadRegister(tenantId, companyId, session.getRegisterId());

        LocalDate transactionDate = LocalDate.now();
        PosTransaction transaction = PosTransaction.open(
                session.getId(), register.getId(),
                register.getWarehouseId(), register.getWarehouseCode(), register.getWarehouseName(),
                context.getUserId(), context.getCurrentUserFullName(),
                transactionDate, NepaliDateConverter.format(transactionDate),
                session.getFiscalYearId(), register.getBranchId(), request.tableId(),
                request.customerId(), null, request.customerName(), request.customerPanNumber(),
                null, request.notes());
        transaction.assignScope(tenantId, companyId);

        // Persist first: the aggregate needs its id before it can own lines.
        PosTransaction saved = transactionRepository.save(transaction);
        int lineNumber = 1;
        for (CheckoutLineRequest lineRequest : request.lines()) {
            saved.addLine(buildLine(lineNumber++, lineRequest));
        }

        // Allocate the number, then deduct stock, then complete — the domain
        // contract. All three share this transaction, so a failure at any step
        // rolls the whole sale back, number allocation included.
        String transactionNumber = allocateNumber(tenantId, companyId, saved);
        StockReceiptRequest stockRequest = buildStockRequest(saved, transactionNumber);
        if (!stockRequest.lines().isEmpty()) {
            stockMovementService.postIssue(stockRequest);
            // If completing the sale or saving the session fails and the checkout rolls
            // back, the stock was already deducted in Inventory - reverse it so the
            // ledger and the (absent) sale stay consistent. Idempotent by
            // (POS_TRANSACTION, id, IN).
            TransactionCallbacks.onRollback(() -> stockMovementService.reverseIssue(stockRequest));
        }

        saved.complete(transactionNumber, request.paymentMethod(), request.amountTendered(),
                request.paymentReference(), context.getUserId());
        PosTransaction completed = transactionRepository.save(saved);

        session.recordCompletedTransaction(
                completed.getPaymentMethod(), completed.getTotalAmount(), completed.getVatAmount());
        sessionRepository.save(session);

        if (idempotencyRecord != null) {
            idempotencyRecord.complete(completed.getId());
            idempotencyRepository.save(idempotencyRecord);
        }

        // Link kitchen ticket for restaurant-style flows when a table is selected.
        if (request.tableId() != null) {
            PosKitchenTicket ticket = PosKitchenTicket.builder()
                    .posTransactionId(completed.getId())
                    .status("PENDING")
                    .build();
            ticket.assignScope(tenantId, companyId);
            PosKitchenTicket savedTicket = kitchenTicketRepository.save(ticket);
            completed.setKitchenTicketId(savedTicket.getId());
            transactionRepository.save(completed);
        }

        // Durable integration outbox: one job per completed POS transaction.
        if (integrationJobRepository.findByTenantIdAndCompanyIdAndPosTransactionId(
                tenantId, companyId, completed.getId()).isEmpty()) {
            PosSalesIntegrationJob job = PosSalesIntegrationJob.builder()
                    .posTransactionId(completed.getId())
                    .status("PENDING")
                    .attemptCount(0)
                    .nextRetryAt(Instant.now())
                    .build();
            job.assignScope(tenantId, companyId);
            integrationJobRepository.save(job);
        }

        // Integration is handled by PosSalesIntegrationWorker (transactional outbox).

        return transactionMapper.toResponse(completed);
    }

    // -----------------------------------------------------------------------
    // Park / resume (an OPEN transaction persisted for later)
    // -----------------------------------------------------------------------

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_CHECKOUT)
    public PosTransactionResponse parkTransaction(Long tenantId, Long companyId, ParkTransactionRequest request) {
        PosSession session = loadSession(tenantId, companyId, request.sessionId());
        PosRegister register = loadRegister(tenantId, companyId, session.getRegisterId());

        LocalDate transactionDate = LocalDate.now();
        PosTransaction transaction = PosTransaction.open(
                session.getId(), register.getId(),
                register.getWarehouseId(), register.getWarehouseCode(), register.getWarehouseName(),
                context.getUserId(), context.getCurrentUserFullName(),
                transactionDate, NepaliDateConverter.format(transactionDate),
                session.getFiscalYearId(), register.getBranchId(), null,
                request.customerId(), null, request.customerName(), request.customerPanNumber(),
                null, request.notes());
        transaction.assignScope(tenantId, companyId);

        PosTransaction saved = transactionRepository.save(transaction);
        int lineNumber = 1;
        for (CheckoutLineRequest lineRequest : request.lines()) {
            saved.addLine(buildLine(lineNumber++, lineRequest));
        }

        return transactionMapper.toResponse(transactionRepository.save(saved));
    }

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_CHECKOUT)
    public PosTransactionResponse addLine(Long tenantId, Long companyId, Long transactionId,
                                          CheckoutLineRequest request) {
        PosTransaction transaction = loadTransaction(tenantId, companyId, transactionId);
        int nextLineNumber = transaction.getLines().stream()
                .mapToInt(PosTransactionLine::getLineNumber)
                .max()
                .orElse(0) + 1;
        transaction.addLine(buildLine(nextLineNumber, request));
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_CHECKOUT)
    public PosTransactionResponse updateLine(Long tenantId, Long companyId, Long transactionId,
                                             int lineNumber, UpdateLineRequest request) {
        if (request.quantity() == null && request.discountPercentage() == null) {
            throw new IllegalArgumentException(
                    "Provide a quantity and/or a discount percentage to update the line.");
        }
        PosTransaction transaction = loadTransaction(tenantId, companyId, transactionId);
        if (request.quantity() != null) {
            transaction.updateLineQuantity(lineNumber, request.quantity());
        }
        if (request.discountPercentage() != null) {
            transaction.applyLineDiscount(lineNumber, request.discountPercentage());
        }
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_CHECKOUT)
    public PosTransactionResponse removeLine(Long tenantId, Long companyId, Long transactionId, int lineNumber) {
        PosTransaction transaction = loadTransaction(tenantId, companyId, transactionId);
        transaction.removeLine(lineNumber);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    // -----------------------------------------------------------------------
    // Void
    // -----------------------------------------------------------------------

    @Override
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VOID)
    public PosTransactionResponse voidTransaction(Long tenantId, Long companyId, Long id,
                                                  VoidTransactionRequest request) {
        PosTransaction transaction = loadTransaction(tenantId, companyId, id);
        boolean wasCompleted = transaction.getStatus() == PosTransactionStatus.COMPLETED;

        // A completed sale has stock out and session totals up: reverse both
        // before the aggregate flips to VOIDED. An OPEN cart has neither.
        if (wasCompleted) {
            PosSession session = loadSession(tenantId, companyId, transaction.getSessionId());
            stockMovementService.reverseIssue(buildStockRequest(transaction, transaction.getTransactionNumber()));
            session.recordVoidedTransaction(
                    transaction.getPaymentMethod(), transaction.getTotalAmount(), transaction.getVatAmount());
            sessionRepository.save(session);
        }

        transaction.voidTransaction(context.getUserId(), request.reason());
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    // -----------------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public PosTransactionResponse getTransactionById(Long tenantId, Long companyId, Long id) {
        return transactionMapper.toResponse(loadTransaction(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public PosTransactionResponse getTransactionByNumber(Long tenantId, Long companyId, String transactionNumber) {
        PosTransaction transaction = transactionRepository
                .findByTenantIdAndCompanyIdAndTransactionNumber(tenantId, companyId, transactionNumber)
                .orElseThrow(() -> new PosTransactionNotFoundException(
                        "Transaction not found with number: " + transactionNumber));
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public PosReceiptResponse getReceipt(Long tenantId, Long companyId, Long id) {
        PosTransaction transaction = loadTransaction(tenantId, companyId, id);
        PosSession session = loadSession(tenantId, companyId, transaction.getSessionId());
        return new PosReceiptResponse(
                transactionMapper.toResponse(transaction),
                session.getSessionNumber(),
                session.getRegisterCode(),
                session.getRegisterName());
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public Page<PosTransactionResponse> listTransactions(Long tenantId, Long companyId, Pageable pageable) {
        return transactionRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public Page<PosTransactionResponse> listTransactionsByStatus(Long tenantId, Long companyId,
                                                                 PosTransactionStatus status,
                                                                 Pageable pageable) {
        return transactionRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public Page<PosTransactionResponse> listTransactionsBySession(Long tenantId, Long companyId,
                                                                  Long sessionId, Pageable pageable) {
        return transactionRepository.findByTenantIdAndCompanyIdAndSessionId(tenantId, companyId, sessionId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public Page<PosTransactionResponse> listTransactionsByRegister(Long tenantId, Long companyId,
                                                                   Long registerId, Pageable pageable) {
        return transactionRepository.findByTenantIdAndCompanyIdAndRegisterId(tenantId, companyId, registerId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public Page<PosTransactionResponse> searchTransactions(Long tenantId, Long companyId, String term,
                                                           Pageable pageable) {
        return transactionRepository.search(tenantId, companyId, term, pageable)
                .map(transactionMapper::toResponse);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PosTransaction loadTransaction(Long tenantId, Long companyId, Long id) {
        return transactionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosTransactionNotFoundException(
                        "Transaction not found with ID: " + id));
    }

    private PosSession loadSession(Long tenantId, Long companyId, Long id) {
        PosSession session = sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosSessionNotFoundException("Session not found with ID: " + id));
        if (session.getStatus() != PosSessionStatus.OPEN) {
            throw new IllegalStateException(
                    "Session '" + session.getSessionNumber() + "' is " + session.getStatus()
                            + ". Sales can only be rung on an OPEN session.");
        }
        return session;
    }

    private PosRegister loadRegister(Long tenantId, Long companyId, Long id) {
        return registerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosRegisterNotFoundException("Register not found with ID: " + id));
    }

    /**
     * Re-prices one requested line server-side and freezes the snapshot onto a
     * domain line. The unit price and VAT come from the catalogue via
     * {@link PosPricingService}; the amounts are computed by the domain.
     *
     * <p>The sellable guard runs first: a RAW_MATERIAL, CONSUMABLE, or SEMI_FINISHED
     * item cannot be added to a customer receipt regardless of what the client sends.
     */
    private PosTransactionLine buildLine(int lineNumber, CheckoutLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));

        // Enforce the sellable flag: only items that have been explicitly enabled
        // for sale may appear on a POS transaction. This prevents staff from
        // accidentally selling Sugar or Tea Leaf directly to a customer when it
        // should only be consumed internally through a recipe.
        if (!Boolean.TRUE.equals(item.getIsSellable())) {
            throw new IllegalArgumentException(
                    "Item '" + item.getName() + "' (ID: " + request.itemId()
                            + ") is not marked as sellable and cannot be added to a POS transaction.");
        }

        PricedLine priced = pricingService.priceLine(item, request.quantity(), request.discountPercentage());
        return PosTransactionLine.of(
                lineNumber, priced.itemId(), priced.itemCode(), priced.itemName(), item.getBarcode(),
                priced.uomCode(), priced.quantity(), priced.unitPrice(), priced.discountPercentage(),
                priced.vatType(), priced.vatRate(), null);
    }

    /**
     * Allocates the next gap-free transaction number for the sale's fiscal
     * period. The period key follows the session service's convention so the
     * numbering series is consistent across POS documents.
     */
    private String allocateNumber(Long tenantId, Long companyId, PosTransaction transaction) {
        Long fiscalYearId = transaction.getFiscalYearId();
        String periodKey = fiscalYearId != null
                ? "FY" + fiscalYearId : "CY" + transaction.getTransactionDate().getYear();
        return documentNumberService.nextNumber(tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);
    }

    /**
     * Builds the idempotent outbound stock request for this transaction. The
     * {@code (referenceType, referenceId)} pair is the idempotency key shared by
     * the issue and its reversal, so a retried checkout after a timeout returns
     * the movements already posted rather than deducting stock twice.
     */
    private StockReceiptRequest buildStockRequest(PosTransaction transaction, String transactionNumber) {
        // Only inventory-tracked items consume stock. Non-tracked items (services,
        // and products such as tea that are produced internally and never purchased)
        // are sold without a stock movement, so a zero on-hand balance never blocks
        // the sale. Unknown items are skipped rather than posted, so a stale catalogue
        // reference cannot create a phantom stock movement.
        List<StockMovementLineRequest> stockLines = transaction.getLines().stream()
                .filter(line -> {
                    ItemService.ItemResponse item = itemService.findById(line.getItemId()).orElse(null);
                    return item != null && Boolean.TRUE.equals(item.getIsStockable());
                })
                .map(line -> new StockMovementLineRequest(
                        transaction.getWarehouseId(), line.getItemId(), line.getUomCode(), line.getQuantity()))
                .toList();
        // The number is allocated before stock is posted, so use the freshly
        // allocated value rather than transaction.getTransactionNumber(), which is
        // only set when the aggregate is completed (after this point).
        String referenceNumber = transactionNumber != null ? transactionNumber : transaction.getTransactionNumber();
        return new StockReceiptRequest(
                STOCK_REFERENCE_TYPE, transaction.getId(), referenceNumber,
                Instant.now(), context.getUserId(), null, stockLines);
    }

    private String hashRequest(CheckoutRequest request) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(request);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash checkout request", ex);
        }
    }
}
