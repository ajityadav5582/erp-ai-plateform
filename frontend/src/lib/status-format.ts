/**
 * Shared status formatting helpers.
 */

export function formatStatus(status: string | null | undefined): string {
  if (!status) return "—";
  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
