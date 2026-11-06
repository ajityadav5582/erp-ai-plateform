"use client";

import { useState } from "react";

export default function NewSalesReturnPage() {
  const [reason, setReason] = useState("Damaged Goods");
  const [dateBs, setDateBs] = useState("");
  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">New Sales Return / Credit Note</h1>
      <div>
        <label>Original Invoice</label>
        <input className="border px-2 py-1" placeholder="Search invoice number" />
      </div>
      <div>
        <label>Reason</label>
        <select className="border px-2 py-1" value={reason} onChange={(e) => setReason(e.target.value)}>
          <option>Damaged Goods</option>
          <option>Order Cancelled</option>
          <option>Pricing Error</option>
        </select>
      </div>
      <div>
        <label>Credit Note Date (BS)</label>
        <input className="border px-2 py-1" value={dateBs} onChange={(e) => setDateBs(e.target.value)} />
      </div>
      <div className="border p-4">
        <div>Taxable Return Amount: 0</div>
        <div>VAT (13%): 0</div>
        <div>Total Refund: 0</div>
      </div>
      <button className="bg-blue-600 text-white px-4 py-2 rounded">Submit</button>
    </div>
  );
}
