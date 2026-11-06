package com.erpai.integration.ird.port;

/**
 * Alias for {@link FiscalSyncResult} matching the task specification naming.
 */
public class IrdSyncResult extends FiscalSyncResult {
    public IrdSyncResult(boolean success, int statusCode, String responseBody, String errorMessage) {
        super(success, statusCode, responseBody, errorMessage);
    }

    public IrdSyncResult(boolean success, int statusCode, String responseBody, String errorMessage, boolean simulated) {
        super(success, statusCode, responseBody, errorMessage, simulated);
    }
}
