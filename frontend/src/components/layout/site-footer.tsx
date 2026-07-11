import Link from "next/link";
import { siteConfig } from "@/config/site";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 px-4 py-8 sm:px-6 md:flex-row lg:px-8">
        <p className="text-sm text-muted-foreground">
          &copy; {year} {siteConfig.name}. All rights reserved.
        </p>
        <nav aria-label="Footer" className="flex items-center gap-6">
          <Link
            href="/privacy"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            Privacy
          </Link>
          <Link
            href="/terms"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            Terms
          </Link>
          <Link
            href="/status"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            Status
          </Link>
        </nav>
      </div>
    </footer>
  );
}
