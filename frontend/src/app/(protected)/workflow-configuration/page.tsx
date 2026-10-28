"use client";

import { useEffect, useState } from "react";
import { Loader2, Save, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import {
  useGetWorkflowConfigurationQuery,
  useUpdateWorkflowConfigurationMutation,
} from "@/services/workflow-configuration.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { PageHeader } from "@/components/common/page-header";
import { ErrorState } from "@/components/common/error-state";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * The company's procurement workflow switches.
 *
 * There is one setting, and it is consequential: with approval required, a requisition
 * must be signed off by a person before it can become a purchase order; with it off, the
 * requester converts their own requisition. The form therefore states plainly which of
 * the two the current setting means, rather than leaving a bare toggle for the reader to
 * interpret - a checkbox labelled "approval required" that is turned *off* is read as
 * "approval not required", which is right, but only if the reader is paying attention to
 * a control whose failure mode is unwanted spend.
 *
 * The switch is scoped to the company in the caller's token. There is no company picker
 * here, because the backend takes the scope from the token and ignores anything a caller
 * sends; a picker would imply a choice that cannot actually be made from this screen.
 */
export default function WorkflowConfigurationPage() {
  const {
    data: configuration,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useGetWorkflowConfigurationQuery();

  const [approvalRequired, setApprovalRequired] = useState(false);

  const [save, { isLoading: isSaving }] = useUpdateWorkflowConfigurationMutation();

  // Seeded from the fetched value only once it arrives, and only when the user has not
  // started editing. Re-seeding on every refetch would discard an unsaved edit made
  // while someone else was updating the same setting.
  const [seeded, setSeeded] = useState(false);
  useEffect(() => {
    if (configuration && !seeded) {
      setApprovalRequired(configuration.purchaseRequisitionApprovalRequired);
      setSeeded(true);
    }
  }, [configuration, seeded]);

  const savedValue = configuration?.purchaseRequisitionApprovalRequired ?? null;
  const isDirty = savedValue !== null && approvalRequired !== savedValue;

  const handleSave = async () => {
    try {
      await save({ purchaseRequisitionApprovalRequired: approvalRequired }).unwrap();
      toast.success("Workflow settings saved", {
        description: approvalRequired
          ? "Requisitions now need approval before they can become purchase orders."
          : "Requisitions can now be converted without a separate approval step.",
      });
    } catch (err) {
      toast.error("Failed to save the workflow settings", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Workflow Configuration"
        description="How requisitions move through approval before becoming purchase orders."
        breadcrumbs={[
          { label: "Procurement", href: "/purchase-requisitions" },
          { label: "Workflow Configuration" },
        ]}
        actions={
          <Button
            size="sm"
            onClick={handleSave}
            disabled={isSaving || isLoading || !isDirty}
          >
            {isSaving ? (
              <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
            ) : (
              <Save className="mr-2 size-4" aria-hidden="true" />
            )}
            Save changes
          </Button>
        }
      />

      {isLoading ? (
        <Card>
          <CardContent className="space-y-3 p-6">
            <Skeleton className="h-6 w-56" />
            <Skeleton className="h-20 w-full" />
          </CardContent>
        </Card>
      ) : error ? (
        <ErrorState
          title="Could not load the workflow settings"
          description="The company's procurement workflow configuration could not be read."
          onRetry={refetch}
        />
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Requisition approval</CardTitle>
            <CardDescription>
              Whether a purchase requisition needs a person to approve it before it can
              be converted into a purchase order.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <label className="flex cursor-pointer items-start gap-3 rounded-md border border-border p-4">
              <input
                type="checkbox"
                checked={approvalRequired}
                onChange={(e) => setApprovalRequired(e.target.checked)}
                disabled={isSaving || isFetching}
                className="mt-0.5 size-4 accent-primary"
              />
              <span className="flex flex-col gap-1">
                <span className="text-sm font-medium text-foreground">
                  Require approval before a requisition becomes a purchase order
                </span>
                <span className="text-sm text-muted-foreground">
                  {approvalRequired
                    ? "On: a requisition is created as a draft, submitted for approval, and only an approved requisition can be converted into a purchase order."
                    : "Off: an approved-looking requisition can be converted straight into a purchase order without waiting for a separate sign-off."}
                </span>
              </span>
            </label>

            <div className="flex items-start gap-2 rounded-md border border-border bg-muted/40 p-3 text-sm text-muted-foreground">
              <ShieldCheck className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <p>
                This switch applies to requisitions raised from now on. Requisitions
                already in flight keep the approval steps they were raised under, and
                changing it never approves anything retroactively.
              </p>
            </div>

            {isDirty && (
              <p className="text-sm text-amber-600 dark:text-amber-400">
                Unsaved change. Choose Save changes to apply it to this company.
              </p>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
