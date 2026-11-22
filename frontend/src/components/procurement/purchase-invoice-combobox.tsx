"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, FileText, Loader2, Search, X } from "lucide-react";
import type { PurchaseInvoice } from "@/services/purchase-invoice.service";
import { cn } from "@/lib/utils";
import { formatMoney } from "@/lib/procurement-format";
import { safeAdToBsISO } from "@/lib/nepali-date";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export interface PurchaseInvoiceComboboxProps {
  value?: number | null;
  invoices: PurchaseInvoice[];
  disabled?: boolean;
  isLoading?: boolean;
  placeholder?: string;
  onSelect: (invoice: PurchaseInvoice | null) => void;
  error?: boolean;
  className?: string;
}

export function PurchaseInvoiceCombobox({
  value,
  invoices,
  disabled = false,
  isLoading = false,
  placeholder = "Search and select supplier invoice…",
  onSelect,
  error = false,
  className,
}: PurchaseInvoiceComboboxProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const selectedInvoice = useMemo(() => {
    if (!value) return null;
    return invoices.find((inv) => inv.id === value) ?? null;
  }, [invoices, value]);

  const filteredInvoices = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return invoices.slice(0, 50);

    return invoices
      .filter((inv) => {
        const invNum = inv.invoiceNumber?.toLowerCase() ?? "";
        const supName = inv.supplierName?.toLowerCase() ?? "";
        const supCode = inv.supplierCode?.toLowerCase() ?? "";
        const taxId = inv.supplierTaxId?.toLowerCase() ?? "";
        const grn = inv.grnNumber?.toLowerCase() ?? "";
        const po = inv.poNumber?.toLowerCase() ?? "";
        return (
          invNum.includes(term) ||
          supName.includes(term) ||
          supCode.includes(term) ||
          taxId.includes(term) ||
          grn.includes(term) ||
          po.includes(term)
        );
      })
      .slice(0, 50);
  }, [invoices, search]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          disabled={disabled || isLoading}
          className={cn(
            "h-10 w-full justify-between px-3 font-normal text-left shadow-none transition-colors",
            error && "border-destructive focus-visible:ring-destructive",
            !selectedInvoice && "text-muted-foreground",
            className
          )}
        >
          {isLoading ? (
            <div className="flex items-center gap-2 text-xs text-muted-foreground sm:text-sm">
              <Loader2 className="size-4 animate-spin text-muted-foreground" />
              <span>Loading purchase invoices…</span>
            </div>
          ) : selectedInvoice ? (
            <div className="flex min-w-0 items-center gap-2 truncate">
              <span className="shrink-0 rounded bg-destructive/10 px-1.5 py-0.5 font-mono text-xs font-semibold text-destructive">
                {selectedInvoice.invoiceNumber}
              </span>
              <span className="truncate text-sm font-medium text-foreground">
                {selectedInvoice.supplierName}
              </span>
              <span className="hidden shrink-0 text-xs text-muted-foreground sm:inline font-mono">
                ({formatMoney(selectedInvoice.totalAmount, selectedInvoice.currency)})
              </span>
            </div>
          ) : (
            <span className="text-xs sm:text-sm">{placeholder}</span>
          )}
          <div className="flex items-center gap-1">
            {selectedInvoice && !disabled ? (
              <span
                role="button"
                tabIndex={0}
                className="rounded p-0.5 hover:bg-muted text-muted-foreground hover:text-foreground"
                onClick={(e) => {
                  e.stopPropagation();
                  onSelect(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.stopPropagation();
                    onSelect(null);
                  }
                }}
              >
                <X className="size-3.5" />
              </span>
            ) : null}
            <ChevronsUpDown className="size-4 shrink-0 opacity-50" />
          </div>
        </Button>
      </PopoverTrigger>

      <PopoverContent
        align="start"
        className="w-[calc(100vw-2rem)] sm:w-[480px] md:w-[540px] p-0 shadow-lg"
      >
        <div className="p-2 border-b">
          <div className="flex items-center gap-2 px-2 py-1 rounded-md border bg-background focus-within:ring-1 focus-within:ring-ring">
            <Search className="size-4 text-muted-foreground shrink-0" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by invoice #, supplier name, PAN, GRN or PO…"
              className="h-7 border-0 p-0 text-xs shadow-none focus-visible:ring-0 placeholder:text-muted-foreground"
              autoFocus
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch("")}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="size-3.5" />
              </button>
            )}
          </div>
        </div>

        <div className="max-h-[300px] overflow-y-auto p-1 text-xs">
          {filteredInvoices.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              <FileText className="size-8 mx-auto mb-2 opacity-40" />
              <p className="font-medium">No purchase invoices found</p>
              <p className="text-[11px] mt-0.5">
                {search ? "Try adjusting your search criteria" : "No invoices available to return"}
              </p>
            </div>
          ) : (
            filteredInvoices.map((inv) => {
              const isSelected = selectedInvoice?.id === inv.id;
              const bsDate = safeAdToBsISO(inv.invoiceDate);

              return (
                <button
                  key={inv.id}
                  type="button"
                  onClick={() => {
                    onSelect(inv);
                    setOpen(false);
                  }}
                  className={cn(
                    "w-full text-left p-2.5 rounded-md hover:bg-accent/60 transition-colors flex items-start justify-between gap-3 group border border-transparent",
                    isSelected && "bg-accent/80 border-border"
                  )}
                >
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono font-bold text-foreground">
                        {inv.invoiceNumber}
                      </span>
                      <Badge variant="outline" className="text-[10px] py-0 px-1.5 font-normal">
                        {inv.status}
                      </Badge>
                      {inv.grnNumber && (
                        <span className="text-[10px] text-muted-foreground font-mono">
                          GRN: {inv.grnNumber}
                        </span>
                      )}
                    </div>

                    <div className="text-foreground font-medium truncate">
                      {inv.supplierName}
                    </div>

                    <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
                      <span>Date: {inv.invoiceDate} {bsDate ? `(${bsDate})` : ""}</span>
                      {inv.supplierTaxId && (
                        <span>PAN: {inv.supplierTaxId}</span>
                      )}
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <div className="font-mono font-semibold text-foreground">
                      {formatMoney(inv.totalAmount, inv.currency)}
                    </div>
                    <div className="text-[11px] text-primary font-mono font-medium">
                      Bal: {formatMoney(inv.balanceAmount, inv.currency)}
                    </div>
                  </div>

                  <div className="flex size-4 items-center justify-center shrink-0 self-center">
                    {isSelected && <Check className="size-4 text-primary" />}
                  </div>
                </button>
              );
            })
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}

