"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { useAppSelector } from "@/store/hooks";
import { readActiveFiscalSelection } from "@/services/business-context";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

import { Switch } from "@/components/ui/switch";

import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

const schema = z
  .object({
  enabledPaymentMethods: z.string().optional().or(z.literal("")),
  cashRounding: z.boolean(),
  serviceChargePercent: z.coerce.number().min(0).max(100),
  tipHandling: z.enum(["NONE", "ADD_TO_BILL", "SEPARATE"]),
  defaultRegisterId: z.coerce.number().optional().or(z.literal("")),
  defaultWarehouseId: z.coerce.number().optional().or(z.literal("")),
  priceListId: z.coerce.number().optional().or(z.literal("")),
  dineIn: z.boolean(),
  takeaway: z.boolean(),
  delivery: z.boolean(),
  tableManagement: z.boolean(),
  kot: z.boolean(),
  kds: z.boolean(),
  kitchenPrinter: z.boolean(),
  waiterAssignment: z.boolean(),
  orderNotes: z.boolean(),
}).refine((v) => !(v.kds && !v.kot), {
  message: "KDS requires KOT to be enabled",
  path: ["kds"],
});

type FormValues = z.infer<typeof schema>;

interface PosCompanyConfig {
  id: number;
  tenantId: number;
  companyId: number;
  enabledPaymentMethods: string | null;
  cashRounding: boolean;
  serviceChargePercent: number;
  tipHandling: string;
  defaultRegisterId: number | null;
  defaultWarehouseId: number | null;
  priceListId: number | null;
  restaurantSettings: string | null;
}

export default function PosCompanyConfigPage() {
  const selection = readActiveFiscalSelection();
  const companyId = selection?.companyId ?? 0;
  const companyName = selection?.companyName ?? "Active Company";
  const tenantId = useAppSelector((s) => s.auth.tenantId);
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      enabledPaymentMethods: "",
      cashRounding: true,
      serviceChargePercent: 0,
      tipHandling: "NONE",
      defaultRegisterId: "",
      defaultWarehouseId: "",
      priceListId: "",
      dineIn: true,
      takeaway: true,
      delivery: false,
      tableManagement: true,
      kot: true,
      kds: true,
      kitchenPrinter: false,
      waiterAssignment: true,
      orderNotes: true,
    },
  });

  const { data: config, isLoading } = useQuery<PosCompanyConfig | null>({
    queryKey: ["pos-company-config", companyId, tenantId],
    queryFn: async () => {
      try {
        const res = await axiosInstance.get(
          `/pos/company-config?tenantId=${tenantId}&companyId=${companyId}`
        );
        return res.data as PosCompanyConfig;
      } catch (e: unknown) {
        const err = e as { response?: { status?: number } };
        if (err?.response?.status === 404) return null;
        throw e;
      }
    },
    enabled: companyId > 0 && !!tenantId,
  });

  useEffect(() => {
    if (config) {
      let rs: Record<string, boolean> = {};
      try {
        rs = config.restaurantSettings ? JSON.parse(config.restaurantSettings) : {};
      } catch {
        rs = {};
      }
      form.reset({
        enabledPaymentMethods: config.enabledPaymentMethods ?? "",
        cashRounding: config.cashRounding,
        serviceChargePercent: config.serviceChargePercent ?? 0,
        tipHandling: (config.tipHandling as "NONE" | "ADD_TO_BILL" | "SEPARATE") ?? "NONE",
        defaultRegisterId: config.defaultRegisterId ?? "",
        defaultWarehouseId: config.defaultWarehouseId ?? "",
        priceListId: config.priceListId ?? "",
        dineIn: rs.dineIn ?? true,
        takeaway: rs.takeaway ?? true,
        delivery: rs.delivery ?? false,
        tableManagement: rs.tableManagement ?? true,
        kot: rs.kot ?? true,
        kds: rs.kds ?? true,
        kitchenPrinter: rs.kitchenPrinter ?? false,
        waiterAssignment: rs.waiterAssignment ?? true,
        orderNotes: rs.orderNotes ?? true,
      });
    }
  }, [config, form]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const restaurantSettings = JSON.stringify({
        dineIn: values.dineIn,
        takeaway: values.takeaway,
        delivery: values.delivery,
        tableManagement: values.tableManagement,
        kot: values.kot,
        kds: values.kds,
        kitchenPrinter: values.kitchenPrinter,
        waiterAssignment: values.waiterAssignment,
        orderNotes: values.orderNotes,
      });
      const payload = {
        enabledPaymentMethods: values.enabledPaymentMethods || null,
        cashRounding: values.cashRounding,
        serviceChargePercent: values.serviceChargePercent,
        tipHandling: values.tipHandling,
        defaultRegisterId:
          values.defaultRegisterId === "" ? null : Number(values.defaultRegisterId),
        defaultWarehouseId:
          values.defaultWarehouseId === "" ? null : Number(values.defaultWarehouseId),
        priceListId: values.priceListId === "" ? null : Number(values.priceListId),
        restaurantSettings,
      };
      const res = await axiosInstance.put(
        `/pos/company-config?tenantId=${tenantId}&companyId=${companyId}`,
        payload
      );
      return res.data as PosCompanyConfig;
    },
    onSuccess: () => {
      toast.success("POS company configuration saved");
      queryClient.invalidateQueries({ queryKey: ["pos-company-config", companyId, tenantId] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e) || "Failed to save configuration"),
  });

  const onSubmit = (values: FormValues) => mutation.mutate(values);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-3xl p-4 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">POS Company Configuration</h1>
        <p className="text-muted-foreground">
          Configure POS behaviour for {companyName}. Shared across all business types.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Payment & Checkout</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="enabledPaymentMethods"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Enabled Payment Methods (comma-separated)</FormLabel>
                    <FormControl>
                      <Input placeholder="CASH,CARD,DIGITAL_WALLET" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="cashRounding"
                render={({ field }) => (
                  <FormItem className="flex items-center justify-between rounded-lg border p-3">
                    <div>
                      <FormLabel>Cash Rounding</FormLabel>
                      <p className="text-sm text-muted-foreground">
                        Round cash totals to nearest unit.
                      </p>
                    </div>
                    <FormControl>
                      <Switch checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="serviceChargePercent"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Service Charge %</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.01" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="tipHandling"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tip Handling</FormLabel>
                    <FormControl>
                      <select
                        className="w-full rounded-md border p-2"
                        value={field.value}
                        onChange={field.onChange}
                      >
                        <option value="NONE">None</option>
                        <option value="ADD_TO_BILL">Add to Bill</option>
                        <option value="SEPARATE">Separate</option>
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <FormField
                  control={form.control}
                  name="defaultRegisterId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Default Register ID</FormLabel>
                      <FormControl>
                        <Input type="number" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="defaultWarehouseId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Default Warehouse ID</FormLabel>
                      <FormControl>
                        <Input type="number" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="priceListId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Price List ID</FormLabel>
                      <FormControl>
                        <Input type="number" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="space-y-3 rounded-lg border p-3">
                <p className="text-sm font-medium">Restaurant Settings</p>
                {[
                  ["dineIn", "Dine-in"],
                  ["takeaway", "Takeaway"],
                  ["delivery", "Delivery"],
                  ["tableManagement", "Table Management"],
                  ["kot", "KOT"],
                  ["kds", "KDS"],
                  ["kitchenPrinter", "Kitchen Printer"],
                  ["waiterAssignment", "Waiter Assignment"],
                  ["orderNotes", "Order Notes"],
                ].map(([name, label]) => (
                  <FormField
                    key={name}
                    control={form.control}
                    name={name as keyof FormValues}
                    render={({ field }) => (
                      <FormItem className="flex items-center justify-between rounded-lg border p-2">
                        <FormLabel>{label}</FormLabel>
                        <FormControl>
                          <Switch checked={field.value as boolean} onCheckedChange={field.onChange} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                ))}
              </div>

              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Saving..." : "Save Configuration"}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
