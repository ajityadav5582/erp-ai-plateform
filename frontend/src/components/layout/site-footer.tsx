import Link from "next/link";
import { siteConfig } from "@/config/site";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-[#29445f] bg-[#102a43] text-slate-200">
      <div className="mx-auto flex max-w-7xl flex-col items-start justify-between gap-4 px-4 py-6 sm:px-6 md:flex-row md:items-center md:py-7 lg:px-8">
        <p className="text-xs text-slate-300 sm:text-sm">
          &copy; {year} {siteConfig.name}. All rights reserved.
        </p>
        <nav aria-label="Footer" className="flex items-center gap-4 sm:gap-6">
          <Link
            href="/privacy"
            className="text-xs text-slate-300 transition-colors hover:text-white sm:text-sm"
          >
            Privacy
          </Link>
          <Link
            href="/terms"
            className="text-xs text-slate-300 transition-colors hover:text-white sm:text-sm"
          >
            Terms
          </Link>
          <Link
            href="/status"
            className="text-xs text-slate-300 transition-colors hover:text-white sm:text-sm"
          >
            Status
          </Link>
        </nav>
      </div>
    </footer>
  );
}
