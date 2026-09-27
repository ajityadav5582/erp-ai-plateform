"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Key } from "lucide-react";
import { toast } from "sonner";
import { useCreatePermissionMutation } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";
import type { Resource, Action } from "@/services/permission.service";

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
import { Button } from "@/components/ui/button";

const createPermissionSchema = z.object({
  resource: z.string().min(1, "Resource is required") as z.ZodType<Resource>,
  action: z.string().min(1, "Action is required") as z.ZodType<Action>,
  description: z
    .string()
    .max(255, "Description must not exceed 255 characters")
    .optional()
    .or(z.literal("")),
});

type CreatePermissionFormValues = z.infer<typeof createPermissionSchema>;

interface CreatePermissionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const RESOURCES: { value: Resource; label: string; group: string }[] = [
  // Identity & Security
  { value: "PLATFORM", label: "Platform Administration", group: "Identity & Security" },
  { value: "TENANT", label: "Tenant Management", group: "Identity & Security" },
  { value: "IDENTITY", label: "Identity Access", group: "Identity & Security" },
  { value: "USER", label: "User Accounts", group: "Identity & Security" },
  { value: "ROLE", label: "Role Definitions", group: "Identity & Security" },
  { value: "PERMISSION", label: "Permissions", group: "Identity & Security" },
  { value: "ROLE_TEMPLATE", label: "Role Templates", group: "Identity & Security" },
  { value: "BRANCH", label: "Branches", group: "Identity & Security" },
  { value: "DEPARTMENT", label: "Departments", group: "Identity & Security" },
  { value: "USER_ROLE", label: "User Role Assignment", group: "Identity & Security" },
  { value: "USER_BRANCH", label: "User Branch Assignment", group: "Identity & Security" },
  { value: "USER_DEPARTMENT", label: "User Department Assignment", group: "Identity & Security" },
  { value: "PROVINCE", label: "Provinces", group: "Identity & Security" },
  { value: "DISTRICT", label: "Districts", group: "Identity & Security" },
  { value: "LOCAL_LEVEL", label: "Local Levels", group: "Identity & Security" },

  // Financials & Accounting
  { value: "ACCOUNTING", label: "Accounting General", group: "Finance & Accounting" },
  { value: "CHART_OF_ACCOUNTS", label: "Chart of Accounts", group: "Finance & Accounting" },
  { value: "JOURNAL_ENTRY", label: "Journal Vouchers", group: "Finance & Accounting" },
  { value: "ACCOUNTS_PAYABLE", label: "Accounts Payable (AP)", group: "Finance & Accounting" },
  { value: "ACCOUNTS_RECEIVABLE", label: "Accounts Receivable (AR)", group: "Finance & Accounting" },
  { value: "TAX_MANAGEMENT", label: "Tax Management", group: "Finance & Accounting" },
  { value: "CURRENCY", label: "Currency & Exchange Rates", group: "Finance & Accounting" },
  { value: "BANK_ACCOUNT", label: "Bank Accounts & Reconciliations", group: "Finance & Accounting" },
  { value: "BUDGET", label: "Budgets & Forecasting", group: "Finance & Accounting" },
  { value: "INVOICE", label: "Invoicing", group: "Finance & Accounting" },
  { value: "PAYMENT", label: "Payments", group: "Finance & Accounting" },

  // Sales & CRM
  { value: "SALES", label: "Sales Domain", group: "Sales & CRM" },
  { value: "CRM", label: "CRM Leads & Opportunities", group: "Sales & CRM" },
  { value: "CUSTOMER", label: "Customers", group: "Sales & CRM" },
  { value: "QUOTATION", label: "Sales Quotes", group: "Sales & CRM" },
  { value: "SALES_ORDER", label: "Sales Orders", group: "Sales & CRM" },
  { value: "SALES_INVOICE", label: "Sales Invoices", group: "Sales & CRM" },
  { value: "POS_SESSION", label: "Point-of-Sale (POS)", group: "Sales & CRM" },

  // Procurement & Supply Chain
  { value: "PURCHASE", label: "Purchase Domain", group: "Procurement & SCM" },
  { value: "VENDOR", label: "Vendors & Suppliers", group: "Procurement & SCM" },
  { value: "PURCHASE_REQUISITION", label: "Purchase Requisitions (PR)", group: "Procurement & SCM" },
  { value: "PURCHASE_ORDER", label: "Purchase Orders (PO)", group: "Procurement & SCM" },
  { value: "GOODS_RECEIPT", label: "Goods Receipt (GRN)", group: "Procurement & SCM" },

  // Inventory & Logistics
  { value: "INVENTORY", label: "Inventory Domain", group: "Inventory & Logistics" },
  { value: "PRODUCT", label: "Products / SKUs", group: "Inventory & Logistics" },
  { value: "PRODUCT_CATEGORY", label: "Product Categories", group: "Inventory & Logistics" },
  { value: "WAREHOUSE", label: "Warehouses & Bins", group: "Inventory & Logistics" },
  { value: "STOCK_MOVEMENT", label: "Stock Transfers & Adjustments", group: "Inventory & Logistics" },
  { value: "SERIAL_LOT", label: "Batch & Serial Tracking", group: "Inventory & Logistics" },

  // Human Resources & Payroll
  { value: "HR", label: "Human Resources Domain", group: "HR & Payroll" },
  { value: "EMPLOYEE", label: "Employees", group: "HR & Payroll" },
  { value: "ATTENDANCE", label: "Attendance & Time Logs", group: "HR & Payroll" },
  { value: "LEAVE_REQUEST", label: "Leave Requests", group: "HR & Payroll" },
  { value: "PAYROLL", label: "Payroll Execution", group: "HR & Payroll" },
  { value: "RECRUITMENT", label: "Recruitment & Hiring", group: "HR & Payroll" },

  // Manufacturing & Quality
  { value: "MANUFACTURING", label: "Manufacturing & MRP", group: "Manufacturing & Quality" },
  { value: "BILL_OF_MATERIALS", label: "Bill of Materials (BOM)", group: "Manufacturing & Quality" },
  { value: "WORK_ORDER", label: "Production Work Orders", group: "Manufacturing & Quality" },
  { value: "WORK_CENTER", label: "Work Centers", group: "Manufacturing & Quality" },
  { value: "QUALITY_CONTROL", label: "Quality Control (QC)", group: "Manufacturing & Quality" },

  // AI & Analytics
  { value: "AI", label: "AI Engine", group: "AI & Platform Analytics" },
  { value: "AI_AGENT", label: "AI Agents", group: "AI & Platform Analytics" },
  { value: "AI_ANALYTICS", label: "Predictive Analytics", group: "AI & Platform Analytics" },
  { value: "REPORTING", label: "BI Reports & Analytics", group: "AI & Platform Analytics" },
  { value: "AUDIT_LOG", label: "Audit Logs", group: "AI & Platform Analytics" },
  { value: "INTEGRATION", label: "Integrations & APIs", group: "AI & Platform Analytics" },
  { value: "NOTIFICATION", label: "System Notifications", group: "AI & Platform Analytics" },
];

const ACTIONS: { value: Action; label: string; category: string }[] = [
  // CRUD
  { value: "CREATE", label: "Create", category: "CRUD" },
  { value: "VIEW", label: "View", category: "CRUD" },
  { value: "UPDATE", label: "Update", category: "CRUD" },
  { value: "DELETE", label: "Delete", category: "CRUD" },
  { value: "LIST", label: "List", category: "CRUD" },

  // Workflow
  { value: "SUBMIT", label: "Submit", category: "Workflow" },
  { value: "APPROVE", label: "Approve", category: "Workflow" },
  { value: "REJECT", label: "Reject", category: "Workflow" },
  { value: "CANCEL", label: "Cancel", category: "Workflow" },
  { value: "CLOSE", label: "Close", category: "Workflow" },
  { value: "REOPEN", label: "Reopen", category: "Workflow" },
  { value: "VOID", label: "Void", category: "Workflow" },

  // Financial
  { value: "POST", label: "Post GL Voucher", category: "Financial" },
  { value: "RECONCILE", label: "Reconcile Account", category: "Financial" },

  // Data I/O
  { value: "EXPORT", label: "Export File", category: "Data I/O" },
  { value: "IMPORT", label: "Import File", category: "Data I/O" },
  { value: "PRINT", label: "Print Document", category: "Data I/O" },
  { value: "REPORT", label: "Generate Report", category: "Data I/O" },

  // Admin
  { value: "MANAGE", label: "Manage Settings", category: "Admin" },
  { value: "ASSIGN", label: "Assign Roles/Permissions", category: "Admin" },
  { value: "LOCK", label: "Lock Record/Period", category: "Admin" },
  { value: "UNLOCK", label: "Unlock Record/Period", category: "Admin" },
  { value: "AUDIT", label: "Audit Inspection", category: "Admin" },

  // Execution
  { value: "EXECUTE", label: "Execute Automation/AI", category: "Execution" },
];


export function CreatePermissionDialog({ open, onOpenChange }: CreatePermissionDialogProps) {
  const [createPermission, { isLoading }] = useCreatePermissionMutation();

  const form = useForm<CreatePermissionFormValues>({
    resolver: zodResolver(createPermissionSchema),
    defaultValues: {
      resource: undefined,
      action: undefined,
      description: "",
    },
  });

  const onSubmit = async (values: CreatePermissionFormValues) => {
    try {
      const payload = {
        resource: values.resource,
        action: values.action,
        description: values.description || undefined,
      };
      const result = await createPermission(payload).unwrap();
      const permissionCode = `${values.resource}_${values.action}`;
      toast.success(`Permission "${permissionCode}" created successfully!`, {
        description: result.description || `Resource: ${values.resource}, Action: ${values.action}`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create permission", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Key className="size-5" />
            </div>
            <div>
              <DialogTitle>Add New Permission</DialogTitle>
              <DialogDescription>
                Create a new permission for fine-grained access control.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="resource"
                render={({ field }) => {
                  const groups = Array.from(new Set(RESOURCES.map((r) => r.group)));
                  return (
                    <FormItem>
                      <FormLabel>Resource *</FormLabel>
                      <FormControl>
                        <select
                          value={field.value || ""}
                          onChange={(e) => field.onChange(e.target.value as Resource)}
                          className="h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                          disabled={isLoading}
                        >
                          <option value="">Select resource</option>
                          {groups.map((group) => (
                            <optgroup key={group} label={group}>
                              {RESOURCES.filter((r) => r.group === group).map((resource) => (
                                <option key={resource.value} value={resource.value}>
                                  {resource.label} ({resource.value})
                                </option>
                              ))}
                            </optgroup>
                          ))}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  );
                }}
              />

              <FormField
                control={form.control}
                name="action"
                render={({ field }) => {
                  const categories = Array.from(new Set(ACTIONS.map((a) => a.category)));
                  return (
                    <FormItem>
                      <FormLabel>Action *</FormLabel>
                      <FormControl>
                        <select
                          value={field.value || ""}
                          onChange={(e) => field.onChange(e.target.value as Action)}
                          className="h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                          disabled={isLoading}
                        >
                          <option value="">Select action</option>
                          {categories.map((category) => (
                            <optgroup key={category} label={category}>
                              {ACTIONS.filter((a) => a.category === category).map((action) => (
                                <option key={action.value} value={action.value}>
                                  {action.label} ({action.value})
                                </option>
                              ))}
                            </optgroup>
                          ))}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  );
                }}
              />
            </div>


            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description</FormLabel>
                  <FormControl>
                    <Input placeholder="Permission description" disabled={isLoading} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Creating..." : "Create Permission"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
