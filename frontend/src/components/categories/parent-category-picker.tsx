"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronDown, Loader2, Search, X } from "lucide-react";

import { useGetCategoriesQuery, type CategoryListResponse } from "@/services/category.service";
import { cn } from "@/lib/utils";

/**
 * A category option offered by the picker.
 *
 * `depth` is the number of ancestors, used purely for indentation so a deep tree
 * is still readable. It is derived from the `parentId` chain rather than from the
 * server, because the list endpoint does not return a hierarchy.
 */
export interface CategoryOption {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  depth: number;
}

/**
 * Searchable "select a parent category" control.
 *
 * Replaces the raw numeric `Parent Category ID` text input that was previously in
 * both the create and edit dialogs. That input asked the user to know an internal
 * database id by heart, and the id was the only thing shown, so picking the wrong
 * parent was easy and the resulting hierarchy error was impossible to interpret.
 *
 * Options are rendered as `name` with the `id` available as secondary text, so
 * the key that will actually be submitted stays visible without being the primary
 * affordance.
 *
 * Rendered inline rather than in a popover on purpose: the parent dialog is a
 * Radix `Dialog` with a focus trap, and a portalled popover inside it fights the
 * trap for focus and for outside-click handling.
 *
 * The category currently being edited, and all of its descendants, are excluded:
 * those are the values the backend rejects as a circular reference, so offering
 * them would only produce a confusing error after the fact.
 */
export function ParentCategoryPicker({
  value,
  onChange,
  disabled,
  /** Id of the category being edited; excluded from the options along with its descendants. */
  excludeCategoryId,
  placeholder = "No parent (root category)",
  id,
}: {
  value: number | null | undefined;
  onChange: (id: number | null) => void;
  disabled?: boolean;
  excludeCategoryId?: number;
  placeholder?: string;
  id?: string;
}) {
  const { data, isFetching } = useGetCategoriesQuery({
    page: 0,
    size: 200,
    sort: "name,asc",
  });

  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [highlighted, setHighlighted] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const all = useMemo(() => data?.content ?? [], [data]);

  /**
   * Walks up the `parentId` chain to compute each category's depth.
   *
   * Tolerates cycles and dangling parents: a category whose ancestor chain loops
   * or references a missing row simply stops counting rather than hanging the UI.
   */
  const options = useMemo<CategoryOption[]>(() => {
    const byId = new Map<number, CategoryListResponse>(all.map((c) => [c.id, c]));

    const depthOf = (category: CategoryListResponse): number => {
      let depth = 0;
      let cursor = category.parentId;
      const visited = new Set<number>([category.id]);
      while (cursor != null && byId.has(cursor) && !visited.has(cursor) && depth < 20) {
        visited.add(cursor);
        depth += 1;
        cursor = byId.get(cursor)!.parentId;
      }
      return depth;
    };

    return all
      .map((c) => ({
        id: c.id,
        name: c.name,
        slug: c.slug,
        parentId: c.parentId,
        depth: depthOf(c),
      }))
      .sort((a, b) => a.depth - b.depth || a.name.localeCompare(b.name));
  }, [all]);

  /**
   * The set of ids that must not be offered as a parent: the category itself plus
   * everything beneath it, since attaching any of those would create a cycle.
   */
  const excluded = useMemo(() => {
    if (excludeCategoryId == null) return new Set<number>();

    const childIdsByParent = new Map<number, number[]>();
    for (const option of options) {
      if (option.parentId == null) continue;
      const siblings = childIdsByParent.get(option.parentId) ?? [];
      siblings.push(option.id);
      childIdsByParent.set(option.parentId, siblings);
    }

    const blocked = new Set<number>();
    const stack = [excludeCategoryId];
    while (stack.length > 0) {
      const current = stack.pop()!;
      if (blocked.has(current)) continue;
      blocked.add(current);
      stack.push(...(childIdsByParent.get(current) ?? []));
    }
    return blocked;
  }, [options, excludeCategoryId]);

  const selectable = useMemo(
    () => options.filter((o) => !excluded.has(o.id)),
    [options, excluded],
  );

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    const base = term
      ? selectable.filter(
          (o) =>
            o.name.toLowerCase().includes(term) ||
            o.slug.toLowerCase().includes(term) ||
            String(o.id) === term,
        )
      : selectable;
    return base.sort((a, b) => a.name.localeCompare(b.name));
  }, [selectable, search]);

  const selected = selectable.find((o) => o.id === value) ?? null;

  // Close when the dialog closes or focus leaves the control entirely.
  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onPointerDown);
    return () => document.removeEventListener("mousedown", onPointerDown);
  }, [open]);

  // Focus the search box and reset the transient state each time the list opens.
  useEffect(() => {
    if (open) {
      setSearch("");
      setHighlighted(0);
      // Defer so the input exists before it is focused.
      const timer = window.setTimeout(() => searchInputRef.current?.focus(), 0);
      return () => window.clearTimeout(timer);
    }
  }, [open]);

  // Keep the highlight inside the list when filtering shrinks it.
  useEffect(() => {
    setHighlighted((current) => Math.min(current, Math.max(filtered.length - 1, 0)));
  }, [filtered.length]);

  const commit = (nextId: number | null) => {
    onChange(nextId);
    setOpen(false);
  };

  const onKeyDown = (event: React.KeyboardEvent) => {
    if (!open && (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      setOpen(true);
      return;
    }
    if (!open) return;

    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        setHighlighted((i) => Math.min(i + 1, filtered.length - 1));
        break;
      case "ArrowUp":
        event.preventDefault();
        setHighlighted((i) => Math.max(i - 1, 0));
        break;
      case "Home":
        event.preventDefault();
        setHighlighted(0);
        break;
      case "End":
        event.preventDefault();
        setHighlighted(Math.max(filtered.length - 1, 0));
        break;
      case "Enter":
        event.preventDefault();
        if (filtered[highlighted]) commit(filtered[highlighted].id);
        break;
      case "Escape":
        event.preventDefault();
        setOpen(false);
        break;
    }
  };

  return (
    <div ref={containerRef} className="relative" onKeyDown={onKeyDown}>
      <button
        type="button"
        id={id}
        disabled={disabled}
        role="combobox"
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={open ? `${id ?? "parent-category"}-listbox` : undefined}
        onClick={() => setOpen((o) => !o)}
        className={cn(
          "flex w-full items-center justify-between gap-2 rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none transition-[color,box-shadow]",
          "focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50",
          "disabled:cursor-not-allowed disabled:opacity-50",
          !selected && "text-muted-foreground",
        )}
      >
        <span className="flex min-w-0 items-center gap-2">
          {selected ? (
            <>
              <span
                className="truncate text-foreground"
                style={{ paddingInlineStart: `${selected.depth * 12}px` }}
              >
                {selected.name}
              </span>
              <span className="shrink-0 font-mono text-xs text-muted-foreground">
                #{selected.id}
              </span>
            </>
          ) : (
            <span>{placeholder}</span>
          )}
        </span>
        <span className="flex shrink-0 items-center gap-1">
          {isFetching && <Loader2 className="size-3.5 animate-spin text-muted-foreground" />}
          {selected ? (
            <span
              role="button"
              tabIndex={-1}
              aria-label="Clear parent category"
              onClick={(e) => {
                e.stopPropagation();
                commit(null);
              }}
              className="rounded p-0.5 text-muted-foreground hover:bg-accent hover:text-foreground"
            >
              <X className="size-3.5" />
            </span>
          ) : (
            <ChevronDown className="size-4 opacity-50" />
          )}
        </span>
      </button>

      {open && (
        <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover p-1 text-popover-foreground shadow-lg">
          <div className="relative mb-1">
            <Search className="pointer-events-none absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <input
              ref={searchInputRef}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setHighlighted(0);
              }}
              placeholder="Search by name, slug or id..."
              aria-label="Search categories"
              className="h-8 w-full rounded-sm border border-input bg-transparent pl-7 pr-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
            />
          </div>

          <ul
            id={`${id ?? "parent-category"}-listbox`}
            role="listbox"
            className="max-h-56 overflow-y-auto"
          >
            <li
              role="option"
              aria-selected={value == null}
              onMouseDown={(e) => {
                e.preventDefault();
                commit(null);
              }}
              onMouseEnter={() => setHighlighted(-1)}
              className={cn(
                "flex cursor-pointer items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-sm",
                highlighted === -1 && "bg-accent text-accent-foreground",
              )}
            >
              <span className="flex items-center gap-2">
                <Check className={cn("size-3.5", value == null ? "opacity-100" : "opacity-0")} />
                {placeholder}
              </span>
            </li>

            {filtered.map((option, index) => (
              <li
                key={option.id}
                role="option"
                aria-selected={option.id === value}
                onMouseDown={(e) => {
                  e.preventDefault();
                  commit(option.id);
                }}
                onMouseEnter={() => setHighlighted(index)}
                className={cn(
                  "flex cursor-pointer items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-sm",
                  highlighted === index && "bg-accent text-accent-foreground",
                )}
              >
                <span
                  className="flex min-w-0 items-center gap-2"
                  style={{ paddingInlineStart: `${option.depth * 12}px` }}
                >
                  <Check
                    className={cn("size-3.5 shrink-0", option.id === value ? "opacity-100" : "opacity-0")}
                  />
                  <span className="truncate">{option.name}</span>
                </span>
                <span className="shrink-0 font-mono text-xs text-muted-foreground">
                  #{option.id}
                </span>
              </li>
            ))}

            {filtered.length === 0 && (
              <li className="px-2 py-4 text-center text-sm text-muted-foreground">
                {selectable.length === 0
                  ? "No categories available yet"
                  : `No category matches "${search.trim()}"`}
              </li>
            )}
          </ul>
        </div>
      )}
    </div>
  );
}
