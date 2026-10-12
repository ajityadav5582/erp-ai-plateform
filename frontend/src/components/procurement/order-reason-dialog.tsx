"use client";

import { useEffect, useState } from "react";
import { Ban, Loader2, XCircle } from "lucide-react";
import { Dialog, DialogContent } from "@/components/ui/dialog";

type ReasonMode = "reject" | "cancel";

interface OrderReasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: ReasonMode;
  /** The order number, so the wording is about a specific document. */
  poNumber: string;
  onConfirm: (reason: string) => Promise<void> | void;
}

const COPY: Record<
  ReasonMode,
  { title: string; description: string; actionLabel: string; placeholder: string }
> = {
  reject: {
    title: "Reject purchase order",
    description:
      "Rejection is terminal. The order cannot be revived and must be raised again as a new draft, so the reason is kept on the record permanently.",
    actionLabel: "Reject order",
    placeholder: "Why is this order being refused?",
  },
  cancel: {
    title: "Cancel purchase order",
    description:
      "Cancelling voids the order. Any goods already received against it stay on record, so this is the action for an order that should never have been issued.",
    actionLabel: "Cancel order",
    placeholder: "Why is this order being cancelled?",
  },
};

/**
 * Collects the mandatory reason for refusing or voiding a purchase order.
 *
 * Reject and cancel share this dialog because they share their contract: both demand a
 * reason, both are terminal, and both carry the same consequence for the audit trail.
 * Splitting them into two near-identical dialogs would let them drift apart - and a
 * rejection without a reason is the exact record that cannot be defended when the
 * supplier asks why.
 *
 * The confirm button stays disabled until there is a non-blank reason, which matches
 * the server's `@NotBlank`. Validating here as well means the user finds out before the
 * round trip, but the server's rule is still the authority.
 */
export function OrderReasonDialog({
  open,
  onOpenChange,
  mode,
  poNumber,
  onConfirm,
}: OrderReasonDialogProps) {
  const [reason, setReason] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const copy = COPY[mode];

  // Clear on open so a reason typed for a previous decision is never carried onto a
  // different one. A stale reason would attach an unrelated explanation to a permanent
  // record.
  useEffect(() => {
    if (open) {
      setReason("");
      setIsSubmitting(false);
    }
  }, [open]);

  const trimmed = reason.trim();
  const canSubmit = trimmed.length > 0 && !isSubmitting;

  const handleConfirm = async () => {
    if (!canSubmit) return;
    setIsSubmitting(true);
    try {
      await onConfirm(trimmed);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <DialogShell open={open} onOpenChange={onOpenChange}>
      <div className="space-y-4">
        <div className="flex flex-col gap-1.5">
          <h2 className="text-lg font-semibold text-foreground">{copy.title}</h2>
          <p className="text-sm text-muted-foreground">
            Purchase order {poNumber}. {copy.description}
          </p>
        </div>

        <div className="space-y-2">
          <label
            htmlFor="order-reason"
            className="text-sm font-medium text-foreground"
          >
            Reason
          </label>
          <textarea
            id="order-reason"
            rows={4}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={copy.placeholder}
            disabled={isSubmitting}
            className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-xs outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
          />
          <p className="text-xs text-muted-foreground">
            Stored on the order and shown wherever it appears. Maximum 500 characters.
          </p>
        </div>

        <div className="flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
            className="inline-flex h-9 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium text-foreground shadow-xs transition-colors hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-50"
          >
            Back
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!canSubmit}
            className="inline-flex h-9 items-center justify-center gap-2 rounded-md bg-destructive px-4 py-2 text-sm font-medium text-white shadow-xs transition-colors hover:bg-destructive/90 disabled:pointer-events-none disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            ) : mode === "reject" ? (
              <XCircle className="size-4" aria-hidden="true" />
            ) : (
              <Ban className="size-4" aria-hidden="true" />
            )}
            {copy.actionLabel}
          </button>
        </div>
      </div>
    </DialogShell>
  );
}

/** Thin wrapper so the dialog composes with the project's Dialog primitive. */
function DialogShell({
  open,
  onOpenChange,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">{children}</DialogContent>
    </Dialog>
  );
}
