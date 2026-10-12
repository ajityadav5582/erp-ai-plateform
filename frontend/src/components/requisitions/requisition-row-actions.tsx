"use client";

import { useState } from "react";
import {
  MoreVertical,
  Eye,
  Pencil,
  Send,
  Gavel,
  Ban,
  Trash2,
  ShieldAlert,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetRequisitionActionsQuery,
  useSubmitRequisitionMutation,
  useDeleteRequisitionMutation,
  type RequisitionListResponse,
} from "@/services/purchase-requisition.service";
import { getApiErrorMessage } from "@/services/error-handler";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface RequisitionRowActionsProps {
  requisition: RequisitionListResponse;
  onView: (id: number) => void;
  onEdit: (id: number) => void;
  onDecide: (id: number) => void;
  onCancel: (requisition: RequisitionListResponse) => void;
}

/**
 * The per-row action menu, driven by the server's actionability report.
 *
 * What used to be a status check here — "is this DRAFT or RETURNED, show Edit" —
 * was wrong in two ways at once: it ignored who was asking, and it ignored the
 * approval chain's current assignee. So the menu offered buttons that answered
 * 403, and hid buttons that would have worked.
 *
 * The report is fetched when the menu is opened rather than on render. Ten rows
 * on screen is ten actionability calls otherwise, and the answer is only ever
 * needed at the moment somebody is about to act on it. It is advisory: every
 * endpoint re-authorises, so a stale answer can at worst offer a button that
 * then fails, never one that succeeds when it should not.
 */
export function RequisitionRowActions({
  requisition,
  onView,
  onEdit,
  onDecide,
  onCancel,
}: RequisitionRowActionsProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  const { data: actions, isFetching } = useGetRequisitionActionsQuery(requisition.id, {
    skip: !menuOpen,
  });

  const [submit, { isLoading: isSubmitting }] = useSubmitRequisitionMutation();
  const [remove, { isLoading: isDeleting }] = useDeleteRequisitionMutation();
  const busy = isSubmitting || isDeleting;

  const handleSubmit = async () => {
    try {
      const result = await submit(requisition.id).unwrap();
      toast.success(`Requisition ${result.prNumber} submitted for approval`);
      setMenuOpen(false);
    } catch (err) {
      toast.error("Failed to submit requisition", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDelete = async () => {
    try {
      await remove(requisition.id).unwrap();
      toast.success(`Requisition ${requisition.prNumber} deleted`);
      setMenuOpen(false);
    } catch (err) {
      toast.error("Failed to delete requisition", {
        description: getApiErrorMessage(err),
      });
    }
  };

  // Nothing actionable while the report is in flight. Rendering the old
  // status-derived guesses in the meantime would be the exact bug this replaced.
  const showActions = Boolean(actions);

  return (
    <DropdownMenu
      modal
      open={menuOpen}
      onOpenChange={setMenuOpen}
    >
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="size-8">
          <MoreVertical className="size-4" />
          <span className="sr-only">Open menu</span>
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="z-[9999] w-60">
        <DropdownMenuItem onClick={() => onView(requisition.id)}>
          <Eye className="mr-2 size-4 text-muted-foreground" />
          View Details
        </DropdownMenuItem>

        {!showActions ? (
          <p className="px-2 py-1.5 text-xs text-muted-foreground">
            {isFetching ? "Checking what you can do…" : "—"}
          </p>
        ) : (
          <>
            {actions?.canEdit && (
              <DropdownMenuItem onClick={() => onEdit(requisition.id)}>
                <Pencil className="mr-2 size-4 text-sky-600" />
                Edit
              </DropdownMenuItem>
            )}

            {actions?.canSubmit && (
              <DropdownMenuItem onClick={handleSubmit} disabled={busy}>
                <Send className="mr-2 size-4 text-sky-600" />
                Submit for Approval
              </DropdownMenuItem>
            )}

            {actions?.canDecide && (
              <DropdownMenuItem onClick={() => onDecide(requisition.id)}>
                <Gavel className="mr-2 size-4 text-emerald-600" />
                Record Decision
              </DropdownMenuItem>
            )}

            {actions?.canCancel && (
              <DropdownMenuItem onClick={() => onCancel(requisition)}>
                <Ban className="mr-2 size-4 text-amber-600" />
                Cancel Requisition
              </DropdownMenuItem>
            )}

            {actions?.canDelete && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={handleDelete}
                  disabled={busy}
                >
                  <Trash2 className="mr-2 size-4" />
                  Delete Draft
                </DropdownMenuItem>
              </>
            )}

            {/* Why nothing is offered. A user who opened the menu and found it
                empty otherwise has no way to tell "not yours" from "wrong
                status" from "someone else's turn". */}
            {actions &&
              !actions.canEdit &&
              !actions.canSubmit &&
              !actions.canDecide &&
              !actions.canCancel &&
              !actions.canDelete && (
                <DropdownMenuItem disabled className="whitespace-normal py-2">
                  <ShieldAlert className="mr-2 size-4 text-muted-foreground" />
                  <span className="text-xs text-muted-foreground">
                    {actions.blockers[0] ?? "No action is available to you here."}
                  </span>
                </DropdownMenuItem>
              )}

            {actions && actions.awaitingStepSequenceNo != null && (
              <p className="px-2 py-1.5 text-xs text-muted-foreground">
                Awaiting approval at step {actions.awaitingStepSequenceNo}.
              </p>
            )}
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
