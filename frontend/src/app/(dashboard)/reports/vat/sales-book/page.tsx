"use client";

import { useState } from "react";

export default function SalesBookPage() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [rows] = useState<any[]>([]);
  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">VAT Sales Book (Annex 5)</h1>
      <div className="grid grid-cols-3 gap-4">
        <div className="border p-4">Total Sales</div>
        <div className="border p-4">Total Taxable</div>
        <div className="border p-4">Total VAT</div>
      </div>
      <div className="flex gap-2">
        <button className="border px-3 py-1">Export Excel</button>
        <button className="border px-3 py-1">Export CSV</button>
        <button className="border px-3 py-1">Print Annex 5</button>
      </div>
      <table className="w-full border">
        <thead>
          <tr>
            <th>Bill No</th><th>Buyer</th><th>PAN</th><th>Total</th><th>Taxable</th><th>VAT</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}><td>{r.billNumber}</td><td>{r.buyerName}</td><td>{r.buyerPan}</td><td>{r.totalSales}</td><td>{r.taxableSales}</td><td>{r.vatAmount}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
