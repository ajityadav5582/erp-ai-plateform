package com.erp.business.pos.cbms;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.shared.context.BusinessContextAccessor;
import com.erp.business.pos.shared.security.PosTransactionPermissions;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CBMS operations for POS: a manual sync trigger for a stuck bill and an
 * outbox depth gauge for monitoring.
 *
 * <p>The routine path is the scheduled {@link PosCbmsSyncWorker}; these
 * endpoints exist for operators, not for the checkout flow.
 */
@RestController
@RequestMapping("/api/v1/pos/cbms")
@RequiredArgsConstructor
public class PosCbmsController {

    private final PosCbmsSyncWorker syncWorker;
    private final PosCbmsClient cbmsClient;
    private final PosTransactionRepository transactionRepository;
    private final BusinessContextAccessor context;

    @PostMapping("/transactions/{id}/sync")
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public ResponseEntity<PosCbmsSyncResult> syncTransaction(@PathVariable Long id) {
        PosTransaction transaction = transactionRepository
                .findByTenantIdAndCompanyIdAndId(context.getTenantId(), context.getCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

        PosCbmsSyncResult result = syncWorker.syncTransaction(transaction);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @RequiresPermission(PosTransactionPermissions.POS_TRANSACTION_VIEW)
    public ResponseEntity<Map<String, Object>> getStatus() {
        int pending = transactionRepository
                .findByTenantIdAndCompanyIdAndCbmsSyncedFalse(context.getTenantId(), context.getCompanyId())
                .size();

        return ResponseEntity.ok(Map.of(
                "pendingTransactions", pending,
                "enabled", cbmsClient.isEnabled()
        ));
    }
}
