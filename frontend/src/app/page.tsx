import Link from "next/link";
import { cn } from "@/lib/utils";
import { siteConfig } from "@/config/site";

const ctaClass = cn(
  "inline-flex h-12 items-center justify-center gap-2 rounded-md px-6 text-base font-medium transition-colors",
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
);

const modules = [
  {
    title: "Finance",
    description:
      "General ledger, accounts payable/receivable, budgeting, and Nepal-compliant tax reporting.",
    href: "/finance",
  },
  {
    title: "Human Resources",
    description:
      "Employee lifecycle, payroll, leave management, and organizational structure.",
    href: "/hr",
  },
  {
    title: "Inventory",
    description:
      "Stock levels, warehouses, SKU management, and real-time movement tracking.",
    href: "/inventory",
  },
  {
    title: "Manufacturing",
    description:
      "Bill of materials, production planning, and shop-floor execution.",
    href: "/manufacturing",
  },
  {
    title: "Procurement",
    description:
      "Purchase requisitions, supplier management, and purchase orders.",
    href: "/procurement",
  },
  {
    title: "Sales",
    description:
      "Quotations, orders, invoicing, and customer relationship management.",
    href: "/sales",
  },
] as const;

export default function HomePage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
      <section className="flex flex-col items-start gap-6">
        <span className="inline-flex items-center rounded-full border border-border bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">
          Enterprise · AI-Augmented
        </span>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
          {siteConfig.name}
        </h1>
        <p className="max-w-2xl text-lg text-muted-foreground">
          {siteConfig.description}
        </p>
        <div className="flex flex-wrap items-center gap-3">
          <Link href="/dashboard" className={cn(ctaClass, "bg-primary text-primary-foreground hover:bg-primary/90")}>
            Open Dashboard
          </Link>
          <Link
            href="/docs"
            className={cn(
              ctaClass,
              "border border-border bg-transparent text-foreground hover:bg-accent hover:text-accent-foreground",
            )}
          >
            View Documentation
          </Link>
        </div>
      </section>

      <section
        aria-labelledby="modules-heading"
        className="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
      >
        <h2 id="modules-heading" className="sr-only">
          Business modules
        </h2>
        {modules.map((module) => (
          <Link
            key={module.href}
            href={module.href}
            className="group rounded-lg border border-border bg-card p-6 transition-colors hover:border-primary hover:bg-accent"
          >
            <h3 className="text-lg font-semibold text-card-foreground">
              {module.title}
            </h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {module.description}
            </p>
          </Link>
        ))}
      </section>
    </div>
  );
}
