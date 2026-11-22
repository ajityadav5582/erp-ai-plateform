"use client";

import { useState } from "react";
import type { Control, UseFormSetValue, UseFormWatch } from "react-hook-form";
import {
  Boxes,
  ChevronDown,
  Layers,
  Package,
  Sparkles,
  Wrench,
  Zap,
} from "lucide-react";
import {
  ITEM_TYPE_LABELS,
  TAXABILITY_TYPES,
  TAXABILITY_LABELS,
  type ItemType,
} from "@/services/item.service";
import { useGetActiveCategoriesQuery } from "@/services/category.service";
import { useGetActiveUnitsQuery } from "@/services/unit.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { useSelectedCompany } from "@/config/company-context";

import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { type ItemFormValues } from "@/components/items/item-form";

const NO_CATEGORY = "__none__";

interface ItemFormFieldsProps {
  control: Control<ItemFormValues>;
  watch: UseFormWatch<ItemFormValues>;
  setValue?: UseFormSetValue<ItemFormValues>;
  disabled: boolean;
  isEdit?: boolean;
}

export function ItemFormFields({
  control,
  watch,
  setValue,
  disabled,
  isEdit = false,
}: ItemFormFieldsProps) {
  const { data: categories = [] } = useGetActiveCategoriesQuery();
  const { data: units = [] } = useGetActiveUnitsQuery();
  const { data: activeWarehouses = [] } = useGetActiveWarehousesQuery();
  const selectedCompany = useSelectedCompany();
  const businessType = selectedCompany?.businessType;

  const [moreOptionsOpen, setMoreOptionsOpen] = useState(false);

  const currentType = watch("itemType") ?? "GOODS";
  const taxabilityType = watch("taxabilityType");
  const isInventoryTracked = watch("isInventoryTracked");
  const isBatchTracked = watch("isBatchTracked");
  const isSellable = watch("isSellable");
  const isPurchasable = watch("isPurchasable");

  const expiryTrackingEnabled = isInventoryTracked && isBatchTracked;

  // Handle archetype preset selection
  const handleSelectArchetype = (type: ItemType) => {
    if (!setValue || disabled) return;

    setValue("itemType", type);

    if (type === "SERVICE") {
      setValue("isPurchasable", true);
      // If the company is primarily a service provider, services are sellable by default
      setValue("isSellable", businessType === "SERVICES");
      setValue("isInventoryTracked", false);
      setValue("isBatchTracked", false);
      setValue("isExpiryTracked", false);
      setValue("isComponent", false);
      setValue("isManufactured", false);
    } else if (type === "RAW_MATERIAL") {
      setValue("isPurchasable", true);
      setValue("isSellable", false);
      setValue("isInventoryTracked", true);
      setValue("isComponent", true);
      setValue("isManufactured", false);
    } else if (type === "FINISHED_GOODS") {
      setValue("isPurchasable", false);
      setValue("isSellable", true);
      setValue("isInventoryTracked", true);
      setValue("isComponent", false);
      setValue("isManufactured", true);
    } else if (type === "CONSUMABLE") {
      setValue("isPurchasable", true);
      setValue("isSellable", false);
      setValue("isInventoryTracked", true);
      setValue("isComponent", false);
      setValue("isManufactured", false);
    } else {
      // GOODS (Standard trading product)
      setValue("isPurchasable", true);
      setValue("isSellable", true);
      setValue("isInventoryTracked", true);
      if (businessType === "PHARMACY") {
        setValue("isBatchTracked", true);
        setValue("isExpiryTracked", true);
      } else {
        setValue("isBatchTracked", false);
        setValue("isExpiryTracked", false);
      }
      setValue("isComponent", false);
      setValue("isManufactured", false);
    }
  };

  const handleGenerateSku = () => {
    if (!setValue || disabled) return;
    const prefix =
      currentType === "SERVICE"
        ? "SRV"
        : currentType === "RAW_MATERIAL"
        ? "RAW"
        : "ITM";
    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    setValue("sku", `${prefix}-${randomSuffix}`);
  };

  return (
    <div className="space-y-6">
      {/* ---------------- 1. Item Type Selector ---------------- */}
      <div className="space-y-2">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
          <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            1. What type of item is this?
          </label>
          {businessType && (
            <span className="text-[11px] font-medium text-primary">
              {businessType === "SERVICES" && "💡 Service Mode: Pre-configured for services & overheads"}
              {businessType === "PHARMACY" && "💡 Pharmacy Mode: Pre-configured with Batch & Expiry tracking"}
              {(businessType === "RESTAURANT" || businessType === "CAFE") && "💡 F&B Mode: Raw Material for kitchen, Goods for menu items"}
              {(businessType === "GENERAL_RETAIL" || businessType === "GROCERY" || businessType === "HARDWARE" || businessType === "SANITARY" || businessType === "CLOTHING") && "💡 Retail Mode: Inventory-tracked goods with barcode"}
            </span>
          )}
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
          {/* Goods */}
          <button
            type="button"
            disabled={disabled}
            onClick={() => handleSelectArchetype("GOODS")}
            className={`flex flex-col items-start p-3 rounded-xl border text-left transition-all ${
              currentType === "GOODS"
                ? "border-primary bg-primary/5 ring-1 ring-primary shadow-xs"
                : "border-border/80 hover:border-primary/40 hover:bg-muted/30"
            }`}
          >
            <div className="flex items-center gap-2 mb-1">
              <Package className="size-4 text-primary" />
              <span className="text-sm font-semibold text-foreground">Goods (सामान)</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Physical product bought, inventoried & sold to customers.
            </p>
          </button>

          {/* Service */}
          <button
            type="button"
            disabled={disabled}
            onClick={() => handleSelectArchetype("SERVICE")}
            className={`flex flex-col items-start p-3 rounded-xl border text-left transition-all ${
              currentType === "SERVICE"
                ? "border-primary bg-primary/5 ring-1 ring-primary shadow-xs"
                : "border-border/80 hover:border-primary/40 hover:bg-muted/30"
            }`}
          >
            <div className="flex items-center gap-2 mb-1">
              <Zap className="size-4 text-amber-500" />
              <span className="text-sm font-semibold text-foreground">Service / Expense</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Non-inventory service (IT SaaS, Rent, Maintenance, Utilities).
            </p>
          </button>

          {/* Raw Material */}
          <button
            type="button"
            disabled={disabled}
            onClick={() => handleSelectArchetype("RAW_MATERIAL")}
            className={`flex flex-col items-start p-3 rounded-xl border text-left transition-all ${
              currentType === "RAW_MATERIAL"
                ? "border-primary bg-primary/5 ring-1 ring-primary shadow-xs"
                : "border-border/80 hover:border-primary/40 hover:bg-muted/30"
            }`}
          >
            <div className="flex items-center gap-2 mb-1">
              <Wrench className="size-4 text-emerald-600 dark:text-emerald-400" />
              <span className="text-sm font-semibold text-foreground">Raw Material</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Input ingredient / supply consumed in kitchen or production.
            </p>
          </button>
        </div>

        {/* Specialized Item Type Dropdown if needed */}
        {currentType !== "GOODS" &&
          currentType !== "SERVICE" &&
          currentType !== "RAW_MATERIAL" && (
            <div className="mt-2 flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Current specialized type:</span>
              <Badge variant="secondary">{ITEM_TYPE_LABELS[currentType]}</Badge>
            </div>
          )}
      </div>

      {/* ---------------- 2. Basic Identification ---------------- */}
      <div className="space-y-3 rounded-xl border border-border/80 bg-muted/20 p-4">
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground block">
          2. Basic Information
        </label>

        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
          {/* Item Name */}
          <FormField
            control={control}
            name="nameEn"
            render={({ field }) => (
              <FormItem className="sm:col-span-2">
                <FormLabel className="text-xs font-medium">
                  Item Name (English) <span className="text-destructive">*</span>
                </FormLabel>
                <FormControl>
                  <Input
                    placeholder="e.g. Cloud Hosting, Wheat Flour, Mineral Water"
                    disabled={disabled}
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* SKU / Code */}
          <FormField
            control={control}
            name="sku"
            render={({ field }) => (
              <FormItem>
                <div className="flex items-center justify-between">
                  <FormLabel className="text-xs font-medium">
                    Item Code / SKU <span className="text-destructive">*</span>
                  </FormLabel>
                  <button
                    type="button"
                    onClick={handleGenerateSku}
                    disabled={disabled}
                    className="flex items-center gap-1 text-[11px] text-primary hover:underline font-medium"
                  >
                    <Sparkles className="size-3" />
                    Auto-generate
                  </button>
                </div>
                <FormControl>
                  <Input
                    placeholder="e.g. ITM-1001, SRV-0042"
                    disabled={disabled}
                    className="font-mono text-xs uppercase"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Unit of Measure */}
          <FormField
            control={control}
            name="uomId"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="text-xs font-medium">
                  Primary Unit of Measure (UOM) <span className="text-destructive">*</span>
                </FormLabel>
                <Select
                  value={field.value ?? ""}
                  onValueChange={field.onChange}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger className="text-xs">
                      <SelectValue placeholder="Select unit (Pcs, Kg, Month…)" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent className="max-h-56">
                    {units.map((unit) => (
                      <SelectItem key={unit.id} value={String(unit.id)}>
                        {unit.name} ({unit.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Secondary / Packaging Unit */}
          <FormField
            control={control}
            name="secondaryUomId"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="text-xs font-medium text-muted-foreground">
                  Secondary / Packaging Unit (Optional)
                </FormLabel>
                <Select
                  value={field.value || NO_CATEGORY}
                  onValueChange={(val) => field.onChange(val === NO_CATEGORY ? "" : val)}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger className="text-xs">
                      <SelectValue placeholder="e.g. Box, Carton, Pack" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent className="max-h-56">
                    <SelectItem value={NO_CATEGORY}>
                      <span className="text-muted-foreground">None (Base Unit Only)</span>
                    </SelectItem>
                    {units.map((unit) => (
                      <SelectItem key={unit.id} value={String(unit.id)}>
                        {unit.name} ({unit.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Conversion Factor */}
          {Boolean(watch("secondaryUomId")) && (
            <FormField
              control={control}
              name="conversionFactor"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel className="text-xs font-medium">
                    Packaging Conversion Factor <span className="text-destructive">*</span>
                  </FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0.0001}
                      step="any"
                      placeholder="e.g. 24 (1 Box = 24 Pcs)"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <p className="text-[11px] text-muted-foreground">
                    Number of primary units contained in 1 secondary unit.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          {/* Category */}
          <FormField
            control={control}
            name="categoryId"
            render={({ field }) => (
              <FormItem className="sm:col-span-2">
                <FormLabel className="text-xs font-medium text-muted-foreground">
                  Category (Optional)
                </FormLabel>
                <Select
                  value={field.value ?? NO_CATEGORY}
                  onValueChange={(val) =>
                    field.onChange(val === NO_CATEGORY ? "" : val)
                  }
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger className="text-xs">
                      <SelectValue placeholder="Select a category" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent className="max-h-56">
                    <SelectItem value={NO_CATEGORY}>
                      <span className="text-muted-foreground">None (No Category)</span>
                    </SelectItem>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={String(cat.id)}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
      </div>

      {/* ---------------- 3. Pricing & Tax ---------------- */}
      <div className="space-y-3 rounded-xl border border-border/80 bg-muted/20 p-4">
        <div className="flex items-center justify-between">
          <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground block">
            3. Pricing & Tax Treatment
          </label>
        </div>

        {/* For services: toggle whether service is also billed to clients */}
        {currentType === "SERVICE" && (
          <div className="rounded-lg border border-amber-500/20 bg-amber-500/5 p-3 flex items-start gap-2.5">
            <Checkbox
              id="sellServiceToggle"
              checked={isSellable}
              onCheckedChange={(checked) => {
                setValue?.("isSellable", Boolean(checked));
              }}
              disabled={disabled}
              className="mt-0.5"
            />
            <div className="space-y-0.5">
              <label
                htmlFor="sellServiceToggle"
                className="text-xs font-semibold text-foreground cursor-pointer"
              >
                I also bill / sell this service to my clients
              </label>
              <p className="text-[11px] text-muted-foreground">
                Leave unchecked if this is strictly an internal overhead expense (e.g. Office Rent, Electricity, Server Cost).
              </p>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
          {/* Selling Price - visible only if sellable */}
          {isSellable && (
            <FormField
              control={control}
              name="sellingPrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium">
                    Retail Selling Price (NPR) <span className="text-destructive">*</span>
                  </FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      placeholder="0.00"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          {/* Wholesale Price */}
          {isSellable && (
            <FormField
              control={control}
              name="wholesalePrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium text-muted-foreground">
                    Wholesale Price (NPR)
                  </FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      placeholder="0.00"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <p className="text-[11px] text-muted-foreground">
                    Tiered rate for bulk / distributor orders.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          {/* Minimum Selling Price Floor */}
          {isSellable && (
            <FormField
              control={control}
              name="minSellingPrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium text-muted-foreground">
                    Min Selling Price Floor (NPR)
                  </FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      placeholder="0.00"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <p className="text-[11px] text-muted-foreground">
                    Safety floor to guard against excessive line discounts.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          {/* Purchase Price - always useful for purchasable items */}
          {isPurchasable && (
            <FormField
              control={control}
              name="purchasePrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium text-muted-foreground">
                    Purchase / Cost Price (NPR)
                  </FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      placeholder="0.00"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <p className="text-[11px] text-muted-foreground">
                    Default cost on Purchase Orders and Direct Invoices.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          {/* Selling Price VAT Inclusive Checkbox (only if selling price active) */}
          {isSellable && (
            <div className="sm:col-span-2">
              <FormField
                control={control}
                name="isVatInclusive"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2 space-y-0">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                        disabled={disabled}
                      />
                    </FormControl>
                    <FormLabel className="text-xs text-muted-foreground cursor-pointer font-normal">
                      Selling price already includes VAT
                    </FormLabel>
                  </FormItem>
                )}
              />
            </div>
          )}

          {/* VAT Treatment */}
          <FormField
            control={control}
            name="taxabilityType"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="text-xs font-medium">
                  VAT Treatment <span className="text-destructive">*</span>
                </FormLabel>
                <Select
                  value={field.value ?? "TAXABLE"}
                  onValueChange={(val) => {
                    field.onChange(val);
                    if (val === "EXEMPT" || val === "ZERO_RATED") {
                      setValue?.("vatRate", "0");
                    } else if (val === "TAXABLE") {
                      setValue?.("vatRate", "13");
                    }
                  }}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger className="text-xs">
                      <SelectValue placeholder="Select VAT treatment" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {TAXABILITY_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {TAXABILITY_LABELS[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* VAT Rate */}
          <FormField
            control={control}
            name="vatRate"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="text-xs font-medium">VAT Rate (%)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    max={100}
                    step="0.01"
                    placeholder="13.00"
                    disabled={disabled || taxabilityType !== "TAXABLE"}
                    {...field}
                  />
                </FormControl>
                <p className="text-[11px] text-muted-foreground">
                  {taxabilityType === "TAXABLE"
                    ? "Nepal Standard VAT is 13%."
                    : "Zero for Exempt or Zero-Rated supplies."}
                </p>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
      </div>

      {/* ---------------- 3b. Opening Stock (Create only, when inventory tracked) ---------------- */}
      {!isEdit && isInventoryTracked && (
        <div className="rounded-xl border border-dashed border-primary/30 bg-primary/5 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Boxes className="size-4 text-primary" />
              <h3 className="text-xs font-semibold text-primary">Opening Stock (Optional)</h3>
            </div>
            <span className="text-[11px] text-muted-foreground">Auto-records opening inventory balance</span>
          </div>

          <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-3">
            {/* Opening Warehouse */}
            <FormField
              control={control}
              name="openingWarehouseId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium">
                    Warehouse {watch("openingQuantity") && Number(watch("openingQuantity")) > 0 && <span className="text-destructive">*</span>}
                  </FormLabel>
                  <Select
                    value={field.value ?? ""}
                    onValueChange={field.onChange}
                    disabled={disabled}
                  >
                    <FormControl>
                      <SelectTrigger className="text-xs">
                        <SelectValue placeholder="Select warehouse" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {activeWarehouses.map((wh) => (
                        <SelectItem key={wh.id} value={String(wh.id)}>
                          {wh.name} ({wh.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Opening Quantity */}
            <FormField
              control={control}
              name="openingQuantity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium">Opening Quantity</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="any"
                      placeholder="0"
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Opening Cost / Valuation Price */}
            <FormField
              control={control}
              name="openingCostPrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-xs font-medium">Valuation / Cost Price</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      placeholder={watch("purchasePrice") || "0.00"}
                      disabled={disabled}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </div>
      )}

      {/* ---------------- 4. Additional Options (Collapsible) ---------------- */}
      <div className="rounded-xl border border-border/80 bg-card overflow-hidden">
        <button
          type="button"
          onClick={() => setMoreOptionsOpen(!moreOptionsOpen)}
          className="flex w-full items-center justify-between p-3.5 text-left text-xs font-semibold text-muted-foreground hover:bg-muted/30 transition-colors"
        >
          <div className="flex items-center gap-2">
            <Layers className="size-4 text-muted-foreground" />
            <span>Additional Details (Barcode, HS Code, Nepali Name, Stock Limits)</span>
          </div>
          <ChevronDown
            className={`size-4 transition-transform duration-200 ${
              moreOptionsOpen ? "rotate-180" : ""
            }`}
          />
        </button>

        {moreOptionsOpen && (
          <div className="space-y-4 p-4 pt-1 border-t border-border/60">
            <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
              {/* Barcode */}
              <FormField
                control={control}
                name="barcode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs font-medium text-muted-foreground">
                      Barcode
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Scan or enter barcode"
                        disabled={disabled}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* HS Code */}
              <FormField
                control={control}
                name="hsCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs font-medium text-muted-foreground">
                      HS / Customs Code
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="e.g. 1001.99.00"
                        disabled={disabled}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Nepali Name */}
              <FormField
                control={control}
                name="nameNp"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs font-medium text-muted-foreground">
                      Item Name (नेपाली नाम)
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="वैकल्पिक नेपाली नाम"
                        disabled={disabled}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* MRP */}
              <FormField
                control={control}
                name="mrp"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs font-medium text-muted-foreground">
                      Maximum Retail Price (MRP)
                    </FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        step="0.01"
                        placeholder="0.00"
                        disabled={disabled}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Description */}
              <FormField
                control={control}
                name="descriptionEn"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel className="text-xs font-medium text-muted-foreground">
                      Description / Line Remarks
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Additional item specifications or notes"
                        disabled={disabled}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Warehouse Stock Controls (Only for physical items) */}
            {currentType !== "SERVICE" && (
              <div className="space-y-3 pt-3 border-t border-border/60">
                <div className="flex items-center gap-2">
                  <Boxes className="size-4 text-muted-foreground" />
                  <span className="text-xs font-semibold text-muted-foreground">
                    Warehouse & Stock Replenishment Limits
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <FormField
                    control={control}
                    name="minStockLevel"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-xs font-medium text-muted-foreground">
                          Minimum Stock Level
                        </FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={0}
                            step="0.001"
                            placeholder="0"
                            disabled={disabled}
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={control}
                    name="reorderLevel"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-xs font-medium text-muted-foreground">
                          Reorder Point
                        </FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={0}
                            step="0.001"
                            placeholder="0"
                            disabled={disabled}
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={control}
                    name="reorderQuantity"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-xs font-medium text-muted-foreground">
                          Reorder Quantity
                        </FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={0}
                            step="0.001"
                            placeholder="0"
                            disabled={disabled}
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                {/* Batch & Expiry checkboxes */}
                <div className="flex flex-wrap gap-4 pt-1">
                  <FormField
                    control={control}
                    name="isBatchTracked"
                    render={({ field }) => (
                      <FormItem className="flex items-center gap-2 space-y-0">
                        <FormControl>
                          <Checkbox
                            checked={field.value}
                            onCheckedChange={field.onChange}
                            disabled={disabled}
                          />
                        </FormControl>
                        <FormLabel className="text-xs font-normal text-muted-foreground cursor-pointer">
                          Track Batch / Lot Numbers
                        </FormLabel>
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={control}
                    name="isExpiryTracked"
                    render={({ field }) => (
                      <FormItem className="flex items-center gap-2 space-y-0">
                        <FormControl>
                          <Checkbox
                            checked={expiryTrackingEnabled ? field.value : false}
                            onCheckedChange={field.onChange}
                            disabled={disabled || !expiryTrackingEnabled}
                          />
                        </FormControl>
                        <FormLabel className="text-xs font-normal text-muted-foreground cursor-pointer">
                          Track Expiry Dates (Requires Batch Tracking)
                        </FormLabel>
                      </FormItem>
                    )}
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
