"use client";

import { useState } from "react";
import { useGetSupplierBalanceSummaryQuery } from "@/services/supplier-payment.service";
import { formatCurrency } from "@/lib/utils";
import { CheckCircle2, CreditCard, FileSpreadsheet, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { SupplierStatementDialog } from "@/components/procurement/supplier-statement-dialog";

interface SupplierBalanceBadgeProps {
  supplierId?: number | null;
  className?: string;
  showZeroNotice?: boolean;
}

export function SupplierBalanceBadge({
  supplierId,
  className = "",
  showZeroNotice = false,
}: SupplierBalanceBadgeProps) {
  const [statementOpen, setStatementOpen] = useState(false);
  const { data: summary, isLoading } = useGetSupplierBalanceSummaryQuery(
    supplierId ?? 0,
    { skip: !supplierId || supplierId <= 0 }
  );

  if (!supplierId || supplierId <= 0) {
    return null;
  }

  if (isLoading) {
    return (
      <div className={`flex items-center gap-2 text-xs text-muted-foreground py-1 ${className}`}>
        <Loader2 className="size-3 animate-spin" />
        <span>Loading supplier financial status...</span>
      </div>
    );
  }

  if (!summary) return null;

  const hasPayables = summary.totalOutstandingPayable > 0;
  const hasAdvance = summary.totalAvailableAdvance > 0;

  if (!hasPayables && !hasAdvance && !showZeroNotice) {
    return null;
  }

  return (
    <div
      className={`flex flex-wrap items-center gap-2 rounded-lg border p-2 text-xs transition-colors ${
        hasAdvance
          ? "border-emerald-500/30 bg-emerald-500/5 text-emerald-950 dark:text-emerald-100"
          : hasPayables
          ? "border-amber-500/30 bg-amber-500/5 text-amber-950 dark:text-amber-100"
          : "border-border bg-muted/20 text-muted-foreground"
      } ${className}`}
    >
      <div className="flex items-center gap-1.5 font-medium">
        <CreditCard className="size-3.5 opacity-70" />
        <span>Live Balance:</span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {hasPayables ? (
          <Badge
            variant="outline"
            className="border-amber-500/40 bg-amber-500/10 text-amber-700 dark:text-amber-300 font-normal text-[11px]"
          >
            Payable: {formatCurrency(summary.totalOutstandingPayable)}{" "}
            ({summary.unpaidInvoiceCount}{" "}
            {summary.unpaidInvoiceCount === 1 ? "invoice" : "invoices"})
          </Badge>
        ) : (
          <span className="flex items-center gap-1 text-[11px] text-muted-foreground">
            <CheckCircle2 className="size-3 text-emerald-500" /> No unpaid invoices
          </span>
        )}

        {hasAdvance && (
          <Badge
            variant="outline"
            className="border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 font-medium text-[11px]"
          >
            Advance Credit: {formatCurrency(summary.totalAvailableAdvance)}{" "}
            ({summary.availableAdvanceCount}{" "}
            {summary.availableAdvanceCount === 1 ? "credit" : "credits"} available)
          </Badge>
        )}

        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setStatementOpen(true)}
          className="h-6 px-2 text-[11px] font-medium text-primary hover:bg-primary/10 gap-1 ml-auto"
        >
          <FileSpreadsheet className="size-3" />
          Statement
        </Button>
      </div>

      <SupplierStatementDialog
        supplierId={supplierId}
        open={statementOpen}
        onOpenChange={setStatementOpen}
      />
    </div>
  );
}
