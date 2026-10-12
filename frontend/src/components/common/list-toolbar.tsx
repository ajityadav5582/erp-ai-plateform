"use client";

import * as React from "react";
import { RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { SearchBox } from "@/components/common/search-box";

/**
 * The filter bar that sits between a list heading and its table.
 *
 * Every list screen needs the same three things - a search field, room for extra
 * filters, and a refresh - and each one had been assembling them differently:
 * some used the debounced `SearchBox`, some used a raw `<Input>` with its own
 * icon, and the refresh button was sometimes in the page header and sometimes
 * here. Consolidating them means a search box typed into on one screen behaves
 * identically on the next, and the refresh control is always in the same place.
 *
 * Search is optional: a handful of screens filter through a dropdown only (a
 * payment history has no free-text index to search against), and passing no
 * `searchValue` renders the filter slot without the field.
 */
interface ListToolbarProps {
  /** Omit to render the toolbar without a search field. */
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  searchPlaceholder?: string;
  /** Extra filters rendered beside the search box - status selects, scope toggles. */
  children?: React.ReactNode;
  /** `true` while any of the toolbar's queries are in flight. */
  isFetching?: boolean;
  onRefresh?: () => void;
  className?: string;
}

export function ListToolbar({
  searchValue,
  onSearchChange,
  searchPlaceholder = "Search...",
  children,
  isFetching = false,
  onRefresh,
  className,
}: ListToolbarProps) {
  const hasSearch = searchValue !== undefined && onSearchChange !== undefined;

  // Nothing to offer: no search, no extra filters and no refresh would leave an
  // empty bordered strip, which reads as a broken control rather than a gap.
  if (!hasSearch && !children && !onRefresh) return null;

  return (
    <div
      className={cn(
        "flex flex-col gap-3 rounded-xl border border-border bg-card p-3 shadow-sm sm:flex-row sm:items-center",
        className
      )}
    >
      <div className="flex flex-1 flex-wrap items-center gap-3">
        {hasSearch && (
          <SearchBox
            value={searchValue}
            onChange={onSearchChange}
            placeholder={searchPlaceholder}
            className="w-full sm:w-64"
          />
        )}
        {children}
      </div>

      {onRefresh && (
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={isFetching}
          className="h-9 gap-1.5 self-end sm:self-auto"
        >
          <RefreshCw className={cn("size-3.5", isFetching && "animate-spin")} />
          Refresh
        </Button>
      )}
    </div>
  );
}
