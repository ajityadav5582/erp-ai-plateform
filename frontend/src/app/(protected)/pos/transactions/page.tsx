"use client";

import * as React from "react";
import {
  Ban,
  Download,
  Eye,
  Printer,
  ReceiptText,
  RefreshCw,
  UploadCloud,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TableSkeletonRows } from "@/components/common/table-state";
import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import {
  useGetPosTransactionsQuery,
  useGetPosReceiptQuery,
  useVoidPosTransactionMutation,
  POS_TRANSACTION_STATUSES,
  POS_TRANSACTION_STATUS_LABELS,
  POS_PAYMENT_METHOD_LABELS,
  type PosTransaction,
  type PosTransactionStatus,
} from "@/services/pos-transaction.service";
import { useGetPosRegistersQuery } from "@/services/pos-register.service";
import { useSyncPosTransactionMutation } from "@/services/pos-cbms.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { PosTransactionStatusBadge } from "@/components/pos/pos-status-badge";
import { formatPosCurrency as currency } from "@/lib/pos-format";
const ALL = "ALL";

/** Escapes a CSV cell and wraps it in quotes. */
const csvCell = (value: unknown) => `"${String(value ?? "").replace(/"/g, '""')}"`;

/**
 * POS transaction history — mobile first.
 *
 * Lists sales with their status and CBMS sync state, shows the printable
 * receipt, a full details view, and hosts the void action. Voiding a completed
 * sale reverses the stock issue and the session totals server-side; a reason is
 * mandatory.
 */
export default function PosTransactionsPage() {
  const [page, setPage] = React.useState(0);
  const [statusFilter, setStatusFilter] = React.useState<string>(ALL);
  const [registerFilter, setRegisterFilter] = React.useState<string>(ALL);
  const [term, setTerm] = React.useState("");
  const [receiptId, setReceiptId] = React.useState<number | null>(null);
  const [detailsTarget, setDetailsTarget] = React.useState<PosTransaction | null>(null);
  const [voidTarget, setVoidTarget] = React.useState<{ id: number; number: string } | null>(null);
  const [voidReason, setVoidReason] = React.useState("");
  const [syncingId, setSyncingId] = React.useState<number | null>(null);

  const { data, isLoading, isFetching, refetch } = useGetPosTransactionsQuery({
    page,
    size: 20,
    status: statusFilter === ALL ? undefined : (statusFilter as PosTransactionStatus),
    registerId: registerFilter === ALL ? undefined : Number(registerFilter),
    term: term.trim() || undefined,
  });
  const { data: receipt } = useGetPosReceiptQuery(receiptId ?? 0, { skip: receiptId === null });
  const { data: registers } = useGetPosRegistersQuery({ size: 100 });
  const [voidTransaction, { isLoading: isVoiding }] = useVoidPosTransactionMutation();
  const [syncCbms] = useSyncPosTransactionMutation();

  const onSubmitVoid = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!voidTarget) return;
    try {
      await voidTransaction({ id: voidTarget.id, body: { reason: voidReason.trim() } }).unwrap();
      toast.success(`Transaction ${voidTarget.number} voided.`);
      setVoidTarget(null);
      setVoidReason("");
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not void the transaction.");
    }
  };

  const onRetrySync = async (transaction: PosTransaction) => {
    setSyncingId(transaction.id);
    try {
      const result = await syncCbms(transaction.id).unwrap();
      if (result.success) {
        toast.success("Pushed to CBMS.");
        refetch();
      } else {
        toast.error(result.message || "CBMS sync failed.");
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "CBMS sync failed.");
    } finally {
      setSyncingId(null);
    }
  };

  const exportCsv = () => {
    const header = [
      "Number",
      "Date (BS)",
      "Cashier",
      "Status",
      "CBMS",
      "Payment",
      "Subtotal",
      "VAT",
      "Total",
    ];
    const rows = transactions.map((t) => [
      t.transactionNumber ?? "",
      t.transactionDateNp,
      t.cashierName,
      POS_TRANSACTION_STATUS_LABELS[t.status],
      t.status === "COMPLETED" ? (t.cbmsSynced ? "Synced" : "Pending") : "",
      t.paymentMethod ? POS_PAYMENT_METHOD_LABELS[t.paymentMethod] : "",
      t.subtotalAmount,
      t.vatAmount,
      t.totalAmount,
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map(csvCell).join(","))
      .join("\r\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "pos-transactions.csv";
    link.click();
    URL.revokeObjectURL(url);
  };

  const transactions = data?.content ?? [];

  const cbmsCell = (transaction: PosTransaction) => {
    if (transaction.status !== "COMPLETED") return "—";
    if (transaction.cbmsSynced) {
      return <span className="text-emerald-600">Synced</span>;
    }
    return (
      <span className="text-amber-600">
        Pending{transaction.cbmsRetryCount > 0 ? ` (${transaction.cbmsRetryCount})` : ""}
      </span>
    );
  };

  return (
    <div className="space-y-4 p-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">POS Transactions</h1>
          <p className="text-sm text-muted-foreground">Sales, receipts and voids.</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={() => void refetch()}
            disabled={isFetching}
            aria-label="Refresh"
          >
            <RefreshCw className={isFetching ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
          </Button>
          <Button variant="outline" onClick={exportCsv} disabled={transactions.length === 0}>
            <Download className="mr-1 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </header>

      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={term}
          onChange={(event) => {
            setTerm(event.target.value);
            setPage(0);
          }}
          placeholder="Search by transaction number…"
          className="max-w-xs"
        />
        <Select
          value={statusFilter}
          onValueChange={(value) => {
            setStatusFilter(value);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-44" aria-label="Filter by status">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All statuses</SelectItem>
            {POS_TRANSACTION_STATUSES.map((status) => (
              <SelectItem key={status} value={status}>
                {POS_TRANSACTION_STATUS_LABELS[status]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={registerFilter}
          onValueChange={(value) => {
            setRegisterFilter(value);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-52" aria-label="Filter by register">
            <SelectValue placeholder="All registers" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All registers</SelectItem>
            {(registers?.content ?? []).map((register) => (
              <SelectItem key={register.id} value={String(register.id)}>
                {register.registerName} ({register.registerCode})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <TableSkeletonRows columns={5} rows={5} />
      ) : transactions.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed py-10">
          <ReceiptText className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">No transactions found.</p>
        </div>
      ) : (
        <>
          {/* Mobile: cards */}
          <ul className="space-y-2 md:hidden">
            {transactions.map((transaction) => (
              <li key={transaction.id} className="rounded-lg border p-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-mono text-sm font-medium">
                      {transaction.transactionNumber ?? "Parked cart"}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {transaction.transactionDateNp} · {transaction.cashierName}
                    </p>
                  </div>
                  <PosTransactionStatusBadge status={transaction.status} />
                </div>
                <p className="mt-1 text-sm font-bold">{currency(transaction.totalAmount)}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button size="sm" variant="outline" onClick={() => setReceiptId(transaction.id)}>
                    <ReceiptText className="mr-1 h-4 w-4" /> Receipt
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => setDetailsTarget(transaction)}>
                    <Eye className="mr-1 h-4 w-4" /> Details
                  </Button>
                  {transaction.status === "COMPLETED" && !transaction.cbmsSynced && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => void onRetrySync(transaction)}
                      disabled={syncingId === transaction.id}
                    >
                      <UploadCloud className="mr-1 h-4 w-4" /> Sync
                    </Button>
                  )}
                  {transaction.status !== "VOIDED" && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setVoidTarget({
                          id: transaction.id,
                          number: transaction.transactionNumber ?? "cart",
                        });
                        setVoidReason("");
                      }}
                    >
                      <Ban className="mr-1 h-4 w-4" /> Void
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>

          {/* Tablet and up: table */}
          <div className="hidden rounded-lg border md:block">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Number</TableHead>
                  <TableHead>Date (BS)</TableHead>
                  <TableHead>Cashier</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead>CBMS</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {transactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell className="font-mono text-xs">
                      {transaction.transactionNumber ?? "—"}
                    </TableCell>
                    <TableCell>{transaction.transactionDateNp}</TableCell>
                    <TableCell>{transaction.cashierName}</TableCell>
                    <TableCell className="text-right">{currency(transaction.totalAmount)}</TableCell>
                    <TableCell>{cbmsCell(transaction)}</TableCell>
                    <TableCell>
                      <PosTransactionStatusBadge status={transaction.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setReceiptId(transaction.id)}>
                        Receipt
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setDetailsTarget(transaction)}>
                        Details
                      </Button>
                      {transaction.status === "COMPLETED" && !transaction.cbmsSynced && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => void onRetrySync(transaction)}
                          disabled={syncingId === transaction.id}
                        >
                          {syncingId === transaction.id ? "Syncing…" : "Sync"}
                        </Button>
                      )}
                      {transaction.status !== "VOIDED" && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setVoidTarget({
                              id: transaction.id,
                              number: transaction.transactionNumber ?? "cart",
                            });
                            setVoidReason("");
                          }}
                        >
                          Void
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <Button variant="outline" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}

      {/* Receipt (reprintable) */}
      <Dialog open={receiptId !== null} onOpenChange={(open) => !open && setReceiptId(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Receipt</DialogTitle>
          </DialogHeader>
          {receipt && (
            <div className="space-y-2 text-sm">
              <div className="text-center">
                <p className="text-lg font-bold">{receipt.transaction.transactionNumber}</p>
                <p className="text-muted-foreground">
                  {receipt.transaction.transactionDateNp} · {receipt.registerName} ({receipt.registerCode})
                </p>
                <p className="text-muted-foreground">
                  Session {receipt.sessionNumber} · {receipt.transaction.cashierName}
                </p>
              </div>
              <Separator />
              <ul className="space-y-1">
                {receipt.transaction.lines.map((line) => (
                  <li key={line.id} className="flex justify-between gap-2">
                    <span className="min-w-0 truncate">
                      {line.quantity} × {line.itemName}
                    </span>
                    <span className="shrink-0">{currency(line.lineAmount)}</span>
                  </li>
                ))}
              </ul>
              <Separator />
              <div className="flex justify-between">
                <span className="text-muted-foreground">Subtotal</span>
                <span>{currency(receipt.transaction.subtotalAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">VAT</span>
                <span>{currency(receipt.transaction.vatAmount)}</span>
              </div>
              <div className="flex justify-between text-base font-bold">
                <span>Total</span>
                <span>{currency(receipt.transaction.totalAmount)}</span>
              </div>
              {receipt.transaction.voidReason && (
                <p className="text-xs text-rose-600">Voided: {receipt.transaction.voidReason}</p>
              )}
              <Button variant="outline" className="w-full" onClick={() => window.print()}>
                <Printer className="mr-2 h-4 w-4" /> Print
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Details */}
      <Dialog
        open={detailsTarget !== null}
        onOpenChange={(open) => !open && setDetailsTarget(null)}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              Transaction {detailsTarget?.transactionNumber ?? "details"}
            </DialogTitle>
          </DialogHeader>
          {detailsTarget && (
            <div className="space-y-3 text-sm">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <p className="text-muted-foreground">Date (BS)</p>
                  <p>{detailsTarget.transactionDateNp}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Cashier</p>
                  <p>{detailsTarget.cashierName}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Customer</p>
                  <p>
                    {detailsTarget.customerName}
                    {detailsTarget.customerPanNumber ? ` · PAN ${detailsTarget.customerPanNumber}` : ""}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground">Payment</p>
                  <p>
                    {detailsTarget.paymentMethod
                      ? POS_PAYMENT_METHOD_LABELS[detailsTarget.paymentMethod]
                      : "—"}
                    {detailsTarget.paymentReference ? ` · ${detailsTarget.paymentReference}` : ""}
                  </p>
                </div>
              </div>

              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Item</TableHead>
                      <TableHead className="text-right">Qty</TableHead>
                      <TableHead className="text-right">Price</TableHead>
                      <TableHead className="text-right">Disc%</TableHead>
                      <TableHead className="text-right">VAT</TableHead>
                      <TableHead className="text-right">Amount</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {detailsTarget.lines.map((line) => (
                      <TableRow key={line.id}>
                        <TableCell>{line.itemName}</TableCell>
                        <TableCell className="text-right">{line.quantity}</TableCell>
                        <TableCell className="text-right">{currency(line.unitPrice)}</TableCell>
                        <TableCell className="text-right">{line.discountPercentage}</TableCell>
                        <TableCell className="text-right">{currency(line.vatAmount)}</TableCell>
                        <TableCell className="text-right">{currency(line.lineAmount)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              <div className="space-y-1">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Taxable</span>
                  <span>{currency(detailsTarget.taxableAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">VAT</span>
                  <span>{currency(detailsTarget.vatAmount)}</span>
                </div>
                <Separator />
                <div className="flex justify-between text-base font-bold">
                  <span>Total</span>
                  <span>{currency(detailsTarget.totalAmount)}</span>
                </div>
                {detailsTarget.paymentMethod && detailsTarget.changeAmount != null && (
                  <div className="flex justify-between text-muted-foreground">
                    <span>Change</span>
                    <span>{currency(detailsTarget.changeAmount)}</span>
                  </div>
                )}
              </div>

              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>
                  CBMS: {detailsTarget.cbmsSynced ? "Synced" : "Pending"}
                  {detailsTarget.cbmsRetryCount > 0 ? ` · ${detailsTarget.cbmsRetryCount} attempt(s)` : ""}
                </span>
                <span>{detailsTarget.isTaxInvoice ? "Tax invoice" : "Invoice"}</span>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Void */}
      <Dialog open={voidTarget !== null} onOpenChange={(open) => !open && setVoidTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Void transaction</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmitVoid} className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Voiding <span className="font-medium">{voidTarget?.number}</span> reverses the stock issue
              and the session totals. A reason is required for the audit trail.
            </p>
            <div className="space-y-1">
              <Label htmlFor="reason">Reason</Label>
              <Input
                id="reason"
                value={voidReason}
                onChange={(event) => setVoidReason(event.target.value)}
                placeholder="e.g. Customer changed mind"
                required
                minLength={3}
              />
            </div>
            <Button type="submit" variant="destructive" className="w-full" disabled={isVoiding}>
              {isVoiding ? "Voiding…" : "Void transaction"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
