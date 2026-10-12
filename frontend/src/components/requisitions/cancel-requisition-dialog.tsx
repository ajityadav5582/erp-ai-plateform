"use client";

import { useEffect, useState } from "react";
import { Loader2, Ban } from "lucide-react";
import { toast } from "sonner";
import {
  useCancelRequisitionMutation,
  type RequisitionListResponse,
} from "@/services/purchase-requisition.service";
import { getApiErrorMessage } from "@/services/error-handler";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

interface CancelRequisitionDialogProps {
  requisition: RequisitionListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * Cancellation is terminal and the reason is mandatory: the aggregate rejects
 * a cancellation without one, because a procurement document that quietly
 * disappears is not an auditable outcome.
 */
export function CancelRequisitionDialog({
  requisition,
  open,
  onOpenChange,
}: CancelRequisitionDialogProps) {
  const [cancel, { isLoading }] = useCancelRequisitionMutation();
  const [reason, setReason] = useState("");

  useEffect(() => {
    if (!open) setReason("");
  }, [open]);

  if (!requisition) return null;

  const onConfirm = async () => {
    const trimmed = reason.trim();
    if (!trimmed) {
      toast.error("A reason is required", {
        description: "Explain why this requisition is being cancelled.",
      });
      return;
    }

    try {
      await cancel({
        requisitionId: requisition.id,
        data: { reason: trimmed },
      }).unwrap();

      toast.success(`Requisition ${requisition.prNumber} cancelled`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to cancel requisition", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
              <Ban className="size-5" />
            </div>
            <div>
              <DialogTitle>Cancel {requisition.prNumber}</DialogTitle>
              <DialogDescription>
                This cannot be undone. The requisition keeps its lines and audit
                history, but stops participating in procurement.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-2 py-2">
          <Label htmlFor="cancel-reason">
            Cancellation Reason
            <span className="ml-1 text-destructive">*</span>
          </Label>
          <Textarea
            id="cancel-reason"
            rows={3}
            placeholder="Budget withdrawn, item no longer needed, duplicate request..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            Keep Requisition
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Cancelling..." : "Cancel Requisition"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
