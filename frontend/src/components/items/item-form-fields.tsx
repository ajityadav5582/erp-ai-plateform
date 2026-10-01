"use client";

import type { Control, UseFormWatch } from "react-hook-form";
import { Loader2 } from "lucide-react";
import {
  EXCISE_TYPES,
  EXCISE_TYPE_LABELS,
  ITEM_TYPES,
  ITEM_TYPE_LABELS,
  TAXABILITY_TYPES,
  TAXABILITY_LABELS,
} from "@/services/item.service";
import { useGetActiveCategoriesQuery } from "@/services/category.service";
import { useGetActiveUnitsQuery } from "@/services/unit.service";

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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { type ItemFormValues } from "@/components/items/item-form";

/** Placeholder value for the "no category" option; Radix forbids empty values. */
const NO_CATEGORY = "__none__";

interface ItemFormFieldsProps {
  control: Control<ItemFormValues>;
  watch: UseFormWatch<ItemFormValues>;
  disabled: boolean;
}

/**
 * The full item form, shared by the create and edit dialogs.
 *
 * It is split into tabs because the item contract carries ~23 fields; a single
 * scrolling dialog would bury the pricing fields under the identifiers.
 */
export function ItemFormFields({ control, watch, disabled }: ItemFormFieldsProps) {
  const { data: categories, isLoading: isLoadingCategories } = useGetActiveCategoriesQuery();
  const { data: units, isLoading: isLoadingUnits } = useGetActiveUnitsQuery();

  const taxabilityType = watch("taxabilityType");
  const exciseType = watch("exciseType");
  const isInventoryTracked = watch("isInventoryTracked");
  const isBatchTracked = watch("isBatchTracked");

  // Expiry only means something when batches exist, so the flag is disabled
  // (and cleared on submit) unless batch tracking is on.
  const expiryTrackingEnabled = isInventoryTracked && isBatchTracked;

  return (
    <Tabs defaultValue="general" className="gap-4">
      <TabsList className="w-full">
        <TabsTrigger value="general">General</TabsTrigger>
        <TabsTrigger value="pricing">Pricing & Tax</TabsTrigger>
        <TabsTrigger value="inventory">Inventory</TabsTrigger>
      </TabsList>

      {/* ---------------- General ---------------- */}
      <TabsContent value="general" className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            control={control}
            name="sku"
            render={({ field }) => (
              <FormItem>
                <FormLabel>SKU *</FormLabel>
                <FormControl>
                  <Input placeholder="ITM-0001" disabled={disabled} {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="nameEn"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Item Name (English) *</FormLabel>
                <FormControl>
                  <Input placeholder="Wheat Flour" disabled={disabled} {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="nameNp"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Item Name (Nepali)</FormLabel>
                <FormControl>
                  <Input placeholder="गेहूँको आटो" disabled={disabled} {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="barcode"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Barcode</FormLabel>
                <FormControl>
                  <Input placeholder="EAN / UPC" disabled={disabled} {...field} />
                </FormControl>
                <FormMessage />
                <p className="text-xs text-muted-foreground">
                  Must be unique within this company.
                </p>
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="hsCode"
            render={({ field }) => (
              <FormItem>
                <FormLabel>HS Code</FormLabel>
                <FormControl>
                  <Input placeholder="1905.10" disabled={disabled} {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="categoryId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Category</FormLabel>
                <Select
                  value={field.value || NO_CATEGORY}
                  onValueChange={(value) =>
                    field.onChange(value === NO_CATEGORY ? "" : value)
                  }
                  disabled={disabled || isLoadingCategories}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select category" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value={NO_CATEGORY}>Uncategorised</SelectItem>
                    {categories?.map((category) => (
                      <SelectItem key={category.id} value={category.id.toString()}>
                        {category.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="uomId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Unit of Measure *</FormLabel>
                <Select
                  value={field.value || ""}
                  onValueChange={field.onChange}
                  disabled={disabled || isLoadingUnits}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue
                        placeholder={
                          isLoadingUnits ? "Loading units..." : "Select unit"
                        }
                      />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {units?.map((unit) => (
                      <SelectItem key={unit.id} value={unit.id.toString()}>
                        {unit.name} ({unit.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
                {isLoadingUnits && (
                  <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Loader2 className="size-3 animate-spin" />
                    Loading units
                  </p>
                )}
                {!isLoadingUnits && units?.length === 0 && (
                  <p className="text-xs text-muted-foreground">
                    No active units. Create one under Inventory &rarr; Units first.
                  </p>
                )}
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="itemType"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Item Type *</FormLabel>
                <Select
                  value={field.value ?? ""}
                  onValueChange={field.onChange}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {ITEM_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {ITEM_TYPE_LABELS[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={control}
          name="descriptionEn"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description (English)</FormLabel>
              <FormControl>
                <Input
                  placeholder="Optional description shown on documents"
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
          name="descriptionNp"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description (Nepali)</FormLabel>
              <FormControl>
                <Input
                  placeholder="वैकल्पिक विवरण"
                  disabled={disabled}
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </TabsContent>

      {/* ---------------- Pricing & Tax ---------------- */}
      <TabsContent value="pricing" className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <FormField
            control={control}
            name="sellingPrice"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Selling Price *</FormLabel>
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

          <FormField
            control={control}
            name="purchasePrice"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Purchase Price</FormLabel>
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

          <FormField
            control={control}
            name="mrp"
            render={({ field }) => (
              <FormItem>
                <FormLabel>MRP</FormLabel>
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
        </div>

        <FormField
          control={control}
          name="isVatInclusive"
          render={({ field }) => (
            <FormItem className="flex flex-row items-start gap-3 rounded-lg border border-border p-3">
              <FormControl>
                <Checkbox
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  disabled={disabled}
                />
              </FormControl>
              <div className="space-y-0.5">
                <FormLabel>Selling price includes VAT</FormLabel>
                <p className="text-xs text-muted-foreground">
                  Tick when the entered selling price already contains VAT. Leave
                  unticked for exclusive pricing.
                </p>
                <FormMessage />
              </div>
            </FormItem>
          )}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            control={control}
            name="taxabilityType"
            render={({ field }) => (
              <FormItem>
                <FormLabel>VAT Treatment *</FormLabel>
                <Select
                  value={field.value ?? ""}
                  onValueChange={field.onChange}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger>
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

          <FormField
            control={control}
            name="vatRate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>VAT Rate (%)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    max={100}
                    step="0.01"
                    placeholder="0.00"
                    disabled={disabled}
                    {...field}
                  />
                </FormControl>
                <FormMessage />
                <p className="text-xs text-muted-foreground">
                  {taxabilityType === "TAXABLE"
                    ? "Standard Nepal VAT is 13%."
                    : "Exempt and zero-rated supplies must use 0%."}
                </p>
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="exciseType"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Excise Duty *</FormLabel>
                <Select
                  value={field.value ?? ""}
                  onValueChange={field.onChange}
                  disabled={disabled}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select excise type" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {EXCISE_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {EXCISE_TYPE_LABELS[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          {exciseType === "PERCENTAGE" && (
            <FormField
              control={control}
              name="exciseRate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Excise Rate (%) *</FormLabel>
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

          {exciseType === "SPECIFIC_AMOUNT" && (
            <FormField
              control={control}
              name="exciseAmount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Excise Amount per Unit *</FormLabel>
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
        </div>
      </TabsContent>

      {/* ---------------- Inventory ---------------- */}
      <TabsContent value="inventory" className="space-y-4">
        <div className="space-y-3 rounded-lg border border-border p-3">
          <FormField
            control={control}
            name="isInventoryTracked"
            render={({ field }) => (
              <FormItem className="flex flex-row items-start gap-3">
                <FormControl>
                  <Checkbox
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    disabled={disabled}
                  />
                </FormControl>
                <div className="space-y-0.5">
                  <FormLabel>Track inventory</FormLabel>
                  <p className="text-xs text-muted-foreground">
                    Untick for services and other non-stock items.
                  </p>
                  <FormMessage />
                </div>
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="isBatchTracked"
            render={({ field }) => (
              <FormItem className="flex flex-row items-start gap-3">
                <FormControl>
                  <Checkbox
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    disabled={disabled || !isInventoryTracked}
                  />
                </FormControl>
                <div className="space-y-0.5">
                  <FormLabel>Track batches</FormLabel>
                  <p className="text-xs text-muted-foreground">
                    Required for items that need lot or batch level traceability.
                  </p>
                  <FormMessage />
                </div>
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="isExpiryTracked"
            render={({ field }) => (
              <FormItem className="flex flex-row items-start gap-3">
                <FormControl>
                  <Checkbox
                    checked={expiryTrackingEnabled ? field.value : false}
                    onCheckedChange={field.onChange}
                    disabled={disabled || !expiryTrackingEnabled}
                  />
                </FormControl>
                <div className="space-y-0.5">
                  <FormLabel>Track expiry dates</FormLabel>
                  <p className="text-xs text-muted-foreground">
                    {expiryTrackingEnabled
                      ? "Stock will prompt for expiry dates on receipts."
                      : "Requires both inventory and batch tracking to be on."}
                  </p>
                  <FormMessage />
                </div>
              </FormItem>
            )}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <FormField
            control={control}
            name="minStockLevel"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Minimum Stock Level</FormLabel>
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
                <p className="text-xs text-muted-foreground">
                  Below this, the item is flagged as short.
                </p>
              </FormItem>
            )}
          />

          <FormField
            control={control}
            name="reorderLevel"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Reorder Level</FormLabel>
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
                <FormLabel>Reorder Quantity</FormLabel>
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
                <p className="text-xs text-muted-foreground">
                  Suggested purchase quantity when reordering.
                </p>
              </FormItem>
            )}
          />
        </div>
      </TabsContent>
    </Tabs>
  );
}
