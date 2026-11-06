"use client";

export default function CreditNotesPage() {
  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">Credit Note Register (Annex 6)</h1>
      <div className="border p-4">Total VAT Refundable/Adjusted</div>
      <div className="flex gap-2">
        <button className="border px-3 py-1">Export Excel</button>
        <button className="border px-3 py-1">Export CSV</button>
      </div>
      <table className="w-full border">
        <thead>
          <tr><th>Credit Note No</th><th>Original Invoice</th><th>Buyer</th><th>PAN</th><th>Return Amount</th><th>VAT</th><th>Reason</th></tr>
        </thead>
        <tbody></tbody>
      </table>
    </div>
  );
}
