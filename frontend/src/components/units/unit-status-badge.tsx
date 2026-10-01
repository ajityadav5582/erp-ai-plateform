"use client";

import { CheckCircle2, PauseCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface UnitStatusBadgeProps {
  isActive: boolean;
  className?: string;
}

export function UnitStatusBadge({ isActive, className }: UnitStatusBadgeProps) {
  if (isActive) {
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400 border border-emerald-500/20",
          className
        )}
      >
        <CheckCircle2 className="size-3.5" />
        Active
      </span>
    );
  }

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600 dark:bg-amber-500/20 dark:text-amber-400 border border-amber-500/20",
        className
      )}
    >
      <PauseCircle className="size-3.5" />
      Inactive
    </span>
  );
}
