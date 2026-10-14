"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Plus, MoreVertical, Eye, Pencil, Trash2, Power } from "lucide-react";
import {
  useGetCustomersQuery,
  useSearchCustomersQuery,
  useDeleteCustomerMutation,
  useActivateCustomerMutation,
  useDeactivateCustomerMutation,
  type Customer,
} from "@/services/customer.service";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import { CUSTOMER_TYPE_LABELS } from "@/services/customer.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { ActiveBadge } from "@/components/sales/sales-status-badge";
import { CreateCustomerDialog } from "@/components/sales/create-customer-dialog";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function CustomersPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [term, setTerm] = React.useState("");
  const [createOpen, setCreateOpen] = React.useState(false);
  const [selected, setSelected] = React.useState<Customer | null>(null);

  const trimmed = term.trim();
  const isSearching = trimmed.length > 0;
  const { data: pagedData, isLoading: pagedLoading, isFetching: pagedFetching } =
    useGetCustomersQuery({ page, size: 20, sort: "createdAt,desc" }, { skip: isSearching });
  const { data: searchData, isLoading: searchLoading, isFetching: searchFetching } =
    useSearchCustomersQuery({ name: trimmed, page, size: 20 }, { skip: !isSearching });

  const data = isSearching ? searchData : pagedData;
  const isLoading = isSearching ? searchLoading : pagedLoading;
  const isFetching = isSearching ? searchFetching : pagedFetching;
  const [remove] = useDeleteCustomerMutation();
  const [activate] = useActivateCustomerMutation();
  const [deactivate] = useDeactivateCustomerMutation();

  const handleActivate = async (id: number) => {
    try {
      await activate(id).unwrap();
      toast.success("Customer activated successfully");
      setSelected((prev) => (prev && prev.id === id ? { ...prev, isActive: true } : prev));
    } catch (err) {
      toast.error("Failed to activate customer", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async (id: number) => {
    try {
      await deactivate(id).unwrap();
      toast.success("Customer deactivated");
      setSelected((prev) => (prev && prev.id === id ? { ...prev, isActive: false } : prev));
    } catch (err) {
      toast.error("Failed to deactivate customer", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this customer?")) return;
    try {
      await remove(id).unwrap();
      toast.success("Customer deleted");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to delete customer", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const customers = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const fields: DetailField[] = selected
    ? [
        { label: "Code", value: selected.code },
        { label: "Type", value: CUSTOMER_TYPE_LABELS[selected.type] },
        { label: "PAN", value: selected.panNumber ?? "—" },
        { label: "VAT registered", value: selected.vatRegistered ? "Yes" : "No" },
        { label: "Email", value: selected.email },
        { label: "Phone", value: selected.phone },
        { label: "Currency", value: selected.currency },
        { label: "Payment terms", value: selected.paymentTermsDays != null ? `${selected.paymentTermsDays} days` : "—" },
        { label: "Address", value: selected.address, full: true },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Customers"
        description="The parties you sell to."
        count={data?.totalElements}
        countLabel="Customers"
        actions={<Button onClick={() => setCreateOpen(true)}><Plus className="mr-1 h-4 w-4" />New Customer</Button>}
      />

      <div className="flex items-center gap-2">
        <Input
          placeholder="Search by name…"
          value={term}
          onChange={(e) => setTerm(e.target.value)}
          className="max-w-xs"
        />
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>PAN</TableHead>
              <TableHead>VAT</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableSkeletonRows columns={7} />
            ) : customers.length === 0 ? (
              <TableEmptyRow columns={7} title="No results" />
            ) : (
              customers.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell>{c.code}</TableCell>
                  <TableCell>{CUSTOMER_TYPE_LABELS[c.type]}</TableCell>
                  <TableCell>{c.panNumber ?? "—"}</TableCell>
                  <TableCell>{c.vatRegistered ? "Registered" : "—"}</TableCell>
                  <TableCell><ActiveBadge active={c.isActive} /></TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setSelected(c)}><Eye className="mr-2 h-4 w-4" />View</DropdownMenuItem>
                        {c.isActive ? (
                          <DropdownMenuItem onClick={() => handleDeactivate(c.id)}><Power className="mr-2 h-4 w-4" />Deactivate</DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem onClick={() => handleActivate(c.id)}><Power className="mr-2 h-4 w-4" />Activate</DropdownMenuItem>
                        )}
                        <DropdownMenuItem className="text-red-600" onClick={() => handleDelete(c.id)}><Trash2 className="mr-2 h-4 w-4" />Delete</DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <ListPagination
        page={page}
        pageSize={20}
        totalPages={totalPages}
        totalElements={data?.totalElements ?? 0} itemLabel="customers"
        onPageChange={(p) => router.push(`/customers?page=${p}`)}
      />

      <CreateCustomerDialog open={createOpen} onOpenChange={setCreateOpen} />
      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected?.name ?? ""}
        subtitle={selected ? `${selected.code} · ${CUSTOMER_TYPE_LABELS[selected.type]}` : "Customer details"}
        badge={selected ? <ActiveBadge active={selected.isActive} /> : null}
        fields={fields}
        actions={
          selected ? (
            <div className="flex items-center gap-2 ml-auto">
              {selected.isActive ? (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDeactivate(selected.id)}
                >
                  <Power className="mr-1.5 h-3.5 w-3.5" />Deactivate
                </Button>
              ) : (
                <Button
                  size="sm"
                  onClick={() => handleActivate(selected.id)}
                >
                  <Power className="mr-1.5 h-3.5 w-3.5" />Activate
                </Button>
              )}
            </div>
          ) : undefined
        }
      />
    </div>
  );
}
