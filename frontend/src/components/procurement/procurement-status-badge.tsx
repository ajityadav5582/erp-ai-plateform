"use client";

import {
  Ban,
  CheckCircle2,
  CircleDashed,
  FileCheck2,
  PackageCheck,
  PackageOpen,
  Receipt,
  Send,
  Truck,
  Wallet,
  XCircle,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  formatPurchaseOrderLineStatus,
  formatPurchaseOrderStatus,
  type PurchaseOrderLineStatus,
  type PurchaseOrderStatus,
} from "@/services/purchase-order.service";
import {
  formatGoodsReceiptStatus,
  type GoodsReceiptStatus,
} from "@/services/goods-receipt.service";
import {
  formatPurchaseInvoiceStatus,
  type PurchaseInvoiceStatus,
} from "@/services/purchase-invoice.service";
import {
  formatPaymentMethod,
  formatSupplierPaymentStatus,
  type PaymentMethod,
  type SupplierPaymentStatus,
} from "@/services/supplier-payment.service";

/**
 * One place that maps a lifecycle state to a colour and an icon for every document
 * that closes out the purchase process.
 *
 * These four documents each have their own enum and their own cadence, so each gets its
 * own table below rather than a generic "green when good" rule. What they share is the
 * question a reader is asking: *where has this got to?* An order at APPROVED and a
 * receipt at POSTED are both healthy but at different points, and the tone alone should
 * not make them look interchangeable.
 *
 * Terminal states (REJECTED, CANCELLED, CLOSED) are deliberately desaturated: they are
 * outcomes, not work in progress, and a full-strength colour on a closed order would
 * keep pulling the eye.
 */
type Tone = "neutral" | "info" | "progress" | "success" | "warning" | "danger";

const TONE_CLASSES: Record<Tone, string> = {
  neutral:
    "bg-muted text-muted-foreground border-border dark:bg-muted/40",
  info: "bg-sky-500/10 text-sky-600 dark:bg-sky-500/20 dark:text-sky-400 border-sky-500/20",
  progress:
    "bg-amber-500/10 text-amber-600 dark:bg-amber-500/20 dark:text-amber-400 border-amber-500/20",
  success:
    "bg-emerald-500/10 text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400 border-emerald-500/20",
  warning:
    "bg-orange-500/10 text-orange-600 dark:bg-orange-500/20 dark:text-orange-400 border-orange-500/20",
  danger:
    "bg-red-500/10 text-red-600 dark:bg-red-500/20 dark:text-red-400 border-red-500/20",
};

/**
 * Order tones follow where the commitment stands, not whether it succeeded.
 * PARTIALLY_RECEIVED is the only genuinely "busy" state, so it is the only one given a
 * progress tone; everything before APPROVED is still in someone's hands.
 */
const ORDER_TONES: Record<PurchaseOrderStatus, Tone> = {
  DRAFT: "neutral",
  SUBMITTED: "info",
  APPROVED: "success",
  PARTIALLY_RECEIVED: "progress",
  RECEIVED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
  CLOSED: "neutral",
};

const ORDER_ICONS: Record<PurchaseOrderStatus, LucideIcon> = {
  DRAFT: FileCheck2,
  SUBMITTED: Send,
  APPROVED: CheckCircle2,
  PARTIALLY_RECEIVED: Truck,
  RECEIVED: PackageCheck,
  REJECTED: XCircle,
  CANCELLED: Ban,
  CLOSED: PackageOpen,
};

/**
 * Line tones mirror the header's reasoning: a line is outstanding work until it is fully
 * received, which is the one state worth noticing.
 */
const ORDER_LINE_TONES: Record<PurchaseOrderLineStatus, Tone> = {
  OPEN: "neutral",
  PARTIALLY_RECEIVED: "progress",
  RECEIVED: "success",
  CANCELLED: "neutral",
};

const ORDER_LINE_ICONS: Record<PurchaseOrderLineStatus, LucideIcon> = {
  OPEN: CircleDashed,
  PARTIALLY_RECEIVED: Truck,
  RECEIVED: CheckCircle2,
  CANCELLED: Ban,
};

const RECEIPT_TONES: Record<GoodsReceiptStatus, Tone> = {
  POSTED: "success",
  CANCELLED: "neutral",
};

const RECEIPT_ICONS: Record<GoodsReceiptStatus, LucideIcon> = {
  POSTED: PackageCheck,
  CANCELLED: Ban,
};

/** An invoice is money owed, so PARTIALLY_PAID is progress rather than a settled state. */
const INVOICE_TONES: Record<PurchaseInvoiceStatus, Tone> = {
  APPROVED: "warning",
  PARTIALLY_PAID: "progress",
  PAID: "success",
  CANCELLED: "neutral",
};

const INVOICE_ICONS: Record<PurchaseInvoiceStatus, LucideIcon> = {
  APPROVED: Receipt,
  PARTIALLY_PAID: Wallet,
  PAID: CheckCircle2,
  CANCELLED: Ban,
};

const PAYMENT_TONES: Record<SupplierPaymentStatus, Tone> = {
  POSTED: "success",
  CANCELLED: "neutral",
};

const PAYMENT_ICONS: Record<SupplierPaymentStatus, LucideIcon> = {
  POSTED: Wallet,
  CANCELLED: Ban,
};

const CHIP_BASE =
  "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium";

function Chip({
  label,
  tone,
  icon: Icon,
  className,
}: {
  label: string;
  tone: Tone;
  icon: LucideIcon;
  className?: string;
}) {
  return (
    <span className={cn(CHIP_BASE, TONE_CLASSES[tone], className)}>
      <Icon className="size-3.5" aria-hidden="true" />
      {label}
    </span>
  );
}

interface BadgeProps {
  label: string;
  tone: Tone;
  icon: LucideIcon;
  className?: string;
}

export function PurchaseOrderStatusBadge({
  status,
  className,
}: {
  status: PurchaseOrderStatus;
  className?: string;
}) {
  return (
    <Chip
      label={formatPurchaseOrderStatus(status)}
      tone={ORDER_TONES[status] ?? "neutral"}
      icon={ORDER_ICONS[status] ?? CircleDashed}
      className={className}
    />
  );
}

export function PurchaseOrderLineStatusBadge({
  status,
  className,
}: {
  status: PurchaseOrderLineStatus;
  className?: string;
}) {
  return (
    <Chip
      label={formatPurchaseOrderLineStatus(status)}
      tone={ORDER_LINE_TONES[status] ?? "neutral"}
      icon={ORDER_LINE_ICONS[status] ?? CircleDashed}
      className={className}
    />
  );
}

export function GoodsReceiptStatusBadge({
  status,
  className,
}: {
  status: GoodsReceiptStatus;
  className?: string;
}) {
  return (
    <Chip
      label={formatGoodsReceiptStatus(status)}
      tone={RECEIPT_TONES[status] ?? "neutral"}
      icon={RECEIPT_ICONS[status] ?? CircleDashed}
      className={className}
    />
  );
}

export function PurchaseInvoiceStatusBadge({
  status,
  className,
}: {
  status: PurchaseInvoiceStatus;
  className?: string;
}) {
  return (
    <Chip
      label={formatPurchaseInvoiceStatus(status)}
      tone={INVOICE_TONES[status] ?? "neutral"}
      icon={INVOICE_ICONS[status] ?? CircleDashed}
      className={className}
    />
  );
}

export function SupplierPaymentStatusBadge({
  status,
  className,
}: {
  status: SupplierPaymentStatus;
  className?: string;
}) {
  return (
    <Chip
      label={formatSupplierPaymentStatus(status)}
      tone={PAYMENT_TONES[status] ?? "neutral"}
      icon={PAYMENT_ICONS[status] ?? CircleDashed}
      className={className}
    />
  );
}

/**
 * Payment method is not a lifecycle state, so it gets a plain neutral chip rather than
 * a tone. It answers "how did the money move", not "how far has this got".
 */
export function PaymentMethodBadge({
  method,
  className,
}: {
  method: PaymentMethod;
  className?: string;
}) {
  return (
    <span
      className={cn(
        CHIP_BASE,
        "bg-muted text-muted-foreground border-border dark:bg-muted/40",
        className
      )}
    >
      {formatPaymentMethod(method)}
    </span>
  );
}

export type { BadgeProps };
