/**
 * API utility functions.
 *
 * Centralizes common API-related helpers so they can be reused across
 * the application without duplicating logic.
 */

import { toast } from "sonner";

import { getApiErrorMessage } from "@/services/error-handler";
export { getApiErrorMessage };

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
