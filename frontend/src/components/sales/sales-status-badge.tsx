"use client";

import { StatusBadge } from "@/components/common/status-badge";

type StatusVariant = "default" | "success" | "warning" | "error" | "info";
import {
  SALES_QUOTATION_STATUS_LABELS,
  type SalesQuotationStatus,
} from "@/services/sales-quotation.service";
import {
  SALES_ORDER_STATUS_LABELS,
  type SalesOrderStatus,
} from "@/services/sales-order.service";
import {
  SALES_INVOICE_STATUS_LABELS,
  type SalesInvoiceStatus,
} from "@/services/sales-invoice.service";
import {
  DELIVERY_STATUS_LABELS,
  type DeliveryStatus,
} from "@/services/sales-delivery.service";
import {
  RECEIPT_STATUS_LABELS,
  type ReceiptStatus,
} from "@/services/customer-receipt.service";
import { RETURN_STATUS_LABELS, type ReturnStatus } from "@/services/sales-return.service";

const quotationVariants: Record<SalesQuotationStatus, StatusVariant> = {
  DRAFT: "default",
  SENT: "info",
  ACCEPTED: "success",
  REJECTED: "error",
  EXPIRED: "warning",
  CONVERTED: "success",
};

const orderVariants: Record<SalesOrderStatus, StatusVariant> = {
  DRAFT: "default",
  CONFIRMED: "info",
  PARTIALLY_DELIVERED: "warning",
  DELIVERED: "success",
  CANCELLED: "error",
  CLOSED: "default",
};

const invoiceVariants: Record<SalesInvoiceStatus, StatusVariant> = {
  DRAFT: "default",
  ISSUED: "info",
  PARTIALLY_PAID: "warning",
  PAID: "success",
  CANCELLED: "error",
};

const deliveryVariants: Record<DeliveryStatus, StatusVariant> = {
  DRAFT: "default",
  POSTED: "success",
  CANCELLED: "error",
};

const receiptVariants: Record<ReceiptStatus, StatusVariant> = {
  POSTED: "success",
  CANCELLED: "error",
};

const returnVariants: Record<ReturnStatus, StatusVariant> = {
  DRAFT: "default",
  POSTED: "success",
  CANCELLED: "error",
};

export function SalesQuotationStatusBadge({ status }: { status: SalesQuotationStatus }) {
  return <StatusBadge status={SALES_QUOTATION_STATUS_LABELS[status] ?? status} variant={quotationVariants[status] ?? "default"} />;
}

export function SalesOrderStatusBadge({ status }: { status: SalesOrderStatus }) {
  return <StatusBadge status={SALES_ORDER_STATUS_LABELS[status] ?? status} variant={orderVariants[status] ?? "default"} />;
}

export function SalesInvoiceStatusBadge({ status }: { status: SalesInvoiceStatus }) {
  return <StatusBadge status={SALES_INVOICE_STATUS_LABELS[status] ?? status} variant={invoiceVariants[status] ?? "default"} />;
}

export function DeliveryStatusBadge({ status }: { status: DeliveryStatus }) {
  return <StatusBadge status={DELIVERY_STATUS_LABELS[status] ?? status} variant={deliveryVariants[status] ?? "default"} />;
}

export function ReceiptStatusBadge({ status }: { status: ReceiptStatus }) {
  return <StatusBadge status={RECEIPT_STATUS_LABELS[status] ?? status} variant={receiptVariants[status] ?? "default"} />;
}

export function ReturnStatusBadge({ status }: { status: ReturnStatus }) {
  return <StatusBadge status={RETURN_STATUS_LABELS[status] ?? status} variant={returnVariants[status] ?? "default"} />;
}

export function ActiveBadge({ active }: { active: boolean }) {
  return <StatusBadge status={active ? "Active" : "Inactive"} variant={active ? "success" : "default"} />;
}
