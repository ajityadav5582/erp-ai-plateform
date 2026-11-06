package com.erpai.integration.ird.port;

/**
 * Outcome of an outbound fiscal synchronization attempt against the Nepal IRD CBMS.
 */
public class FiscalSyncResult {

    private final boolean success;
    private final int statusCode;
    private final String responseBody;
    private final String errorMessage;
    private final boolean simulated;

    public FiscalSyncResult(boolean success, int statusCode, String responseBody, String errorMessage) {
        this(success, statusCode, responseBody, errorMessage, false);
    }

    public FiscalSyncResult(boolean success, int statusCode, String responseBody, String errorMessage, boolean simulated) {
        this.success = success;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
        this.simulated = simulated;
    }

    public boolean isSimulated() { return simulated; }

    public boolean success() { return success; }
    public int statusCode() { return statusCode; }
    public String responseBody() { return responseBody; }
    public String errorMessage() { return errorMessage; }

    public boolean isSuccess() { return success; }
    public String getResponseBody() { return responseBody; }
    public String getErrorMessage() { return errorMessage; }
    public int getStatusCode() { return statusCode; }
}
