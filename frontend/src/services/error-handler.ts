/**
 * Centralized error handling utilities for the ERP AI Platform frontend.
 *
 * Maps backend error codes and HTTP status codes to user-friendly messages
 * and provides helpers for extracting error information from API responses.
 */

/**
 * Backend error code to user-friendly message mapping.
 *
 * These correspond to the error codes returned by the backend
 * `GlobalExceptionHandler` in the identity service.
 */
const ERROR_CODE_MESSAGES: Record<string, string> = {
  // Authentication errors
  INVALID_CREDENTIALS:
    "Invalid username or password. Please check your credentials and try again.",
  ACCOUNT_LOCKED:
    "Your account has been temporarily locked due to multiple failed login attempts. Please try again later or reset your password.",
  AUTHENTICATION_ERROR:
    "Your account is not active. Please contact support for assistance.",
  INVALID_REFRESH_TOKEN:
    "Your session has expired. Please sign in again.",

  // Validation errors
  VALIDATION_ERROR: "Please check the information you entered and try again.",

  // Password reset errors
  PASSWORD_RESET_TOKEN_ERROR:
    "The password reset link is invalid or has expired. Please request a new one.",

  // Not found errors
  NOT_FOUND: "The requested resource was not found.",
  ROLE_NOT_FOUND: "The requested role was not found.",
  PERMISSION_NOT_FOUND: "The requested permission was not found.",

  // Conflict errors
  DUPLICATE_EMAIL: "An account with this email already exists.",
  DUPLICATE_ROLE_CODE: "A role with this code already exists.",
  USER_OPERATION_CONFLICT: "This operation cannot be completed due to a conflict.",
  CANNOT_ACTIVATE_USER: "This user cannot be activated.",
  CANNOT_DEACTIVATE_USER: "This user cannot be deactivated.",
  CANNOT_DELETE_USER: "This user cannot be deleted.",
  CANNOT_DELETE_ROLE: "This role cannot be deleted.",

  // Server errors
  INTERNAL_ERROR: "An unexpected error occurred. Please try again later.",
};

/**
 * HTTP status code to user-friendly message mapping.
 */
const STATUS_CODE_MESSAGES: Record<number, string> = {
  400: "The request was invalid. Please check your input and try again.",
  401: "You are not authorized to access this resource. Please sign in.",
  403: "You do not have permission to perform this action.",
  404: "The requested resource was not found.",
  409: "A conflict occurred. The resource may already exist.",
  422: "The data you submitted is invalid. Please check and try again.",
  429: "Too many requests. Please wait a moment and try again.",
  500: "An internal server error occurred. Please try again later.",
  502: "The server is temporarily unavailable. Please try again later.",
  503: "The service is temporarily unavailable. Please try again later.",
};

/**
 * Extracts a user-friendly error message from an API error.
 *
 * Handles:
 * - Backend `ApiError` responses with `code` and `message` fields
 * - HTTP status code fallbacks
 * - Network errors (no response)
 * - Unknown error shapes
 *
 * @param error The error object from RTK Query or Axios
 * @returns A user-friendly error message
 */
export function getApiErrorMessage(error: unknown): string {
  // Handle Axios-like errors (from RTK Query base query or unwrap)
  if (error && typeof error === "object") {
    const err = error as {
      data?: { code?: string; message?: string };
      status?: number;
      message?: string;
    };

    // Try backend error code first
    const errorCode = err.data?.code;
    if (errorCode && ERROR_CODE_MESSAGES[errorCode]) {
      return ERROR_CODE_MESSAGES[errorCode];
    }

    // Try backend error message
    if (err.data?.message) {
      return err.data.message;
    }

    // Try HTTP status code
    const status = err.status;
    if (status && STATUS_CODE_MESSAGES[status]) {
      return STATUS_CODE_MESSAGES[status];
    }

    // Try generic error message
    if (err.message) {
      // Network errors often have messages like "Network Error" or "timeout of ..."
      if (err.message === "Network Error") {
        return "Unable to connect to the server. Please check your internet connection and try again.";
      }
      if (err.message.includes("timeout")) {
        return "The request timed out. Please check your connection and try again.";
      }
      return err.message;
    }
  }

  // Fallback for unknown error shapes
  if (error instanceof Error) {
    if (error.message === "Network Error") {
      return "Unable to connect to the server. Please check your internet connection and try again.";
    }
    if (error.message.includes("timeout")) {
      return "The request timed out. Please check your connection and try again.";
    }
    return error.message;
  }

  return "An unexpected error occurred. Please try again.";
}

/**
 * Extracts the backend error code from an API error, if available.
 *
 * @param error The error object from RTK Query or Axios
 * @returns The error code string or null
 */
export function getApiErrorCode(error: unknown): string | null {
  if (error && typeof error === "object") {
    const err = error as { data?: { code?: string } };
    return err.data?.code ?? null;
  }
  return null;
}

/**
 * Checks if an API error is a specific backend error code.
 *
 * @param error The error object from RTK Query or Axios
 * @param code The error code to check for
 * @returns True if the error matches the code
 */
export function isApiErrorCode(error: unknown, code: string): boolean {
  return getApiErrorCode(error) === code;
}

/**
 * Checks if an error is a network error (no response received).
 *
 * @param error The error object from RTK Query or Axios
 * @returns True if the error is a network error
 */
export function isNetworkError(error: unknown): boolean {
  if (error && typeof error === "object") {
    const err = error as { message?: string };
    if (err.message === "Network Error") return true;
    if (err.message?.includes("timeout")) return true;
  }
  if (error instanceof Error) {
    if (error.message === "Network Error") return true;
    if (error.message.includes("timeout")) return true;
  }
  return false;
}
