"use client";

import * as React from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { cn } from "@/lib/utils";

export interface DetailField {
  label: string;
  value: React.ReactNode;
  /** Render full-width (spans both columns) for long values like notes. */
  full?: boolean;
}

export interface DetailLine {
  key: string;
  cells: React.ReactNode[];
}

interface SalesDetailsSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  subtitle?: string;
  fields: DetailField[];
  lines?: {
    headers: string[];
    rows: DetailLine[];
    footer?: React.ReactNode;
  };
  actions?: React.ReactNode;
}

/**
 * A generic read-only details sheet shared by every sales document.
 *
 * The seven documents differ in their fields and lines but not in how they are
 * read, so one sheet driven by data keeps them consistent and avoids seven
 * near-identical copies drifting apart.
 */
export function SalesDetailsSheet({
  open,
  onOpenChange,
  title,
  subtitle,
  fields,
  lines,
  actions,
}: SalesDetailsSheetProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{title}</SheetTitle>
          {subtitle ? <SheetDescription>{subtitle}</SheetDescription> : null}
        </SheetHeader>

        {actions ? <div className="flex flex-wrap gap-2 px-1">{actions}</div> : null}

        <div className="h-[calc(100vh-220px)] overflow-y-auto pr-4">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-3 py-4 text-sm">
            {fields.map((f, i) => (
              <div key={i} className={cn(f.full && "col-span-2")}>
                <dt className="text-muted-foreground">{f.label}</dt>
                <dd className="font-medium">{f.value ?? "—"}</dd>
              </div>
            ))}
          </dl>

          {lines && lines.rows.length > 0 ? (
            <div className="mt-2">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-muted-foreground">
                    {lines.headers.map((h, i) => (
                      <th key={i} className="py-1 pr-2 font-medium">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {lines.rows.map((row) => (
                    <tr key={row.key} className="border-t">
                      {row.cells.map((c, i) => (
                        <td key={i} className="py-1 pr-2">
                          {c}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
              {lines.footer ? <div className="mt-2 text-sm font-medium">{lines.footer}</div> : null}
            </div>
          ) : null}
        </div>
      </SheetContent>
    </Sheet>
  );
}
