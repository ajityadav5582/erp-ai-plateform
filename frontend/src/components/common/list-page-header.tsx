"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * The heading every list screen in the product uses.
 *
 * This exists because the same header had grown three different shapes: inventory
 * and admin screens used a bold title with a count pill, while the procurement
 * screens used `PageHeader`, which has no count and puts its actions on a separate
 * row. A user moving between Procurement and Inventory saw the page title jump
 * size and the count appear and disappear. Both shapes are now this one.
 */
interface ListPageHeaderProps {
  title: string;
  description?: string;
  /**
   * Total rows across all pages, shown as a pill beside the title. Pass `undefined`
   * for screens whose total is not meaningful (a form, a picker).
   */
  count?: number;
  /** Noun rendered after the count, e.g. `"Orders"`. Defaults to `"Items"`. */
  countLabel?: string;
  /** Primary actions, rendered top-right on wide screens. */
  actions?: React.ReactNode;
  className?: string;
}

export function ListPageHeader({
  title,
  description,
  count,
  countLabel = "Items",
  actions,
  className,
}: ListPageHeaderProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between",
        className
      )}
    >
      <div>
        <div className="flex items-center gap-2">
          <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            {title}
          </h1>
          {count !== undefined && (
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {count} {countLabel}
            </span>
          )}
        </div>
        {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      </div>

      {actions && (
        <div className="flex items-center gap-2 self-start sm:self-auto">{actions}</div>
      )}
    </div>
  );
}
