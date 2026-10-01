"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { SupplierStatusBadge } from "./supplier-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Truck,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Building2,
  Mail,
  Phone,
  MapPin,
  FileText,
  CalendarClock,
  Coins,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetSupplierQuery,
  useActivateSupplierMutation,
  useDeactivateSupplierMutation,
  formatSupplierType,
} from "@/services/supplier.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface SupplierDetailsSheetProps {
  supplierId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function SupplierDetailsSheet({
  supplierId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: SupplierDetailsSheetProps) {
  const { data: supplier, isLoading, isError } = useGetSupplierQuery(supplierId ?? "", {
    skip: !supplierId || !open,
  });

  const [activateSupplier, { isLoading: isActivating }] = useActivateSupplierMutation();
  const [deactivateSupplier, { isLoading: isDeactivating }] = useDeactivateSupplierMutation();

  const handleActivate = async () => {
    if (!supplier) return;
    try {
      await activateSupplier(supplier.id).unwrap();
      toast.success(`Supplier "${supplier.name}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate supplier", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!supplier) return;
    try {
      await deactivateSupplier(supplier.id).unwrap();
      toast.success(`Supplier "${supplier.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate supplier", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Supplier Details</SheetTitle>
          <SheetDescription>
            Comprehensive supplier information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load supplier details.
          </div>
        )}

        {supplier && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Truck className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {supplier.name}
                </h3>
                <p className="truncate text-sm text-muted-foreground font-mono">
                  {supplier.code}
                </p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <SupplierStatusBadge isActive={supplier.isActive} />
                  {supplier.type && (
                    <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                      {formatSupplierType(supplier.type)}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {!supplier.isActive ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30"
                  onClick={handleActivate}
                  disabled={isActivating}
                >
                  {isActivating ? (
                    <Loader2 className="mr-1.5 size-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="mr-1.5 size-4" />
                  )}
                  Activate
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? (
                    <Loader2 className="mr-1.5 size-4 animate-spin" />
                  ) : (
                    <PauseCircle className="mr-1.5 size-4" />
                  )}
                  Deactivate
                </Button>
              )}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete}>
                <Trash2 className="size-4" />
              </Button>
            </div>

            <Separator />

            {/* Contact & commercial info */}
            <div className="space-y-3 text-sm">
              {supplier.email && (
                <div className="flex items-start gap-3">
                  <Mail className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <a
                    href={`mailto:${supplier.email}`}
                    className="text-foreground hover:underline break-all"
                  >
                    {supplier.email}
                  </a>
                </div>
              )}

              {supplier.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{supplier.phone}</span>
                </div>
              )}

              {supplier.address && (
                <div className="flex items-start gap-3">
                  <MapPin className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <span className="text-foreground">{supplier.address}</span>
                </div>
              )}

              {supplier.taxId && (
                <div className="flex items-center gap-3">
                  <FileText className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Tax ID: {supplier.taxId}</span>
                </div>
              )}

              {supplier.paymentTermsDays !== null && supplier.paymentTermsDays !== undefined && (
                <div className="flex items-center gap-3">
                  <CalendarClock className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">
                    Payment Terms: {supplier.paymentTermsDays} days
                  </span>
                </div>
              )}

              {supplier.currency && (
                <div className="flex items-center gap-3">
                  <Coins className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Currency: {supplier.currency}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Building2 className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Company ID: {supplier.companyId}</span>
              </div>
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Supplier ID:</span>
                <span className="font-mono">{supplier.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{supplier.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{supplier.version}</span>
              </div>
              {supplier.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(supplier.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {supplier.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(supplier.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
