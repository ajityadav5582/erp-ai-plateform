"use client";

import { useState, useEffect, useCallback } from "react";

type Tab = "bills" | "voids" | "audit" | "cbms";

interface BillRow {
  billNumber: string;
  billDate: string;
  buyerName: string;
  buyerPan: string;
  total: number;
  taxable: number;
  vat: number;
  status: string;
  seqNo: number;
  gap: boolean;
}

interface VoidRow {
  creditNoteNo: string;
  originalInvoice: string;
  buyerName: string;
  buyerPan: string;
  returnAmount: number;
  vat: number;
  reason: string;
  cancelledAt: string;
}

interface AuditRow {
  id: number;
  createdAt: string;
  userName: string;
  actionType: string;
  entityName: string;
  entityId: string;
  details: string;
  ipAddress: string;
}

interface CbmsRow {
  invoiceNumber: string;
  responseCode: number;
  responseBody: string;
  saleTime: string;
  syncTime: string;
  status: string;
}

const TABS: { key: Tab; label: string }[] = [
  { key: "bills", label: "Sequential Bill Register" },
  { key: "voids", label: "Void / Credit Note Register" },
  { key: "audit", label: "System Change & User Audit Trail" },
  { key: "cbms", label: "Raw CBMS Logs" },
];

export default function AuditorPortalPage() {
  const [tab, setTab] = useState<Tab>("bills");
  const [companyId, setCompanyId] = useState<string>("");
  const [fromDate, setFromDate] = useState<string>("");
  const [toDate, setToDate] = useState<string>("");
  const [bills, setBills] = useState<BillRow[]>([]);
  const [voids, setVoids] = useState<VoidRow[]>([]);
  const [audit, setAudit] = useState<AuditRow[]>([]);
  const [cbms, setCbms] = useState<CbmsRow[]>([]);
  const [gaps, setGaps] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchBills = useCallback(async () => {
    if (!companyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/v1/pos/auditor/bills?companyId=${companyId}&from=${fromDate}&to=${toDate}`
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setBills(data.bills ?? []);
      setGaps(data.gaps ?? []);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [companyId, fromDate, toDate]);

  const fetchVoids = useCallback(async () => {
    if (!companyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/v1/pos/auditor/voids?companyId=${companyId}&from=${fromDate}&to=${toDate}`
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setVoids(data ?? []);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [companyId, fromDate, toDate]);

  const fetchAudit = useCallback(async () => {
    if (!companyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/v1/pos/auditor/audit-trail?companyId=${companyId}&from=${fromDate}&to=${toDate}`
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setAudit(data ?? []);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [companyId, fromDate, toDate]);

  const fetchCbms = useCallback(async () => {
    if (!companyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/v1/pos/auditor/cbms-logs?companyId=${companyId}&from=${fromDate}&to=${toDate}`
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setCbms(data ?? []);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [companyId, fromDate, toDate]);

  useEffect(() => {
    if (tab === "bills") fetchBills();
    else if (tab === "voids") fetchVoids();
    else if (tab === "audit") fetchAudit();
    else if (tab === "cbms") fetchCbms();
  }, [tab, fetchBills, fetchVoids, fetchAudit, fetchCbms]);

  const downloadDossier = async () => {
    if (!companyId) return;
    try {
      const res = await fetch(
        `/api/v1/pos/auditor/dossier?companyId=${companyId}&from=${fromDate}&to=${toDate}`
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `tax-inspector-dossier-${companyId}.zip`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Tax Auditor Portal (IRD Inspection Mode)</h1>
        <span className="text-xs text-gray-500">Read-only access · ROLE_TAX_AUDITOR</span>
      </div>

      <div className="flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-xs">Company ID</label>
          <input
            className="border px-2 py-1"
            value={companyId}
            onChange={(e) => setCompanyId(e.target.value)}
            placeholder="1"
          />
        </div>
        <div>
          <label className="block text-xs">From</label>
          <input
            type="date"
            className="border px-2 py-1"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-xs">To</label>
          <input
            type="date"
            className="border px-2 py-1"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
        <button
          className="border px-3 py-1 bg-blue-600 text-white"
          onClick={downloadDossier}
        >
          Download Tax Inspector Dossier (ZIP)
        </button>
      </div>

      <div className="flex gap-2 border-b">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`px-3 py-2 ${tab === t.key ? "border-b-2 border-blue-600 font-semibold" : ""}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && <div className="text-red-600 text-sm">{error}</div>}
      {loading && <div className="text-sm text-gray-500">Loading…</div>}

      {tab === "bills" && (
        <div className="space-y-2">
          {gaps.length > 0 && (
            <div className="bg-red-50 border border-red-300 text-red-700 p-3 rounded">
              <strong>Sequence gaps detected:</strong> {gaps.join(", ")}
            </div>
          )}
          {gaps.length === 0 && bills.length > 0 && (
            <div className="bg-green-50 border border-green-300 text-green-700 p-3 rounded">
              No gaps found — sequence continuity intact.
            </div>
          )}
          <table className="w-full border text-sm">
            <thead>
              <tr className="bg-gray-100">
                <th className="border px-2">Bill No</th>
                <th className="border px-2">Date</th>
                <th className="border px-2">Buyer</th>
                <th className="border px-2">PAN</th>
                <th className="border px-2">Total</th>
                <th className="border px-2">Taxable</th>
                <th className="border px-2">VAT</th>
                <th className="border px-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {bills.map((b, i) => (
                <tr
                  key={i}
                  className={b.gap ? "bg-red-100 text-red-800" : ""}
                >
                  <td className="border px-2">{b.billNumber}</td>
                  <td className="border px-2">{b.billDate}</td>
                  <td className="border px-2">{b.buyerName}</td>
                  <td className="border px-2">{b.buyerPan}</td>
                  <td className="border px-2">{b.total}</td>
                  <td className="border px-2">{b.taxable}</td>
                  <td className="border px-2">{b.vat}</td>
                  <td className="border px-2">{b.status}</td>
                </tr>
              ))}
              {bills.length === 0 && !loading && (
                <tr>
                  <td colSpan={8} className="border px-2 text-center text-gray-500">
                    No bills found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === "voids" && (
        <table className="w-full border text-sm">
          <thead>
            <tr className="bg-gray-100">
              <th className="border px-2">Credit Note No</th>
              <th className="border px-2">Original Invoice</th>
              <th className="border px-2">Buyer</th>
              <th className="border px-2">PAN</th>
              <th className="border px-2">Return Amount</th>
              <th className="border px-2">VAT</th>
              <th className="border px-2">Reason</th>
              <th className="border px-2">Cancelled At</th>
            </tr>
          </thead>
          <tbody>
            {voids.map((v, i) => (
              <tr key={i}>
                <td className="border px-2">{v.creditNoteNo}</td>
                <td className="border px-2">{v.originalInvoice}</td>
                <td className="border px-2">{v.buyerName}</td>
                <td className="border px-2">{v.buyerPan}</td>
                <td className="border px-2">{v.returnAmount}</td>
                <td className="border px-2">{v.vat}</td>
                <td className="border px-2">{v.reason}</td>
                <td className="border px-2">{v.cancelledAt}</td>
              </tr>
            ))}
            {voids.length === 0 && !loading && (
              <tr>
                <td colSpan={8} className="border px-2 text-center text-gray-500">
                  No voids / credit notes found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {tab === "audit" && (
        <table className="w-full border text-sm">
          <thead>
            <tr className="bg-gray-100">
              <th className="border px-2">Time</th>
              <th className="border px-2">User</th>
              <th className="border px-2">Action</th>
              <th className="border px-2">Entity</th>
              <th className="border px-2">Entity ID</th>
              <th className="border px-2">Details</th>
              <th className="border px-2">IP</th>
            </tr>
          </thead>
          <tbody>
            {audit.map((a, i) => (
              <tr key={i}>
                <td className="border px-2">{a.createdAt}</td>
                <td className="border px-2">{a.userName}</td>
                <td className="border px-2">{a.actionType}</td>
                <td className="border px-2">{a.entityName}</td>
                <td className="border px-2">{a.entityId}</td>
                <td className="border px-2">{a.details}</td>
                <td className="border px-2">{a.ipAddress}</td>
              </tr>
            ))}
            {audit.length === 0 && !loading && (
              <tr>
                <td colSpan={7} className="border px-2 text-center text-gray-500">
                  No audit entries found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {tab === "cbms" && (
        <table className="w-full border text-sm">
          <thead>
            <tr className="bg-gray-100">
              <th className="border px-2">Invoice No</th>
              <th className="border px-2">Response Code</th>
              <th className="border px-2">Raw Response Body</th>
              <th className="border px-2">Sale Time</th>
              <th className="border px-2">Sync Time</th>
              <th className="border px-2">Status</th>
            </tr>
          </thead>
          <tbody>
            {cbms.map((c, i) => (
              <tr key={i}>
                <td className="border px-2">{c.invoiceNumber}</td>
                <td className="border px-2">{c.responseCode}</td>
                <td className="border px-2 font-mono text-xs">{c.responseBody}</td>
                <td className="border px-2">{c.saleTime}</td>
                <td className="border px-2">{c.syncTime}</td>
                <td className="border px-2">{c.status}</td>
              </tr>
            ))}
            {cbms.length === 0 && !loading && (
              <tr>
                <td colSpan={6} className="border px-2 text-center text-gray-500">
                  No CBMS logs found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
