"use client";

import { useState } from "react";

interface Props {
  expectedCash: number;
  onSubmit: (counts: Record<string, number>) => void;
}

export function SessionCloseModal({ expectedCash, onSubmit }: Props) {
  const [counts, setCounts] = useState<Record<string, number>>({
    note1000: 0, note500: 0, note100: 0, note50: 0,
    note20: 0, note10: 0, note5: 0, coins: 0,
  });

  const total =
    (counts.note1000 ?? 0) * 1000 + (counts.note500 ?? 0) * 500 + (counts.note100 ?? 0) * 100 +
    (counts.note50 ?? 0) * 50 + (counts.note20 ?? 0) * 20 + (counts.note10 ?? 0) * 10 +
    (counts.note5 ?? 0) * 5 + (counts.coins ?? 0);

  const variance = total - expectedCash;

  return (
    <div className="p-4 space-y-3">
      {Object.keys(counts).map((k) => (
        <div key={k} className="flex items-center gap-2">
          <label className="w-24">{k}</label>
          <input
            type="number"
            className="border px-2 py-1 w-24"
            value={counts[k]}
            onChange={(e) => setCounts({ ...counts, [k]: Number(e.target.value) })}
          />
        </div>
      ))}
      <div className="font-bold">Drawer Total: {total}</div>
      <div className={variance < 0 ? "text-red-600" : "text-green-600"}>
        {variance < 0 ? "Shortage" : "Overage"}: {Math.abs(variance)}
      </div>
      <button
        className="bg-blue-600 text-white px-4 py-2 rounded"
        onClick={() => onSubmit(counts)}
      >
        Confirm & Print Z-Report
      </button>
    </div>
  );
}
