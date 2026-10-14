package com.erp.business.pos.transaction;

import com.erp.business.pos.domain.PosTransactionStatus;
import com.erp.business.pos.transaction.dto.CheckoutLineRequest;
import com.erp.business.pos.transaction.dto.CheckoutRequest;
import com.erp.business.pos.transaction.dto.ParkTransactionRequest;
import com.erp.business.pos.transaction.dto.PosReceiptResponse;
import com.erp.business.pos.transaction.dto.PosTransactionResponse;
import com.erp.business.pos.transaction.dto.UpdateLineRequest;
import com.erp.business.pos.transaction.dto.VoidTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * POS transaction (checkout) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from
 * the payload. Authorisation is declared with {@code @RequiresPermission} on
 * the implementation and enforced by the shared aspect before the body runs.
 *
 * <p><strong>Checkout is one atomic call.</strong> Re-pricing, number
 * allocation, stock issue and the session's running totals all commit or all
 * roll back together — a half-rung sale can never exist.
 */
public interface PosTransactionService {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /**
     * Completes a sale in one atomic call: re-prices every line server-side,
     * allocates the document number, deducts stock from the register's
     * warehouse and updates the session totals.
     */
    PosTransactionResponse checkout(Long tenantId, Long companyId, CheckoutRequest request);

    /**
     * Parks a cart as an OPEN transaction so it can be resumed later. Takes no
     * payment, deducts no stock and allocates no document number.
     */
    PosTransactionResponse parkTransaction(Long tenantId, Long companyId, ParkTransactionRequest request);

    /**
     * Adds a re-priced line to a parked (OPEN) transaction.
     */
    PosTransactionResponse addLine(Long tenantId, Long companyId, Long transactionId,
                                   CheckoutLineRequest request);

    /**
     * Updates the quantity and/or discount of one line on a parked (OPEN)
     * transaction.
     */
    PosTransactionResponse updateLine(Long tenantId, Long companyId, Long transactionId,
                                      int lineNumber, UpdateLineRequest request);

    /**
     * Removes one line from a parked (OPEN) transaction.
     */
    PosTransactionResponse removeLine(Long tenantId, Long companyId, Long transactionId, int lineNumber);

    /**
     * Voids an OPEN or COMPLETED transaction. A completed sale additionally
     * has its stock issue reversed and the session totals rolled back.
     */
    PosTransactionResponse voidTransaction(Long tenantId, Long companyId, Long id,
                                           VoidTransactionRequest request);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    PosTransactionResponse getTransactionById(Long tenantId, Long companyId, Long id);

    PosTransactionResponse getTransactionByNumber(Long tenantId, Long companyId, String transactionNumber);

    /**
     * Builds the printable receipt payload: the transaction plus the session
     * and register context it was rung on.
     */
    PosReceiptResponse getReceipt(Long tenantId, Long companyId, Long id);

    Page<PosTransactionResponse> listTransactions(Long tenantId, Long companyId, Pageable pageable);

    Page<PosTransactionResponse> listTransactionsByStatus(Long tenantId, Long companyId,
                                                          PosTransactionStatus status, Pageable pageable);

    Page<PosTransactionResponse> listTransactionsBySession(Long tenantId, Long companyId,
                                                           Long sessionId, Pageable pageable);

    Page<PosTransactionResponse> listTransactionsByRegister(Long tenantId, Long companyId,
                                                            Long registerId, Pageable pageable);

    Page<PosTransactionResponse> searchTransactions(Long tenantId, Long companyId, String term,
                                                    Pageable pageable);
}
