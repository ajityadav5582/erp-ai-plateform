"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

/**
 * Page sizes offered on every paged list. Deliberately the same on every screen:
 * a user who learns "50 per page" on orders expects it to work on invoices too.
 */
const PAGE_SIZE_OPTIONS = [10, 20, 50];

/**
 * The footer every paged list shares.
 *
 * Four different footers existed before this: the inventory screens offered a
 * page-size selector and said "Showing N of M total items", while the procurement
 * screens hard-coded ten rows per page and only offered previous/next arrows. So
 * changing the page size was possible on an item list but not on an order list,
 * and the summary line was worded differently on each screen.
 *
 * One implementation now fixes both. Pages that only ever want a fixed window can
 * pass `hidePageSize`, but the default is the full control.
 */
interface ListPaginationProps {
  /** Zero-based index of the page on screen. */
  page: number;
  /** Rows requested per page. */
  pageSize: number;
  onPageChange: (page: number) => void;
  /**
   * Committed when the page size changes. The parent owns the value because it
   * feeds the query args, and it is also expected to reset to page 0 - a new page
   * size makes the current page index meaningless (page 7 of 10 does not exist
   * when each page now holds 50 rows).
   */
  onPageSizeChange?: (pageSize: number) => void;
  /** Total pages reported by the server, at least 1. */
  totalPages: number;
  /** Total rows across all pages. */
  totalElements: number;
  /** Plural noun for the rows, e.g. `"orders"`. */
  itemLabel: string;
  /** `true` while a page change is in flight; disables the arrows. */
  isFetching?: boolean;
  /** Set false to pin the page size and hide the selector. */
  showPageSize?: boolean;
  /** Extra context appended after the summary, e.g. a page subtotal. */
  trailingNote?: React.ReactNode;
  className?: string;
}

export function ListPagination({
  page,
  pageSize,
  onPageChange,
  onPageSizeChange,
  totalPages,
  totalElements,
  itemLabel,
  isFetching = false,
  showPageSize = true,
  trailingNote,
  className,
}: ListPaginationProps) {
  // Generated rather than hardcoded: two lists can share a page (the order screen's
  // scope switch renders one, but a future split-screen view would render two), and a
  // duplicated DOM id would break the label's association.
  const pageSizeId = React.useId();

  const safeTotalPages = Math.max(totalPages, 1);
  const isFirstPage = page <= 0;
  const isLastPage = page >= safeTotalPages - 1;

  // The first page is often only partly full, and the last page almost always is,
  // so "Page N of M" alone does not say how many rows are actually on screen.
  const firstRowOnPage = totalElements === 0 ? 0 : page * pageSize + 1;
  const lastRowOnPage = Math.min((page + 1) * pageSize, totalElements);

  // Nothing to page through: a single empty page should not render a pager that
  // offers arrows to nowhere.
  if (totalElements === 0) return null;

  return (
    <div
      className={cn(
        "flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between",
        className
      )}
    >
      <div className="text-muted-foreground">
        Showing{" "}
        <span className="font-medium text-foreground">
          {firstRowOnPage}-{lastRowOnPage}
        </span>{" "}
        of <span className="font-medium text-foreground">{totalElements}</span> {itemLabel}
        {trailingNote ? <span className="text-muted-foreground"> · {trailingNote}</span> : null}
      </div>

      <div className="flex items-center gap-4">
        {showPageSize && onPageSizeChange && (
          <div className="flex items-center gap-2">
            <label htmlFor={pageSizeId} className="text-xs text-muted-foreground">
              Per page:
            </label>
            <select
              id={pageSizeId}
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="h-8 rounded border border-input bg-background px-2 py-0.5 text-xs shadow-sm"
            >
              {PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="flex items-center gap-1">
          <span className="mr-2 text-xs text-muted-foreground">
            Page {page + 1} of {safeTotalPages}
          </span>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(page - 1)}
            disabled={isFirstPage || isFetching}
            aria-label="Previous page"
          >
            <ChevronLeft className="size-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onPageChange(page + 1)}
            disabled={isLastPage || isFetching}
            aria-label="Next page"
          >
            <ChevronRight className="size-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
