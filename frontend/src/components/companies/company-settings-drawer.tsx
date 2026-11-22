"use client";

import { memo, useEffect, useState } from "react";
import { skipToken } from "@reduxjs/toolkit/query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance } from "@/services/http-client";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { Loader2, Save } from "lucide-react";
import { useUpdateCompanyMutation, type Company, type CompanyType, type BusinessType, type FiscalYearType } from "@/services/company.service";
import { useGetProvincesQuery, useGetDistrictsByProvinceQuery, useGetLocalLevelsByDistrictQuery } from "@/services/geography.service";

import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

const COMPANY_TYPES: { value: CompanyType; label: string }[] = [
  { value: "PRIVATE_LIMITED", label: "Private limited" },
  { value: "PUBLIC_LIMITED", label: "Public limited" },
  { value: "SOLE_PROPRIETORSHIP", label: "Sole proprietorship" },
  { value: "NGO_INGO", label: "NGO / INGO" },
  { value: "TRUST", label: "Trust" },
  { value: "COOPERATIVE", label: "Cooperative" },
  { value: "GOVERNMENT_ENTITY", label: "Government entity" },
];

const BUSINESS_TYPES: { value: BusinessType; label: string }[] = [
  { value: "RESTAURANT", label: "Restaurant" },
  { value: "CAFE", label: "Cafe" },
  { value: "HOTEL", label: "Hotel" },
  { value: "SWEETS_RETAIL", label: "Sweets retail" },
  { value: "GENERAL_RETAIL", label: "General retail" },
  { value: "GROCERY", label: "Grocery" },
  { value: "HARDWARE", label: "Hardware" },
  { value: "SANITARY", label: "Sanitary" },
  { value: "PHARMACY", label: "Pharmacy" },
  { value: "CLOTHING", label: "Clothing" },
  { value: "SERVICES", label: "Services" },
  { value: "OTHER", label: "Other" },
];

interface Props {
  company: Company | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tenantId: string | number | null;
}

const generalSchema = z.object({
  companyName: z.string().min(1, "Company name is required"),
  legalName: z.string().optional().or(z.literal("")),
  panNumber: z.string().regex(/^\d{9}$/, "PAN must be 9 digits"),
  companyType: z.string().optional().or(z.literal("")),
  businessType: z.string().optional().or(z.literal("")),
  fiscalYearType: z.string().optional().or(z.literal("")),
  city: z.string().optional().or(z.literal("")),
  provinceId: z.number().optional().nullable(),
  districtId: z.number().optional().nullable(),
  localLevelId: z.number().optional().nullable(),
  wardNo: z.string().optional().or(z.literal("")),
  toleStreet: z.string().optional().or(z.literal("")),
  phone: z.string().optional().or(z.literal("")),
  email: z.string().optional().or(z.literal("")),
  website: z.string().optional().or(z.literal("")),
  address: z.string().optional().or(z.literal("")),
  vatRegistered: z.boolean(),
  vatRegistrationNumber: z.string().optional().or(z.literal("")),
  vatEffectiveFrom: z.string().optional().or(z.literal("")),
  vatEffectiveTo: z.string().optional().or(z.literal("")),
  bankName: z.string().optional().or(z.literal("")),
  bankAccountName: z.string().optional().or(z.literal("")),
  bankAccountNumber: z.string().optional().or(z.literal("")),
  bankBranch: z.string().optional().or(z.literal("")),
  qrCodeUrl: z.string().optional().or(z.literal("")),
});

type GeneralValues = z.infer<typeof generalSchema>;

const irdSchema = z.object({
  sellerPan: z.string().length(9, "Seller PAN must be 9 digits").regex(/^\d+$/, "Seller PAN must be numeric"),
  apiUsername: z.string().min(1, "API username is required"),
  apiPassword: z.string().min(4, "API password must be at least 4 characters").or(z.literal("")),
  isLiveEnvironment: z.boolean(),
  isCbmsActive: z.boolean(),
});

type IrdValues = z.infer<typeof irdSchema>;

const posSchema = z
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
  })
  .refine((v) => !(v.kds && !v.kot), { message: "KDS requires KOT to be enabled", path: ["kds"] });

type PosValues = z.infer<typeof posSchema>;

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
}

export default memo(function CompanySettingsDrawer({ company, open, onOpenChange, tenantId }: Props) {
  const [activeTab, setActiveTab] = useState("general");
  const [showPassword, setShowPassword] = useState(false);
  const [updateCompany, { isLoading: isUpdating }] = useUpdateCompanyMutation();
  const queryClient = useQueryClient();

  // ---------- General form ----------
  // Structured address state (cascading dropdowns)
  const [province] = useState<number | null>(null);
  const [district] = useState<number | null>(null);

  const generalForm = useForm<GeneralValues>({
    resolver: zodResolver(generalSchema),
    defaultValues: {
      companyName: "",
      legalName: "",
      panNumber: "",
      companyType: "",
      businessType: "",
      fiscalYearType: "NEPALI_BS",
      city: "",
      provinceId: null,
      districtId: null,
      localLevelId: null,
      wardNo: "",
      toleStreet: "",
      phone: "",
      email: "",
      website: "",
      address: "",
      vatRegistered: false,
      vatRegistrationNumber: "",
      vatEffectiveFrom: "",
      vatEffectiveTo: "",
      bankName: "",
      bankAccountName: "",
      bankAccountNumber: "",
      bankBranch: "",
      qrCodeUrl: "",
    },
  });

  useEffect(() => {
    if (company) {
      generalForm.reset({
        companyName: company.companyName ?? "",
        legalName: company.legalName ?? "",
        panNumber: company.panNumber ?? "",
        companyType: company.companyType ?? "",
        businessType: company.businessType ?? "",
        fiscalYearType: company.fiscalYearType ?? "",
        city: company.city ?? "",
        provinceId: company.provinceId ?? null,
        districtId: company.districtId ?? null,
        localLevelId: company.localLevelId ?? null,
        wardNo: company.wardNo ?? "",
        toleStreet: company.toleStreet ?? "",
        phone: company.phone ?? "",
        email: company.email ?? "",
        website: company.website ?? "",
        address: company.address ?? "",
        vatRegistered: Boolean(company.vatRegistered),
        vatRegistrationNumber: company.vatRegistrationNumber ?? "",
        vatEffectiveFrom: company.vatEffectiveFrom ?? "",
        vatEffectiveTo: company.vatEffectiveTo ?? "",
        bankName: company.bankName ?? "",
        bankAccountName: company.bankAccountName ?? "",
        bankAccountNumber: company.bankAccountNumber ?? "",
        bankBranch: company.bankBranch ?? "",
        qrCodeUrl: company.qrCodeUrl ?? "",
      });
      setActiveTab("general");
    }
  }, [company, generalForm]);

  const { data: _provinces = [], isLoading: _provincesLoading } = useGetProvincesQuery();
  const { data: _districts = [], isLoading: _districtsLoading } = useGetDistrictsByProvinceQuery(province ?? skipToken);
  const { data: _localLevels = [], isLoading: _localLevelsLoading } = useGetLocalLevelsByDistrictQuery(district ?? skipToken);

  const onGeneralSubmit = async (values: GeneralValues) => {
    if (!company) return;
    try {
      await updateCompany({
        id: company.id,
        data: {
          companyName: values.companyName.trim() || undefined,
          legalName: values.legalName?.trim() || undefined,
          panNumber: values.panNumber.trim() || undefined,
          city: values.city?.trim() || undefined,
          provinceId: values.provinceId ?? undefined,
          districtId: values.districtId ?? undefined,
          localLevelId: values.localLevelId ?? undefined,
          wardNo: values.wardNo?.trim() || undefined,
          toleStreet: values.toleStreet?.trim() || undefined,
          phone: values.phone?.trim() || undefined,
          email: values.email?.trim() || undefined,
          website: values.website?.trim() || undefined,
          address: values.address?.trim() || undefined,
          companyType: values.companyType ? (values.companyType as CompanyType) : undefined,
          businessType: values.businessType ? (values.businessType as BusinessType) : undefined,
          fiscalYearType: values.fiscalYearType ? (values.fiscalYearType as FiscalYearType) : undefined,
          vatRegistered: values.vatRegistered,
          vatRegistrationNumber: values.vatRegistered ? values.vatRegistrationNumber?.trim() : undefined,
          vatEffectiveFrom: values.vatRegistered && values.vatEffectiveFrom ? values.vatEffectiveFrom : undefined,
          vatEffectiveTo: values.vatRegistered && values.vatEffectiveTo ? values.vatEffectiveTo : undefined,
          bankName: values.bankName?.trim() || undefined,
          bankAccountName: values.bankAccountName?.trim() || undefined,
          bankAccountNumber: values.bankAccountNumber?.trim() || undefined,
          bankBranch: values.bankBranch?.trim() || undefined,
          qrCodeUrl: values.qrCodeUrl?.trim() || undefined,
        },
      }).unwrap();
      toast.success("Company profile saved");
    } catch (cause) {
      toast.error(getApiErrorMessage(cause) || "Failed to save");
    }
  };

  // ---------- IRD form ----------
  const irdForm = useForm<IrdValues>({
    resolver: zodResolver(irdSchema),
    defaultValues: { sellerPan: "", apiUsername: "", apiPassword: "", isLiveEnvironment: false, isCbmsActive: false },
  });

  const { data: irdConfig, isLoading: irdLoading, refetch: refetchIrd } = useQuery<IrdCompanyConfig | null>({
    queryKey: ["ird-company-config", company?.id, tenantId],
    queryFn: async () => {
      if (!company) return null;
      try {
        const res = await axiosInstance.get(`/ird/company-config?tenantId=${tenantId}&companyId=${company.id}`);
        return res.data as IrdCompanyConfig;
      } catch (e: unknown) {
        const err = e as { response?: { status?: number } };
        if (err?.response?.status === 404) return null;
        throw e;
      }
    },
    enabled: !!company && open,
  });

  useEffect(() => {
    if (irdConfig) {
      irdForm.reset({
        sellerPan: irdConfig.sellerPan ?? "",
        apiUsername: irdConfig.apiUsername ?? "",
        apiPassword: "",
        isLiveEnvironment: irdConfig.environment === "PRODUCTION",
        isCbmsActive: irdConfig.cbmsActive ?? false,
      });
    }
  }, [irdConfig, irdForm]);

  const saveIrdMutation = useMutation({
    mutationFn: async (values: IrdValues) => {
      if (!company) throw new Error("No company");
      const payload = {
        sellerPan: values.sellerPan,
        apiUsername: values.apiUsername,
        ...(values.apiPassword ? { apiPassword: values.apiPassword } : {}),
        environment: values.isLiveEnvironment ? "PRODUCTION" : "SANDBOX",
      };
      const url = `/ird/company-config?tenantId=${tenantId}&companyId=${company.id}`;
      const res = irdConfig ? await axiosInstance.put(url, payload) : await axiosInstance.post(url, payload);
      return res.data;
    },
    onSuccess: () => {
      toast.success("IRD settings saved");
      refetchIrd();
    },
    onError: (err: unknown) => toast.error("Failed to save IRD settings", { description: getApiErrorMessage(err) }),
  });

  const activationMutation = useMutation({
    mutationFn: async (active: boolean) => {
      if (!company) throw new Error("No company");
      const action = active ? "activate-cbms" : "deactivate-cbms";
      const res = await axiosInstance.post(`/ird/company-config/${action}?tenantId=${tenantId}&companyId=${company.id}`);
      return res.data as IrdCompanyConfig;
    },
    onSuccess: () => refetchIrd(),
    onError: (err: unknown) => {
      toast.error("Unable to change CBMS activation", { description: getApiErrorMessage(err) });
      refetchIrd();
    },
  });

  const onIrdSubmit = (values: IrdValues) => {
    if (!irdConfig && !values.apiPassword) {
      irdForm.setError("apiPassword", { message: "CBMS API password is required for initial setup." });
      return;
    }
    saveIrdMutation.mutate(values);
  };

  const irdVerificationStatus = irdConfig?.verificationStatus ?? "NOT_CONFIGURED";
  const irdVerified = irdVerificationStatus === "VERIFIED";

  // ---------- POS form ----------
  const posForm = useForm<PosValues>({
    resolver: zodResolver(posSchema),
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

  const { data: posConfig, isLoading: posLoading } = useQuery<PosCompanyConfig | null>({
    queryKey: ["pos-company-config", company?.id, tenantId],
    queryFn: async () => {
      if (!company) return null;
      try {
        const res = await axiosInstance.get(`/pos/company-config?tenantId=${tenantId}&companyId=${company.id}`);
        return res.data as PosCompanyConfig;
      } catch (e: unknown) {
        const err = e as { response?: { status?: number } };
        if (err?.response?.status === 404) return null;
        throw e;
      }
    },
    enabled: !!company && open,
  });

  useEffect(() => {
    if (posConfig) {
      let rs: Record<string, boolean> = {};
      try {
        rs = posConfig.restaurantSettings ? JSON.parse(posConfig.restaurantSettings) : {};
      } catch {
        rs = {};
      }
      posForm.reset({
        enabledPaymentMethods: posConfig.enabledPaymentMethods ?? "",
        cashRounding: posConfig.cashRounding,
        serviceChargePercent: posConfig.serviceChargePercent ?? 0,
        tipHandling: (posConfig.tipHandling as "NONE" | "ADD_TO_BILL" | "SEPARATE") ?? "NONE",
        defaultRegisterId: posConfig.defaultRegisterId ?? "",
        defaultWarehouseId: posConfig.defaultWarehouseId ?? "",
        priceListId: posConfig.priceListId ?? "",
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
  }, [posConfig, posForm]);

  const savePosMutation = useMutation({
    mutationFn: async (values: PosValues) => {
      if (!company) throw new Error("No company");
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
        defaultRegisterId: values.defaultRegisterId === "" ? null : Number(values.defaultRegisterId),
        defaultWarehouseId: values.defaultWarehouseId === "" ? null : Number(values.defaultWarehouseId),
        priceListId: values.priceListId === "" ? null : Number(values.priceListId),
        restaurantSettings,
      };
      const res = await axiosInstance.put(`/pos/company-config?tenantId=${tenantId}&companyId=${company.id}`, payload);
      return res.data as PosCompanyConfig;
    },
    onSuccess: () => {
      toast.success("POS configuration saved");
      queryClient.invalidateQueries({ queryKey: ["pos-company-config", company?.id, tenantId] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e) || "Failed to save POS configuration"),
  });

  // ---------- Workflow form ----------
  const [approvalRequired, setApprovalRequired] = useState(true);
  const [workflowLoading, setWorkflowLoading] = useState(false);

  const loadWorkflow = async () => {
    if (!company) return;
    setWorkflowLoading(true);
    try {
      const res = await axiosInstance.get(`/procurement/workflow-configuration`);
      setApprovalRequired(Boolean(res.data?.purchaseRequisitionApprovalRequired ?? true));
    } catch {
      setApprovalRequired(true);
    } finally {
      setWorkflowLoading(false);
    }
  };

  useEffect(() => {
    if (open && company && activeTab === "workflows") {
      loadWorkflow();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, company, activeTab]);

  const saveWorkflow = async () => {
    if (!company) return;
    try {
      await axiosInstance.put(`/procurement/workflow-configuration`, { purchaseRequisitionApprovalRequired: approvalRequired });
      toast.success("Workflow settings saved");
    } catch (e) {
      toast.error(getApiErrorMessage(e) || "Failed to save workflow settings");
    }
  };

  const isRestaurant = company?.businessType === "RESTAURANT" || company?.businessType === "CAFE" || company?.businessType === "HOTEL";

  const inputCls = "h-9 w-full rounded-md border bg-background px-3 text-sm";

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-2xl overflow-y-auto">
        <SheetHeader>
          <SheetTitle>Configure Company: {company?.companyName ?? ""}</SheetTitle>
          <SheetDescription>Manage all settings for this company in one place.</SheetDescription>
        </SheetHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="mt-4">
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="general">1. General</TabsTrigger>
            <TabsTrigger value="tax">2. Tax & IRD</TabsTrigger>
            <TabsTrigger value="banking">3. Bank & QR</TabsTrigger>
            <TabsTrigger value="pos">4. POS</TabsTrigger>
            <TabsTrigger value="workflows">5. Workflows</TabsTrigger>
          </TabsList>

          {/* Tab 1 General */}
          <TabsContent value="general" className="space-y-4 pt-4">
            <Form {...generalForm}>
              <form onSubmit={generalForm.handleSubmit(onGeneralSubmit)} className="space-y-4">
                <Card>
                  <CardContent className="space-y-4 pt-6">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <FormField control={generalForm.control} name="companyName" render={({ field }) => (
                        <FormItem><FormLabel>Company Name</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="legalName" render={({ field }) => (
                        <FormItem><FormLabel>Legal Registered Name</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="companyType" render={({ field }) => (
                        <FormItem><FormLabel>Company Type</FormLabel><FormControl>
                          <select className={inputCls} {...field}><option value="">Select</option>{COMPANY_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}</select>
                        </FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="businessType" render={({ field }) => (
                        <FormItem><FormLabel>Business Type</FormLabel><FormControl>
                          <select className={inputCls} {...field}><option value="">Select</option>{BUSINESS_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}</select>
                        </FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="fiscalYearType" render={({ field }) => (
                        <FormItem><FormLabel>Calendar System</FormLabel><FormControl>
                          <select className={inputCls} {...field}><option value="NEPALI_BS">Nepali BS</option><option value="ENGLISH_AD">English AD</option></select>
                        </FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="city" render={({ field }) => (
                        <FormItem><FormLabel>City</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="phone" render={({ field }) => (
                        <FormItem><FormLabel>Phone</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="email" render={({ field }) => (
                        <FormItem><FormLabel>Email</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="website" render={({ field }) => (
                        <FormItem><FormLabel>Website</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="address" render={({ field }) => (
                        <FormItem className="sm:col-span-2"><FormLabel>Address / Street</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                      )} />
                    </div>
                  </CardContent>
                </Card>
                <div className="flex justify-end">
                  <Button type="submit" disabled={isUpdating}>
                    {isUpdating ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save General
                  </Button>
                </div>
              </form>
            </Form>
          </TabsContent>

          {/* Tab 2 Tax & IRD */}
          <TabsContent value="tax" className="space-y-4 pt-4">
            <Form {...generalForm}>
              <Card>
                <CardContent className="space-y-4 pt-6">
                <div className="space-y-2">
                  <Label>Tax Scheme</Label>
                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 text-sm">
                      <input type="radio" checked={generalForm.watch("vatRegistered")} onChange={() => generalForm.setValue("vatRegistered", true)} /> VAT Registered (13%)
                    </label>
                    <label className="flex items-center gap-2 text-sm">
                      <input type="radio" checked={!generalForm.watch("vatRegistered")} onChange={() => generalForm.setValue("vatRegistered", false)} /> PAN Only
                    </label>
                  </div>
                </div>
                <FormField control={generalForm.control} name="panNumber" render={({ field }) => (
                  <FormItem><FormLabel>9-Digit PAN Number</FormLabel><FormControl><Input {...field} maxLength={9} /></FormControl><FormMessage /></FormItem>
                )} />
                {generalForm.watch("vatRegistered") && (
                  <div className="grid gap-4 sm:grid-cols-2">
                    <FormField control={generalForm.control} name="vatRegistrationNumber" render={({ field }) => (
                      <FormItem><FormLabel>VAT Registration Number</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={generalForm.control} name="vatEffectiveFrom" render={({ field }) => (
                      <FormItem><FormLabel>VAT Effective From</FormLabel><FormControl><Input type="date" {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={generalForm.control} name="vatEffectiveTo" render={({ field }) => (
                      <FormItem><FormLabel>VAT Effective To</FormLabel><FormControl><Input type="date" {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>
                )}
                <div className="flex justify-end">
                  <Button onClick={generalForm.handleSubmit(onGeneralSubmit)} disabled={isUpdating}>
                    {isUpdating ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save Tax
                  </Button>
                </div>
              </CardContent>
            </Card>
            </Form>

            {/* CBMS */}
            <Card>
              <CardContent className="space-y-4 pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-semibold">CBMS Real-Time Sync</h3>
                    <p className="text-xs text-muted-foreground">Sync invoices to IRD in real time.</p>
                  </div>
                  <Switch
                    checked={irdForm.watch("isCbmsActive")}
                    onCheckedChange={(v) => {
                      irdForm.setValue("isCbmsActive", v);
                      activationMutation.mutate(v);
                    }}
                  />
                </div>

                {irdLoading ? (
                  <div className="flex items-center justify-center p-6"><Loader2 className="size-5 animate-spin" /></div>
                ) : (
                  <Form {...irdForm}>
                    <form onSubmit={irdForm.handleSubmit(onIrdSubmit)} className="space-y-4">
                      <div className="rounded-md border p-4 space-y-4">
                        <div className="space-y-1.5">
                          <Label>IRD Environment</Label>
                          <div className="flex gap-4">
                            <label className="flex items-center gap-2 text-sm">
                              <input type="radio" checked={irdForm.watch("isLiveEnvironment")} onChange={() => irdForm.setValue("isLiveEnvironment", true)} /> Production
                            </label>
                            <label className="flex items-center gap-2 text-sm">
                              <input type="radio" checked={!irdForm.watch("isLiveEnvironment")} onChange={() => irdForm.setValue("isLiveEnvironment", false)} /> Sandbox (Test)
                            </label>
                          </div>
                        </div>
                        <FormField control={irdForm.control} name="sellerPan" render={({ field }) => (
                          <FormItem><FormLabel>Seller PAN</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={irdForm.control} name="apiUsername" render={({ field }) => (
                          <FormItem><FormLabel>API Username</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={irdForm.control} name="apiPassword" render={({ field }) => (
                          <FormItem>
                            <FormLabel>API Password</FormLabel>
                            <FormControl>
                              <div className="relative">
                                <Input type={showPassword ? "text" : "password"} {...field} />
                                <button type="button" className="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-muted-foreground" onClick={() => setShowPassword((s) => !s)}>
                                  {showPassword ? "Hide" : "Show"}
                                </button>
                              </div>
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )} />
                        <div className="flex items-center gap-2">
                          <Badge variant="outline">{irdVerified ? "🟢 VERIFIED" : irdVerificationStatus}</Badge>
                          {irdConfig?.lastVerifiedAt && <span className="text-xs text-muted-foreground">Last checked: {irdConfig.lastVerifiedAt}</span>}
                        </div>
                        {irdConfig?.lastErrorMessage && <p className="text-xs text-destructive">{irdConfig.lastErrorMessage}</p>}
                      </div>
                      <div className="flex justify-end gap-2">
                        <Button type="submit" disabled={saveIrdMutation.isPending}>
                          {saveIrdMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save CBMS
                        </Button>
                      </div>
                    </form>
                  </Form>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* Tab 3 Bank & QR */}
          <TabsContent value="banking" className="space-y-4 pt-4">
            <Form {...generalForm}>
              <form onSubmit={generalForm.handleSubmit(onGeneralSubmit)} className="space-y-4">
                <Card>
                  <CardContent className="space-y-4 pt-6">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <FormField control={generalForm.control} name="bankName" render={({ field }) => (
                        <FormItem><FormLabel>Bank Name</FormLabel><FormControl><Input {...field} placeholder="e.g. Nabil Bank Ltd" /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="bankBranch" render={({ field }) => (
                        <FormItem><FormLabel>Branch</FormLabel><FormControl><Input {...field} placeholder="e.g. New Road" /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="bankAccountName" render={({ field }) => (
                        <FormItem><FormLabel>Account Name</FormLabel><FormControl><Input {...field} placeholder="e.g. Acme Nepal Pvt Ltd" /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="bankAccountNumber" render={({ field }) => (
                        <FormItem><FormLabel>Account Number</FormLabel><FormControl><Input {...field} placeholder="e.g. 01201017500012" className="font-mono" /></FormControl><FormMessage /></FormItem>
                      )} />
                      <FormField control={generalForm.control} name="qrCodeUrl" render={({ field }) => (
                        <FormItem className="sm:col-span-2"><FormLabel>Payment QR Image URL</FormLabel><FormControl><Input {...field} placeholder="https://storage.erpai.np/qr/..." /></FormControl><FormMessage /></FormItem>
                      )} />
                    </div>
                  </CardContent>
                </Card>
                <div className="flex justify-end">
                  <Button type="submit" disabled={isUpdating}>
                    {isUpdating ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save Bank Details
                  </Button>
                </div>
              </form>
            </Form>
          </TabsContent>

          {/* Tab 4 POS & Counter */}
          <TabsContent value="pos" className="space-y-4 pt-4">
            {posLoading ? (
              <div className="flex items-center justify-center p-8"><Loader2 className="size-6 animate-spin" /></div>
            ) : (
              <Form {...posForm}>
                <form onSubmit={posForm.handleSubmit((v) => savePosMutation.mutate(v))} className="space-y-4">
                  <Card>
                    <CardContent className="space-y-4 pt-6">
                      <div className="grid gap-4 sm:grid-cols-2">
                        <FormField control={posForm.control} name="serviceChargePercent" render={({ field }) => (
                          <FormItem><FormLabel>Service Charge %</FormLabel><FormControl><Input type="number" step="0.01" {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={posForm.control} name="cashRounding" render={({ field }) => (
                          <FormItem className="flex items-center justify-between rounded-md border p-3">
                            <FormLabel className="text-sm font-medium">Cash Rounding</FormLabel>
                            <FormControl><Switch checked={field.value} onCheckedChange={field.onChange} /></FormControl>
                          </FormItem>
                        )} />
                        <FormField control={posForm.control} name="tipHandling" render={({ field }) => (
                          <FormItem><FormLabel>Tip Handling</FormLabel><FormControl>
                            <select className={inputCls} {...field}><option value="NONE">None</option><option value="ADD_TO_BILL">Add to bill</option><option value="SEPARATE">Separate</option></select>
                          </FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={posForm.control} name="enabledPaymentMethods" render={({ field }) => (
                          <FormItem><FormLabel>Enabled Payment Methods</FormLabel><FormControl><Input {...field} placeholder="CASH,CARD,..." /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={posForm.control} name="defaultRegisterId" render={({ field }) => (
                          <FormItem><FormLabel>Default Register ID</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={posForm.control} name="defaultWarehouseId" render={({ field }) => (
                          <FormItem><FormLabel>Default Warehouse ID</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                        <FormField control={posForm.control} name="priceListId" render={({ field }) => (
                          <FormItem><FormLabel>Price List ID</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
                        )} />
                      </div>

                      {isRestaurant && (
                        <div className="space-y-3">
                          <h3 className="text-sm font-semibold">Restaurant / Cafe Workflow</h3>
                          <div className="grid gap-3 sm:grid-cols-2">
                            {(["dineIn", "takeaway", "delivery", "tableManagement", "kot", "kds", "kitchenPrinter", "waiterAssignment", "orderNotes"] as const).map((name) => (
                              <FormField key={name} control={posForm.control} name={name} render={({ field }) => (
                                <FormItem className="flex items-center justify-between rounded-md border p-3">
                                  <FormLabel className="text-sm font-medium capitalize">{name.replace(/([A-Z])/g, " $1")}</FormLabel>
                                  <FormControl><Switch checked={field.value as boolean} onCheckedChange={field.onChange} /></FormControl>
                                </FormItem>
                              )} />
                            ))}
                          </div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                  <div className="flex justify-end">
                    <Button type="submit" disabled={savePosMutation.isPending}>
                      {savePosMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save POS
                    </Button>
                  </div>
                </form>
              </Form>
            )}
          </TabsContent>

          {/* Tab 4 Workflows */}
          <TabsContent value="workflows" className="space-y-4 pt-4">
            <Card>
              <CardContent className="space-y-4 pt-6">
                {workflowLoading ? (
                  <div className="flex items-center justify-center p-6"><Loader2 className="size-5 animate-spin" /></div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between rounded-md border p-3">
                      <div>
                        <p className="text-sm font-medium">Purchase Requisition Approval Required</p>
                        <p className="text-xs text-muted-foreground">Route requisitions through approval chain.</p>
                      </div>
                      <Switch checked={approvalRequired} onCheckedChange={setApprovalRequired} />
                    </div>
                    <div className="flex justify-end">
                      <Button onClick={saveWorkflow}><Save className="size-4" /> Save Workflows</Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </SheetContent>
    </Sheet>
  );
});
