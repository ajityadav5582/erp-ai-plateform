"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { useAppSelector } from "@/store/hooks";
import { readActiveFiscalSelection } from "@/services/business-context";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { CheckCircle, AlertTriangle, Eye, EyeOff, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

const schema = z.object({
  sellerPan: z.string().length(9, "Seller PAN must be 9 digits").regex(/^\d+$/, "Seller PAN must be numeric"),
  apiUsername: z.string().min(1, "API username is required"),
  apiPassword: z.string().min(4, "API password must be at least 4 characters"),
  isLiveEnvironment: z.boolean(),
  isCbmsActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

interface IrdCompanyConfig {
  id: number;
  tenantId: number;
  companyId: number;
  sellerPan: string;
  apiUsername: string;
  environment: "SANDBOX" | "PRODUCTION";
  cbmsActive: boolean;
  verificationStatus: "NOT_CONFIGURED" | "PENDING_VERIFICATION" | "VERIFIED" | "FAILED";
  lastVerifiedAt?: string | null;
  lastErrorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export default function CompanyIrdSettingsPage() {
  const selection = readActiveFiscalSelection();
  const companyId = selection?.companyId ?? 0;
  const companyName = selection?.companyName ?? "Active Company";
  const tenantId = useAppSelector((s) => s.auth.tenantId);

  const [showPassword, setShowPassword] = useState(false);
  const [testError, setTestError] = useState<string | null>(null);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      sellerPan: "",
      apiUsername: "",
      apiPassword: "",
      isLiveEnvironment: false,
      isCbmsActive: false,
    },
  });

  const { data: config, isLoading, refetch } = useQuery<IrdCompanyConfig>({
    queryKey: ["ird-company-config", companyId, tenantId],
    queryFn: async () => {
      const res = await axiosInstance.get(`/ird/company-config?tenantId=${tenantId}&companyId=${companyId}`);
      return res.data as IrdCompanyConfig;
    },
    enabled: companyId > 0,
  });

  useEffect(() => {
    if (config) {
      form.reset({
        sellerPan: config.sellerPan ?? "",
        apiUsername: config.apiUsername ?? "",
        apiPassword: "",
        isLiveEnvironment: config.environment === "PRODUCTION",
        isCbmsActive: config.cbmsActive ?? false,
      });
    }
  }, [config, form]);

  const testMutation = useMutation({
    mutationFn: async () => {
      const res = await axiosInstance.post(`/api/v1/integration/ird/companies/${companyId}/test`);
      return res.data;
    },
    onSuccess: () => {
      setTestError(null);
      toast.success("Connection verified with IRD");
      form.setValue("isCbmsActive", true);
      refetch();
    },
    onError: (err: unknown) => {
      const message = getApiErrorMessage(err) || "Connection test failed";
      setTestError(message);
    },
  });

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const res = await axiosInstance.put(`/api/v1/integration/ird/companies/${companyId}`, {
        sellerPan: values.sellerPan,
        apiUsername: values.apiUsername,
        apiPassword: values.apiPassword,
        environment: values.isLiveEnvironment ? "PRODUCTION" : "SANDBOX",
        cbmsActive: values.isCbmsActive,
      });
      return res.data;
    },
    onSuccess: () => {
      toast.success("IRD settings saved");
      refetch();
    },
    onError: (err: unknown) => {
      toast.error("Failed to save settings", { description: getApiErrorMessage(err) });
    },
  });

  const verificationStatus = config?.verificationStatus ?? "NOT_CONFIGURED";
  const isVerified = verificationStatus === "VERIFIED";

  const onSubmit = (values: FormValues) => {
    saveMutation.mutate(values);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold">Company IRD Settings</h1>
            <p className="text-sm text-muted-foreground">
              Configure Nepal IRD CBMS credentials for {companyName}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="font-mono">
              PAN: {config?.sellerPan ?? "—"}
            </Badge>
            {isVerified ? (
              <Badge className="bg-green-100 text-green-800 border-green-200">
                <CheckCircle className="mr-1 h-3.5 w-3.5" />
                VERIFIED
              </Badge>
            ) : (
              <Badge className="bg-amber-100 text-amber-800 border-amber-200">
                <AlertTriangle className="mr-1 h-3.5 w-3.5" />
                {verificationStatus}
              </Badge>
            )}
          </div>
        </div>

        {testError && (
          <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {testError}
          </div>
        )}

        <Card>
          <CardHeader>
            <CardTitle>CBMS Configuration</CardTitle>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                <FormField
                  control={form.control}
                  name="sellerPan"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Seller PAN</FormLabel>
                      <FormControl>
                        <Input {...field} disabled className="bg-muted" />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="apiUsername"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>
                        CBMS API User ID
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="ml-1 cursor-help text-muted-foreground">ⓘ</span>
                          </TooltipTrigger>
                          <TooltipContent>Reference: taxpayer.ird.gov.np</TooltipContent>
                        </Tooltip>
                      </FormLabel>
                      <FormControl>
                        <Input {...field} placeholder="IRD CBMS username" />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="apiPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>CBMS API Password</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            {...field}
                            type={showPassword ? "text" : "password"}
                            placeholder="••••••••"
                          />
                          <button
                            type="button"
                            className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground"
                            onClick={() => setShowPassword((v) => !v)}
                          >
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isLiveEnvironment"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Environment</FormLabel>
                      <FormControl>
                        <Tabs
                          value={field.value ? "live" : "sandbox"}
                          onValueChange={(v) => field.onChange(v === "live")}
                        >
                          <TabsList>
                            <TabsTrigger value="sandbox">Sandbox / Staging</TabsTrigger>
                            <TabsTrigger value="live">Live IRD Production</TabsTrigger>
                          </TabsList>
                        </Tabs>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isCbmsActive"
                  render={({ field }) => (
                    <FormItem className="flex items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel>Enable Real-Time CBMS Sync</FormLabel>
                        <p className="text-sm text-muted-foreground">
                          Automatically push invoices to IRD after checkout.
                        </p>
                      </div>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span>
                            <Switch
                              checked={field.value}
                              onCheckedChange={field.onChange}
                              disabled={!isVerified}
                            />
                          </span>
                        </TooltipTrigger>
                        {!isVerified && (
                          <TooltipContent>Must verify connection first</TooltipContent>
                        )}
                      </Tooltip>
                    </FormItem>
                  )}
                />

                <div className="flex items-center gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => testMutation.mutate()}
                    disabled={testMutation.isPending}
                  >
                    {testMutation.isPending && (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    )}
                    {testMutation.isPending ? "Pinging IRD Server..." : "Test Connection"}
                  </Button>

                  <Button type="submit" disabled={saveMutation.isPending}>
                    {saveMutation.isPending && (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    )}
                    Save Settings
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </TooltipProvider>
  );
}

