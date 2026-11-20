"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, Building2, Search, X, Loader2 } from "lucide-react";
import type { SupplierListResponse } from "@/services/supplier.service";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export interface SupplierComboboxProps {
  value?: number | string | null;
  suppliers: SupplierListResponse[];
  disabled?: boolean;
  isLoading?: boolean;
  placeholder?: string;
  onSelect: (supplier: SupplierListResponse) => void;
  error?: boolean;
  className?: string;
}

export function SupplierCombobox({
  value,
  suppliers,
  disabled = false,
  isLoading = false,
  placeholder = "Search and select supplier…",
  onSelect,
  error = false,
  className,
}: SupplierComboboxProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const selectedSupplier = useMemo(() => {
    if (!value) return null;
    return suppliers.find((s) => String(s.id) === String(value)) ?? null;
  }, [suppliers, value]);

  const filteredSuppliers = useMemo(() => {
    const term = search.trim().toLowerCase();
    const activeSuppliers = suppliers.filter((s) => s.isActive);
    if (!term) return activeSuppliers.slice(0, 50);

    return activeSuppliers
      .filter((s) => {
        const name = s.name?.toLowerCase() ?? "";
        const code = s.code?.toLowerCase() ?? "";
        const taxId = s.taxId?.toLowerCase() ?? "";
        const phone = s.phone?.toLowerCase() ?? "";
        const email = s.email?.toLowerCase() ?? "";
        return (
          name.includes(term) ||
          code.includes(term) ||
          taxId.includes(term) ||
          phone.includes(term) ||
          email.includes(term)
        );
      })
      .slice(0, 50);
  }, [suppliers, search]);

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
            !selectedSupplier && "text-muted-foreground",
            className
          )}
        >
          {isLoading ? (
            <div className="flex items-center gap-2 text-xs text-muted-foreground sm:text-sm">
              <Loader2 className="size-4 animate-spin text-muted-foreground" />
              <span>Loading suppliers…</span>
            </div>
          ) : selectedSupplier ? (
            <div className="flex min-w-0 items-center gap-2 truncate">
              <span className="shrink-0 rounded bg-primary/10 px-1.5 py-0.5 font-mono text-xs font-semibold text-primary">
                {selectedSupplier.code}
              </span>
              <span className="truncate text-sm font-medium text-foreground">
                {selectedSupplier.name}
              </span>
              {selectedSupplier.taxId ? (
                <span className="hidden shrink-0 text-xs text-muted-foreground sm:inline">
                  (PAN: {selectedSupplier.taxId})
                </span>
              ) : null}
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs text-muted-foreground sm:text-sm">
              <Search className="size-4 shrink-0 opacity-50" />
              <span className="truncate">{placeholder}</span>
            </div>
          )}
          <ChevronsUpDown className="ml-1 size-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>

      <PopoverContent
        align="start"
        sideOffset={4}
        className="w-[calc(100vw-2.5rem)] p-0 sm:w-[480px]"
      >
        <div className="flex items-center border-b border-border px-3 py-2">
          <Search className="mr-2 size-4 shrink-0 text-muted-foreground" />
          <Input
            autoFocus
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by supplier name, code, PAN, phone…"
            className="h-8 border-none bg-transparent p-0 text-xs shadow-none focus-visible:ring-0 sm:text-sm"
          />
          {search ? (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-6 text-muted-foreground hover:text-foreground"
              onClick={() => setSearch("")}
            >
              <X className="size-3.5" />
            </Button>
          ) : null}
        </div>

        <div className="max-h-64 overflow-y-auto p-1.5 sm:max-h-72">
          {filteredSuppliers.length === 0 ? (
            <div className="py-7 text-center text-xs text-muted-foreground">
              <Building2 className="mx-auto mb-1.5 size-6 opacity-40" />
              {search ? (
                <>No suppliers matching &ldquo;{search}&rdquo;</>
              ) : (
                <>No active suppliers found</>
              )}
            </div>
          ) : (
            filteredSuppliers.map((supplier) => {
              const isSelected = String(supplier.id) === String(value);
              return (
                <button
                  key={supplier.id}
                  type="button"
                  onClick={() => {
                    onSelect(supplier);
                    setOpen(false);
                    setSearch("");
                  }}
                  className={cn(
                    "flex w-full items-start justify-between gap-2 rounded-md px-3 py-2.5 text-left text-xs transition-colors hover:bg-accent",
                    isSelected && "bg-accent/80 font-medium"
                  )}
                >
                  <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <div className="flex items-center gap-2">
                      <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground">
                        {supplier.code}
                      </span>
                      <span className="truncate text-sm font-medium text-foreground">
                        {supplier.name}
                      </span>
                      {isSelected ? (
                        <Check className="size-4 shrink-0 text-primary" />
                      ) : null}
                    </div>

                    <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-muted-foreground">
                      {supplier.taxId ? (
                        <span>PAN: {supplier.taxId}</span>
                      ) : null}
                      {supplier.phone ? (
                        <span>• Tel: {supplier.phone}</span>
                      ) : null}
                      {supplier.email ? (
                        <span className="truncate">• {supplier.email}</span>
                      ) : null}
                    </div>
                  </div>

                  {supplier.type ? (
                    <span className="shrink-0 rounded bg-muted/80 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                      {supplier.type}
                    </span>
                  ) : null}
                </button>
              );
            })
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}

