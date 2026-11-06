"use client";

import * as React from "react";
import { RefreshCw, AlertTriangle, CheckCircle2, Clock, Database, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { TableSkeletonRows } from "@/components/common/table-state";
import { toast } from "sonner";
import {
  useGetSyncStatusQuery,
  useGetFailuresQuery,
  useRetryAllFailedMutation,
} from "@/services/ird-sync-monitor.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { readActiveFiscalSelection } from "@/services/business-context";

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Pending",
  FAILED: "Failed",
  DEAD_LETTER: "Dead Letter",
  SYNCED: "Synced",
  SKIPPED: "Skipped",
};

const SOURCE_LABELS: Record<string, string> = {
  POS: "POS",
  PROCUREMENT: "Procurement",
};

export default function IrdSyncMonitorPage() {
  const selection = readActiveFiscalSelection();
  const companyId = selection?.companyId ?? 1;
  const companyName = selection?.companyName ?? "Active Company";

  const [retryAllFailed, { isLoading: isRetrying }] = useRetryAllFailedMutation();

  const handleRetry = async () => {
    try {
      await retryAllFailed(companyId).unwrap();
      toast.success("All failed sync records have been re-queued." );
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b bg-card p-4">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-lg font-semibold">IRD CBMS Sync Monitor</h1>
          <Button onClick={handleRetry} disabled={isRetrying}>
            {isRetrying ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
            Re-queue Failed
          </Button>
        </div>
        <p className="text-sm text-muted-foreground">Company: {companyName} (ID: {companyId})</p>
      </div>

      <div className="p-4 space-y-4">
        <SyncStatusCard companyId={companyId} />
        <FailuresTable companyId={companyId} />
      </div>
    </div>
  );
}

function SyncStatusCard({ companyId }: { companyId: number }) {
  const { data: status, isLoading } = useGetSyncStatusQuery(companyId);

  if (isLoading || !status) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Sync Status</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-6">
          <StatBox label="Synced" value={status.syncedCount} icon={CheckCircle2} color="text-green-600" />
          <StatBox label="Simulated" value={status.simulatedCount} icon={CheckCircle2} color="text-blue-600" />
          <StatBox label="Pending" value={status.pendingCount} icon={Clock} color="text-yellow-600" />
          <StatBox label="Failed" value={status.failedCount} icon={AlertTriangle} color="text-red-600" />
          <StatBox label="Dead Letter" value={status.deadLetterCount} icon={Database} color="text-red-700" />
          <StatBox label="Last Synced" value={status.lastSyncedAt ?? "—"} icon={CheckCircle2} color="text-muted-foreground" />
        </div>
      </CardContent>
    </Card>
  );
}

function StatBox({ label, value, icon: Icon, color }: { label: string; value: number | string; icon: React.ElementType; color: string }) {
  return (
    <div className="rounded-lg border p-3">
      <div className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground">{label}</span>
        <Icon className="h-4 w-4 shrink-0" style={{ color }} />
      </div>
      <p className="mt-1 text-xl font-bold">{value}</p>
    </div>
  );
}

function FailuresTable({ companyId }: { companyId: number }) {
  const { data: failures, isLoading } = useGetFailuresQuery(companyId);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Failed / Dead Letter Records</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <TableSkeletonRows columns={7} rows={5} />
        ) : failures && failures.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Invoice No</TableHead>
                <TableHead>Source</TableHead>
                <TableHead>Transaction ID</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Retries</TableHead>
                <TableHead>Response Code</TableHead>
                <TableHead>Error Message</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {failures.map((f) => (
                <TableRow key={f.id}>
                  <TableCell>{f.invoiceNumber}</TableCell>
                  <TableCell>{SOURCE_LABELS[f.sourceModule] ?? f.sourceModule}</TableCell>
                  <TableCell>{f.sourceTransactionId}</TableCell>
                  <TableCell><Badge variant="destructive">{STATUS_LABELS[f.syncStatus] ?? f.syncStatus}</Badge></TableCell>
                  <TableCell>{f.retryCount}/{f.maxRetries}</TableCell>
                  <TableCell>{f.lastResponseCode ?? "—"}</TableCell>
                  <TableCell className="max-w-xs truncate" title={f.lastErrorMessage}>{f.lastErrorMessage || "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : (
          <div className="py-8 text-center text-sm text-muted-foreground">
            No failed or dead letter records. All invoices are synced successfully.
          </div>
        )}
      </CardContent>
    </Card>
  );
}
