package com.erp.business.pos.transaction;

import com.erp.business.pos.domain.PosTransactionStatus;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.pos.transaction.dto.CheckoutLineRequest;
import com.erp.business.pos.transaction.dto.CheckoutRequest;
import com.erp.business.pos.transaction.dto.ParkTransactionRequest;
import com.erp.business.pos.transaction.dto.PosReceiptResponse;
import com.erp.business.pos.transaction.dto.PosTransactionResponse;
import com.erp.business.pos.transaction.dto.UpdateLineRequest;
import com.erp.business.pos.transaction.dto.VoidTransactionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * POS transaction endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, enforced by the shared aspect. The controller only
 * reads the authenticated tenant and company, so a caller cannot widen its own
 * scope through the payload.
 *
 * <p>Checkout is a single {@code POST} — the terminal sends the whole cart once
 * and the server re-prices, numbers, deducts stock and updates the session in
 * one atomic call.
 */
@RestController
@RequestMapping("/api/v1/pos/transactions")
@RequiredArgsConstructor
public class PosTransactionController {

    private static final String BASE_PATH = "/api/v1/pos/transactions";

    private final PosTransactionService transactionService;
    private final BusinessContextAccessor context;

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<PosTransactionResponse> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request,
            UriComponentsBuilder uriBuilder) {

        PosTransactionResponse response = transactionService.checkout(
                context.getTenantId(), context.getCompanyId(), idempotencyKey, request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @PostMapping("/park")
    public ResponseEntity<PosTransactionResponse> parkTransaction(
            @Valid @RequestBody ParkTransactionRequest request,
            UriComponentsBuilder uriBuilder) {

        PosTransactionResponse response = transactionService.parkTransaction(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<PosTransactionResponse> addLine(
            @PathVariable Long id, @Valid @RequestBody CheckoutLineRequest request) {
        return ResponseEntity.ok(transactionService.addLine(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PutMapping("/{id}/lines/{lineNumber}")
    public ResponseEntity<PosTransactionResponse> updateLine(
            @PathVariable Long id,
            @PathVariable int lineNumber,
            @Valid @RequestBody UpdateLineRequest request) {
        return ResponseEntity.ok(transactionService.updateLine(
                context.getTenantId(), context.getCompanyId(), id, lineNumber, request));
    }

    @DeleteMapping("/{id}/lines/{lineNumber}")
    public ResponseEntity<PosTransactionResponse> removeLine(
            @PathVariable Long id, @PathVariable int lineNumber) {
        return ResponseEntity.ok(transactionService.removeLine(
                context.getTenantId(), context.getCompanyId(), id, lineNumber));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<PosTransactionResponse> voidTransaction(
            @PathVariable Long id, @Valid @RequestBody VoidTransactionRequest request) {
        return ResponseEntity.ok(transactionService.voidTransaction(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<Page<PosTransactionResponse>> listTransactions(
            @PageableDefault(size = 20, sort = {"transactionDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactions(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PosTransactionResponse>> searchTransactions(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"transactionDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.searchTransactions(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<PosTransactionResponse>> listTransactionsByStatus(
            @RequestParam("status") PosTransactionStatus status,
            @PageableDefault(size = 20, sort = {"transactionDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-session")
    public ResponseEntity<Page<PosTransactionResponse>> listTransactionsBySession(
            @RequestParam("sessionId") Long sessionId,
            @PageableDefault(size = 20, sort = {"transactionDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsBySession(
                context.getTenantId(), context.getCompanyId(), sessionId, pageable));
    }

    @GetMapping("/by-register")
    public ResponseEntity<Page<PosTransactionResponse>> listTransactionsByRegister(
            @RequestParam("registerId") Long registerId,
            @PageableDefault(size = 20, sort = {"transactionDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByRegister(
                context.getTenantId(), context.getCompanyId(), registerId, pageable));
    }

    @GetMapping("/by-number")
    public ResponseEntity<PosTransactionResponse> getTransactionByNumber(
            @RequestParam("number") String number) {
        return ResponseEntity.ok(transactionService.getTransactionByNumber(
                context.getTenantId(), context.getCompanyId(), number));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<PosReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getReceipt(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosTransactionResponse> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
