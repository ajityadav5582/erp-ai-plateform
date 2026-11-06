"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
    dineIn: z.boolean(),
    takeaway: z.boolean(),
    delivery: z.boolean(),
    tableManagement: z.boolean(),
    kot: z.boolean(),
    kds: z.boolean(),
    kitchenPrinter: z.boolean(),
    waiterAssignment: z.boolean(),
    orderNotes: z.boolean(),
  })
  .refine((v) => !(v.kds && !v.kot), {
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

interface Props {
  companyId: number;
  tenantId: string | number | null;
}

export default function PosRestaurantSettingsTab({ companyId, tenantId }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
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

  const { data: config, isLoading, isError, error: queryError } = useQuery<PosCompanyConfig | null>({
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
        const parsed = config.restaurantSettings ? JSON.parse(config.restaurantSettings) : {};
        rs = parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
      } catch {
        rs = {};
      }
      form.reset({
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
        enabledPaymentMethods: config?.enabledPaymentMethods ?? null,
        cashRounding: config?.cashRounding ?? true,
        serviceChargePercent: config?.serviceChargePercent ?? 0,
        tipHandling: config?.tipHandling ?? "NONE",
        defaultRegisterId: config?.defaultRegisterId ?? null,
        defaultWarehouseId: config?.defaultWarehouseId ?? null,
        priceListId: config?.priceListId ?? null,
        restaurantSettings,
      };
      const res = await axiosInstance.put(
        `/pos/company-config?tenantId=${tenantId}&companyId=${companyId}`,
        payload
      );
      return res.data as PosCompanyConfig;
    },
    onSuccess: () => {
      toast.success("Restaurant settings saved");
      queryClient.invalidateQueries({ queryKey: ["pos-company-config", companyId, tenantId] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e) || "Failed to save settings"),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-4 text-sm text-destructive">
        {getApiErrorMessage(queryError) || "Failed to load restaurant settings."}
      </div>
    );
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values);

  const toggle = (name: keyof FormValues, label: string, description?: string) => (
    <FormField
      control={form.control}
      name={name}
      render={({ field }) => (
        <FormItem className="flex items-center justify-between rounded-md border p-3">
          <div>
            <FormLabel className="text-sm font-medium">{label}</FormLabel>
            {description && <p className="text-xs text-muted-foreground">{description}</p>}
          </div>
          <FormControl>
            <Switch checked={field.value as boolean} onCheckedChange={field.onChange} />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <Card>
          <CardContent className="space-y-3 pt-6">
            {toggle("dineIn", "Dine-in", "Allow dine-in orders")}
            {toggle("takeaway", "Takeaway", "Allow takeaway orders")}
            {toggle("delivery", "Delivery", "Allow delivery orders")}
            {toggle("tableManagement", "Table management", "Manage restaurant tables")}
            {toggle("kot", "KOT", "Kitchen Order Ticket printing")}
            {toggle("kds", "KOD / KDS", "Kitchen Order Display / Kitchen Display System")}
            {toggle("kitchenPrinter", "Kitchen printer", "Print to kitchen printer")}
            {toggle("waiterAssignment", "Waiter assignment", "Assign waiters to tables/orders")}
            {toggle("orderNotes", "Order notes", "Allow notes on orders")}
          </CardContent>
        </Card>
        <div className="flex justify-end">
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Saving…" : "Save restaurant settings"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
