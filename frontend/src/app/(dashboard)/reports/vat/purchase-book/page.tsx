"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { readActiveFiscalSelection } from "@/services/business-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface PurchaseBookRow {
  mitiBs: string;
  dateAd: string;
  invoiceNumber: string;
  customDeclarationNumber: string | null;
  supplierName: string;
  supplierPan: string;
  totalPurchaseAmount: number;
  exemptPurchaseAmount: number;
  taxablePurchaseAmount: number;
  inputVatAmount: number;
  importTaxableAmount: number;
  importVatAmount: number;
  capitalAsset: boolean;
}

interface PurchaseBookTotals {
  totalPurchaseAmount: number;
  exemptPurchaseAmount: number;
  taxablePurchaseAmount: number;
  inputVatAmount: number;
  importTaxableAmount: number;
  importVatAmount: number;
}

interface PurchaseBookResponse {
  rows: PurchaseBookRow[];
  totals: PurchaseBookTotals;
  page: number;
  size: number;
  totalElements: number;
}

export default function PurchaseBookPage() {
  const selection = readActiveFiscalSelection();
  const companyId = selection?.companyId;
  const fiscalYearId = selection?.fiscalYearId;

  const [startDateNp, setStartDateNp] = useState("");
  const [endDateNp, setEndDateNp] = useState("");
  const [supplierId, setSupplierId] = useState("");

  const params = useMemo(() => {
    const p: Record<string, string | number> = { page: 0, size: 100 };
    if (companyId) p.companyId = companyId;
    if (fiscalYearId) p.fiscalYearId = fiscalYearId;
    if (startDateNp) p.startDateNp = startDateNp;
    if (endDateNp) p.endDateNp = endDateNp;
    if (supplierId) p.supplierId = supplierId;
    return p;
  }, [companyId, fiscalYearId, startDateNp, endDateNp, supplierId]);

  const { data, isLoading, isError, refetch, isFetching } = useQuery<PurchaseBookResponse>({
    queryKey: ["purchase-book", params],
    queryFn: async () => {
      const res = await axiosInstance.get("/api/v1/procurement/reports/purchase-book", { params });
      return res.data as PurchaseBookResponse;
    },
    enabled: !!companyId,
  });

  const rows = data?.rows ?? [];
  const totals = data?.totals;

  const handleExport = async (type: "excel" | "csv") => {
    const res = await axiosInstance.get(
      `/api/v1/procurement/reports/purchase-book/export-${type === "excel" ? "excel" : "csv"}`,
      { params, responseType: "blob" }
    );
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement("a");
    a.href = url;
    a.download = type === "excel" ? "purchase-book-annex7.xlsx" : "purchase-book-annex7.csv";
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">VAT Purchase Book (खरिद खाता - Annex 7)</h1>

      <div className="grid grid-cols-3 gap-4">
        <div className="border p-4 rounded">
          <div className="text-sm text-muted-foreground">Total Purchases</div>
          <div className="text-lg font-bold">{totals?.totalPurchaseAmount ?? 0}</div>
        </div>
        <div className="border p-4 rounded">
          <div className="text-sm text-muted-foreground">Total Taxable</div>
          <div className="text-lg font-bold">{totals?.taxablePurchaseAmount ?? 0}</div>
        </div>
        <div className="border p-4 rounded">
          <div className="text-sm text-muted-foreground">Total Input VAT</div>
          <div className="text-lg font-bold">{totals?.inputVatAmount ?? 0}</div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 items-end">
        <div>
          <label className="text-sm">Start Date (BS)</label>
          <Input value={startDateNp} onChange={(e) => setStartDateNp(e.target.value)} placeholder="2081.01.01" />
        </div>
        <div>
          <label className="text-sm">End Date (BS)</label>
          <Input value={endDateNp} onChange={(e) => setEndDateNp(e.target.value)} placeholder="2081.12.30" />
        </div>
        <div>
          <label className="text-sm">Supplier ID</label>
          <Input value={supplierId} onChange={(e) => setSupplierId(e.target.value)} placeholder="optional" />
        </div>
        <Button onClick={() => refetch()} disabled={isFetching}>Apply</Button>
        <Button variant="outline" onClick={() => handleExport("excel")}>Export Annex 7 (Excel)</Button>
        <Button variant="outline" onClick={() => handleExport("csv")}>Export CSV</Button>
      </div>

      {isLoading && <div>Loading...</div>}
      {isError && <div className="text-red-600">Failed to load purchase book.</div>}

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Miti (BS)</TableHead>
              <TableHead>Date (AD)</TableHead>
              <TableHead>Invoice No</TableHead>
              <TableHead>Supplier</TableHead>
              <TableHead>PAN</TableHead>
              <TableHead className="text-right">Total</TableHead>
              <TableHead className="text-right">Exempt</TableHead>
              <TableHead className="text-right">Taxable</TableHead>
              <TableHead className="text-right">VAT</TableHead>
              <TableHead>Capital Asset</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((r, i) => (
              <TableRow key={i}>
                <TableCell>{r.mitiBs}</TableCell>
                <TableCell>{r.dateAd}</TableCell>
                <TableCell>{r.invoiceNumber}</TableCell>
                <TableCell>{r.supplierName}</TableCell>
                <TableCell>{r.supplierPan}</TableCell>
                <TableCell className="text-right">{r.totalPurchaseAmount}</TableCell>
                <TableCell className="text-right">{r.exemptPurchaseAmount}</TableCell>
                <TableCell className="text-right">{r.taxablePurchaseAmount}</TableCell>
                <TableCell className="text-right">{r.inputVatAmount}</TableCell>
                <TableCell>{r.capitalAsset ? "Capital" : "Raw/Trading"}</TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && !isLoading && (
              <TableRow>
                <TableCell colSpan={10} className="text-center text-muted-foreground">
                  No records found.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
