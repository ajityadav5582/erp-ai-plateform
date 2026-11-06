"use client";

export default function IrdMonitorPage() {
  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">IRD CBMS Sync Monitor</h1>
      <div className="grid grid-cols-4 gap-4">
        <div className="border p-4">Government Cloud Connection</div>
        <div className="border p-4">Synced Today</div>
        <div className="border p-4">Pending Offline Queue</div>
        <div className="border p-4">Critical / Dead Letter</div>
      </div>
      <div className="flex gap-2">
        <button className="border px-3 py-1">Force Sync All</button>
      </div>
      <table className="w-full border">
        <thead>
          <tr><th>Invoice No</th><th>Type</th><th>Date</th><th>Total</th><th>Code</th><th>Error</th><th>Actions</th></tr>
        </thead>
        <tbody></tbody>
      </table>
    </div>
  );
}
