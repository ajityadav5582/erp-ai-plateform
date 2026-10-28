"use client";

import { useEffect, useState } from "react";
import { Loader2, Gavel } from "lucide-react";
import { toast } from "sonner";
import {
  useDecideRequisitionMutation,
  type ApprovalDecisionRequest,
  type ApprovalAction,
  type Requisition,
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface RequisitionDecisionDialogProps {
  requisition: Requisition | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const ACTION_LABELS: Record<ApprovalAction, string> = {
  APPROVED: "Approve",
  REJECTED: "Reject",
};

/** What the approver wants to happen to one line of an approval. */
type LineOutcome = "APPROVE" | "REJECT" | "CANCEL";

const LINE_OUTCOMES: { value: LineOutcome; label: string; hint: string }[] = [
  { value: "APPROVE", label: "Approve", hint: "Grants this line's demand" },
  { value: "REJECT", label: "Reject", hint: "Denies this line" },
  { value: "CANCEL", label: "Cancel", hint: "Withdraws this line" },
];

/**
 * Records an approval decision.
 *
 * Three degrees of partiality are available, because "approve some of it" means
 * different things to different approvers:
 *
 * - grant less than requested on a line, by typing a smaller quantity;
 * - reject a line outright, which keeps it on the document as evidence of what
 *   was asked for but carries no demand forward;
 * - cancel a line, which says the need itself has gone away.
 *
 * All three are optional. An untouched line is approved in full server-side, so
 * only lines the approver actually changed are sent — sending the unchanged ones
 * would be noise, and a grant above the request is rejected by the aggregate.
 *
 * Line outcomes are only meaningful on an `APPROVED` decision. The server rejects
 * them on `REJECTED` rather than silently discarding them, so the grid is hidden
 * for that action rather than sending a payload known to be refused.
 */
export function RequisitionDecisionDialog({
  requisition,
  open,
  onOpenChange,
}: RequisitionDecisionDialogProps) {
  const [decide, { isLoading }] = useDecideRequisitionMutation();
  const [action, setAction] = useState<ApprovalAction>("APPROVED");
  const [comments, setComments] = useState("");
  const [grants, setGrants] = useState<Record<number, string>>({});
  const [outcomes, setOutcomes] = useState<Record<number, LineOutcome>>({});

  // Reset per open so a previous decision does not leak into a new one.
  useEffect(() => {
    if (!open) {
      setAction("APPROVED");
      setComments("");
      setGrants({});
      setOutcomes({});
    }
  }, [open]);

  if (!requisition) return null;

  const handleGrantChange = (lineId: number, value: string) => {
    setGrants((prev) => ({ ...prev, [lineId]: value }));
  };

  const handleOutcomeChange = (lineId: number, outcome: LineOutcome) => {
    setOutcomes((prev) => ({ ...prev, [lineId]: outcome }));
    // A refused line has no grant to speak of, so drop any quantity typed
    // against it rather than sending a value that will be ignored.
    if (outcome !== "APPROVE") {
      setGrants((prev) => {
        const { [lineId]: _dropped, ...rest } = prev;
        return rest;
      });
    }
  };

  const onSubmit = async () => {
    const trimmedComments = comments.trim();

    // The aggregate requires a comment to reject, so the dialog refuses to
    // submit rather than letting the round trip fail.
    if (action === "REJECTED" && !trimmedComments) {
      toast.error("A comment is required", {
        description: `Explain why this requisition is being ${action.toLowerCase()}.`,
      });
      return;
    }

    const refusing = requisition.lines.filter(
      (line) => outcomes[line.id] === "REJECT" || outcomes[line.id] === "CANCEL"
    );

    // Approving a document where every line has been refused commits nothing and
    // is almost always a misclick on the action. The server refuses it too;
    // catching it here explains why instead of surfacing a validation error.
    if (action === "APPROVED" && refusing.length === requisition.lines.length) {
      toast.error("Every line is refused", {
        description:
          "Reject the requisition instead — approving it would commit no demand at all.",
      });
      return;
    }

    // Only send grants the approver actually changed, so an untouched line is
    // approved in full server-side.
    const lineApprovals = Object.entries(grants)
      .filter(([_lineId, value]) => value.trim() !== "")
      .filter(([lineId]) => (outcomes[Number(lineId)] ?? "APPROVE") === "APPROVE")
      .map(([lineId, value]) => ({
        lineId: Number(lineId),
        approvedQuantity: Number(value),
      }))
      .filter((entry) => !Number.isNaN(entry.approvedQuantity));

    const overGranted = lineApprovals.filter(
      (entry) => entry.approvedQuantity < 0 || Number.isNaN(entry.approvedQuantity)
    );
    if (overGranted.length > 0) {
      toast.error("Invalid approved quantity", {
        description: "Approved quantity must be zero or greater.",
      });
      return;
    }

    const payload: ApprovalDecisionRequest = {
      action,
      lineApprovals: action === "APPROVED" ? lineApprovals : undefined,
      rejectedLineIds:
        action === "APPROVED"
          ? requisition.lines
              .filter((line) => outcomes[line.id] === "REJECT")
              .map((line) => line.id)
          : undefined,
      cancelledLineIds:
        action === "APPROVED"
          ? requisition.lines
              .filter((line) => outcomes[line.id] === "CANCEL")
              .map((line) => line.id)
          : undefined,
      comments: trimmedComments || undefined,
    };

    // Omit empty arrays so the wire payload for the common case — approve it all,
    // no per-line changes — is exactly what it was before.
    if (payload.rejectedLineIds?.length === 0) delete payload.rejectedLineIds;
    if (payload.cancelledLineIds?.length === 0) delete payload.cancelledLineIds;

    try {
      const result = await decide({
        requisitionId: requisition.id,
        data: payload,
      }).unwrap();

      const refusedNote =
        refusing.length > 0
          ? ` ${refusing.length} line(s) were refused and carry no demand forward.`
          : "";

      toast.success(
        `Decision recorded on ${result.prNumber}`,
        {
          description: `Status is now ${result.status.replace(/_/g, " ").toLowerCase()}.${refusedNote}`,
        }
      );
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to record decision", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const summary = (() => {
    const rejected = requisition.lines.filter((l) => outcomes[l.id] === "REJECT").length;
    const cancelled = requisition.lines.filter((l) => outcomes[l.id] === "CANCEL").length;
    if (rejected === 0 && cancelled === 0) return null;
    const parts: string[] = [];
    if (rejected) parts.push(`${rejected} rejected`);
    if (cancelled) parts.push(`${cancelled} cancelled`);
    return parts.join(", ");
  })();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Gavel className="size-5" />
            </div>
            <div>
              <DialogTitle>Review {requisition.prNumber}</DialogTitle>
              <DialogDescription>
                {requisition.lines.length} line(s), estimated{" "}
                {requisition.currency} {Number(requisition.estimatedTotalAmount).toLocaleString()}.
                Grant less than requested, reject, or cancel any individual line.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="decision-action">Decision</Label>
              <Select
                value={action}
                onValueChange={(value) => setAction(value as ApprovalAction)}
                disabled={isLoading}
              >
                <SelectTrigger id="decision-action">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(["APPROVED", "REJECTED"] as const).map((value) => (
                    <SelectItem key={value} value={value}>
                      {ACTION_LABELS[value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {action === "APPROVED" && (
            <div className="overflow-hidden rounded-lg border border-border">
              <Table>
                <TableHeader className="bg-muted/40">
                  <TableRow>
                    <TableHead>Item</TableHead>
                    <TableHead className="text-right">Requested</TableHead>
                    <TableHead className="w-36">Outcome</TableHead>
                    <TableHead className="w-32 text-right">Quantity</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {requisition.lines.map((line) => {
                    const outcome = outcomes[line.id] ?? "APPROVE";
                    const isApproving = outcome === "APPROVE";
                    return (
                      <TableRow
                        key={line.id}
                        className={isApproving ? undefined : "bg-muted/30"}
                      >
                        <TableCell>
                          <p className="font-medium text-foreground">
                            {line.itemName}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {line.itemCode} · {line.uomCode}
                          </p>
                        </TableCell>
                        <TableCell className="text-right text-sm text-muted-foreground">
                          {Number(line.requestedQuantity).toLocaleString()}
                        </TableCell>
                        <TableCell>
                          <Select
                            value={outcome}
                            onValueChange={(value) =>
                              handleOutcomeChange(line.id, value as LineOutcome)
                            }
                            disabled={isLoading}
                          >
                            <SelectTrigger
                              className="h-8"
                              aria-label={`Outcome for ${line.itemName}`}
                            >
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {LINE_OUTCOMES.map((option) => (
                                <SelectItem
                                  key={option.value}
                                  value={option.value}
                                >
                                  {option.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        <TableCell>
                          <Input
                            type="number"
                            step="0.000001"
                            min="0"
                            max={line.requestedQuantity}
                            className="text-right"
                            placeholder={String(line.requestedQuantity)}
                            value={grants[line.id] ?? ""}
                            onChange={(e) =>
                              handleGrantChange(line.id, e.target.value)
                            }
                            disabled={isLoading || !isApproving}
                            title={
                              isApproving
                                ? "Leave blank to approve the full requested quantity"
                                : "A refused line carries no demand forward"
                            }
                          />
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>

              {summary && (
                <div className="flex items-center gap-2 border-t border-border bg-muted/20 px-3 py-2">
                  <span className="text-xs text-muted-foreground">
                    On approval:
                  </span>
                  <Badge variant="secondary">{summary}</Badge>
                  <span className="text-xs text-muted-foreground">
                    Refused lines stay on the document for the record but can
                    never be awarded to a purchase order.
                  </span>
                </div>
              )}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="decision-comments">
              Comments
              {action === "REJECTED" ? (
                <span className="ml-1 text-destructive">*</span>
              ) : null}
            </Label>
            <Input
              id="decision-comments"
              placeholder={
                action === "APPROVED"
                  ? "Optional note for the requester"
                  : "Explain the decision"
              }
              value={comments}
              onChange={(e) => setComments(e.target.value)}
              disabled={isLoading}
            />
          </div>
        </div>

        <DialogFooter className="pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant={action === "APPROVED" ? "default" : "destructive"}
            onClick={onSubmit}
            disabled={isLoading}
          >
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Recording..." : ACTION_LABELS[action]}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
