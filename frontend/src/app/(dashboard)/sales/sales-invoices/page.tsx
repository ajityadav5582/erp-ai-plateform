"use client";

import { useState } from "react";

export default function SalesInvoicesPage() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [rows] = useState<any[]>([]);
  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">Sales Invoices</h1>
      <table className="w-full border">
        <thead>
          <tr>
            <th>Invoice No</th>
            <th>Buyer</th>
            <th>Total</th>
            <th>IRD Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              <td>{r.invoiceNumber}</td>
              <td>{r.customerName}</td>
              <td>{r.totalAmount}</td>
              <td>
                {r.cbmsSynced ? (
                  <span className="text-green-600">Synced to IRD</span>
                ) : r.cbmsRetryCount === 0 ? (
                  <span className="text-amber-600">Queued</span>
                ) : (
                  <span className="text-red-600" title={r.cbmsResponse}>Failed</span>
                )}
              </td>
              <td>
                <button>View</button>
                <button>Print</button>
                {!r.cbmsSynced && <button>Retry IRD Sync</button>}
                <button>Create Sales Return</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
