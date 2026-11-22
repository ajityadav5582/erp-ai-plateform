"use client";

import { useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  FileSpreadsheet,
  Loader2,
  Receipt,
  RotateCcw,
} from "lucide-react";
import {
  useGetSupplierStatementQuery,
  type SupplierStatementLine,
} from "@/services/supplier-payment.service";
import { formatMoney } from "@/lib/procurement-format";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";

interface SupplierStatementDialogProps {
  supplierId: number | null;
  supplierName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function SupplierStatementDialog({
  supplierId,
  supplierName,
  open,
  onOpenChange,
}: SupplierStatementDialogProps) {
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const { data: statement, isLoading, isFetching } = useGetSupplierStatementQuery(
    {
      supplierId: supplierId ?? 0,
      fromDate: fromDate || undefined,
      toDate: toDate || undefined,
    },
    { skip: !open || !supplierId }
  );

  const currency = statement?.currency ?? "NPR";

  const handleResetFilters = () => {
    setFromDate("");
    setToDate("");
  };

  const getDocBadge = (type: SupplierStatementLine["documentType"]) => {
    switch (type) {
      case "INVOICE":
        return (
          <Badge variant="outline" className="gap-1 border-blue-500/30 bg-blue-500/10 text-blue-700 dark:text-blue-300 text-[10px]">
            <Receipt className="size-3" /> Invoice
          </Badge>
        );
      case "PAYMENT":
        return (
          <Badge variant="outline" className="gap-1 border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 text-[10px]">
            <ArrowDownLeft className="size-3" /> Payment
          </Badge>
        );
      case "DEBIT_NOTE":
        return (
          <Badge variant="outline" className="gap-1 border-purple-500/30 bg-purple-500/10 text-purple-700 dark:text-purple-300 text-[10px]">
            <ArrowUpRight className="size-3" /> Debit Note
          </Badge>
        );
      default:
        return <Badge variant="outline">{type}</Badge>;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl overflow-y-auto p-4 sm:p-6">
        <DialogHeader className="border-b border-border pb-4">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-primary/10 p-2.5 text-primary ring-1 ring-primary/20">
                <FileSpreadsheet className="size-6" />
              </div>
              <div>
                <DialogTitle className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
                  Supplier Statement of Account
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
                  {statement?.supplierName ?? supplierName ?? "Supplier"} ({statement?.supplierCode ?? "—"}) · Chronological Running Balance
                </DialogDescription>
              </div>
            </div>
            {isFetching && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="size-3.5 animate-spin" /> Refreshing statement…
              </div>
            )}
          </div>
        </DialogHeader>

        <div className="space-y-6 pt-2">
          {/* Filter Bar */}
          <div className="flex flex-wrap items-end gap-3 rounded-xl border border-border/70 bg-card p-3.5 shadow-xs">
            <div className="space-y-1.5">
              <Label htmlFor="stmt-from-date" className="text-xs font-semibold text-muted-foreground">
                From Date
              </Label>
              <Input
                id="stmt-from-date"
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="h-8 text-xs font-mono"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="stmt-to-date" className="text-xs font-semibold text-muted-foreground">
                To Date
              </Label>
              <Input
                id="stmt-to-date"
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                className="h-8 text-xs font-mono"
              />
            </div>
            {(fromDate || toDate) && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleResetFilters}
                className="h-8 text-xs text-muted-foreground hover:text-foreground gap-1"
              >
                <RotateCcw className="size-3.5" /> Clear Filters
              </Button>
            )}
          </div>

          {/* Statement Financial Metrics */}
          {statement && (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
              <div className="rounded-xl border border-border/70 bg-card p-3 shadow-xs">
                <span className="text-[11px] font-medium text-muted-foreground">Opening Balance</span>
                <p className="mt-1 font-mono text-sm font-semibold text-foreground">
                  {formatMoney(statement.openingBalance, currency)}
                </p>
              </div>

              <div className="rounded-xl border border-border/70 bg-card p-3 shadow-xs">
                <span className="text-[11px] font-medium text-muted-foreground">Total Invoiced (+)</span>
                <p className="mt-1 font-mono text-sm font-semibold text-blue-600 dark:text-blue-400">
                  {formatMoney(statement.totalInvoiced, currency)}
                </p>
              </div>

              <div className="rounded-xl border border-border/70 bg-card p-3 shadow-xs">
                <span className="text-[11px] font-medium text-muted-foreground">Total Paid (-)</span>
                <p className="mt-1 font-mono text-sm font-semibold text-emerald-600 dark:text-emerald-400">
                  {formatMoney(statement.totalPaid, currency)}
                </p>
              </div>

              <div className="rounded-xl border border-border/70 bg-card p-3 shadow-xs">
                <span className="text-[11px] font-medium text-muted-foreground">Total Returns (-)</span>
                <p className="mt-1 font-mono text-sm font-semibold text-purple-600 dark:text-purple-400">
                  {formatMoney(statement.totalReturns, currency)}
                </p>
              </div>

              <div className="col-span-2 sm:col-span-1 rounded-xl border border-primary/30 bg-primary/5 p-3 shadow-xs">
                <span className="text-[11px] font-medium text-primary">Closing Balance</span>
                <p className="mt-1 font-mono text-base font-bold text-primary">
                  {formatMoney(statement.closingBalance, currency)}
                </p>
              </div>
            </div>
          )}

          {/* Statement Ledger Table */}
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-muted-foreground">
              <Loader2 className="mr-2 size-5 animate-spin" /> Generating supplier ledger statement…
            </div>
          ) : !statement || statement.lines.length === 0 ? (
            <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
              No financial transactions found for this supplier within the selected period.
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-border/80 shadow-xs">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-border bg-muted/50 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                    <th className="py-2.5 px-3">Date</th>
                    <th className="py-2.5 px-3">Type</th>
                    <th className="py-2.5 px-3">Document #</th>
                    <th className="py-2.5 px-3">Ref #</th>
                    <th className="py-2.5 px-3 min-w-[200px]">Description</th>
                    <th className="py-2.5 px-3 text-right">Debit (Paid/Return)</th>
                    <th className="py-2.5 px-3 text-right">Credit (Billed)</th>
                    <th className="py-2.5 px-3 text-right font-bold">Running Balance</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {/* Opening Balance Row */}
                  <tr className="bg-muted/20 font-medium">
                    <td className="py-2 px-3 text-muted-foreground">{statement.fromDate ?? "Initial"}</td>
                    <td className="py-2 px-3">
                      <Badge variant="outline" className="text-[10px]">OPENING</Badge>
                    </td>
                    <td className="py-2 px-3 text-muted-foreground">—</td>
                    <td className="py-2 px-3 text-muted-foreground">—</td>
                    <td className="py-2 px-3 text-muted-foreground">Balance brought forward</td>
                    <td className="py-2 px-3 text-right text-muted-foreground font-mono">—</td>
                    <td className="py-2 px-3 text-right text-muted-foreground font-mono">—</td>
                    <td className="py-2 px-3 text-right font-mono font-semibold text-foreground">
                      {formatMoney(statement.openingBalance, currency)}
                    </td>
                  </tr>

                  {statement.lines.map((line, idx) => (
                    <tr key={`${line.documentType}-${line.documentId}-${idx}`} className="hover:bg-muted/30 transition-colors">
                      <td className="py-2 px-3 font-mono text-muted-foreground">{line.date}</td>
                      <td className="py-2 px-3">{getDocBadge(line.documentType)}</td>
                      <td className="py-2 px-3 font-mono font-medium text-foreground">{line.documentNumber}</td>
                      <td className="py-2 px-3 font-mono text-muted-foreground">{line.referenceNumber || "—"}</td>
                      <td className="py-2 px-3 text-muted-foreground">{line.description}</td>
                      <td className="py-2 px-3 text-right font-mono text-emerald-600 dark:text-emerald-400">
                        {line.debitAmount > 0 ? formatMoney(line.debitAmount, currency) : "—"}
                      </td>
                      <td className="py-2 px-3 text-right font-mono text-blue-600 dark:text-blue-400">
                        {line.creditAmount > 0 ? formatMoney(line.creditAmount, currency) : "—"}
                      </td>
                      <td className="py-2 px-3 text-right font-mono font-semibold text-foreground">
                        {formatMoney(line.runningBalance, currency)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

