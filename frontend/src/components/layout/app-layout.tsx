"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Building2, ChevronDown } from "lucide-react";
import { SiteFooter } from "@/components/layout/site-footer";
import { Breadcrumb } from "@/components/layout/breadcrumb";
import { NotificationArea } from "@/components/layout/notification-area";
import { UserMenu } from "@/components/layout/user-menu";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { ThemeToggle } from "@/components/theme-toggle";
import { siteConfig } from "@/config/site";
import { ACTIVE_FISCAL_SELECTION_STORAGE_KEY } from "@/config/company-context";
import { useGetCompaniesQuery, useGetCompanyFiscalYearsQuery } from "@/services/company.service";
import { cn } from "@/lib/utils";

interface NavSubItem {
  title: string;
  href: string;
  icon?: React.ComponentType<{ className?: string }>;
}

interface NavItem {
  title: string;
  href?: string;
  icon?: React.ComponentType<{ className?: string }>;
  isActive?: boolean;
  children?: NavSubItem[];
}

interface ActiveFiscalSelection {
  companyId: number;
  fiscalYearId: number;
  companyName: string;
  fiscalYearName: string;
}

const navigation: NavItem[] = [
  {
    title: "Dashboard",
    href: "/",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
      </svg>
    ),
  },
  {
    title: "Administrations",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    ),
    children: [
      {
        title: "Users",
        href: "/users",
        icon: ({ className }) => (
          <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        ),
      },
      {
        title: "Branches",
        href: "/branches",
        icon: ({ className }) => (
          <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M2 20a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8l-7 5V8l-7 5V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
          </svg>
        ),
      },
      {
        title: "Roles",
        href: "/roles",
        icon: ({ className }) => (
          <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
        ),
      },
      {
        title: "Departments",
        href: "/departments",
        icon: ({ className }) => (
          <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
          </svg>
        ),
      },
      {
        title: "Permissions",
        href: "/permissions",
        icon: ({ className }) => (
          <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        ),
      },
    ],
  },
  {
    title: "Finance",
    href: "/finance",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="12" y1="1" x2="12" y2="23" />
        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
      </svg>
    ),
  },
  {
    title: "Inventory",
    href: "/inventory",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
        <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
        <line x1="12" y1="22.08" x2="12" y2="12" />
      </svg>
    ),
  },
  {
    title: "HR",
    href: "/hr",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
      </svg>
    ),
  },
  {
    title: "Manufacturing",
    href: "/manufacturing",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 20a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8l-7 5V8l-7 5V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
      </svg>
    ),
  },
  {
    title: "Procurement",
    href: "/procurement",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="8" cy="21" r="1" />
        <circle cx="19" cy="21" r="1" />
        <path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12" />
      </svg>
    ),
  },
  {
    title: "Reports",
    href: "/reports",
    icon: ({ className }) => (
      <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
        <polyline points="14 2 14 8 20 8" />
        <line x1="16" y1="13" x2="8" y2="13" />
        <line x1="16" y1="17" x2="8" y2="17" />
        <line x1="10" y1="9" x2="8" y2="9" />
      </svg>
    ),
  },
];

function CollapsibleNavItem({
  item,
  pathname,
}: {
  item: NavItem;
  pathname: string;
}) {
  const isChildActive = React.useMemo(() => {
    return (
      item.children?.some(
        (child) => pathname === child.href || pathname.startsWith(child.href + "/")
      ) ?? false
    );
  }, [item.children, pathname]);

  const [isOpen, setIsOpen] = React.useState(isChildActive);

  React.useEffect(() => {
    if (isChildActive) {
      setIsOpen(true);
    }
  }, [isChildActive]);

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        onClick={() => setIsOpen((prev) => !prev)}
        isActive={isChildActive}
        tooltip={item.title}
      >
        {item.icon && <item.icon className="h-4 w-4" />}
        <span className="flex-1">{item.title}</span>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 transition-transform duration-200 group-data-[collapsible=icon]:hidden",
            isOpen && "rotate-180"
          )}
        />
      </SidebarMenuButton>

      {isOpen && item.children && (
        <SidebarMenuSub>
          {item.children.map((child) => {
            const isSubActive = pathname === child.href || pathname.startsWith(child.href + "/");
            return (
              <SidebarMenuSubItem key={child.href}>
                <SidebarMenuSubButton asChild isActive={isSubActive}>
                  <Link href={child.href}>
                    {child.icon && <child.icon className="h-3.5 w-3.5" />}
                    <span>{child.title}</span>
                  </Link>
                </SidebarMenuSubButton>
              </SidebarMenuSubItem>
            );
          })}
        </SidebarMenuSub>
      )}
    </SidebarMenuItem>
  );
}

function AppSidebar({ selection }: { selection: ActiveFiscalSelection }) {
  const pathname = usePathname();

  return (
    <Sidebar collapsible="icon" variant="inset">
      <SidebarHeader>
        <div className="flex items-center gap-2 px-2 py-1.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <span className="text-sm font-bold">E</span>
          </div>
          <span className="text-sm font-semibold group-data-[collapsible=icon]:hidden">
            {siteConfig.shortName}
          </span>
        </div>
        <Link
          href={`/companies/${selection.companyId}/fiscal-years`}
          className="group-data-[collapsible=icon]:hidden mx-2 mt-2 flex min-w-0 flex-col border-t border-sidebar-border px-1 pt-3 transition-colors hover:text-white"
          title={`${selection.companyName} · ${selection.fiscalYearName}`}
        >
          <span className="truncate text-sm font-semibold">{selection.companyName}</span>
          <span className="mt-0.5 truncate text-xs text-sidebar-foreground/70">{selection.fiscalYearName} · click to switch</span>
        </Link>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Platform</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navigation.map((item) => {
                if (item.children) {
                  return (
                    <CollapsibleNavItem
                      key={item.title}
                      item={item}
                      pathname={pathname}
                    />
                  );
                }
                const isActive = pathname === item.href;
                return (
                  <SidebarMenuItem key={item.href}>
                    <SidebarMenuButton asChild isActive={isActive} tooltip={item.title}>
                      <Link href={item.href!}>
                        {item.icon && <item.icon className="h-4 w-4" />}
                        <span>{item.title}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <div className="p-2 text-xs text-muted-foreground group-data-[collapsible=icon]:hidden">
          <p>&copy; {new Date().getFullYear()} {siteConfig.name}</p>
        </div>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  );
}

interface AppLayoutProps {
  children: React.ReactNode;
  breadcrumbs?: { label: string; href?: string }[];
  className?: string;
  /** When true, renders only the children without the app shell (sidebar, header, footer). */
  noShell?: boolean;
}

export function AppLayout({ children, breadcrumbs, className, noShell }: AppLayoutProps) {
  const pathname = usePathname();
  const router = useRouter();
  const isAuthPage = pathname === "/login" ||
    pathname.startsWith("/forgot-password") ||
    pathname.startsWith("/reset-password");
  const isCompanySetupPage = pathname === "/companies" || pathname.startsWith("/companies/");
  const [fiscalSelectionReady, setFiscalSelectionReady] = React.useState(false);
  const [activeFiscalSelection, setActiveFiscalSelection] = React.useState<ActiveFiscalSelection | null>(null);
  const shouldResolveFiscalSelection = fiscalSelectionReady && activeFiscalSelection !== null && !isAuthPage && !isCompanySetupPage;
  const { data: contextCompanies = [] } = useGetCompaniesQuery(undefined, { skip: !shouldResolveFiscalSelection });
  const { data: contextFiscalYears = [] } = useGetCompanyFiscalYearsQuery(
    activeFiscalSelection?.companyId ?? 0,
    { skip: !shouldResolveFiscalSelection }
  );

  React.useEffect(() => {
    if (isAuthPage) return;
    if (pathname === "/companies") {
      window.localStorage.removeItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY);
      setActiveFiscalSelection(null);
      setFiscalSelectionReady(true);
      return;
    }

    let selection: ActiveFiscalSelection | null = null;
    try {
      const stored = window.localStorage.getItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY);
      if (stored) {
        const candidate = JSON.parse(stored) as Partial<ActiveFiscalSelection>;
        if (candidate.companyId && candidate.fiscalYearId) {
          selection = {
            companyId: candidate.companyId,
            fiscalYearId: candidate.fiscalYearId,
            companyName: candidate.companyName || "Selected company",
            fiscalYearName: candidate.fiscalYearName || "Active fiscal year",
          };
        }
      }
    } catch {
      window.localStorage.removeItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY);
    }
    setActiveFiscalSelection(selection);
    setFiscalSelectionReady(true);
    if (!selection && !isCompanySetupPage) {
      router.replace("/companies");
    }
  }, [isAuthPage, isCompanySetupPage, pathname, router]);

  React.useEffect(() => {
    if (!activeFiscalSelection) return;
    const company = contextCompanies.find((item) => item.id === activeFiscalSelection.companyId);
    const fiscalYear = contextFiscalYears.find((item) => item.id === activeFiscalSelection.fiscalYearId);
    if (!company || !fiscalYear) return;
    if (company.companyName === activeFiscalSelection.companyName && fiscalYear.name === activeFiscalSelection.fiscalYearName) return;

    const resolvedSelection = {
      ...activeFiscalSelection,
      companyName: company.companyName,
      fiscalYearName: fiscalYear.name,
    };
    setActiveFiscalSelection(resolvedSelection);
    window.localStorage.setItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY, JSON.stringify(resolvedSelection));
  }, [activeFiscalSelection, contextCompanies, contextFiscalYears]);

  if (noShell || isAuthPage) {
    return <>{children}</>;
  }

  if (isCompanySetupPage) {
    return (
      <div className="min-h-screen bg-background">
        <header className="flex min-h-14 items-center justify-between gap-2 border-b border-[#29445f] bg-[#102a43] px-3 text-white sm:h-16 sm:px-8">
          <Link href="/companies" className="flex min-w-0 items-center gap-2 font-semibold">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">E</span>
            {siteConfig.shortName}
          </Link>
          <div className="flex shrink-0 items-center gap-1 sm:gap-2">
            <ThemeToggle />
            <UserMenu />
          </div>
        </header>
        {children}
      </div>
    );
  }

  if (!fiscalSelectionReady || !activeFiscalSelection) {
    return <div className="min-h-screen bg-background" />;
  }

  return (
    <SidebarProvider defaultOpen>
      <AppSidebar selection={activeFiscalSelection!} />
      <main
        data-slot="sidebar-inset"
        className={cn(
          "relative flex w-full min-w-0 flex-1 flex-col bg-background",
          "md:peer-data-[variant=inset]:m-2 md:peer-data-[variant=inset]:ml-0 md:peer-data-[variant=inset]:rounded-xl md:peer-data-[variant=inset]:shadow-sm md:peer-data-[variant=inset]:peer-data-[state=collapsed]:ml-2",
          className
        )}
      >
        <SiteHeaderWithControls breadcrumbs={breadcrumbs} selection={activeFiscalSelection} />
        <div className="flex flex-col gap-3 border-b border-border bg-card px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold tracking-tight text-foreground sm:text-xl">{activeFiscalSelection.companyName}</p>
            <p className="mt-1 text-sm text-muted-foreground">Current fiscal year: <span className="font-medium text-primary">{activeFiscalSelection.fiscalYearName}</span></p>
          </div>
          <Link
            href={`/companies/${activeFiscalSelection.companyId}/fiscal-years`}
            className="inline-flex h-11 w-full shrink-0 items-center justify-center rounded-lg border border-border bg-background px-4 text-sm font-medium text-foreground transition-colors hover:bg-accent hover:text-accent-foreground sm:h-9 sm:w-auto"
          >
            Switch fiscal year
          </Link>
        </div>
        <div className="flex flex-1 flex-col">
          <div className="px-4 py-3 sm:px-6 lg:px-8">
            {breadcrumbs && breadcrumbs.length > 0 && (
              <Breadcrumb items={breadcrumbs} />
            )}
          </div>
          <Separator />
          <div className="flex flex-1 flex-col">
            {children}
          </div>
        </div>
        <SiteFooter />
      </main>
    </SidebarProvider>
  );
}

function SiteHeaderWithControls({
  breadcrumbs,
  selection,
}: {
  breadcrumbs?: { label: string; href?: string }[];
  selection: ActiveFiscalSelection;
}) {
  return (
    <header className="sticky top-0 z-40 flex h-14 items-center justify-between gap-2 border-b border-[#29445f] bg-[#102a43] px-3 text-white shadow-sm sm:h-16 sm:px-6 lg:px-8">
      <div className="flex min-w-0 items-center gap-2">
        <SidebarTrigger className="size-11 shrink-0 text-white hover:bg-white/10 hover:text-white sm:size-9" />
        <div className="hidden min-w-0 items-center gap-2 truncate text-sm text-slate-200 md:flex">
          <span className="max-w-48 truncate font-medium">{selection.companyName}</span>
          <span className="text-slate-400">/</span>
          <span className="max-w-40 truncate">{selection.fiscalYearName}</span>
          <span className="text-slate-400">/</span>
          <span className="max-w-40 truncate text-white">{breadcrumbs?.[breadcrumbs.length - 1]?.label || "Dashboard"}</span>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-1 sm:gap-2">
        <Link
          href="/companies"
          title="Switch company"
          className="inline-flex h-11 w-11 items-center justify-center rounded-lg text-slate-200 transition-colors hover:bg-white/10 hover:text-white sm:h-9 sm:w-auto sm:gap-2 sm:px-3"
        >
          <Building2 className="size-4" />
          <span className="hidden text-sm font-medium sm:inline">Switch company</span>
        </Link>
        <NotificationArea />
        <ThemeToggle />
        <UserMenu />
      </div>
    </header>
  );
}
