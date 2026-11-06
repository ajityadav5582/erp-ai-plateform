"use client";

import { useState } from "react";

export default function PosSessionPage() {
  const [openDialog, setOpenDialog] = useState(false);
  const [closeModal, setCloseModal] = useState(false);
  const [counts, setCounts] = useState({ n1000: 0, n500: 0, n100: 0, n50: 0, n20: 0, n10: 0, n5: 0, coins: 0 });
  const expected = 5000;
  const counted = counts.n1000 * 1000 + counts.n500 * 500 + counts.n100 * 100 + counts.n50 * 50 + counts.n20 * 20 + counts.n10 * 10 + counts.n5 * 5 + counts.coins;
  const variance = counted - expected;

  return (
    <div className="p-6 space-y-4">
      <h1 className="text-xl font-bold">POS Session</h1>
      <button className="border px-3 py-1" onClick={() => setOpenDialog(true)}>Open Shift</button>
      <button className="border px-3 py-1" onClick={() => setCloseModal(true)}>Close Shift</button>

      {openDialog && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center">
          <div className="bg-white p-6 rounded space-y-3 w-96">
            <h2 className="font-bold">Open Shift</h2>
            <input className="border px-2 py-1 w-full" placeholder="Register Name" />
            <input className="border px-2 py-1 w-full" placeholder="Opening Cash Float" />
            <input className="border px-2 py-1 w-full" placeholder="Cashier Name" />
            <button className="bg-blue-600 text-white px-3 py-1 rounded" onClick={() => setOpenDialog(false)}>Start Register Shift</button>
          </div>
        </div>
      )}

      {closeModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center">
          <div className="bg-white p-6 rounded space-y-3 w-96">
            <h2 className="font-bold">Close Shift</h2>
            {Object.entries(counts).map(([k, v]) => (
              <div key={k} className="flex gap-2">
                <label className="w-20">{k}</label>
                <input type="number" className="border px-2 py-1 w-24" value={v} onChange={(e) => setCounts({ ...counts, [k]: Number(e.target.value) })} />
              </div>
            ))}
            <div>Counted: {counted}</div>
            <div>Expected: {expected}</div>
            <div className={variance === 0 ? "text-green-600" : variance < 0 ? "text-red-600" : "text-yellow-600"}>
              {variance === 0 ? "Exact Match" : variance < 0 ? `Shortage: ${Math.abs(variance)}` : `Overage: ${variance}`}
            </div>
            <button className="bg-blue-600 text-white px-3 py-1 rounded" onClick={() => setCloseModal(false)}>Close Register & Print Z-Report</button>
          </div>
        </div>
      )}
    </div>
  );
}
