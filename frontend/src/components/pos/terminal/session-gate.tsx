"use client";

import * as React from "react";
import { DoorOpen, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useGetPosRegistersQuery } from "@/services/pos-register.service";
import { useOpenPosSessionMutation } from "@/services/pos-session.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";

/**
 * The terminal's gate: no open session, no sale.
 *
 * Shown when the cashier has no open session. Picks an active register, takes
 * the opening cash count and opens the shift — the same coordination the
 * backend performs in one transaction (session number allocated, register's
 * open-session pointer flipped).
 */
export function SessionGate() {
  const { data: registers, isLoading } = useGetPosRegistersQuery({ status: "ACTIVE", size: 100 });
  const [openSession, { isLoading: isOpening }] = useOpenPosSessionMutation();

  const [registerId, setRegisterId] = React.useState<string>("");
  const [openingCash, setOpeningCash] = React.useState("0");

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!registerId) {
      toast.error("Select a register first.");
      return;
    }
    try {
      await openSession({
        registerId: Number(registerId),
        openingCashAmount: Number(openingCash) || 0,
      }).unwrap();
      toast.success("Session opened. Ready to sell.");
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not open the session.");
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const registerList = registers?.content ?? [];

  return (
    <div className="mx-auto max-w-md rounded-lg border p-6">
      <div className="mb-4 flex items-center gap-2">
        <DoorOpen className="h-5 w-5 text-primary" />
        <h1 className="text-lg font-semibold">Open a session</h1>
      </div>
      <p className="mb-4 text-sm text-muted-foreground">
        Sales can only be rung on an open session. Pick a register and count the opening cash.
      </p>

      <form onSubmit={onSubmit} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="register">Register</Label>
          <Select value={registerId} onValueChange={setRegisterId}>
            <SelectTrigger id="register" className="h-12">
              <SelectValue placeholder="Select a register" />
            </SelectTrigger>
            <SelectContent>
              {registerList.map((register) => (
                <SelectItem key={register.id} value={String(register.id)}>
                  {register.registerName} ({register.registerCode})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {registerList.length === 0 && (
            <p className="text-xs text-muted-foreground">
              No active registers. Ask a supervisor to activate one.
            </p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="opening-cash">Opening cash</Label>
          <Input
            id="opening-cash"
            type="number"
            min={0}
            step="0.01"
            value={openingCash}
            onChange={(event) => setOpeningCash(event.target.value)}
            className="h-12 text-lg"
          />
        </div>

        <Button type="submit" className="h-12 w-full" disabled={isOpening || !registerId}>
          {isOpening && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Open session
        </Button>
      </form>
    </div>
  );
}
