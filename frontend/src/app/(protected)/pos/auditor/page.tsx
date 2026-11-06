"use client";

import * as React from "react";
import { FileDown, FileText, Receipt, ScrollText, Database, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
  useGetAuditorBillsQuery,
  useGetAuditorVoidsQuery,
  useGetAuditorAuditTrailQuery,
  useGetAuditorCbmsLogsQuery,
  useDownloadDossierMutation,
} from "@/services/pos-auditor.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatPosCurrency as formatCurrency } from "@/lib/pos-format";

const TABS = [
  { id: "bills", label: "Sales Book", icon: Receipt },
  { id: "voids", label: "Voided Invoices", icon: FileText },
  { id: "audit-trail", label: "Audit Trail", icon: ScrollText },
  { id: "cbms-logs", label: "CBMS Sync Logs", icon: Database },
  { id: "dossier", label: "Dossier Download", icon: FileDown },
];

export default function PosAuditorPage() {
  const [activeTab, setActiveTab] = React.useState("bills");
  const [fromDate, setFromDate] = React.useState<string | null>(null);
  const [toDate, setToDate] = React.useState<string | null>(null);
  const [downloadDossier, { isLoading: isDownloading }] = useDownloadDossierMutation();

  const queryParams = { companyId: 1, from: fromDate ?? undefined, to: toDate ?? undefined };

  const handleDownload = async () => {
    try {
      const blob = await downloadDossier(queryParams).unwrap();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "tax-inspector-dossier.zip";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success("Dossier downloaded successfully.");
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b bg-card p-4">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-lg font-semibold">IRD Tax Auditor Portal</h1>
          <Button onClick={handleDownload} disabled={isDownloading}>
            {isDownloading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <FileDown className="mr-2 h-4 w-4" />}
            Download Dossier
          </Button>
        </div>

        <div className="flex gap-2 mb-4 overflow-x-auto">
          {TABS.map((tab) => (
            <Button
              key={tab.id}
              variant={activeTab === tab.id ? "default" : "outline"}
              size="sm"
              className="shrink-0"
              onClick={() => setActiveTab(tab.id)}
            >
              <tab.icon className="mr-1 h-4 w-4" />
              {tab.label}
            </Button>
          ))}
        </div>

        <div className="flex items-center gap-3 mb-4">
          <Label htmlFor="from-date">From</Label>
          <Input
            id="from-date"
            type="date"
            value={fromDate ?? ""}
            onChange={(e) => setFromDate(e.target.value || null)}
            className="w-40"
          />
          <Label htmlFor="to-date">To</Label>
          <Input
            id="to-date"
            type="date"
            value={toDate ?? ""}
            onChange={(e) => setToDate(e.target.value || null)}
            className="w-40"
          />
          <Button variant="outline" size="sm" onClick={() => { setFromDate(null); setToDate(null); }}>
            Clear
          </Button>
        </div>
      </div>

      <div className="p-4">
        {activeTab === "bills" && (
          <AuditorBillsTab queryParams={queryParams} />
        )}
        {activeTab === "voids" && (
          <AuditorVoidsTab queryParams={queryParams} />
        )}
        {activeTab === "audit-trail" && (
          <AuditorAuditTrailTab queryParams={queryParams} />
        )}
        {activeTab === "cbms-logs" && (
          <AuditorCbmsLogsTab queryParams={queryParams} />
        )}
        {activeTab === "dossier" && (
          <div className="text-center py-12">
            <FileDown className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
            <h3 className="mb-2 text-lg font-semibold">Tax Inspector Dossier</h3>
            <p className="mb-6 text-sm text-muted-foreground">
              The dossier is a ZIP archive containing:
            </p>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>• Sales Book CSV — all bills with buyer PAN and VAT breakdown</li>
              <li>• Credit Notes CSV — voided invoices with return amounts</li>
              <li>• Audit Log CSV — every auditor GET request recorded</li>
            </ul>
            <Button onClick={handleDownload} disabled={isDownloading} className="mt-6">
              {isDownloading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <FileDown className="mr-2 h-4 w-4" />}
              Download Dossier
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}

function AuditorBillsTab({ queryParams }: { queryParams: { companyId: number; from?: string; to?: string } }) {
  const { data, isLoading } = useGetAuditorBillsQuery(queryParams);
  const bills = data?.bills ?? [];
  const gaps = data?.gaps ?? [];

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <span className="text-sm text-muted-foreground">
          {bills.length} bills · {gaps.length} gaps
        </span>
      </div>
      {isLoading ? (
        <TableSkeletonRows columns={8} rows={5} />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Bill No</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Buyer</TableHead>
              <TableHead>PAN</TableHead>
              <TableHead>Total</TableHead>
              <TableHead>Taxable</TableHead>
              <TableHead>VAT</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {bills.map((bill) => (
              <TableRow key={bill.billNumber}>
                <TableCell>{bill.billNumber}</TableCell>
                <TableCell>{bill.billDate}</TableCell>
                <TableCell>{bill.buyerName}</TableCell>
                <TableCell>{bill.buyerPan}</TableCell>
                <TableCell>{formatCurrency(bill.total)}</TableCell>
                <TableCell>{formatCurrency(bill.taxable)}</TableCell>
                <TableCell>{formatCurrency(bill.vat)}</TableCell>
                <TableCell>
                  {bill.gap ? (
                    <span className="text-xs bg-yellow-100 px-2 py-0.5 rounded">GAP</span>
                  ) : (
                    bill.status
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

function AuditorVoidsTab({ queryParams }: { queryParams: { companyId: number; from?: string; to?: string } }) {
  const { data, isLoading } = useGetAuditorVoidsQuery(queryParams);
  const voids = data ?? [];

  return (
    <div>
      <div className="mb-3 text-sm text-muted-foreground">
        {voids.length} voided invoices
      </div>
      {isLoading ? (
        <TableSkeletonRows columns={8} rows={5} />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Credit Note No</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Buyer</TableHead>
              <TableHead>PAN</TableHead>
              <TableHead>Return Amount</TableHead>
              <TableHead>VAT</TableHead>
              <TableHead>Reason</TableHead>
              <TableHead>Cancelled At</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {voids.map((v) => (
              <TableRow key={v.creditNoteNo}>
                <TableCell>{v.creditNoteNo}</TableCell>
                <TableCell>{v.billDate}</TableCell>
                <TableCell>{v.buyerName}</TableCell>
                <TableCell>{v.buyerPan}</TableCell>
                <TableCell>{formatCurrency(v.returnAmount)}</TableCell>
                <TableCell>{formatCurrency(v.vat)}</TableCell>
                <TableCell>{v.reason}</TableCell>
                <TableCell>{v.cancelledAt}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

function AuditorAuditTrailTab({ queryParams }: { queryParams: { companyId: number; from?: string; to?: string } }) {
  const { data, isLoading } = useGetAuditorAuditTrailQuery(queryParams);
  const logs = data ?? [];

  return (
    <div>
      <div className="mb-3 text-sm text-muted-foreground">
        {logs.length} audit log entries
      </div>
      {isLoading ? (
        <TableSkeletonRows columns={7} rows={8} />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Time</TableHead>
              <TableHead>User</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Entity</TableHead>
              <TableHead>Entity ID</TableHead>
              <TableHead>IP Address</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map((log) => (
              <TableRow key={log.id}>
                <TableCell>{log.id}</TableCell>
                <TableCell>{log.createdAt}</TableCell>
                <TableCell>{log.userName}</TableCell>
                <TableCell>{log.actionType}</TableCell>
                <TableCell>{log.entityName}</TableCell>
                <TableCell>{log.entityId}</TableCell>
                <TableCell>{log.ipAddress}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

function AuditorCbmsLogsTab({ queryParams }: { queryParams: { companyId: number; from?: string; to?: string } }) {
  const { data, isLoading } = useGetAuditorCbmsLogsQuery(queryParams);
  const logs = data ?? [];

  return (
    <div>
      <div className="mb-3 text-sm text-muted-foreground">
        {logs.length} CBMS sync entries
      </div>
      {isLoading ? (
        <TableSkeletonRows columns={6} rows={5} />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Invoice No</TableHead>
              <TableHead>Response Code</TableHead>
              <TableHead>Error Body</TableHead>
              <TableHead>Sale Time</TableHead>
              <TableHead>Sync Time</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map((log) => (
              <TableRow key={log.invoiceNumber}>
                <TableCell>{log.invoiceNumber}</TableCell>
                <TableCell>{log.responseCode}</TableCell>
                <TableCell className="max-w-xs truncate" title={log.responseBody}>
                  {log.responseBody || "—"}
                </TableCell>
                <TableCell>{log.saleTime}</TableCell>
                <TableCell>{log.syncTime}</TableCell>
                <TableCell>{log.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
