"use client";

import { useState, useMemo, useEffect } from "react";
import { useForm, type FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Shield,
  Lock,
  Search,
  CheckSquare,
  Square,
  ChevronDown,
  ChevronRight,
  Globe,
  Building2,
  Users,
  User,
  ArrowRight,
  RotateCcw,
  CheckCheck,
  X,
  Layers,
  Filter,
  Loader2,
  Sparkles,
  Package,
  ShoppingBag,
  Receipt,
  Truck,
  Factory,
} from "lucide-react";
import { toast } from "sonner";
import { useCreateRoleMutation } from "@/services/role.service";
import { useGetPermissionsQuery, type PermissionResponse } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const createRoleSchema = z.object({
  roleCode: z
    .string()
    .min(1, "Role code is required")
    .max(50, "Role code must not exceed 50 characters")
    .regex(/^[A-Z_]+$/, "Role code must contain only uppercase letters and underscores"),
  roleName: z
    .string()
    .min(1, "Role name is required")
    .max(100, "Role name must not exceed 100 characters"),
  description: z.string().max(500, "Description must not exceed 500 characters").optional(),
  dataScope: z.enum(["ALL", "OWN_BRANCH", "OWN_DEPARTMENT", "SELF_ONLY"]),
  permissionIds: z.array(z.number()),
});

type CreateRoleFormValues = z.infer<typeof createRoleSchema>;

interface CreateRoleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// Data scope cards configuration
const DATA_SCOPES: {
  value: "ALL" | "OWN_BRANCH" | "OWN_DEPARTMENT" | "SELF_ONLY";
  title: string;
  badge: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
}[] = [
  {
    value: "ALL",
    title: "Organization Wide",
    badge: "Full Access",
    description: "User can access data across all branches, warehouses, and departments.",
    icon: Globe,
  },
  {
    value: "OWN_BRANCH",
    title: "Own Branch Only",
    badge: "Branch Scoped",
    description: "Restricted strictly to transactions and records within the user's assigned branch.",
    icon: Building2,
  },
  {
    value: "OWN_DEPARTMENT",
    title: "Own Department",
    badge: "Dept Scoped",
    description: "Restricted to data and workflows within the user's designated operational department.",
    icon: Users,
  },
  {
    value: "SELF_ONLY",
    title: "Personal / Self Only",
    badge: "Owner Scoped",
    description: "User can only view and operate on records they personally created or are assigned to.",
    icon: User,
  },
];

// Module definitions grouping permissions logically
interface ModuleCategory {
  id: string;
  name: string;
  icon: React.ComponentType<{ className?: string }>;
  resources: string[];
}

const MODULE_CATEGORIES: ModuleCategory[] = [
  {
    id: "IDENTITY",
    name: "Identity & Admin",
    icon: Shield,
    resources: [
      "USER",
      "ROLE",
      "PERMISSION",
      "BRANCH",
      "DEPARTMENT",
      "COMPANY",
      "TENANT",
      "PLATFORM",
      "ROLE_TEMPLATE",
      "USER_ROLE",
      "USER_BRANCH",
      "USER_DEPARTMENT",
      "PROVINCE",
      "DISTRICT",
      "LOCAL_LEVEL",
      "RESOURCE",
      "AUDIT_LOG",
    ],
  },
  {
    id: "SALES",
    name: "Sales & POS",
    icon: ShoppingBag,
    resources: [
      "SALE",
      "SALES",
      "CUSTOMER",
      "QUOTATION",
      "SALES_ORDER",
      "SALES_INVOICE",
      "POS_SESSION",
      "CRM",
      "TABLE",
      "KOT",
      "KDS",
    ],
  },
  {
    id: "INVENTORY",
    name: "Inventory & Warehouse",
    icon: Package,
    resources: [
      "PRODUCT",
      "PRODUCT_CATEGORY",
      "CATEGORY",
      "INVENTORY",
      "WAREHOUSE",
      "STOCK_MOVEMENT",
      "STOCK",
      "SERIAL_LOT",
    ],
  },
  {
    id: "PROCUREMENT",
    name: "Procurement & Purchasing",
    icon: Truck,
    resources: [
      "PURCHASE",
      "VENDOR",
      "SUPPLIER",
      "PR",
      "PURCHASE_ORDER",
      "PURCHASE_INVOICE",
      "GOODS_RECEIPT",
      "SUPPLIER_PAYMENT",
    ],
  },
  {
    id: "FINANCE",
    name: "Financials & Accounting",
    icon: Receipt,
    resources: [
      "ACCOUNTING",
      "CHART_OF_ACCOUNTS",
      "JOURNAL_ENTRY",
      "ACCOUNTS_PAYABLE",
      "ACCOUNTS_RECEIVABLE",
      "TAX_MANAGEMENT",
      "TAX",
      "CURRENCY",
      "BANK_ACCOUNT",
      "BUDGET",
      "INVOICE",
      "PAYMENT",
    ],
  },
  {
    id: "HR",
    name: "HR & Payroll",
    icon: Users,
    resources: [
      "HR",
      "EMPLOYEE",
      "ATTENDANCE",
      "LEAVE_REQUEST",
      "PAYROLL",
      "RECRUITMENT",
    ],
  },
  {
    id: "MANUFACTURING",
    name: "Manufacturing",
    icon: Factory,
    resources: [
      "MANUFACTURING",
      "BILL_OF_MATERIALS",
      "WORK_ORDER",
      "WORK_CENTER",
      "QUALITY_CONTROL",
    ],
  },
  {
    id: "ANALYTICS",
    name: "Analytics & System",
    icon: Layers,
    resources: [
      "REPORTING",
      "AI",
      "AI_AGENT",
      "AI_ANALYTICS",
      "INTEGRATION",
      "NOTIFICATION",
    ],
  },
];

// Human-friendly resource titles
function formatResourceName(resource: string): string {
  const map: Record<string, string> = {
    USER: "Users & Accounts",
    ROLE: "Roles & Access Control",
    BRANCH: "Branches & Locations",
    DEPARTMENT: "Departments",
    COMPANY: "Companies & Organizations",
    TENANT: "Tenant Workspace",
    PLATFORM: "Platform Administration",
    ROLE_TEMPLATE: "Role Templates",
    USER_ROLE: "User Role Assignments",
    USER_BRANCH: "User Branch Assignments",
    USER_DEPARTMENT: "User Department Assignments",
    PROVINCE: "Provinces",
    DISTRICT: "Districts",
    LOCAL_LEVEL: "Local Levels (Palikas)",
    RESOURCE: "Resource Catalog",
    AUDIT_LOG: "Audit & Security Logs",
    ACCOUNTING: "General Ledger & Accounting",
    CHART_OF_ACCOUNTS: "Chart of Accounts",
    JOURNAL_ENTRY: "Journal Entries & Vouchers",
    ACCOUNTS_PAYABLE: "Accounts Payable (AP)",
    ACCOUNTS_RECEIVABLE: "Accounts Receivable (AR)",
    TAX_MANAGEMENT: "Tax & VAT Management",
    TAX: "Tax Schemes",
    CURRENCY: "Multi-Currency Rates",
    BANK_ACCOUNT: "Bank Accounts & Cash",
    BUDGET: "Budgets & Forecasting",
    INVOICE: "Invoicing",
    PAYMENT: "Payments & Receipts",
    SALES: "Sales Management",
    SALE: "Sales Orders & Point of Sale",
    CUSTOMER: "Customers & Accounts",
    QUOTATION: "Quotations & Estimates",
    SALES_ORDER: "Sales Orders",
    SALES_INVOICE: "Sales Invoices",
    POS_SESSION: "POS Register Sessions",
    CRM: "Customer Relationship (CRM)",
    TABLE: "Restaurant Dining Tables",
    KOT: "Kitchen Order Tickets (KOT)",
    KDS: "Kitchen Display Systems (KDS)",
    PURCHASE: "Purchasing & Procurement",
    VENDOR: "Vendors & Suppliers",
    SUPPLIER: "Suppliers",
    PR: "Purchase Requisitions (PR)",
    PURCHASE_ORDER: "Purchase Orders (PO)",
    PURCHASE_INVOICE: "Purchase Invoices",
    GOODS_RECEIPT: "Goods Receipts (GRN)",
    SUPPLIER_PAYMENT: "Supplier Payments",
    PRODUCT: "Products & Items",
    PRODUCT_CATEGORY: "Product Categories",
    CATEGORY: "Categories",
    INVENTORY: "Inventory Management",
    WAREHOUSE: "Warehouses & Storage",
    STOCK_MOVEMENT: "Stock Movements & Transfers",
    STOCK: "Stock Levels",
    SERIAL_LOT: "Serial & Lot Tracking",
    HR: "Human Resources",
    EMPLOYEE: "Employees & Staff",
    ATTENDANCE: "Attendance Tracking",
    LEAVE_REQUEST: "Leave Requests",
    PAYROLL: "Payroll & Compensation",
    RECRUITMENT: "Recruitment & Hiring",
    MANUFACTURING: "Manufacturing Operations",
    BILL_OF_MATERIALS: "Bill of Materials (BOM)",
    WORK_ORDER: "Production Work Orders",
    WORK_CENTER: "Work Centers",
    QUALITY_CONTROL: "Quality Control (QC)",
    REPORTING: "Reports & Analytics",
    AI: "AI Assistant",
    AI_AGENT: "AI Agents",
    AI_ANALYTICS: "AI Predictive Analytics",
    INTEGRATION: "API & Integrations",
    NOTIFICATION: "Notifications & Alerts",
  };

  if (map[resource]) return map[resource];
  return resource
    .split("_")
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(" ");
}

// Visual color styling for actions
function getActionBadgeStyle(action: string, isChecked: boolean) {
  if (isChecked) {
    return "bg-primary text-primary-foreground border-primary shadow-xs font-semibold";
  }

  const act = action.toUpperCase();
  if (act.includes("VIEW") || act.includes("LIST") || act.includes("READ") || act.includes("REPORT")) {
    return "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-300 hover:bg-sky-500/20";
  }
  if (act.includes("CREATE") || act.includes("NEW") || act.includes("ADD")) {
    return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-500/20";
  }
  if (act.includes("UPDATE") || act.includes("EDIT") || act.includes("ADJUST") || act.includes("CHANGE")) {
    return "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300 hover:bg-amber-500/20";
  }
  if (act.includes("DELETE") || act.includes("CANCEL") || act.includes("VOID") || act.includes("REJECT")) {
    return "border-rose-500/30 bg-rose-500/10 text-rose-700 dark:text-rose-300 hover:bg-rose-500/20";
  }
  if (act.includes("APPROVE") || act.includes("POST") || act.includes("SUBMIT") || act.includes("RECONCILE")) {
    return "border-purple-500/30 bg-purple-500/10 text-purple-700 dark:text-purple-300 hover:bg-purple-500/20";
  }
  if (act.includes("EXPORT") || act.includes("IMPORT") || act.includes("PRINT")) {
    return "border-slate-500/30 bg-slate-500/10 text-slate-700 dark:text-slate-300 hover:bg-slate-500/20";
  }
  return "border-border bg-muted/40 text-muted-foreground hover:bg-muted";
}

export function CreateRoleDialog({ open, onOpenChange }: CreateRoleDialogProps) {
  const [createRole, { isLoading }] = useCreateRoleMutation();
  const { data: permissionsData, isLoading: isPermissionsLoading } = useGetPermissionsQuery(
    { size: 500 },
    { skip: !open }
  );

  // Tabs & UI state
  const [activeTab, setActiveTab] = useState<"details" | "permissions">("details");
  const [selectedModule, setSelectedModule] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "SELECTED" | "UNSELECTED">("ALL");
  const [permissionSearch, setPermissionSearch] = useState("");
  const [collapsedResources, setCollapsedResources] = useState<Record<string, boolean>>({});
  const [autoGenerateCode, setAutoGenerateCode] = useState(true);

  const form = useForm<CreateRoleFormValues>({
    resolver: zodResolver(createRoleSchema),
    defaultValues: {
      roleCode: "",
      roleName: "",
      description: "",
      dataScope: "ALL",
      permissionIds: [],
    } satisfies CreateRoleFormValues,
  });

  const selectedPermissionIds = form.watch("permissionIds") || [];
  const currentRoleCode = form.watch("roleCode");

  // Reset form when dialog opens
  useEffect(() => {
    if (open) {
      form.reset({
        roleCode: "",
        roleName: "",
        description: "",
        dataScope: "ALL",
        permissionIds: [],
      });
      setActiveTab("details");
      setSelectedModule("ALL");
      setStatusFilter("ALL");
      setPermissionSearch("");
      setAutoGenerateCode(true);
    }
  }, [open, form]);

  const allPermissions = useMemo(() => {
    return permissionsData?.permissions ?? [];
  }, [permissionsData]);

  // Auto-generate uppercase role code from role name if enabled
  const handleRoleNameChange = (nameValue: string) => {
    form.setValue("roleName", nameValue, { shouldValidate: true });
    if (autoGenerateCode) {
      const generated = nameValue
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "");
      form.setValue("roleCode", generated, { shouldValidate: !!generated });
    }
  };

  // Map each resource to its category
  const resourceToModuleMap = useMemo(() => {
    const map = new Map<string, string>();
    MODULE_CATEGORIES.forEach((mod) => {
      mod.resources.forEach((res) => map.set(res.toUpperCase(), mod.id));
    });
    return map;
  }, []);

  // Compute selected count per module for badges
  const moduleCounts = useMemo(() => {
    const counts: Record<string, { total: number; selected: number }> = {};
    MODULE_CATEGORIES.forEach((mod) => {
      counts[mod.id] = { total: 0, selected: 0 };
    });

    allPermissions.forEach((p) => {
      const modId = resourceToModuleMap.get(p.resource?.toUpperCase()) ?? "OTHER";
      if (!counts[modId]) {
        counts[modId] = { total: 0, selected: 0 };
      }
      counts[modId].total += 1;
      if (selectedPermissionIds.includes(p.id)) {
        counts[modId].selected += 1;
      }
    });

    return counts;
  }, [allPermissions, resourceToModuleMap, selectedPermissionIds]);

  // Count distinct modules with selections
  const selectedModulesCount = useMemo(() => {
    const activeModIds = new Set<string>();
    allPermissions.forEach((p) => {
      if (selectedPermissionIds.includes(p.id)) {
        const modId = resourceToModuleMap.get(p.resource?.toUpperCase()) ?? "OTHER";
        activeModIds.add(modId);
      }
    });
    return activeModIds.size;
  }, [allPermissions, resourceToModuleMap, selectedPermissionIds]);

  // Filter permissions based on active module, search text, and status filter
  const filteredPermissions = useMemo(() => {
    const searchLower = permissionSearch.toLowerCase().trim();

    return allPermissions.filter((p) => {
      // Module category filter
      if (selectedModule !== "ALL") {
        const modId = resourceToModuleMap.get(p.resource?.toUpperCase()) ?? "OTHER";
        if (modId !== selectedModule) return false;
      }

      // Status filter
      const isSelected = selectedPermissionIds.includes(p.id);
      if (statusFilter === "SELECTED" && !isSelected) return false;
      if (statusFilter === "UNSELECTED" && isSelected) return false;

      // Text search
      if (searchLower) {
        const matchCode = p.permissionCode.toLowerCase().includes(searchLower);
        const matchResource = p.resource?.toLowerCase().includes(searchLower);
        const matchAction = p.action?.toLowerCase().includes(searchLower);
        const matchDesc = p.description?.toLowerCase().includes(searchLower);
        const matchName = p.permissionName?.toLowerCase().includes(searchLower);
        if (!matchCode && !matchResource && !matchAction && !matchDesc && !matchName) {
          return false;
        }
      }

      return true;
    });
  }, [allPermissions, selectedModule, resourceToModuleMap, statusFilter, selectedPermissionIds, permissionSearch]);

  // Group filtered permissions by resource
  const groupedByResource = useMemo(() => {
    const groups: Record<string, PermissionResponse[]> = {};
    filteredPermissions.forEach((p) => {
      const res = p.resource || "OTHER";
      if (!groups[res]) groups[res] = [];
      groups[res].push(p);
    });
    return groups;
  }, [filteredPermissions]);

  // Permission selection handlers
  const togglePermission = (id: number) => {
    const current = new Set(selectedPermissionIds);
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
  };

  const toggleResource = (resourcePerms: PermissionResponse[]) => {
    const current = new Set(selectedPermissionIds);
    const allIds = resourcePerms.map((p) => p.id);
    const isAllSelected = allIds.every((id) => current.has(id));

    if (isAllSelected) {
      allIds.forEach((id) => current.delete(id));
    } else {
      allIds.forEach((id) => current.add(id));
    }
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
  };

  // Bulk actions for visible permissions
  const selectAllInView = () => {
    const current = new Set(selectedPermissionIds);
    filteredPermissions.forEach((p) => current.add(p.id));
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
    toast.success(`Selected all ${filteredPermissions.length} permissions in current view`);
  };

  const clearAllInView = () => {
    const current = new Set(selectedPermissionIds);
    filteredPermissions.forEach((p) => current.delete(p.id));
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
    toast.info("Cleared permissions from current view");
  };

  const selectReadOnlyInView = () => {
    const current = new Set(selectedPermissionIds);
    let count = 0;
    filteredPermissions.forEach((p) => {
      const act = p.action.toUpperCase();
      if (act.includes("VIEW") || act.includes("LIST") || act.includes("READ") || act.includes("REPORT")) {
        current.add(p.id);
        count++;
      }
    });
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
    toast.success(`Granted ${count} read-only permissions in current view`);
  };

  const toggleCollapseResource = (resource: string) => {
    setCollapsedResources((prev) => ({
      ...prev,
      [resource]: !prev[resource],
    }));
  };

  // Form submission handler
  const onSubmit = async (values: CreateRoleFormValues) => {
    try {
      const payload = {
        roleCode: values.roleCode.trim(),
        roleName: values.roleName.trim(),
        description: values.description?.trim() || undefined,
        dataScope: values.dataScope,
        permissionIds: values.permissionIds,
      };

      const result = await createRole(payload).unwrap();
      toast.success(`Role "${result.roleName}" created successfully!`, {
        description: `Code: ${result.roleCode} • ${values.permissionIds.length} permissions assigned`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create role", {
        description: getApiErrorMessage(err),
      });
    }
  };

  // Auto-switch to Details tab if validation fails on details fields
  const onError = (errors: FieldErrors<CreateRoleFormValues>) => {
    if (errors.roleCode || errors.roleName || errors.description || errors.dataScope) {
      setActiveTab("details");
      toast.error("Please fill in all required role details.");
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl lg:max-w-5xl h-[92vh] max-h-[850px] flex flex-col p-0 overflow-hidden gap-0 border-border/80 shadow-2xl">
        {/* Dialog Header */}
        <DialogHeader className="px-6 py-4 border-b bg-card shrink-0">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary shadow-2xs">
                <Shield className="size-6" />
              </div>
              <div>
                <DialogTitle className="text-xl font-bold tracking-tight">Add Custom Role</DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground mt-0.5">
                  Define role identity, enterprise data access scope, and granular permissions.
                </DialogDescription>
              </div>
            </div>

            <div className="flex items-center gap-2 self-start sm:self-auto">
              {currentRoleCode && (
                <Badge variant="outline" className="font-mono text-xs border-primary/30 text-primary bg-primary/5">
                  {currentRoleCode}
                </Badge>
              )}
              <Badge variant="secondary" className="text-xs font-semibold">
                {selectedPermissionIds.length} Permissions Selected
              </Badge>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit, onError)}
            className="flex flex-1 flex-col overflow-hidden"
          >
            {/* Top Step Tabs */}
            <Tabs
              value={activeTab}
              onValueChange={(val) => setActiveTab(val as "details" | "permissions")}
              className="flex-1 flex flex-col overflow-hidden gap-0"
            >
              <div className="px-6 border-b bg-muted/20 shrink-0">
                <TabsList className="bg-transparent h-11 p-0 gap-6 border-b-0">
                  <TabsTrigger
                    value="details"
                    className="h-11 rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none font-semibold text-xs px-2 gap-2"
                  >
                    <Shield className="size-4" />
                    1. Role Details & Scope
                  </TabsTrigger>
                  <TabsTrigger
                    value="permissions"
                    className="h-11 rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none font-semibold text-xs px-2 gap-2"
                  >
                    <Lock className="size-4" />
                    2. Assign Permissions
                    <span className="ml-1 inline-flex size-5 items-center justify-center rounded-full bg-primary/10 text-[11px] font-bold text-primary">
                      {selectedPermissionIds.length}
                    </span>
                  </TabsTrigger>
                </TabsList>
              </div>

              {/* TAB 1: Role Details & Data Scope */}
              <TabsContent value="details" className="flex-1 overflow-y-auto p-6 space-y-6 m-0">
                <div className="space-y-4">
                  <div>
                    <h3 className="text-sm font-bold text-foreground">Role Identification</h3>
                    <p className="text-xs text-muted-foreground">
                      Assign a distinct name and unique system code used for access policies and audit logs.
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Role Name */}
                    <FormField
                      control={form.control}
                      name="roleName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-xs font-semibold">
                            Role Name <span className="text-destructive">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input
                              placeholder="e.g. Branch Inventory Supervisor"
                              disabled={isLoading}
                              value={field.value}
                              onChange={(e) => handleRoleNameChange(e.target.value)}
                              className="h-10 text-sm"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    {/* Role Code */}
                    <FormField
                      control={form.control}
                      name="roleCode"
                      render={({ field }) => (
                        <FormItem>
                          <div className="flex items-center justify-between">
                            <FormLabel className="text-xs font-semibold">
                              Role Code <span className="text-destructive">*</span>
                            </FormLabel>
                            <button
                              type="button"
                              onClick={() => {
                                setAutoGenerateCode(!autoGenerateCode);
                                if (!autoGenerateCode && form.getValues("roleName")) {
                                  const gen = form
                                    .getValues("roleName")
                                    .trim()
                                    .toUpperCase()
                                    .replace(/[^A-Z0-9]+/g, "_")
                                    .replace(/^_+|_+$/g, "");
                                  form.setValue("roleCode", gen, { shouldValidate: true });
                                }
                              }}
                              className="text-[11px] text-primary hover:underline flex items-center gap-1 font-medium"
                            >
                              <RotateCcw className="size-3" />
                              {autoGenerateCode ? "Auto-sync on" : "Auto-generate"}
                            </button>
                          </div>
                          <FormControl>
                            <Input
                              placeholder="e.g. INVENTORY_SUPERVISOR"
                              disabled={isLoading}
                              {...field}
                              onChange={(e) => {
                                setAutoGenerateCode(false);
                                field.onChange(e.target.value.toUpperCase());
                              }}
                              className="h-10 font-mono text-sm tracking-wide"
                            />
                          </FormControl>
                          <p className="text-[11px] text-muted-foreground">
                            Uppercase letters and underscores only.
                          </p>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  {/* Description */}
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-xs font-semibold">Description (Optional)</FormLabel>
                        <FormControl>
                          <Textarea
                            placeholder="Describe the operational responsibilities and purpose of this role..."
                            disabled={isLoading}
                            {...field}
                            value={field.value ?? ""}
                            rows={3}
                            className="text-sm resize-none"
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                {/* Data Scope Section */}
                <div className="space-y-3 pt-2 border-t">
                  <div>
                    <h3 className="text-sm font-bold text-foreground">Default Access & Data Scope (ABAC)</h3>
                    <p className="text-xs text-muted-foreground">
                      Defines the organizational boundary for this role. Dictates which branch or department records users with this role can view or modify.
                    </p>
                  </div>

                  <FormField
                    control={form.control}
                    name="dataScope"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {DATA_SCOPES.map((scope) => {
                              const Icon = scope.icon;
                              const isSelected = field.value === scope.value;
                              return (
                                <div
                                  key={scope.value}
                                  onClick={() => field.onChange(scope.value)}
                                  className={`relative flex items-start gap-3 rounded-xl border p-4 cursor-pointer transition-all ${
                                    isSelected
                                      ? "border-primary bg-primary/5 ring-1 ring-primary shadow-xs"
                                      : "border-border hover:border-primary/50 hover:bg-muted/40"
                                  }`}
                                >
                                  <div
                                    className={`flex size-9 shrink-0 items-center justify-center rounded-lg ${
                                      isSelected
                                        ? "bg-primary text-primary-foreground shadow-2xs"
                                        : "bg-muted text-muted-foreground"
                                    }`}
                                  >
                                    <Icon className="size-4.5" />
                                  </div>
                                  <div className="min-w-0 flex-1">
                                    <div className="flex items-center justify-between gap-1">
                                      <p className="text-sm font-bold text-foreground">{scope.title}</p>
                                      <span
                                        className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                          isSelected
                                            ? "bg-primary/10 text-primary"
                                            : "bg-muted text-muted-foreground"
                                        }`}
                                      >
                                        {scope.badge}
                                      </span>
                                    </div>
                                    <p className="mt-1 text-xs text-muted-foreground leading-relaxed">
                                      {scope.description}
                                    </p>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                {/* Continue CTA */}
                <div className="flex items-center justify-between rounded-xl border bg-muted/20 p-4 mt-6">
                  <div>
                    <p className="text-xs font-semibold text-foreground">Ready to assign permissions?</p>
                    <p className="text-xs text-muted-foreground">
                      Select specific module capabilities for this role.
                    </p>
                  </div>
                  <Button
                    type="button"
                    onClick={() => setActiveTab("permissions")}
                    className="gap-2 shadow-sm"
                  >
                    <span>Configure Permissions ({selectedPermissionIds.length})</span>
                    <ArrowRight className="size-4" />
                  </Button>
                </div>
              </TabsContent>

              {/* TAB 2: Assign Permissions */}
              <TabsContent value="permissions" className="flex-1 flex flex-col overflow-hidden p-0 m-0">
                {/* Module Category Filter Bar */}
                <div className="border-b bg-card px-6 py-2.5 shrink-0 overflow-x-auto no-scrollbar">
                  <div className="flex items-center gap-1.5 min-w-max">
                    <button
                      type="button"
                      onClick={() => setSelectedModule("ALL")}
                      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-all ${
                        selectedModule === "ALL"
                          ? "bg-primary text-primary-foreground shadow-2xs font-semibold"
                          : "bg-muted text-muted-foreground hover:bg-muted/80"
                      }`}
                    >
                      <Layers className="size-3.5" />
                      <span>All Modules</span>
                      <span className="text-[11px] opacity-80">({allPermissions.length})</span>
                    </button>

                    {MODULE_CATEGORIES.map((mod) => {
                      const Icon = mod.icon;
                      const counts = moduleCounts[mod.id] || { total: 0, selected: 0 };
                      const isSelected = selectedModule === mod.id;
                      if (counts.total === 0) return null;

                      return (
                        <button
                          key={mod.id}
                          type="button"
                          onClick={() => setSelectedModule(mod.id)}
                          className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-all ${
                            isSelected
                              ? "bg-primary text-primary-foreground shadow-2xs font-semibold"
                              : "bg-muted text-muted-foreground hover:bg-muted/80"
                          }`}
                        >
                          <Icon className="size-3.5" />
                          <span>{mod.name}</span>
                          <span
                            className={`rounded-full px-1.5 py-0.2 text-[10px] font-bold ${
                              isSelected
                                ? "bg-white/20 text-white"
                                : counts.selected > 0
                                ? "bg-primary/10 text-primary"
                                : "bg-black/5 dark:bg-white/5 text-muted-foreground"
                            }`}
                          >
                            {counts.selected}/{counts.total}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Search & Bulk Action Toolbar */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b bg-muted/10 px-6 py-3 shrink-0">
                  <div className="flex items-center gap-2 flex-1 max-w-md">
                    <div className="relative w-full">
                      <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                      <Input
                        placeholder="Search by action, resource, or code (e.g. USER_CREATE)..."
                        value={permissionSearch}
                        onChange={(e) => setPermissionSearch(e.target.value)}
                        className="h-9 pl-9 pr-8 text-xs bg-card"
                      />
                      {permissionSearch && (
                        <button
                          type="button"
                          onClick={() => setPermissionSearch("")}
                          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                          <X className="size-3.5" />
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    {/* Status Filter Segment */}
                    <div className="inline-flex items-center rounded-lg border bg-card p-0.5 text-xs">
                      <button
                        type="button"
                        onClick={() => setStatusFilter("ALL")}
                        className={`rounded-md px-2.5 py-1 font-medium transition-colors ${
                          statusFilter === "ALL"
                            ? "bg-primary text-primary-foreground shadow-2xs"
                            : "text-muted-foreground hover:text-foreground"
                        }`}
                      >
                        All
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatusFilter("SELECTED")}
                        className={`rounded-md px-2.5 py-1 font-medium transition-colors flex items-center gap-1 ${
                          statusFilter === "SELECTED"
                            ? "bg-primary text-primary-foreground shadow-2xs"
                            : "text-muted-foreground hover:text-foreground"
                        }`}
                      >
                        <span>Selected</span>
                        <span className="text-[10px] font-bold">({selectedPermissionIds.length})</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatusFilter("UNSELECTED")}
                        className={`rounded-md px-2.5 py-1 font-medium transition-colors ${
                          statusFilter === "UNSELECTED"
                            ? "bg-primary text-primary-foreground shadow-2xs"
                            : "text-muted-foreground hover:text-foreground"
                        }`}
                      >
                        Unselected
                      </button>
                    </div>

                    {/* Bulk Helpers */}
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={selectAllInView}
                      className="h-8 px-2.5 text-xs gap-1"
                      disabled={filteredPermissions.length === 0}
                    >
                      <CheckCheck className="size-3.5" />
                      <span className="hidden sm:inline">Select All</span>
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={selectReadOnlyInView}
                      className="h-8 px-2.5 text-xs gap-1"
                      disabled={filteredPermissions.length === 0}
                      title="Select all View / Read / Report permissions in active view"
                    >
                      <Sparkles className="size-3.5 text-sky-500" />
                      <span className="hidden sm:inline">Read-Only</span>
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={clearAllInView}
                      className="h-8 px-2.5 text-xs text-muted-foreground hover:text-destructive"
                      disabled={selectedPermissionIds.length === 0}
                    >
                      Clear
                    </Button>
                  </div>
                </div>

                {/* Permissions List / Resource Matrix */}
                <div className="flex-1 overflow-y-auto p-6 space-y-4">
                  {isPermissionsLoading ? (
                    <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
                      <Loader2 className="size-8 animate-spin text-primary" />
                      <p className="text-xs font-medium">Loading permission catalog…</p>
                    </div>
                  ) : Object.keys(groupedByResource).length === 0 ? (
                    <div className="flex h-56 flex-col items-center justify-center rounded-xl border border-dashed p-8 text-center">
                      <Filter className="size-8 text-muted-foreground/50 mb-2" />
                      <p className="text-sm font-semibold text-foreground">No permissions match your criteria</p>
                      <p className="text-xs text-muted-foreground mt-1 max-w-sm">
                        Try clearing your search query, choosing a different module, or setting status to &quot;All&quot;.
                      </p>
                      {(permissionSearch || statusFilter !== "ALL" || selectedModule !== "ALL") && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setPermissionSearch("");
                            setStatusFilter("ALL");
                            setSelectedModule("ALL");
                          }}
                          className="mt-4 text-xs"
                        >
                          Reset Filters
                        </Button>
                      )}
                    </div>
                  ) : (
                    Object.entries(groupedByResource).map(([resourceKey, perms]) => {
                      const isCollapsed = collapsedResources[resourceKey];
                      const selectedCount = perms.filter((p) => selectedPermissionIds.includes(p.id)).length;
                      const isAllSelected = selectedCount === perms.length;

                      return (
                        <div
                          key={resourceKey}
                          className="rounded-xl border border-border bg-card overflow-hidden shadow-2xs transition-shadow hover:shadow-xs"
                        >
                          {/* Resource Header */}
                          <div className="flex items-center justify-between bg-muted/30 px-4 py-2.5 border-b text-xs">
                            <button
                              type="button"
                              onClick={() => toggleCollapseResource(resourceKey)}
                              className="flex items-center gap-2.5 font-bold text-foreground hover:text-primary transition-colors text-left"
                            >
                              {isCollapsed ? (
                                <ChevronRight className="size-4 text-muted-foreground" />
                              ) : (
                                <ChevronDown className="size-4 text-muted-foreground" />
                              )}
                              <span className="text-sm">{formatResourceName(resourceKey)}</span>
                              <Badge variant="outline" className="font-mono text-[10px] uppercase font-normal">
                                {resourceKey}
                              </Badge>
                              <span
                                className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                  selectedCount > 0
                                    ? "bg-primary/10 text-primary"
                                    : "bg-muted text-muted-foreground"
                                }`}
                              >
                                {selectedCount} of {perms.length} granted
                              </span>
                            </button>

                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => toggleResource(perms)}
                              className="h-7 px-2.5 text-xs hover:bg-primary/10 hover:text-primary font-medium"
                            >
                              {isAllSelected ? "Deselect All" : "Select All"}
                            </Button>
                          </div>

                          {/* Action Badges Matrix */}
                          {!isCollapsed && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5 p-3.5 bg-background/50">
                              {perms.map((perm) => {
                                const isChecked = selectedPermissionIds.includes(perm.id);
                                const actionStyle = getActionBadgeStyle(perm.action, isChecked);

                                return (
                                  <div
                                    key={perm.id}
                                    onClick={() => togglePermission(perm.id)}
                                    className={`group flex items-start gap-2.5 rounded-lg border p-2.5 text-xs cursor-pointer transition-all ${
                                      isChecked
                                        ? "border-primary bg-primary/5 shadow-2xs"
                                        : "border-border/70 hover:border-border hover:bg-muted/30"
                                    }`}
                                  >
                                    <div className="mt-0.5 shrink-0 text-primary">
                                      {isChecked ? (
                                        <CheckSquare className="size-4 fill-primary/10 text-primary" />
                                      ) : (
                                        <Square className="size-4 text-muted-foreground group-hover:text-foreground" />
                                      )}
                                    </div>

                                    <div className="min-w-0 flex-1">
                                      <div className="flex items-center gap-1.5 flex-wrap">
                                        <span
                                          className={`inline-flex rounded-md border px-1.5 py-0.5 text-[10px] font-bold tracking-wider ${actionStyle}`}
                                        >
                                          {perm.action}
                                        </span>
                                        <span className="font-mono font-semibold text-foreground text-[11px] truncate">
                                          {perm.permissionCode}
                                        </span>
                                      </div>

                                      {perm.description ? (
                                        <p className="text-[11px] text-muted-foreground line-clamp-1 mt-1">
                                          {perm.description}
                                        </p>
                                      ) : perm.permissionName ? (
                                        <p className="text-[11px] text-muted-foreground line-clamp-1 mt-1">
                                          {perm.permissionName}
                                        </p>
                                      ) : null}
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      );
                    })
                  )}
                </div>
              </TabsContent>
            </Tabs>

            {/* Dialog Footer */}
            <DialogFooter className="px-6 py-3 border-t bg-card shrink-0 flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Shield className="size-4 text-primary shrink-0" />
                <span>
                  <strong className="text-foreground">{selectedPermissionIds.length}</strong> permissions assigned across{" "}
                  <strong className="text-foreground">{selectedModulesCount}</strong> modules
                </span>
              </div>

              <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onOpenChange(false)}
                  disabled={isLoading}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={isLoading} className="shadow-sm font-semibold">
                  {isLoading ? (
                    <>
                      <Loader2 className="mr-2 size-4 animate-spin" />
                      Creating Role…
                    </>
                  ) : (
                    `Create Role (${selectedPermissionIds.length} Perms)`
                  )}
                </Button>
              </div>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
