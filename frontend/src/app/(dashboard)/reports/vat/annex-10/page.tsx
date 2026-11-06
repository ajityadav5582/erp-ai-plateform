"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { readActiveFiscalSelection } from "@/services/business-context";
import { Button } from "@/components/ui/button";

interface VatReturnResponse {
  companyId: number;
  fiscalYearId: number;
  nepaliMonth: number;
  totalSales: number;
  taxExemptSales: number;
  zeroRatedExports: number;
  taxableSales: number;
  outputVatCollected: number;
  lessSalesReturnVat: number;
  netOutputVatPayable: number;
  totalLocalPurchases: number;
  taxExemptPurchases: number;
  taxableLocalPurchases: number;
  importPurchasesTaxable: number;
  capitalGoodsPurchases: number;
  totalEligibleInputVat: number;
  lessPurchaseReturnVat: number;
  netEligibleInputVat: number;
  previousMonthCreditBroughtForward: number;
  netVatPayableToGoN: number;
  netVatCarriedForward: number;
}

const MONTHS = [
  "Baisakh", "Jestha", "Asar", "Shrawan", "Bhadra", "Ashwin",
  "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra",
];

export default function Annex10Page() {
  const selection = readActiveFiscalSelection();
  const companyId = selection?.companyId;
  const fiscalYearId = selection?.fiscalYearId;

  const [month, setMonth] = useState<number>(7);
  const [copied, setCopied] = useState(false);

  const params = useMemo(() => {
    const p: Record<string, string | number> = { nepaliMonth: month };
    if (companyId) p.companyId = companyId;
    if (fiscalYearId) p.fiscalYearId = fiscalYearId;
    return p;
  }, [companyId, fiscalYearId, month]);

  const { data, isLoading, isError, refetch, isFetching } = useQuery<VatReturnResponse>({
    queryKey: ["vat-annex-10", params],
    queryFn: async () => {
      const res = await axiosInstance.get("/api/v1/reports/vat/annex-10", { params });
      return res.data as VatReturnResponse;
    },
    enabled: !!companyId,
  });

  const portalArray = useMemo(() => {
    if (!data) return [];
    return [
      data.totalSales, data.taxExemptSales, data.zeroRatedExports, data.taxableSales,
      data.outputVatCollected, data.lessSalesReturnVat, data.netOutputVatPayable,
      data.totalLocalPurchases, data.taxExemptPurchases, data.taxableLocalPurchases,
      data.importPurchasesTaxable, data.capitalGoodsPurchases, data.totalEligibleInputVat,
      data.lessPurchaseReturnVat, data.netEligibleInputVat,
      data.previousMonthCreditBroughtForward,
      data.netVatPayableToGoN > 0 ? data.netVatPayableToGoN : data.netVatCarriedForward,
    ];
  }, [data]);

  const copyPortal = async () => {
    await navigator.clipboard.writeText(portalArray.join("\t"));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const exportPdf = () => {
    const url = `/api/v1/reports/vat/annex-10/export-pdf?companyId=${companyId}&fiscalYearId=${fiscalYearId}&nepaliMonth=${month}`;
    window.open(url, "_blank");
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">VAT Return (Annex 10) - अनुसूची १०</h1>
        <div className="flex gap-2">
          <Button onClick={copyPortal} disabled={!data}>{copied ? "Copied!" : "Copy Portal Values"}</Button>
          <Button variant="outline" onClick={exportPdf} disabled={!data}>Export PDF</Button>
          <Button variant="outline" onClick={() => refetch()} disabled={isFetching}>Refresh</Button>
        </div>
      </div>

      <div className="flex gap-4 items-center">
        <label className="text-sm font-medium">Nepali Month</label>
        <select
          className="border rounded px-2 py-1"
          value={month}
          onChange={(e) => setMonth(Number(e.target.value))}
        >
          {MONTHS.map((m, i) => (
            <option key={i} value={i + 1}>{m} ({i + 1})</option>
          ))}
        </select>
      </div>

      {isLoading && <div>Loading...</div>}
      {isError && <div className="text-red-600">Failed to load.</div>}

      {data && (
        <div className="space-y-4">
          <details open className="border rounded p-4">
            <summary className="font-semibold cursor-pointer">Section 1: Sales / Output VAT (बिक्री कर)</summary>
            <table className="w-full mt-2 text-sm">
              <tbody>
                <Row label="Row 1: Total Sales (कुल बिक्री)" value={data.totalSales} />
                <Row label="Row 2: Tax-Exempt Sales (कर छुट भएको बिक्री)" value={data.taxExemptSales} />
                <Row label="Row 3: Zero-Rated Exports (शून्य दरको निकासी)" value={data.zeroRatedExports} />
                <Row label="Row 4: Taxable Sales (करयोग्य बिक्री)" value={data.taxableSales} />
                <Row label="Row 5: Output VAT Collected (13%)" value={data.outputVatCollected} />
                <Row label="Row 6: Less Sales Return VAT (बिक्री फिर्ताको कर)" value={data.lessSalesReturnVat} />
                <Row label="Row 7: Net Output VAT Payable (खुद बिक्री कर)" value={data.netOutputVatPayable} bold />
              </tbody>
            </table>
          </details>

          <details open className="border rounded p-4">
            <summary className="font-semibold cursor-pointer">Section 2: Purchase / Input VAT (खरिद कर कट्टी)</summary>
            <table className="w-full mt-2 text-sm">
              <tbody>
                <Row label="Row 8: Total Local Purchases (जम्मा स्थानीय खरिद)" value={data.totalLocalPurchases} />
                <Row label="Row 9: Tax-Exempt Purchases (कर छुट भएको खरिद)" value={data.taxExemptPurchases} />
                <Row label="Row 10: Taxable Local Purchases (करयोग्य स्थानीय खरिद)" value={data.taxableLocalPurchases} />
                <Row label="Row 11: Import Purchases - Taxable (पैठारी खरिद)" value={data.importPurchasesTaxable} />
                <Row label="Row 12: Capital Goods Purchases (पूँजीगत सम्पत्ति खरिद)" value={data.capitalGoodsPurchases} />
                <Row label="Row 13: Total Eligible Input VAT (जम्मा दाबी गर्न पाउने खरिद कर)" value={data.totalEligibleInputVat} />
                <Row label="Row 14: Less Purchase Return VAT (खरिद फिर्ताको कर)" value={data.lessPurchaseReturnVat} />
                <Row label="Row 15: Net Eligible Input VAT (खुद दाबी गर्न पाउने खरिद कर)" value={data.netEligibleInputVat} bold />
              </tbody>
            </table>
          </details>

          <details open className="border rounded p-4">
            <summary className="font-semibold cursor-pointer">Section 3: Tax Settlement (कर मिलान)</summary>
            <table className="w-full mt-2 text-sm">
              <tbody>
                <Row label="Row 16: Previous Month Credit Brought Forward (गत महिनाको बढी भएको कर रकम)" value={data.previousMonthCreditBroughtForward} />
                <Row label="Row 17: Net VAT Payable to GoN (दाखिला गर्नुपर्ने खुद कर)" value={data.netVatPayableToGoN} bold />
                <Row label="Row 17: Net VAT Carried Forward (अर्को महिना सार्ने कर)" value={data.netVatCarriedForward} bold />
              </tbody>
            </table>
          </details>
        </div>
      )}
    </div>
  );
}

function Row({ label, value, bold }: { label: string; value: number; bold?: boolean }) {
  return (
    <tr className={bold ? "font-bold" : ""}>
      <td className="py-1 pr-4">{label}</td>
      <td className="py-1 text-right">{Number(value ?? 0).toLocaleString()}</td>
    </tr>
  );
}
