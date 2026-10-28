"use client";

import * as React from "react";
import { Clock, DoorClosed, DoorOpen, Printer, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
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
import { toast } from "sonner";
import {
  useGetPosSessionsQuery,
  useOpenPosSessionMutation,
  useClosePosSessionMutation,
  type PosSession,
} from "@/services/pos-session.service";
import { useGetPosRegistersQuery as useRegisters } from "@/services/pos-register.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { PosSessionStatusBadge } from "@/components/pos/pos-status-badge";
import { formatPosCurrency as currency } from "@/lib/pos-format";

/**
 * Cashier session administration — mobile first.
 *
 * Lists shifts with their running totals and cash variance, and hosts the
 * open/close actions. Closing records the physical cash count; the domain
 * computes the variance against opening cash plus cash sales.
 */
export default function PosSessionsPage() {
  const [page, setPage] = React.useState(0);
  const [statusFilter, setStatusFilter] = React.useState<string>("ALL");
  const [isOpenOpen, setIsOpenOpen] = React.useState(false);
  const [closeTarget, setCloseTarget] = React.useState<PosSession | null>(null);
  const [closingCash, setClosingCash] = React.useState("");
  const [reportTarget, setReportTarget] = React.useState<PosSession | null>(null);

  const { data, isLoading, isFetching, refetch } = useGetPosSessionsQuery({
    page,
    size: 20,
    status: statusFilter === "ALL" ? undefined : (statusFilter as "OPEN" | "CLOSED"),
  });
  const { data: registers } = useRegisters({ status: "ACTIVE", size: 100 });
  const [openSession, { isLoading: isOpening }] = useOpenPosSessionMutation();
  const [closeSession, { isLoading: isClosing }] = useClosePosSessionMutation();

  const [openForm, setOpenForm] = React.useState({ registerId: "", openingCashAmount: "0" });

  const onSubmitOpen = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!openForm.registerId) {
      toast.error("Select a register.");
      return;
    }
    try {
      await openSession({
        registerId: Number(openForm.registerId),
        openingCashAmount: Number(openForm.openingCashAmount) || 0,
      }).unwrap();
      toast.success("Session opened.");
      setIsOpenOpen(false);
      setOpenForm({ registerId: "", openingCashAmount: "0" });
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not open the session.");
    }
  };

  const onSubmitClose = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!closeTarget) return;
    try {
      await closeSession({
        id: closeTarget.id,
        body: { closingCashAmount: Number(closingCash) || 0 },
      }).unwrap();
      toast.success("Session closed.");
      setCloseTarget(null);
      setClosingCash("");
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not close the session.");
    }
  };

  const sessions = data?.content ?? [];

  return (
    <div className="space-y-4 p-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">POS Sessions</h1>
          <p className="text-sm text-muted-foreground">Cashier shifts and their cash variance.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="icon" onClick={() => void refetch()} disabled={isFetching}>
            <RefreshCw className={isFetching ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
          </Button>
          <Button onClick={() => setIsOpenOpen(true)}>
            <DoorOpen className="mr-1 h-4 w-4" />
            Open session
          </Button>
        </div>
      </header>

      <div className="flex items-center gap-2">
        <Label htmlFor="status" className="text-sm">
          Status
        </Label>
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger id="status" className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All</SelectItem>
            <SelectItem value="OPEN">Open</SelectItem>
            <SelectItem value="CLOSED">Closed</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <TableSkeletonRows columns={5} rows={5} />
      ) : sessions.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed py-10">
          <Clock className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">No sessions found.</p>
        </div>
      ) : (
        <>
          {/* Mobile: cards */}
          <ul className="space-y-2 md:hidden">
            {sessions.map((session) => (
              <li key={session.id} className="rounded-lg border p-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-mono text-sm font-medium">{session.sessionNumber}</p>
                    <p className="text-xs text-muted-foreground">
                      {session.registerName} · {session.cashierName}
                    </p>
                  </div>
                  <PosSessionStatusBadge status={session.status} />
                </div>
                <dl className="mt-2 grid grid-cols-2 gap-1 text-xs">
                  <div>
                    <dt className="text-muted-foreground">Sales</dt>
                    <dd className="font-medium">{currency(session.grossSales)}</dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Transactions</dt>
                    <dd className="font-medium">{session.totalTransactions}</dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Variance</dt>
                    <dd className="font-medium">{currency(session.cashVariance)}</dd>
                  </div>
                </dl>
                <div className="mt-2 flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => setReportTarget(session)}>
                    <Printer className="mr-1 h-4 w-4" /> Z-report
                  </Button>
                  {session.status === "OPEN" && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setCloseTarget(session);
                        setClosingCash("");
                      }}
                    >
                      <DoorClosed className="mr-1 h-4 w-4" /> Close
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
                  <TableHead>Session</TableHead>
                  <TableHead>Register</TableHead>
                  <TableHead>Cashier</TableHead>
                  <TableHead className="text-right">Sales</TableHead>
                  <TableHead className="text-right">Txns</TableHead>
                  <TableHead className="text-right">Variance</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {sessions.map((session) => (
                  <TableRow key={session.id}>
                    <TableCell className="font-mono text-xs">{session.sessionNumber}</TableCell>
                    <TableCell>{session.registerName}</TableCell>
                    <TableCell>{session.cashierName}</TableCell>
                    <TableCell className="text-right">{currency(session.grossSales)}</TableCell>
                    <TableCell className="text-right">{session.totalTransactions}</TableCell>
                    <TableCell className="text-right">{currency(session.cashVariance)}</TableCell>
                    <TableCell>
                      <PosSessionStatusBadge status={session.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setReportTarget(session)}>
                        Z-report
                      </Button>
                      {session.status === "OPEN" && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setCloseTarget(session);
                            setClosingCash("");
                          }}
                        >
                          Close
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

      <Dialog open={isOpenOpen} onOpenChange={setIsOpenOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Open session</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmitOpen} className="space-y-3">
            <div className="space-y-1">
              <Label>Register</Label>
              <Select
                value={openForm.registerId}
                onValueChange={(value) => setOpenForm({ ...openForm, registerId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a register" />
                </SelectTrigger>
                <SelectContent>
                  {(registers?.content ?? []).map((register) => (
                    <SelectItem key={register.id} value={String(register.id)}>
                      {register.registerName} ({register.registerCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="opening-cash">Opening cash</Label>
              <Input
                id="opening-cash"
                type="number"
                min={0}
                step="0.01"
                value={openForm.openingCashAmount}
                onChange={(e) => setOpenForm({ ...openForm, openingCashAmount: e.target.value })}
              />
            </div>
            <Button type="submit" className="w-full" disabled={isOpening}>
              {isOpening ? "Opening…" : "Open session"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={closeTarget !== null} onOpenChange={(open) => !open && setCloseTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Close session</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmitClose} className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Count the physical cash in the drawer. The variance against expected cash is recorded
              automatically.
            </p>
            {closeTarget && (
              <div className="rounded-lg border p-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Opening cash</span>
                  <span>{currency(closeTarget.openingCashAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Cash sales</span>
                  <span>{currency(closeTarget.totalCashSales)}</span>
                </div>
                <div className="flex justify-between font-medium">
                  <span>Expected cash</span>
                  <span>{currency(closeTarget.openingCashAmount + closeTarget.totalCashSales)}</span>
                </div>
              </div>
            )}
            <div className="space-y-1">
              <Label htmlFor="closing-cash">Closing cash</Label>
              <Input
                id="closing-cash"
                type="number"
                min={0}
                step="0.01"
                value={closingCash}
                onChange={(e) => setClosingCash(e.target.value)}
                required
              />
            </div>
            {closeTarget && closingCash.trim() !== "" && (
              <p
                className={
                  Math.abs(
                    (Number(closingCash) || 0) -
                      (closeTarget.openingCashAmount + closeTarget.totalCashSales)
                  ) < 0.005
                    ? "text-sm text-emerald-600"
                    : "text-sm text-amber-600"
                }
              >
                Variance:{" "}
                {currency(
                  (Number(closingCash) || 0) -
                    (closeTarget.openingCashAmount + closeTarget.totalCashSales)
                )}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={isClosing}>
              {isClosing ? "Closing…" : "Close session"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>

      {/* Z-report */}
      <Dialog open={reportTarget !== null} onOpenChange={(open) => !open && setReportTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Z-report · {reportTarget?.sessionNumber}</DialogTitle>
          </DialogHeader>
          {reportTarget && (
            <div className="space-y-2 text-sm">
              <div className="text-center">
                <p className="font-medium">{reportTarget.registerName}</p>
                <p className="text-muted-foreground">{reportTarget.cashierName}</p>
                <p className="text-xs text-muted-foreground">
                  Opened {new Date(reportTarget.openedAt).toLocaleString()}
                  {reportTarget.closedAt
                    ? ` · Closed ${new Date(reportTarget.closedAt).toLocaleString()}`
                    : ""}
                </p>
              </div>
              <Separator />
              <div className="flex justify-between">
                <span className="text-muted-foreground">Opening cash</span>
                <span>{currency(reportTarget.openingCashAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Cash sales</span>
                <span>{currency(reportTarget.totalCashSales)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Card sales</span>
                <span>{currency(reportTarget.totalCardSales)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Wallet sales</span>
                <span>{currency(reportTarget.totalWalletSales)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Other sales</span>
                <span>{currency(reportTarget.totalOtherSales)}</span>
              </div>
              <Separator />
              <div className="flex justify-between">
                <span className="text-muted-foreground">Transactions</span>
                <span>{reportTarget.totalTransactions}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Voided</span>
                <span>{reportTarget.totalVoided}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">VAT collected</span>
                <span>{currency(reportTarget.totalVatCollected)}</span>
              </div>
              <div className="flex justify-between text-base font-bold">
                <span>Gross sales</span>
                <span>{currency(reportTarget.grossSales)}</span>
              </div>
              {reportTarget.status === "CLOSED" && (
                <>
                  <Separator />
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Expected cash</span>
                    <span>{currency(reportTarget.expectedCash ?? 0)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Counted cash</span>
                    <span>{currency(reportTarget.closingCashAmount ?? 0)}</span>
                  </div>
                  <div className="flex justify-between font-medium">
                    <span>Variance</span>
                    <span>{currency(reportTarget.cashVariance ?? 0)}</span>
                  </div>
                </>
              )}
              <Button variant="outline" className="w-full" onClick={() => window.print()}>
                <Printer className="mr-2 h-4 w-4" /> Print
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
