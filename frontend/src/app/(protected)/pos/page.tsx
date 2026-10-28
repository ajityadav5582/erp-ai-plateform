"use client";

import * as React from "react";
import Link from "next/link";
import {
  ArrowRight,
  Clock,
  LayoutGrid,
  Monitor,
  ReceiptText,
  Store,
  UploadCloud,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useGetMyOpenPosSessionsQuery } from "@/services/pos-session.service";
import { useGetPosTransactionsQuery } from "@/services/pos-transaction.service";
import { useGetCbmsStatusQuery } from "@/services/pos-cbms.service";
import { formatPosCurrency, formatPosDateTime } from "@/lib/pos-format";

const quickLinks = [
  { href: "/pos/terminal", label: "Terminal", description: "Ring up a sale", Icon: Monitor },
  { href: "/pos/sessions", label: "Sessions", description: "Shifts & cash", Icon: Clock },
  { href: "/pos/transactions", label: "Transactions", description: "Sales & receipts", Icon: ReceiptText },
  { href: "/pos/registers", label: "Registers", description: "Terminals & warehouses", Icon: Store },
  { href: "/pos/quick-items", label: "Quick Items", description: "Terminal grid tiles", Icon: LayoutGrid },
];

/**
 * POS landing dashboard.
 *
 * A supervisor's at-a-glance view: resume selling, the cashier's open shifts,
 * IRD/CBMS outbox health, and the latest sales. Everything here is read from
 * endpoints the terminal already uses, so it adds no new backend surface.
 */
export default function PosDashboardPage() {
  const { data: openSessions, isLoading: sessionsLoading } = useGetMyOpenPosSessionsQuery();
  const { data: cbms, isLoading: cbmsLoading } = useGetCbmsStatusQuery();
  const { data: recent, isLoading: recentLoading } = useGetPosTransactionsQuery({
    size: 5,
    status: "COMPLETED",
  });

  const sessions = openSessions ?? [];
  const sales = recent?.content ?? [];

  return (
    <div className="space-y-6 p-4">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Point of Sale</h1>
          <p className="text-sm text-muted-foreground">
            Sell, settle the drawer, and keep the IRD/CBMS feed healthy.
          </p>
        </div>
        <Button asChild>
          <Link href="/pos/terminal">
            Open terminal
            <ArrowRight className="ml-1 h-4 w-4" />
          </Link>
        </Button>
      </header>

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground">
            <Clock className="h-4 w-4" />
            <span className="text-xs font-medium">Open sessions</span>
          </div>
          <p className="mt-2 text-2xl font-bold">{sessionsLoading ? "—" : sessions.length}</p>
          <p className="text-xs text-muted-foreground">yours, currently open</p>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground">
            <UploadCloud className="h-4 w-4" />
            <span className="text-xs font-medium">CBMS pending</span>
          </div>
          <p className="mt-2 text-2xl font-bold">
            {cbmsLoading ? "—" : (cbms?.pendingTransactions ?? 0)}
          </p>
          <p className="text-xs text-muted-foreground">
            {cbms?.enabled ? "sync enabled" : "sync disabled"}
          </p>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Open sessions */}
        <section className="rounded-lg border bg-card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Your open sessions</h2>
            <Link href="/pos/sessions" className="text-xs text-muted-foreground hover:underline">
              View all
            </Link>
          </div>
          {sessionsLoading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : sessions.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No open session. Open one from the terminal to start selling.
            </p>
          ) : (
            <ul className="space-y-2">
              {sessions.map((session) => (
                <li
                  key={session.id}
                  className="flex items-center justify-between gap-2 rounded-md border p-3"
                >
                  <div>
                    <p className="text-sm font-medium">{session.registerName}</p>
                    <p className="text-xs text-muted-foreground">
                      Session {session.sessionNumber} · opened {formatPosDateTime(session.openedAt)}
                    </p>
                  </div>
                  <p className="text-sm font-semibold">{formatPosCurrency(session.grossSales)}</p>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Recent sales */}
        <section className="rounded-lg border bg-card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Recent sales</h2>
            <Link href="/pos/transactions" className="text-xs text-muted-foreground hover:underline">
              View all
            </Link>
          </div>
          {recentLoading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : sales.length === 0 ? (
            <p className="text-sm text-muted-foreground">No completed sales yet.</p>
          ) : (
            <ul className="space-y-2">
              {sales.map((transaction) => (
                <li
                  key={transaction.id}
                  className="flex items-center justify-between gap-2 rounded-md border p-3"
                >
                  <div className="min-w-0">
                    <p className="truncate font-mono text-sm font-medium">
                      {transaction.transactionNumber}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {transaction.transactionDateNp} · {transaction.cashierName}
                    </p>
                  </div>
                  <p className="shrink-0 text-sm font-semibold">
                    {formatPosCurrency(transaction.totalAmount)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Quick links */}
      <section>
        <h2 className="mb-3 text-sm font-semibold">Go to</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {quickLinks.map(({ href, label, description, Icon }) => (
            <Link
              key={href}
              href={href}
              className="rounded-lg border bg-card p-4 transition-colors hover:bg-accent"
            >
              <Icon className="h-5 w-5 text-muted-foreground" />
              <p className="mt-2 text-sm font-medium">{label}</p>
              <p className="text-xs text-muted-foreground">{description}</p>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
