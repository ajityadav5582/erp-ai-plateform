package com.erp.platform.common.nepali.date;

/**
 * Thrown when a Bikram Sambat date is malformed or outside the supported calendar range.
 *
 * <p>Intentionally extends {@link IllegalArgumentException} rather than a Spring or
 * common-core exception, so this library stays framework-agnostic. Services that
 * want RFC-7807 problem responses should translate it in their global handler.
 */
public class NepaliDateException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public NepaliDateException(String message) {
        super(message);
    }

    public NepaliDateException(String message, Throwable cause) {
        super(message, cause);
    }
}
