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
  if (error && typeof error === "object") {
    const err = error as { data?: { message?: string; code?: string }; error?: string; message?: string };
    if (typeof err.data?.message === "string" && err.data.message.trim().length > 0) {
      return err.data.message;
    }
    if (err.error) {
      return err.error;
    }
    if (err.message) {
      return err.message;
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
