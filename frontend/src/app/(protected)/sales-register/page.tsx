"use client";

import * as React from "react";
import { useGetSalesRegisterQuery } from "@/services/sales-register.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { formatMoney } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";

function today() {
  return new Date().toISOString().slice(0, 10);
}
function firstOfMonth() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
}

export default function SalesRegisterPage() {
  const [from, setFrom] = React.useState(firstOfMonth());
  const [to, setTo] = React.useState(today());
  const [query, setQuery] = React.useState({ from: firstOfMonth(), to: today() });

  const { data, isLoading, isFetching } = useGetSalesRegisterQuery(query, { skip: !query.from || !query.to });

  const rows: Array<[string, number]> = data
    ? [
        ["Taxable sales", data.taxableSales],
        ["Exempt sales", data.exemptSales],
        ["Zero-rated sales", data.zeroRatedSales],
        ["Output VAT (on sales)", data.outputVat],
        ["Credit notes — taxable", data.creditTaxable],
        ["Credit notes — VAT reversed", data.creditVat],
        ["Net taxable sales", data.netTaxableSales],
        ["Net output VAT (VAT-201)", data.netOutputVat],
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Sales Register"
        description="VAT-201 figures for a period, net of credit notes."
        count={data?.invoiceCount}
        countLabel="Invoices"
      />

      <div className="flex flex-wrap items-end gap-3">
        <div className="grid gap-1">
          <Label>From</Label>
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="grid gap-1">
          <Label>To</Label>
          <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <Button onClick={() => setQuery({ from, to })}>Run</Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Measure</TableHead>
              <TableHead className="text-right">Amount (NPR)</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableRow><TableCell colSpan={2} className="h-24 text-center text-muted-foreground">Loading…</TableCell></TableRow>
            ) : (
              rows.map(([label, value]) => (
                <TableRow key={label}>
                  <TableCell className="font-medium">{label}</TableCell>
                  <TableCell className="text-right">{formatMoney(value, "NPR")}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {data ? (
        <p className="text-sm text-muted-foreground">
          {data.invoiceCount} invoice(s) and {data.creditNoteCount} credit note(s) in the period.
        </p>
      ) : null}
    </div>
  );
}
