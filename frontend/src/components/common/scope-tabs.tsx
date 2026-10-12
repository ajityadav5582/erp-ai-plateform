"use client";

import * as React from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * A tab strip that switches which slice of a list is shown.
 *
 * Purchase Invoices used shadcn's `Tabs` while Purchase Requisitions used a
 * hand-rolled strip, so the same control looked different one click apart within
 * Procurement. This is the requisitions strip, which matches the rest of the app's
 * pill-shaped navigation, promoted to a shared component.
 *
 * These are filters, not routes, so they are plain buttons with `role="tab"`
 * rather than links - switching scope must not push a history entry.
 */
export interface ScopeTab {
  value: string;
  label: string;
  /** Optional leading icon. */
  icon?: LucideIcon;
  /** Tooltip explaining what the scope covers. */
  hint?: string;
  /** Badge shown after the label, e.g. a pending-work count. */
  badge?: number;
}

interface ScopeTabsProps {
  tabs: ScopeTab[];
  value: string;
  onValueChange: (value: string) => void;
  /** Accessible name for the tab list. */
  ariaLabel: string;
  className?: string;
}

export function ScopeTabs({
  tabs,
  value,
  onValueChange,
  ariaLabel,
  className,
}: ScopeTabsProps) {
  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className={cn(
        "flex flex-wrap gap-1 rounded-xl border border-border bg-card p-1 shadow-sm",
        className
      )}
    >
      {tabs.map((tab) => {
        const selected = tab.value === value;
        const Icon = tab.icon;

        return (
          <button
            key={tab.value}
            role="tab"
            type="button"
            aria-selected={selected}
            title={tab.hint}
            onClick={() => onValueChange(tab.value)}
            className={cn(
              "inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
              selected
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
          >
            {Icon ? <Icon className="size-4" /> : null}
            {tab.label}
            {tab.badge !== undefined && tab.badge > 0 && (
              <span
                className={cn(
                  "rounded-full px-1.5 py-0.5 text-xs font-semibold",
                  selected
                    ? "bg-primary-foreground/20 text-primary-foreground"
                    : "bg-primary/10 text-primary"
                )}
              >
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
