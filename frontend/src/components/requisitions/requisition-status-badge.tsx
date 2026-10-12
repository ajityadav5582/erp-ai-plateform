"use client";

import {
  Ban,
  CheckCircle2,
  CircleDashed,
  Clock,
  FileEdit,
  PackageCheck,
  PackageX,
  Truck,
  XCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  formatLineStatus,
  formatRequisitionPriority,
  formatRequisitionStatus,
  type RequisitionLineStatus,
  type RequisitionPriority,
  type RequisitionStatus,
} from "@/services/purchase-requisition.service";

/**
 * One place that maps a lifecycle state to a colour and an icon.
 *
 * The requisition has six states, and letting each call site pick its own colour
 * is how a document ends up amber for `SUBMITTED` in one table and grey in
 * another. `APPROVED` and `CONVERTED_TO_PO` are the two states worth scanning
 * for, so they are the only ones given an affirmative colour.
 */
type Tone = "neutral" | "info" | "progress" | "success" | "warning" | "danger";

const TONE_CLASSES: Record<Tone, string> = {
  neutral:
    "bg-muted text-muted-foreground border-border dark:bg-muted/40",
  info: "bg-sky-500/10 text-sky-600 dark:bg-sky-500/20 dark:text-sky-400 border-sky-500/20",
  progress:
    "bg-amber-500/10 text-amber-600 dark:bg-amber-500/20 dark:text-amber-400 border-amber-500/20",
  success:
    "bg-emerald-500/10 text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400 border-emerald-500/20",
  warning:
    "bg-orange-500/10 text-orange-600 dark:bg-orange-500/20 dark:text-orange-400 border-orange-500/20",
  danger:
    "bg-red-500/10 text-red-600 dark:bg-red-500/20 dark:text-red-400 border-red-500/20",
};

const STATUS_TONES: Record<RequisitionStatus, Tone> = {
  DRAFT: "neutral",
  SUBMITTED: "info",
  APPROVED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
  CONVERTED_TO_PO: "success",
};

const PRIORITY_TONES: Record<RequisitionPriority, Tone> = {
  LOW: "neutral",
  NORMAL: "info",
  HIGH: "warning",
  URGENT: "danger",
};

const LINE_TONES: Record<RequisitionLineStatus, Tone> = {
  REQUESTED: "neutral",
  APPROVED: "success",
  PARTIALLY_APPROVED: "progress",
  PARTIALLY_ORDERED: "progress",
  ORDERED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
};

const STATUS_ICONS: Record<RequisitionStatus, typeof Clock> = {
  DRAFT: FileEdit,
  SUBMITTED: Clock,
  APPROVED: CheckCircle2,
  REJECTED: XCircle,
  CANCELLED: Ban,
  CONVERTED_TO_PO: PackageX,
};

const PRIORITY_ICONS: Record<RequisitionPriority, typeof Clock> = {
  LOW: CircleDashed,
  NORMAL: Clock,
  HIGH: CircleDashed,
  URGENT: Clock,
};

const LINE_ICONS: Record<RequisitionLineStatus, typeof Clock> = {
  REQUESTED: CircleDashed,
  APPROVED: CheckCircle2,
  PARTIALLY_APPROVED: CircleDashed,
  PARTIALLY_ORDERED: Truck,
  ORDERED: PackageCheck,
  REJECTED: XCircle,
  CANCELLED: Ban,
};

const CHIP_BASE =
  "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium";

interface ChipProps {
  label: string;
  tone: Tone;
  icon: typeof Clock;
  className?: string;
}

function Chip({ label, tone, icon: Icon, className }: ChipProps) {
  return (
    <span className={cn(CHIP_BASE, TONE_CLASSES[tone], className)}>
      <Icon className="size-3.5" />
      {label}
    </span>
  );
}

interface RequisitionStatusBadgeProps {
  status: RequisitionStatus;
  className?: string;
}

export function RequisitionStatusBadge({
  status,
  className,
}: RequisitionStatusBadgeProps) {
  return (
    <Chip
      label={formatRequisitionStatus(status)}
      tone={STATUS_TONES[status]}
      icon={STATUS_ICONS[status]}
      className={className}
    />
  );
}

interface RequisitionPriorityBadgeProps {
  priority: RequisitionPriority;
  className?: string;
}

export function RequisitionPriorityBadge({
  priority,
  className,
}: RequisitionPriorityBadgeProps) {
  return (
    <Chip
      label={formatRequisitionPriority(priority)}
      tone={PRIORITY_TONES[priority]}
      icon={PRIORITY_ICONS[priority]}
      className={className}
    />
  );
}

interface RequisitionLineStatusBadgeProps {
  status: RequisitionLineStatus;
  className?: string;
}

export function RequisitionLineStatusBadge({
  status,
  className,
}: RequisitionLineStatusBadgeProps) {
  return (
    <Chip
      label={formatLineStatus(status)}
      tone={LINE_TONES[status]}
      icon={LINE_ICONS[status]}
      className={className}
    />
  );
}
