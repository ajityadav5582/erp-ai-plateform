"use client";

import * as React from "react";
import type { LucideIcon } from "lucide-react";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { TableCell, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";

/**
 * Placeholder rows rendered inside a table while the first page is loading.
 *
 * The inventory screens drew skeletons *inside* the table so the header and
 * column widths stayed put, while the procurement screens swapped the whole table
 * for a stack of grey bars - so a page visibly jumped when data arrived. These
 * rows keep the table's shape stable on both.
 */
export function TableSkeletonRows({
  columns,
  rows = 5,
}: {
  columns: number;
  rows?: number;
}) {
  return (
    <>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <TableRow key={rowIndex}>
          {Array.from({ length: columns }).map((__, colIndex) => (
            <TableCell key={colIndex}>
              <Skeleton className="h-4 w-full max-w-28" />
            </TableCell>
          ))}
        </TableRow>
      ))}
    </>
  );
}

/**
 * The full-width row shown when a list has nothing to display.
 *
 * `reason` distinguishes "nothing exists yet" from "nothing matched what you
 * searched for" - the same distinction the inventory screens draw by swapping the
 * description text, but handled in one place so every empty table offers the way
 * out (clearing the search) rather than just stating the problem.
 */
export function TableEmptyRow({
  icon: Icon,
  title,
  description,
  columns,
  action,
}: {
  icon?: LucideIcon;
  title: string;
  description?: React.ReactNode;
  columns: number;
  action?: React.ReactNode;
}) {
  const FallbackIcon = Icon ?? AlertTriangle;

  return (
    <TableRow>
      <TableCell colSpan={columns} className="h-64 text-center">
        <div className="flex flex-col items-center justify-center gap-2">
          <div className="flex size-12 items-center justify-center rounded-full bg-muted">
            <FallbackIcon className="size-6 text-muted-foreground" />
          </div>
          <p className="text-base font-semibold text-foreground">{title}</p>
          {description && (
            <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
          )}
          {action}
        </div>
      </TableCell>
    </TableRow>
  );
}

/**
 * The row shown when the request itself failed.
 *
 * Distinct from the empty row because nothing is known about the data - not how
 * much of it there is, not whether it exists - so the only useful thing to offer
 * is a retry.
 */
export function TableErrorRow({
  columns,
  title = "Could not load this list",
  description = "The request did not complete. Check your connection and try again.",
  onRetry,
}: {
  columns: number;
  title?: string;
  description?: string;
  onRetry: () => void;
}) {
  return (
    <TableEmptyRow
      icon={AlertTriangle}
      title={title}
      description={description}
      columns={columns}
      action={
        <Button variant="outline" size="sm" onClick={onRetry} className="mt-1 gap-1.5">
          <RefreshCw className="size-3.5" />
          Try again
        </Button>
      }
    />
  );
}
