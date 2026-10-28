"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Trash2, Loader2, FileText, Search, Package, Check } from "lucide-react";
import {
  useCreateQuotationMutation,
  type CreateSalesQuotationRequest,
  type SalesQuotationLineRequest,
} from "@/services/sales-quotation.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { CreateCustomerDialog } from "@/components/sales/create-customer-dialog";
import {
  useGetActiveItemsQuery,
  useGetItemsQuery,
  type ItemListResponse,
} from "@/services/item.service";
import { useGetActiveCategoriesQuery } from "@/services/category.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { useGetStockOnHandQuery } from "@/services/stock.service";
import { formatMoney } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface DraftLine extends Omit<SalesQuotationLineRequest, "quantity"> {
  key: number;
  quantity: string;
}

function lineTotal(l: DraftLine): number {
  const gross = Number(l.quantity || 0) * (l.unitPrice || 0);
  const disc = (gross * (l.discountPercentage ?? 0)) / 100;
  const net = gross - disc;
  const tax = (net * (l.taxRate ?? 0)) / 100;
  return net + tax;
}

function itemTaxRate(item: ItemListResponse): number {
  if (item.taxabilityType !== "TAXABLE") return 0;
  return item.vatRate ?? 13;
}

export function CreateQuotationDialog({
  open,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}) {
  const [createQuotation, { isLoading }] = useCreateQuotationMutation();
  const { data: customers, isLoading: customersLoading } =
    useGetActiveCustomersQuery(undefined, { skip: !open });
  const { data: itemsPage, isLoading: itemsLoading } =
    useGetActiveItemsQuery({ size: 100 }, { skip: !open });
  const { data: warehouses, isLoading: warehousesLoading } =
    useGetActiveWarehousesQuery(undefined, { skip: !open });

  const allItems = React.useMemo(() => itemsPage?.content ?? [], [itemsPage]);

  const [customerId, setCustomerId] = React.useState("");
  const [customerSearch, setCustomerSearch] = React.useState("");
  const [customerDropdownOpen, setCustomerDropdownOpen] = React.useState(false);
  const [createCustomerOpen, setCreateCustomerOpen] = React.useState(false);
  const [warehouseId, setWarehouseId] = React.useState("");
  const [quotationDate, setQuotationDate] = React.useState(() => new Date().toISOString().slice(0, 10));
  const [validUntilDate, setValidUntilDate] = React.useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().slice(0, 10);
  });
  const [reference, setReference] = React.useState("");
  const [termsAndConditions, setTermsAndConditions] = React.useState(
    "1. Prices are inclusive of 13% VAT.\n2. Delivery within 7 business days from PO confirmation.\n3. Payment term: 30 days from invoice date."
  );
  const [notes, setNotes] = React.useState("");
  const [lines, setLines] = React.useState<DraftLine[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const nextKey = React.useRef(1);

  // Autocomplete state
  const [searchTerm, setSearchTerm] = React.useState("");
  const [debouncedSearch, setDebouncedSearch] = React.useState("");
  const [dropdownOpen, setDropdownOpen] = React.useState(false);
  const [highlighted, setHighlighted] = React.useState(0);
  const searchInputRef = React.useRef<HTMLInputElement>(null);

  // Browse modal state
  const [browseOpen, setBrowseOpen] = React.useState(false);
  const [browseSearch, setBrowseSearch] = React.useState("");
  const [browseCategory, setBrowseCategory] = React.useState("");
  const [selectedBrowse, setSelectedBrowse] = React.useState<Set<number>>(new Set());

  const filteredCustomers = React.useMemo(() => {
    const term = customerSearch.trim().toLowerCase();
    const list = customers ?? [];
    if (!term) return list.slice(0, 20);
    return list
      .filter(
        (c) =>
          c.name.toLowerCase().includes(term) ||
          c.code.toLowerCase().includes(term) ||
          (c.phone ?? "").toLowerCase().includes(term) ||
          (c.email ?? "").toLowerCase().includes(term) ||
          (c.panNumber ?? "").toLowerCase().includes(term)
      )
      .slice(0, 20);
  }, [customers, customerSearch]);

  const selectedCustomer = (customers ?? []).find((c) => String(c.id) === customerId);

  React.useEffect(() => {
    if (open) {
      setCustomerId("");
      setCustomerSearch("");
      setCustomerDropdownOpen(false);
      setCreateCustomerOpen(false);
      setWarehouseId("");
      setQuotationDate(new Date().toISOString().slice(0, 10));
      const d = new Date();
      d.setDate(d.getDate() + 30);
      setValidUntilDate(d.toISOString().slice(0, 10));
      setReference("");
      setNotes("");
      setLines([]);
      nextKey.current = 1;
      setError(null);
      setSearchTerm("");
      setDebouncedSearch("");
      setDropdownOpen(false);
      setBrowseOpen(false);
      setBrowseSearch("");
      setBrowseCategory("");
      setSelectedBrowse(new Set());
    }
  }, [open]);

  // Debounce autocomplete search
  React.useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchTerm), 300);
    return () => clearTimeout(t);
  }, [searchTerm]);

  const updateLine = (key: number, patch: Partial<DraftLine>) =>
    setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  const addItem = React.useCallback((item: ItemListResponse) => {
    setLines((prev) => {
      const existingIdx = prev.findIndex((l) => l.itemId === item.id);
      if (existingIdx >= 0) {
        const next = [...prev];
        const existing = next[existingIdx];
        if (existing) {
          next[existingIdx] = { ...existing, quantity: String(Number(existing.quantity || 0) + 1) };
        }
        return next;
      }
      const key = nextKey.current++;
      return [
        ...prev,
        {
          key,
          itemId: item.id,
          uomCode: item.uomCode ?? "PCS",
          quantity: "1",
          unitPrice: item.sellingPrice ?? 0,
          discountPercentage: 0,
          taxRate: itemTaxRate(item),
        },
      ];
    });
  }, []);

  const removeLine = (key: number) => setLines((ls) => ls.filter((x) => x.key !== key));

  // Stock map for selected warehouse
  const stockParams = warehouseId ? { warehouseId: Number(warehouseId), size: 1000 } : undefined;
  const { data: stockPage } = useGetStockOnHandQuery(stockParams, {
    skip: !open || !warehouseId,
  });
  const stockMap = React.useMemo(() => {
    const m = new Map<number, number>();
    for (const r of stockPage?.content ?? []) m.set(r.itemId, r.available);
    return m;
  }, [stockPage]);

  // Autocomplete results
  const searchQuery = debouncedSearch.trim();
  const { data: searchPage, isFetching: searchFetching } = useGetItemsQuery(
    { search: searchQuery, size: 20 },
    { skip: !open || searchQuery.length < 2 }
  );
  const autocompleteResults = React.useMemo(() => {
    if (searchQuery.length >= 2) {
      return (searchPage?.content ?? []).filter((i) => i.isActive);
    }
    return allItems.slice(0, 20);
  }, [searchQuery, searchPage, allItems]);
  const autocompleteLoading = searchQuery.length >= 2 ? searchFetching : itemsLoading;

  React.useEffect(() => {
    setHighlighted(0);
  }, [autocompleteResults.length, debouncedSearch]);

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!dropdownOpen) {
      if (e.key === "ArrowDown" || e.key === "Enter") setDropdownOpen(true);
      return;
    }
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlighted((i) => Math.min(i + 1, autocompleteResults.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlighted((i) => Math.max(i - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const item = autocompleteResults[highlighted];
      if (item) {
        addItem(item);
        setSearchTerm("");
        setDropdownOpen(false);
      }
    } else if (e.key === "Escape") {
      setDropdownOpen(false);
    }
  };

  // Browse modal data
  const browseSearchTerm = browseSearch.trim();
  const { data: browseSearchPage, isFetching: browseSearchFetching } = useGetItemsQuery(
    { search: browseSearchTerm, size: 100 },
    { skip: !browseOpen || browseSearchTerm.length < 2 }
  );
  const { data: browseActivePage, isLoading: browseActiveLoading } = useGetActiveItemsQuery(
    { size: 100 },
    { skip: !browseOpen || browseSearchTerm.length >= 2 }
  );
  const browseBaseItems = React.useMemo(() => {
    const raw =
      browseSearchTerm.length >= 2
        ? (browseSearchPage?.content ?? [])
        : (browseActivePage?.content ?? []);
    return raw.filter((i) => i.isActive);
  }, [browseSearchTerm, browseSearchPage, browseActivePage]);
  const browseFiltered = React.useMemo(() => {
    if (!browseCategory) return browseBaseItems;
    const catId = Number(browseCategory);
    return browseBaseItems.filter((i) => i.categoryId === catId);
  }, [browseBaseItems, browseCategory]);
  const browseLoading = browseSearchTerm.length >= 2 ? browseSearchFetching : browseActiveLoading;

  const { data: categories } = useGetActiveCategoriesQuery(undefined, { skip: !browseOpen });

  const toggleBrowseSelect = (id: number) => {
    setSelectedBrowse((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const addBrowseSelected = () => {
    const chosen = browseFiltered.filter((i) => selectedBrowse.has(i.id));
    for (const item of chosen) addItem(item);
    setSelectedBrowse(new Set());
    setBrowseOpen(false);
  };

  const validLines = lines.filter((l) => l.itemId > 0 && Number(l.quantity) > 0);

  const { subtotal, totalDiscount, taxableSubtotal, totalTax, grandTotal } = React.useMemo(() => {
    let sub = 0;
    let disc = 0;
    let tax = 0;

    for (const l of validLines) {
      const gross = Number(l.quantity) * l.unitPrice;
      const lineDisc = (gross * (l.discountPercentage ?? 0)) / 100;
      const net = gross - lineDisc;
      const lineTax = (net * (l.taxRate ?? 0)) / 100;

      sub += gross;
      disc += lineDisc;
      tax += lineTax;
    }

    return {
      subtotal: sub,
      totalDiscount: disc,
      taxableSubtotal: sub - disc,
      totalTax: tax,
      grandTotal: sub - disc + tax,
    };
  }, [validLines]);

  const canSubmit = !!customerId && validLines.length > 0 && !isLoading;

  const submit = async () => {
    setError(null);
    if (!customerId) return setError("Please select a customer.");
    if (validLines.length === 0) return setError("Add at least one item line.");

    const body: CreateSalesQuotationRequest = {
      customerId: Number(customerId),
      quotationDate,
      validUntilDate: validUntilDate || undefined,
      warehouseId: warehouseId ? Number(warehouseId) : undefined,
      reference: reference.trim() || undefined,
      termsAndConditions: termsAndConditions.trim() || undefined,
      notes: notes.trim() || undefined,
      lines: validLines.map(({ key: _, ...l }) => ({
        ...l,
        quantity: Number(l.quantity),
        discountPercentage: l.discountPercentage ?? 0,
        taxRate: l.taxRate ?? 13,
      })),
    };

    try {
      await createQuotation(body).unwrap();
      onOpenChange(false);
      onSuccess?.();
    } catch (e: unknown) {
      setError(getApiErrorMessage(e));
    }
  };

  const itemById = React.useMemo(() => {
    const m = new Map<number, ItemListResponse>();
    for (const it of allItems) m.set(it.id, it);
    return m;
  }, [allItems]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-5xl max-h-[92vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-primary" />
            New Sales Quotation / Price Estimate
          </DialogTitle>
          <DialogDescription>
            Issue a formal quotation or estimate to prospective or existing clients with pricing, discounts, and validity terms.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 text-sm py-1">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="grid gap-1">
              <Label>Customer *</Label>
              <div className="relative">
                <Input
                  placeholder={customersLoading ? "Loading customers…" : "Search customer by name, code, phone, email, PAN…"}
                  value={customerSearch}
                  onChange={(e) => {
                    setCustomerSearch(e.target.value);
                    setCustomerDropdownOpen(true);
                  }}
                  onFocus={() => setCustomerDropdownOpen(true)}
                  onBlur={() => setTimeout(() => setCustomerDropdownOpen(false), 150)}
                />
                {customerDropdownOpen && (
                  <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover shadow-md max-h-60 overflow-y-auto">
                    {customersLoading ? (
                      <div className="flex items-center gap-2 px-3 py-2 text-sm text-muted-foreground">
                        <Loader2 className="h-4 w-4 animate-spin" /> Loading…
                      </div>
                    ) : filteredCustomers.length === 0 ? (
                      <div className="px-3 py-2 text-sm text-muted-foreground">No matching customers</div>
                    ) : (
                      filteredCustomers.map((c) => (
                        <button
                          type="button"
                          key={c.id}
                          className="w-full text-left px-3 py-2 text-sm hover:bg-accent"
                          onMouseDown={(e) => {
                            e.preventDefault();
                            setCustomerId(String(c.id));
                            setCustomerSearch(c.name);
                            setCustomerDropdownOpen(false);
                          }}
                        >
                          <div className="font-medium truncate">{c.name}</div>
                          <div className="text-xs text-muted-foreground truncate">
                            {c.code}
                            {c.phone ? ` · ${c.phone}` : ""}
                            {c.panNumber ? ` · PAN ${c.panNumber}` : ""}
                          </div>
                        </button>
                      ))
                    )}
                    <button
                      type="button"
                      className="w-full text-left px-3 py-2 text-sm text-primary hover:bg-accent border-t"
                      onMouseDown={(e) => {
                        e.preventDefault();
                        setCustomerDropdownOpen(false);
                        setCreateCustomerOpen(true);
                      }}
                    >
                      + Create new customer
                    </button>
                  </div>
                )}
              </div>
              {selectedCustomer && (
                <p className="text-[10px] text-muted-foreground truncate">
                  Selected: {selectedCustomer.name} ({selectedCustomer.code})
                  {selectedCustomer.panNumber ? ` · PAN ${selectedCustomer.panNumber}` : ""}
                </p>
              )}
            </div>

            <div className="grid gap-1">
              <Label>Quotation Date *</Label>
              <Input
                type="date"
                value={quotationDate}
                onChange={(e) => setQuotationDate(e.target.value)}
              />
            </div>

            <div className="grid gap-1">
              <Label>Valid Until</Label>
              <Input
                type="date"
                value={validUntilDate}
                onChange={(e) => setValidUntilDate(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Warehouse / Branch</Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger>
                  <SelectValue placeholder={warehousesLoading ? "Loading…" : "Select fulfillment warehouse"} />
                </SelectTrigger>
                <SelectContent>
                  {warehousesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (warehouses ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No warehouses configured</div>
                  ) : (
                    (warehouses ?? []).map((w) => (
                      <SelectItem key={w.id} value={String(w.id)}>
                        {w.name} ({w.code})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-1">
              <Label>Customer Inquiry / RFQ Ref</Label>
              <Input
                placeholder="e.g. RFQ-OCT-2081 or Email Ref"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2 pt-1 border-t">
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <Label className="text-base font-semibold">Quoted Products & Services</Label>
              <div className="flex items-center gap-2">
                <div className="relative w-72">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    ref={searchInputRef}
                    placeholder="Search by name, SKU, or barcode…"
                    value={searchTerm}
                    onChange={(e) => {
                      setSearchTerm(e.target.value);
                      setDropdownOpen(true);
                    }}
                    onFocus={() => setDropdownOpen(true)}
                    onBlur={() => setTimeout(() => setDropdownOpen(false), 150)}
                    onKeyDown={handleSearchKeyDown}
                    className="pl-8"
                    aria-label="Search products"
                    aria-autocomplete="list"
                    aria-expanded={dropdownOpen}
                  />
                  {dropdownOpen && (
                    <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover shadow-md max-h-72 overflow-y-auto">
                      {autocompleteLoading ? (
                        <div className="flex items-center gap-2 px-3 py-2 text-sm text-muted-foreground">
                          <Loader2 className="h-4 w-4 animate-spin" /> Searching…
                        </div>
                      ) : autocompleteResults.length === 0 ? (
                        <div className="px-3 py-2 text-sm text-muted-foreground">
                          {searchTerm.trim().length >= 2
                            ? "No matching products found"
                            : "No products available"}
                        </div>
                      ) : (
                        autocompleteResults.map((it, idx) => {
                          const stock = stockMap.get(it.id);
                          return (
                            <button
                              type="button"
                              key={it.id}
                              className={`w-full text-left px-3 py-2 text-sm flex items-center justify-between gap-2 hover:bg-accent ${
                                idx === highlighted ? "bg-accent" : ""
                              }`}
                              onMouseEnter={() => setHighlighted(idx)}
                              onMouseDown={(e) => {
                                e.preventDefault();
                                addItem(it);
                                setSearchTerm("");
                                setDropdownOpen(false);
                              }}
                            >
                              <div className="min-w-0">
                                <div className="font-medium truncate">{it.nameEn}</div>
                                <div className="text-xs text-muted-foreground truncate">
                                  {it.sku}
                                  {it.barcode ? ` · ${it.barcode}` : ""}
                                  {it.uomCode ? ` · ${it.uomCode}` : ""}
                                </div>
                              </div>
                              <div className="text-right shrink-0">
                                <div className="font-mono text-xs">{formatMoney(it.sellingPrice)}</div>
                                {warehouseId && stockMap.has(it.id) && (
                                  <div className="text-[10px] text-muted-foreground">
                                    Stock: {stock ?? 0}
                                  </div>
                                )}
                              </div>
                            </button>
                          );
                        })
                      )}
                    </div>
                  )}
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setBrowseOpen(true)}
                >
                  <Package className="mr-1 h-3.5 w-3.5" /> Browse All Items
                </Button>
              </div>
            </div>

            {lines.length === 0 ? (
              <div className="rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground">
                Search above or browse all items to add products and services to this quotation.
              </div>
            ) : (
              <>
                <div className="grid grid-cols-12 gap-2 text-xs font-medium text-muted-foreground px-1">
                  <div className="col-span-4">Item</div>
                  <div className="col-span-1">UOM</div>
                  <div className="col-span-2 text-right">Qty</div>
                  <div className="col-span-2 text-right">Unit Price</div>
                  <div className="col-span-1 text-right">Disc %</div>
                  <div className="col-span-1 text-right">VAT %</div>
                  <div className="col-span-1 text-center" />
                </div>

                <div className="space-y-2">
                  {lines.map((l) => {
                    const item = itemById.get(l.itemId);
                    return (
                      <div key={l.key} className="grid grid-cols-12 items-center gap-2 p-1.5 rounded-md border bg-card">
                        <div className="col-span-4">
                          <div className="text-sm font-medium truncate">
                            {item?.nameEn ?? (l.itemId ? `Item #${l.itemId}` : "—")}
                          </div>
                          <div className="text-[10px] text-muted-foreground truncate">
                            {item?.sku ?? ""}
                            {warehouseId && stockMap.has(l.itemId) && (
                              <span className="ml-2">Stock: {stockMap.get(l.itemId) ?? 0}</span>
                            )}
                          </div>
                          <div className="text-[10px] text-muted-foreground">
                            Line total: {formatMoney(lineTotal(l))}
                          </div>
                        </div>
                        <Input
                          className="col-span-1 text-xs"
                          placeholder="UOM"
                          value={l.uomCode}
                          onChange={(e) => updateLine(l.key, { uomCode: e.target.value })}
                        />
                        <Input
                          className="col-span-2 text-right"
                          type="number"
                          min="0"
                          step="any"
                          inputMode="decimal"
                          value={l.quantity}
                          onChange={(e) => updateLine(l.key, { quantity: e.target.value })}
                        />
                        <Input
                          className="col-span-2 text-right"
                          type="number"
                          min="0"
                          step="any"
                          value={l.unitPrice}
                          onChange={(e) => updateLine(l.key, { unitPrice: Number(e.target.value) })}
                        />
                        <Input
                          className="col-span-1 text-right text-xs"
                          type="number"
                          min="0"
                          max="100"
                          value={l.discountPercentage ?? 0}
                          onChange={(e) => updateLine(l.key, { discountPercentage: Number(e.target.value) })}
                        />
                        <Input
                          className="col-span-1 text-right text-xs"
                          type="number"
                          min="0"
                          max="100"
                          value={l.taxRate ?? 13}
                          onChange={(e) => updateLine(l.key, { taxRate: Number(e.target.value) })}
                        />
                        <div className="col-span-1 flex justify-center">
                          <Button
                            size="icon"
                            variant="ghost"
                            className="h-8 w-8 text-muted-foreground hover:text-red-600"
                            onClick={() => removeLine(l.key)}
                            aria-label="Remove line"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-3 border-t">
            <div className="space-y-2">
              <div className="space-y-1">
                <Label>Terms & Conditions</Label>
                <textarea
                  rows={3}
                  className="w-full rounded-md border border-input bg-transparent px-3 py-1.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  value={termsAndConditions}
                  onChange={(e) => setTermsAndConditions(e.target.value)}
                />
              </div>
              <div className="space-y-1">
                <Label>Internal Remarks</Label>
                <Input
                  placeholder="e.g. Special price approved by Sales Director"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </div>
            </div>

            <div className="rounded-lg border bg-muted/30 p-3 space-y-1.5 text-xs">
              <div className="flex justify-between text-muted-foreground">
                <span>Gross Quoted:</span>
                <span className="font-mono tabular-nums">{formatMoney(subtotal)}</span>
              </div>
              {totalDiscount > 0 && (
                <div className="flex justify-between text-muted-foreground">
                  <span>Discount:</span>
                  <span className="font-mono tabular-nums text-emerald-600">-{formatMoney(totalDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between text-muted-foreground">
                <span>Taxable Subtotal:</span>
                <span className="font-mono tabular-nums">{formatMoney(taxableSubtotal)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>VAT:</span>
                <span className="font-mono tabular-nums">{formatMoney(totalTax)}</span>
              </div>
              <div className="flex justify-between font-bold text-sm text-foreground pt-1.5 border-t">
                <span>Grand Total (NPR):</span>
                <span className="font-mono text-base tabular-nums">{formatMoney(grandTotal)}</span>
              </div>
            </div>
          </div>

          {error ? <p className="text-sm text-red-600 font-medium">{error}</p> : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} disabled={!canSubmit}>
            {isLoading ? "Saving…" : "Generate Quotation"}
          </Button>
        </DialogFooter>
      </DialogContent>

      <CreateCustomerDialog
        open={createCustomerOpen}
        onOpenChange={setCreateCustomerOpen}
        onCreated={(c) => {
          setCustomerId(String(c.id));
          setCustomerSearch(c.name);
          setCreateCustomerOpen(false);
        }}
      />

      {/* Browse All Items modal */}
      <Dialog open={browseOpen} onOpenChange={setBrowseOpen}>
        <DialogContent className="sm:max-w-3xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Package className="h-5 w-5 text-primary" />
              Browse All Items
            </DialogTitle>
            <DialogDescription>
              Search, filter by category, and select multiple products to add to the quotation.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-3 text-sm">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name, SKU, or barcode…"
                  value={browseSearch}
                  onChange={(e) => setBrowseSearch(e.target.value)}
                  className="pl-8"
                />
              </div>
              <Select value={browseCategory} onValueChange={setBrowseCategory}>
                <SelectTrigger>
                  <SelectValue placeholder="All categories" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All categories</SelectItem>
                  {(categories ?? []).map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {browseLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : browseFiltered.length === 0 ? (
              <div className="rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground">
                No items found. Try a different search or category.
              </div>
            ) : (
              <div className="max-h-80 overflow-y-auto rounded-md border">
                <table className="w-full text-sm">
                  <thead className="sticky top-0 bg-muted/50">
                    <tr className="text-xs text-muted-foreground">
                      <th className="w-8 px-2 py-1.5" />
                      <th className="text-left px-2 py-1.5">Product</th>
                      <th className="text-left px-2 py-1.5">SKU</th>
                      <th className="text-left px-2 py-1.5">UOM</th>
                      <th className="text-right px-2 py-1.5">Price</th>
                      {warehouseId && <th className="text-right px-2 py-1.5">Stock</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {browseFiltered.map((it) => {
                      const checked = selectedBrowse.has(it.id);
                      const stock = stockMap.get(it.id);
                      return (
                        <tr
                          key={it.id}
                          className="border-t hover:bg-accent/50 cursor-pointer"
                          onClick={() => toggleBrowseSelect(it.id)}
                        >
                          <td className="px-2 py-1.5">
                            <div
                              className={`h-4 w-4 rounded border flex items-center justify-center ${
                                checked ? "bg-primary border-primary text-primary-foreground" : "border-input"
                              }`}
                            >
                              {checked && <Check className="h-3 w-3" />}
                            </div>
                          </td>
                          <td className="px-2 py-1.5">
                            <div className="font-medium truncate">{it.nameEn}</div>
                            <div className="text-[10px] text-muted-foreground truncate">
                              {it.categoryName ?? "Uncategorized"} · {it.itemType ?? "—"}
                            </div>
                          </td>
                          <td className="px-2 py-1.5 text-xs text-muted-foreground">{it.sku}</td>
                          <td className="px-2 py-1.5 text-xs">{it.uomCode ?? "—"}</td>
                          <td className="px-2 py-1.5 text-right font-mono text-xs">
                            {formatMoney(it.sellingPrice)}
                          </td>
                          {warehouseId && (
                            <td className="px-2 py-1.5 text-right text-xs text-muted-foreground">
                              {stockMap.has(it.id) ? stock ?? 0 : "—"}
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setBrowseOpen(false)}>Cancel</Button>
            <Button
              onClick={addBrowseSelected}
              disabled={selectedBrowse.size === 0}
            >
              Add Selected ({selectedBrowse.size})
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Dialog>
  );
}
