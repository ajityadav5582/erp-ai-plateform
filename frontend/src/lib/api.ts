/**
 * API utility functions.
 *
 * Centralizes common API-related helpers so they can be reused across
 * the application without duplicating logic.
 */

import { toast } from "sonner";
import type { ApiError } from "@/services/api";

/**
 * Extracts a user-friendly error message from an API error.
 */
export function getApiErrorMessage(error: unknown): string {
  if (error && typeof error === "object" && "error" in error) {
    const apiError = error as ApiError;
    if (apiError.data && typeof apiError.data === "object" && "message" in apiError.data) {
      return (apiError.data as { message: string }).message;
    }
    if (apiError.error) {
      return apiError.error;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred. Please try again.";
}

/**
 * Shows a toast notification for an API error.
 */
export function showApiErrorToast(error: unknown): void {
  const message = getApiErrorMessage(error);
  toast.error(message);
}

/**
 * Shows a success toast.
 */
export function showSuccessToast(message: string): void {
  toast.success(message);
}

/**
 * Shows an info toast.
 */
export function showInfoToast(message: string): void {
  toast.info(message);
}
