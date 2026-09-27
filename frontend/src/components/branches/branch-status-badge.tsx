"use client";

import { CheckCircle2, PauseCircle } from "lucide-react";
import type { BranchStatus } from "@/services/branch.service";
import { cn } from "@/lib/utils";

interface BranchStatusBadgeProps {
  status: BranchStatus;
  className?: string;
}

export function BranchStatusBadge({ status, className }: BranchStatusBadgeProps) {
  switch (status) {
    case "ACTIVE":
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

    case "INACTIVE":
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

    default:
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground",
            className
          )}
        >
          {status}
        </span>
      );
  }
}
