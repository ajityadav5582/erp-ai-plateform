"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  ArrowRight,
  ArrowUpRight,
  Building2,
  CheckCircle2,
  CreditCard,
  FileCheck,
  Inbox,
  Layers,
  Plus,
  RefreshCw,
  RotateCcw,
  Settings,
  ShieldAlert,
  ShieldCheck,
  ShoppingCart,
  Truck,
} from "lucide-react";

import { useGetRequisitionsQuery, useGetApprovalInboxQuery } from "@/services/purchase-requisition.service";
import { useGetPurchaseOrdersQuery } from "@/services/purchase-order.service";
import { useGetGoodsReceiptsQuery } from "@/services/goods-receipt.service";
import { useGetPurchaseInvoicesQuery, useGetOutstandingInvoicesQuery } from "@/services/purchase-invoice.service";
import { useGetSupplierPaymentsQuery } from "@/services/supplier-payment.service";
import { useGetSuppliersQuery } from "@/services/supplier.service";
import { useGetWorkflowConfigurationQuery } from "@/services/workflow-configuration.service";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  RequisitionStatusBadge,
} from "@/components/requisitions/requisition-status-badge";
import {
  PurchaseOrderStatusBadge,
  PurchaseInvoiceStatusBadge,
} from "@/components/procurement/procurement-status-badge";
import { formatMoney } from "@/lib/procurement-format";
import { cn } from "@/lib/utils";

export default function ProcurementDashboardPage() {
  const [activeTab, setActiveTab] = useState<"approvals" | "orders" | "invoices" | "receipts">("approvals");

  // Fetch real procurement datasets
  const {
    data: requisitionsData,
    isLoading: reqLoading,
    isFetching: reqFetching,
    refetch: refetchReqs,
  } = useGetRequisitionsQuery({ page: 0, size: 100, sort: "createdAt,desc" });

  const {
    data: inboxData,
    isLoading: inboxLoading,
    isFetching: inboxFetching,
    refetch: refetchInbox,
  } = useGetApprovalInboxQuery({ page: 0, size: 50 });

  const {
    data: ordersData,
    isLoading: ordersLoading,
    isFetching: ordersFetching,
    refetch: refetchOrders,
  } = useGetPurchaseOrdersQuery({ page: 0, size: 100, sort: "orderDate,desc" });

  const {
    data: receiptsData,
    isLoading: receiptsLoading,
    isFetching: receiptsFetching,
    refetch: refetchReceipts,
  } = useGetGoodsReceiptsQuery({ page: 0, size: 50, sort: "receiptDate,desc" });

  const {
    data: invoicesData,
    isLoading: invoicesLoading,
    isFetching: invoicesFetching,
    refetch: refetchInvoices,
  } = useGetPurchaseInvoicesQuery({ page: 0, size: 100, sort: "invoiceDate,desc" });

  const {
    data: outstandingInvoicesData,
    isLoading: outstandingLoading,
    isFetching: outstandingFetching,
    refetch: refetchOutstanding,
  } = useGetOutstandingInvoicesQuery({ page: 0, size: 50 });

  const {
    data: paymentsData,
    isLoading: paymentsLoading,
    isFetching: paymentsFetching,
    refetch: refetchPayments,
  } = useGetSupplierPaymentsQuery({ page: 0, size: 50, sort: "paymentDate,desc" });

  const {
    data: suppliersData,
    isLoading: suppliersLoading,
    isFetching: suppliersFetching,
    refetch: refetchSuppliers,
  } = useGetSuppliersQuery({ page: 0, size: 100, sort: "name,asc" });

  const {
    data: workflowConfig,
    refetch: refetchWorkflow,
  } = useGetWorkflowConfigurationQuery();

  const isRefreshing =
    reqFetching ||
    inboxFetching ||
    ordersFetching ||
    receiptsFetching ||
    invoicesFetching ||
    outstandingFetching ||
    paymentsFetching ||
    suppliersFetching;

  const handleRefreshAll = () => {
    refetchReqs();
    refetchInbox();
    refetchOrders();
    refetchReceipts();
    refetchInvoices();
    refetchOutstanding();
    refetchPayments();
    refetchSuppliers();
    refetchWorkflow();
  };

  // Derive accurate aggregated metrics from actual backend responses
  const metrics = useMemo(() => {
    const requisitions = requisitionsData?.content ?? [];
    const orders = ordersData?.content ?? [];
    const receipts = receiptsData?.content ?? [];
    const outstandingInvoices = outstandingInvoicesData?.content ?? [];
    const suppliers = suppliersData?.content ?? [];

    const pendingApprovalsCount = inboxData?.totalElements ?? requisitions.filter((r) => r.status === "SUBMITTED").length;
    const approvedRequisitions = requisitions.filter((r) => r.status === "APPROVED");
    const approvedReqsCount = approvedRequisitions.length;
    const approvedReqsValue = approvedRequisitions.reduce((sum, r) => sum + Number(r.approvedTotalAmount || 0), 0);

    const activeOrders = orders.filter((o) => o.status === "APPROVED" || o.status === "PARTIALLY_RECEIVED");
    const totalOrdersCount = ordersData?.totalElements ?? orders.length;
    const totalCommittedValue = orders.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);
    const awaitingReceiptCount = orders.filter((o) => (o.status === "APPROVED" || o.status === "PARTIALLY_RECEIVED") && Number(o.outstandingAmount || 0) > 0).length;

    const totalReceiptsCount = receiptsData?.totalElements ?? receipts.length;
    const totalReceivedValue = receipts.reduce((sum, r) => sum + Number(r.receivedAmount || 0), 0);

    const unpaidInvoicesCount = outstandingInvoicesData?.totalElements ?? outstandingInvoices.length;
    const totalUnpaidBalance = outstandingInvoices.reduce((sum, inv) => sum + Number(inv.balanceAmount || 0), 0);

    const totalPaymentsCount = paymentsData?.totalElements ?? 0;
    const activeSuppliersCount = suppliers.filter((s) => s.isActive).length;

    return {
      pendingApprovalsCount,
      approvedReqsCount,
      approvedReqsValue,
      totalOrdersCount,
      activeOrdersCount: activeOrders.length,
      totalCommittedValue,
      awaitingReceiptCount,
      totalReceiptsCount,
      totalReceivedValue,
      unpaidInvoicesCount,
      totalUnpaidBalance,
      totalPaymentsCount,
      activeSuppliersCount,
    };
  }, [
    requisitionsData,
    inboxData,
    ordersData,
    receiptsData,
    outstandingInvoicesData,
    paymentsData,
    suppliersData,
  ]);

  const approvalRequired = workflowConfig?.purchaseRequisitionApprovalRequired ?? true;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Header with Title, Live Status & Quick Action Buttons */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Procurement Overview
            </h1>
            <Badge
              variant="outline"
              className={cn(
                "hidden items-center gap-1.5 py-0.5 text-xs font-medium sm:inline-flex",
                approvalRequired
                  ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
                  : "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-400"
              )}
            >
              {approvalRequired ? (
                <>
                  <ShieldCheck className="size-3 text-emerald-600 dark:text-emerald-400" />
                  <span>Approval Required</span>
                </>
              ) : (
                <>
                  <ShieldAlert className="size-3 text-sky-600 dark:text-sky-400" />
                  <span>Auto-Approval Active</span>
                </>
              )}
            </Badge>
          </div>
          <p className="mt-1 text-xs text-muted-foreground sm:text-sm">
            End-to-end demand lifecycle: requisitions, purchase orders, goods receipts, billing & payments.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 sm:self-center">
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefreshAll}
            disabled={isRefreshing}
            className="h-9 gap-1.5 shadow-none"
          >
            <RefreshCw className={cn("size-3.5", isRefreshing && "animate-spin")} />
            <span className="hidden sm:inline">Refresh Data</span>
          </Button>

          <Button asChild size="sm" variant="secondary" className="h-9 gap-1.5 shadow-sm">
            <Link href="/purchase-orders?action=new-direct">
              <ShoppingCart className="size-4" />
              <span>Direct PO</span>
            </Link>
          </Button>

          <Button asChild size="sm" variant="outline" className="h-9 gap-1.5 shadow-sm text-destructive hover:text-destructive">
            <Link href="/purchase-returns">
              <RotateCcw className="size-4" />
              <span>Returns & Debit Notes</span>
            </Link>
          </Button>

          <Button asChild size="sm" className="h-9 gap-1.5 shadow-sm">
            <Link href="/purchase-requisitions">
              <Plus className="size-4" />
              <span>New Requisition</span>
            </Link>
          </Button>
        </div>
      </div>

      {/* Primary KPI Metric Cards (Interactive, Clickable Count Boxes) */}
      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6">
        {/* 1. Pending Approvals */}
        <Link
          href="/purchase-requisitions"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-amber-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Pending Approval</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
                <Inbox className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {inboxLoading ? <Skeleton className="h-8 w-14" /> : metrics.pendingApprovalsCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span>Awaiting decision</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>

        {/* 2. Approved Requisitions (Ready for PO) */}
        <Link
          href="/purchase-orders"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-emerald-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Approved Demand</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <FileCheck className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {reqLoading ? <Skeleton className="h-8 w-14" /> : metrics.approvedReqsCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span>Ready for PO issue</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>

        {/* 3. Purchase Orders */}
        <Link
          href="/purchase-orders"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-sky-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Purchase Orders</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-sky-500/10 text-sky-600 dark:text-sky-400">
                <ShoppingCart className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {ordersLoading ? <Skeleton className="h-8 w-14" /> : metrics.totalOrdersCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span>{metrics.activeOrdersCount} active orders</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>

        {/* 4. Goods Receipts */}
        <Link
          href="/goods-receipts"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-teal-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Goods Receipts</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-teal-500/10 text-teal-600 dark:text-teal-400">
                <Truck className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {receiptsLoading ? <Skeleton className="h-8 w-14" /> : metrics.totalReceiptsCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span>{metrics.awaitingReceiptCount} awaiting delivery</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>

        {/* 5. Unpaid Invoices */}
        <Link
          href="/purchase-invoices"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-rose-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Unpaid Invoices</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-rose-500/10 text-rose-600 dark:text-rose-400">
                <CreditCard className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {outstandingLoading ? <Skeleton className="h-8 w-14" /> : metrics.unpaidInvoicesCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span className="truncate">Payable: {formatMoney(metrics.totalUnpaidBalance, "NPR")}</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>

        {/* 6. Active Suppliers */}
        <Link
          href="/suppliers"
          className="group relative flex flex-col justify-between rounded-xl border border-border/80 bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-purple-500/50 hover:shadow-md"
        >
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">Active Suppliers</span>
              <div className="flex size-7 items-center justify-center rounded-lg bg-purple-500/10 text-purple-600 dark:text-purple-400">
                <Building2 className="size-4" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {suppliersLoading ? <Skeleton className="h-8 w-14" /> : metrics.activeSuppliersCount}
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between border-t border-border/50 pt-2 text-[11px] text-muted-foreground group-hover:text-foreground">
            <span>Verified vendors</span>
            <ArrowUpRight className="size-3.5 opacity-60 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </Link>
      </div>

      {/* Procurement Process Lifecycle Pipeline Banner */}
      <div className="rounded-2xl border border-border/80 bg-gradient-to-r from-muted/60 via-card to-muted/40 p-4 sm:p-5">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            <Layers className="size-4 text-primary" />
            <span>Procurement Lifecycle Funnel</span>
          </div>
          <Link
            href="/workflow-configuration"
            className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
          >
            <Settings className="size-3.5" />
            <span>Configure Workflow</span>
          </Link>
        </div>

        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-5">
          <Link
            href="/purchase-requisitions"
            className="group flex flex-col rounded-xl border border-border/70 bg-card/80 p-3 transition-colors hover:border-primary/50"
          >
            <span className="text-[10px] font-semibold uppercase text-muted-foreground">Step 1: Demand</span>
            <span className="text-sm font-semibold text-foreground group-hover:text-primary">Requisitions</span>
            <span className="mt-1 font-mono text-xs text-muted-foreground">
              {reqLoading ? "…" : `${requisitionsData?.totalElements ?? 0} Raised`}
            </span>
          </Link>

          <Link
            href="/purchase-orders"
            className="group flex flex-col rounded-xl border border-border/70 bg-card/80 p-3 transition-colors hover:border-primary/50"
          >
            <span className="text-[10px] font-semibold uppercase text-muted-foreground">Step 2: Commit</span>
            <span className="text-sm font-semibold text-foreground group-hover:text-primary">Purchase Orders</span>
            <span className="mt-1 font-mono text-xs text-muted-foreground">
              {ordersLoading ? "…" : `${metrics.totalOrdersCount} Issued`}
            </span>
          </Link>

          <Link
            href="/goods-receipts"
            className="group flex flex-col rounded-xl border border-border/70 bg-card/80 p-3 transition-colors hover:border-primary/50"
          >
            <span className="text-[10px] font-semibold uppercase text-muted-foreground">Step 3: Land</span>
            <span className="text-sm font-semibold text-foreground group-hover:text-primary">Goods Receipts</span>
            <span className="mt-1 font-mono text-xs text-muted-foreground">
              {receiptsLoading ? "…" : `${metrics.totalReceiptsCount} Delivered`}
            </span>
          </Link>

          <Link
            href="/purchase-invoices"
            className="group flex flex-col rounded-xl border border-border/70 bg-card/80 p-3 transition-colors hover:border-primary/50"
          >
            <span className="text-[10px] font-semibold uppercase text-muted-foreground">Step 4: Match</span>
            <span className="text-sm font-semibold text-foreground group-hover:text-primary">Purchase Invoices</span>
            <span className="mt-1 font-mono text-xs text-muted-foreground">
              {invoicesLoading ? "…" : `${invoicesData?.totalElements ?? 0} Invoiced`}
            </span>
          </Link>

          <Link
            href="/supplier-payments"
            className="group flex flex-col rounded-xl border border-border/70 bg-card/80 p-3 transition-colors hover:border-primary/50"
          >
            <span className="text-[10px] font-semibold uppercase text-muted-foreground">Step 5: Settle</span>
            <span className="text-sm font-semibold text-foreground group-hover:text-primary">Supplier Payments</span>
            <span className="mt-1 font-mono text-xs text-muted-foreground">
              {paymentsLoading ? "…" : `${metrics.totalPaymentsCount} Paid`}
            </span>
          </Link>
        </div>
      </div>

      {/* Main Operational Tables / Recent Documents Tabs */}
      <Card className="border-border/80 shadow-sm">
        <CardHeader className="border-b border-border/60 pb-3">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle className="text-base font-semibold tracking-tight sm:text-lg">
                Procurement Action Center
              </CardTitle>
              <CardDescription className="text-xs text-muted-foreground">
                Real-time active items across requisitions, orders, deliveries, and payables.
              </CardDescription>
            </div>

            <Tabs
              value={activeTab}
              onValueChange={(val) => setActiveTab(val as typeof activeTab)}
              className="w-full sm:w-auto"
            >
              <TabsList className="grid w-full grid-cols-4 sm:w-auto">
                <TabsTrigger value="approvals" className="text-xs">
                  Approvals {metrics.pendingApprovalsCount > 0 && `(${metrics.pendingApprovalsCount})`}
                </TabsTrigger>
                <TabsTrigger value="orders" className="text-xs">
                  Orders
                </TabsTrigger>
                <TabsTrigger value="invoices" className="text-xs">
                  Invoices {metrics.unpaidInvoicesCount > 0 && `(${metrics.unpaidInvoicesCount})`}
                </TabsTrigger>
                <TabsTrigger value="receipts" className="text-xs">
                  Receipts
                </TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {/* TAB 1: Pending Approvals */}
          {activeTab === "approvals" && (
            <div className="divide-y divide-border/60">
              {inboxLoading ? (
                <div className="space-y-3 p-4">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : (inboxData?.content ?? []).length === 0 ? (
                <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                  <CheckCircle2 className="size-10 text-emerald-500/60" />
                  <p className="mt-2 text-sm font-medium text-foreground">All Requisitions Handled</p>
                  <p className="text-xs text-muted-foreground">
                    There are no purchase requisitions currently awaiting approval.
                  </p>
                </div>
              ) : (
                (inboxData?.content ?? []).map((req) => (
                  <div
                    key={req.id}
                    className="flex flex-col justify-between gap-3 p-4 transition-colors hover:bg-muted/40 sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {req.prNumber}
                        </span>
                        <RequisitionStatusBadge status={req.status} />
                        {req.priority && (
                          <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                            {req.priority}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>Requested by: User #{req.requestedBy}</span>
                        {req.branchId && <span>• Branch #{req.branchId}</span>}
                        {req.requestDate && <span>• Date: {req.requestDate}</span>}
                      </div>
                    </div>

                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <div className="text-left sm:text-right">
                        <div className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {formatMoney(req.estimatedTotalAmount, req.currency)}
                        </div>
                        <span className="text-[11px] text-muted-foreground">Estimated Amount</span>
                      </div>

                      <Button asChild size="sm" variant="outline" className="h-8 gap-1 text-xs">
                        <Link href="/purchase-requisitions">
                          <span>Review</span>
                          <ArrowRight className="size-3" />
                        </Link>
                      </Button>
                    </div>
                  </div>
                ))
              )}

              <div className="p-3 text-center sm:text-right">
                <Button asChild variant="ghost" size="sm" className="text-xs text-primary">
                  <Link href="/purchase-requisitions">
                    View All Requisitions <ArrowRight className="ml-1 size-3" />
                  </Link>
                </Button>
              </div>
            </div>
          )}

          {/* TAB 2: Recent Purchase Orders */}
          {activeTab === "orders" && (
            <div className="divide-y divide-border/60">
              {ordersLoading ? (
                <div className="space-y-3 p-4">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : (ordersData?.content ?? []).length === 0 ? (
                <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                  <ShoppingCart className="size-10 opacity-30" />
                  <p className="mt-2 text-sm font-medium text-foreground">No Purchase Orders Found</p>
                  <p className="text-xs text-muted-foreground">
                    Convert approved requisitions into orders to see them here.
                  </p>
                </div>
              ) : (
                (ordersData?.content ?? []).slice(0, 8).map((po) => (
                  <div
                    key={po.id}
                    className="flex flex-col justify-between gap-3 p-4 transition-colors hover:bg-muted/40 sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {po.poNumber}
                        </span>
                        <PurchaseOrderStatusBadge status={po.status} />
                        <span className="truncate rounded bg-muted/80 px-1.5 py-0.5 text-xs text-foreground">
                          {po.supplierName}
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>Order Date: {po.orderDate}</span>
                        {po.warehouseName && <span>• Target: {po.warehouseName}</span>}
                        {po.reference && <span>• Ref: {po.reference}</span>}
                      </div>
                    </div>

                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <div className="text-left sm:text-right">
                        <div className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {formatMoney(po.totalAmount, po.currency)}
                        </div>
                        <span className="text-[11px] text-muted-foreground">
                          Outstanding: {formatMoney(po.outstandingAmount, po.currency)}
                        </span>
                      </div>

                      <Button asChild size="sm" variant="outline" className="h-8 gap-1 text-xs">
                        <Link href="/purchase-orders">
                          <span>Details</span>
                          <ArrowRight className="size-3" />
                        </Link>
                      </Button>
                    </div>
                  </div>
                ))
              )}

              <div className="p-3 text-center sm:text-right">
                <Button asChild variant="ghost" size="sm" className="text-xs text-primary">
                  <Link href="/purchase-orders">
                    View All Purchase Orders <ArrowRight className="ml-1 size-3" />
                  </Link>
                </Button>
              </div>
            </div>
          )}

          {/* TAB 3: Unpaid Invoices */}
          {activeTab === "invoices" && (
            <div className="divide-y divide-border/60">
              {outstandingLoading ? (
                <div className="space-y-3 p-4">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : (outstandingInvoicesData?.content ?? []).length === 0 ? (
                <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                  <CheckCircle2 className="size-10 text-emerald-500/60" />
                  <p className="mt-2 text-sm font-medium text-foreground">No Outstanding Invoices</p>
                  <p className="text-xs text-muted-foreground">
                    All supplier invoices are settled and up to date.
                  </p>
                </div>
              ) : (
                (outstandingInvoicesData?.content ?? []).map((inv) => (
                  <div
                    key={inv.id}
                    className="flex flex-col justify-between gap-3 p-4 transition-colors hover:bg-muted/40 sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {inv.invoiceNumber}
                        </span>
                        <PurchaseInvoiceStatusBadge status={inv.status} />
                        <span className="truncate text-xs font-medium text-foreground">
                          {inv.supplierName}
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>Invoice Date: {inv.invoiceDate}</span>
                        {inv.dueDate && <span>• Due: {inv.dueDate}</span>}
                        {inv.poNumber && <span>• PO: {inv.poNumber}</span>}
                      </div>
                    </div>

                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <div className="text-left sm:text-right">
                        <div className="font-mono text-xs font-semibold text-destructive sm:text-sm">
                          {formatMoney(inv.balanceAmount, inv.currency)}
                        </div>
                        <span className="text-[11px] text-muted-foreground">
                          Total: {formatMoney(inv.totalAmount, inv.currency)}
                        </span>
                      </div>

                      <Button asChild size="sm" variant="outline" className="h-8 gap-1 text-xs">
                        <Link href="/purchase-invoices">
                          <span>Settle</span>
                          <ArrowRight className="size-3" />
                        </Link>
                      </Button>
                    </div>
                  </div>
                ))
              )}

              <div className="p-3 text-center sm:text-right">
                <Button asChild variant="ghost" size="sm" className="text-xs text-primary">
                  <Link href="/purchase-invoices">
                    View All Purchase Invoices <ArrowRight className="ml-1 size-3" />
                  </Link>
                </Button>
              </div>
            </div>
          )}

          {/* TAB 4: Goods Receipts */}
          {activeTab === "receipts" && (
            <div className="divide-y divide-border/60">
              {receiptsLoading ? (
                <div className="space-y-3 p-4">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : (receiptsData?.content ?? []).length === 0 ? (
                <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                  <Truck className="size-10 opacity-30" />
                  <p className="mt-2 text-sm font-medium text-foreground">No Goods Receipts Recorded</p>
                  <p className="text-xs text-muted-foreground">
                    Record goods receipts against purchase orders when items arrive.
                  </p>
                </div>
              ) : (
                (receiptsData?.content ?? []).slice(0, 8).map((grn) => (
                  <div
                    key={grn.id}
                    className="flex flex-col justify-between gap-3 p-4 transition-colors hover:bg-muted/40 sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {grn.grnNumber}
                        </span>
                        <span className="rounded bg-primary/10 px-1.5 py-0.5 text-xs text-primary font-medium">
                          PO #{grn.poNumber}
                        </span>
                        <span className="truncate text-xs text-foreground font-medium">
                          {grn.supplierName}
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span>Receipt Date: {grn.receiptDate}</span>
                        {grn.warehouseName && <span>• Landed at: {grn.warehouseName}</span>}
                        {grn.deliveryChallanNo && <span>• Challan: {grn.deliveryChallanNo}</span>}
                      </div>
                    </div>

                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <div className="text-left sm:text-right">
                        <div className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                          {formatMoney(grn.receivedAmount, grn.currency)}
                        </div>
                        <span className="text-[11px] text-muted-foreground">Received Value</span>
                      </div>

                      <Button asChild size="sm" variant="outline" className="h-8 gap-1 text-xs">
                        <Link href="/goods-receipts">
                          <span>Details</span>
                          <ArrowRight className="size-3" />
                        </Link>
                      </Button>
                    </div>
                  </div>
                ))
              )}

              <div className="p-3 text-center sm:text-right">
                <Button asChild variant="ghost" size="sm" className="text-xs text-primary">
                  <Link href="/goods-receipts">
                    View All Goods Receipts <ArrowRight className="ml-1 size-3" />
                  </Link>
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
