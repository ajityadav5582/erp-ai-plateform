"use client";

import { CheckCircle2, Clock, Lock, PauseCircle, Archive } from "lucide-react";
import type { UserStatus } from "@/services/user.service";
import { cn } from "@/lib/utils";

interface UserStatusBadgeProps {
  status: UserStatus;
  className?: string;
}

export function UserStatusBadge({ status, className }: UserStatusBadgeProps) {
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

    case "LOCKED":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full bg-rose-500/10 px-2.5 py-0.5 text-xs font-medium text-rose-600 dark:bg-rose-500/20 dark:text-rose-400 border border-rose-500/20",
            className
          )}
        >
          <Lock className="size-3.5" />
          Locked
        </span>
      );

    case "PENDING_ACTIVATION":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full bg-sky-500/10 px-2.5 py-0.5 text-xs font-medium text-sky-600 dark:bg-sky-500/20 dark:text-sky-400 border border-sky-500/20",
            className
          )}
        >
          <Clock className="size-3.5" />
          Pending
        </span>
      );

    case "ARCHIVED":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full bg-slate-500/10 px-2.5 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-500/20 dark:text-slate-400 border border-slate-500/20",
            className
          )}
        >
          <Archive className="size-3.5" />
          Archived
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
