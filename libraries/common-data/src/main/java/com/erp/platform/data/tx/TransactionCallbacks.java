package com.erp.platform.data.tx;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Registers an action to run <strong>only if the current transaction rolls
 * back</strong>.
 *
 * <p>This compensates a remote side effect that has already committed outside
 * the local transaction. The canonical case is a stock movement posted to the
 * Inventory service during a POS checkout or a sales delivery: that HTTP call
 * commits in Inventory independently of the caller's transaction, so if the
 * caller then fails and rolls back, the ledger and the (now absent) source
 * document would diverge. Registering the reverse movement here undoes the
 * remote effect exactly when the local transaction rolls back, and the
 * compensating call is itself idempotent by reference, so a retry is safe.
 *
 * <p>Register the compensation immediately after each successful remote post,
 * so a partial failure compensates only what actually succeeded.
 */
public final class TransactionCallbacks {

    private TransactionCallbacks() {
    }

    /**
     * Runs {@code action} after the current transaction completes, but only when
     * it rolled back. A no-op when there is no active transaction synchronization
     * (e.g. called outside a transactional boundary).
     */
    public static void onRollback(Runnable action) {
        if (action == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }
}
