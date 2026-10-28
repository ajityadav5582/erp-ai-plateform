"use client";

import * as React from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from "@/components/ui/sheet";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

export interface DetailField {
  label: string;
  value: React.ReactNode;
  /** Render full-width (spans across) for long values like notes, address, terms. */
  full?: boolean;
}

export interface DetailLine {
  key: string;
  cells: React.ReactNode[];
}

export interface SalesDetailsSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  subtitle?: string;
  badge?: React.ReactNode;
  fields: DetailField[];
  lines?: {
    headers: string[];
    rows: DetailLine[];
    footer?: React.ReactNode;
  };
  actions?: React.ReactNode;
}

/**
 * Mobile-responsive details sheet shared across sales modules.
 * Consistent with procurement module detail sheets, featuring responsive tabs,
 * cleanly formatted key-value summary rows, scrollable tables, and sticky headers/footers.
 */
export function SalesDetailsSheet({
  open,
  onOpenChange,
  title,
  subtitle,
  badge,
  fields,
  lines,
  actions,
}: SalesDetailsSheetProps) {
  const hasLines = Boolean(lines && lines.rows.length > 0);
  const regularFields = fields.filter((f) => !f.full);
  const fullFields = fields.filter((f) => f.full);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col overflow-y-auto sm:max-w-2xl lg:max-w-3xl">
        <SheetHeader className="sticky top-0 z-10 -mx-6 -mt-6 border-b border-border bg-background px-6 py-4">
          <div className="flex items-center justify-between gap-3 pr-6">
            <div className="min-w-0">
              <SheetTitle className="truncate text-base font-semibold sm:text-lg">
                {title}
              </SheetTitle>
              {subtitle ? (
                <SheetDescription className="mt-0.5 truncate text-xs sm:text-sm">
                  {subtitle}
                </SheetDescription>
              ) : null}
            </div>
            {badge ? <div className="shrink-0">{badge}</div> : null}
          </div>
        </SheetHeader>

        <div className="flex-1 py-4">
          {hasLines ? (
            <Tabs defaultValue="summary" className="w-full">
              <TabsList className="grid w-full grid-cols-2 mb-4">
                <TabsTrigger value="summary">Summary</TabsTrigger>
                <TabsTrigger value="lines">
                  Items ({lines?.rows.length ?? 0})
                </TabsTrigger>
              </TabsList>

              <TabsContent value="summary" className="space-y-4 pt-1">
                <div className="divide-y divide-border/60 rounded-lg border bg-card p-3 sm:p-4">
                  {regularFields.map((f, i) => (
                    <div
                      key={i}
                      className="flex items-start justify-between gap-4 py-2.5 text-xs sm:text-sm first:pt-1 last:pb-1"
                    >
                      <span className="font-medium text-muted-foreground">{f.label}</span>
                      <span className="text-right font-medium text-foreground break-words max-w-[65%]">
                        {f.value ?? "—"}
                      </span>
                    </div>
                  ))}
                </div>

                {fullFields.map((f, i) => (
                  <div key={i} className="space-y-1.5">
                    <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      {f.label}
                    </span>
                    <div className="rounded-lg border bg-muted/30 p-3 text-xs leading-relaxed text-foreground whitespace-pre-wrap">
                      {f.value ?? "—"}
                    </div>
                  </div>
                ))}
              </TabsContent>

              <TabsContent value="lines" className="space-y-3 pt-1">
                <div className="overflow-x-auto rounded-lg border border-border bg-card">
                  <Table>
                    <TableHeader className="bg-muted/50">
                      <TableRow>
                        {lines?.headers.map((h, i) => (
                          <TableHead
                            key={i}
                            className={cn(
                              "text-xs font-semibold uppercase tracking-wider text-muted-foreground py-2.5 px-3",
                              i > 1 && "text-right"
                            )}
                          >
                            {h}
                          </TableHead>
                        ))}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {lines?.rows.map((row) => (
                        <TableRow key={row.key} className="hover:bg-muted/30">
                          {row.cells.map((cell, i) => (
                            <TableCell
                              key={i}
                              className={cn(
                                "py-2.5 px-3 text-xs sm:text-sm",
                                i > 1 && "text-right tabular-nums",
                                i === 0 && "font-medium text-foreground"
                              )}
                            >
                              {cell}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>

                {lines?.footer ? (
                  <div className="rounded-lg border bg-muted/20 p-3 text-xs font-medium text-foreground sm:text-sm">
                    {lines.footer}
                  </div>
                ) : null}
              </TabsContent>
            </Tabs>
          ) : (
            <div className="space-y-4">
              <div className="divide-y divide-border/60 rounded-lg border bg-card p-3 sm:p-4">
                {regularFields.map((f, i) => (
                  <div
                    key={i}
                    className="flex items-start justify-between gap-4 py-2.5 text-xs sm:text-sm first:pt-1 last:pb-1"
                  >
                    <span className="font-medium text-muted-foreground">{f.label}</span>
                    <span className="text-right font-medium text-foreground break-words max-w-[65%]">
                      {f.value ?? "—"}
                    </span>
                  </div>
                ))}
              </div>

              {fullFields.map((f, i) => (
                <div key={i} className="space-y-1.5">
                  <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {f.label}
                  </span>
                  <div className="rounded-lg border bg-muted/30 p-3 text-xs leading-relaxed text-foreground whitespace-pre-wrap">
                    {f.value ?? "—"}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {actions ? (
          <SheetFooter className="sticky bottom-0 z-10 -mx-6 -mb-6 mt-auto border-t border-border bg-background px-6 py-3 flex-row flex-wrap gap-2">
            {actions}
          </SheetFooter>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}
