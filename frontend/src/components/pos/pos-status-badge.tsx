import { Badge } from "@/components/ui/badge";
import {
  POS_TRANSACTION_STATUS_LABELS,
  type PosTransactionStatus,
} from "@/services/pos-transaction.service";
import {
  POS_SESSION_STATUS_LABELS,
  type PosSessionStatus,
} from "@/services/pos-session.service";
import {
  POS_REGISTER_STATUS_LABELS,
  type PosRegisterStatus,
} from "@/services/pos-register.service";

const TRANSACTION_VARIANTS: Record<PosTransactionStatus, string> = {
  OPEN: "bg-amber-100 text-amber-800 hover:bg-amber-100 dark:bg-amber-900/30 dark:text-amber-300",
  COMPLETED: "bg-emerald-100 text-emerald-800 hover:bg-emerald-100 dark:bg-emerald-900/30 dark:text-emerald-300",
  VOIDED: "bg-rose-100 text-rose-800 hover:bg-rose-100 dark:bg-rose-900/30 dark:text-rose-300",
};

const SESSION_VARIANTS: Record<PosSessionStatus, string> = {
  OPEN: "bg-emerald-100 text-emerald-800 hover:bg-emerald-100 dark:bg-emerald-900/30 dark:text-emerald-300",
  CLOSED: "bg-slate-100 text-slate-700 hover:bg-slate-100 dark:bg-slate-800 dark:text-slate-300",
};

const REGISTER_VARIANTS: Record<PosRegisterStatus, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 hover:bg-emerald-100 dark:bg-emerald-900/30 dark:text-emerald-300",
  INACTIVE: "bg-slate-100 text-slate-700 hover:bg-slate-100 dark:bg-slate-800 dark:text-slate-300",
};

/** Status pill for a POS transaction (parked / completed / voided). */
export function PosTransactionStatusBadge({ status }: { status: PosTransactionStatus }) {
  return (
    <Badge variant="secondary" className={TRANSACTION_VARIANTS[status]}>
      {POS_TRANSACTION_STATUS_LABELS[status]}
    </Badge>
  );
}

/** Status pill for a cashier session (open / closed). */
export function PosSessionStatusBadge({ status }: { status: PosSessionStatus }) {
  return (
    <Badge variant="secondary" className={SESSION_VARIANTS[status]}>
      {POS_SESSION_STATUS_LABELS[status]}
    </Badge>
  );
}

/** Status pill for a register (active / inactive). */
export function PosRegisterStatusBadge({ status }: { status: PosRegisterStatus }) {
  return (
    <Badge variant="secondary" className={REGISTER_VARIANTS[status]}>
      {POS_REGISTER_STATUS_LABELS[status]}
    </Badge>
  );
}
