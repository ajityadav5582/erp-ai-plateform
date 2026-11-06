"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatPosCurrency } from "@/lib/pos-format";

const DENOMS = [1000, 500, 100, 50, 20, 10, 5];

interface ZReportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  expectedCash: number;
  onSubmit: (countedCash: number, denominations: Record<number, number>, remarks: string) => void;
  isSubmitting: boolean;
}

export function ZReportDialog({ open, onOpenChange, expectedCash, onSubmit, isSubmitting }: ZReportDialogProps) {
  const [counts, setCounts] = React.useState<Record<number, number>>({});
  const [remarks, setRemarks] = React.useState("");

  const countedCash = DENOMS.reduce((sum, d) => sum + d * (counts[d] || 0), 0);
  const variance = countedCash - expectedCash;

  const submit = () => {
    onSubmit(countedCash, counts, remarks);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Close Session (Z-Report)</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="text-sm">
            <p>Expected cash: {formatPosCurrency(expectedCash)}</p>
            <p className={variance < 0 ? "text-destructive" : variance > 0 ? "text-emerald-600" : ""}>
              Counted: {formatPosCurrency(countedCash)} · Variance: {formatPosCurrency(variance)}
            </p>
          </div>
          <div className="space-y-2">
            {DENOMS.map((d) => (
              <div key={d} className="flex items-center gap-2">
                <Label className="w-16">Rs {d}</Label>
                <Input
                  type="number"
                  min={0}
                  value={counts[d] || ""}
                  onChange={(e) => setCounts((prev) => ({ ...prev, [d]: Number(e.target.value) || 0 }))}
                />
                <span className="w-20 text-right text-sm">{formatPosCurrency(d * (counts[d] || 0))}</span>
              </div>
            ))}
          </div>
          <div className="space-y-1">
            <Label htmlFor="remarks">Remarks</Label>
            <Input id="remarks" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </div>
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={isSubmitting}>
            {isSubmitting ? "Closing…" : "Close session"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
