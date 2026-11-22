"use client";

import { useEffect, useState, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import { skipToken } from "@reduxjs/toolkit/query";
import {
  Building2,
  MapPin,
  Phone,
  Globe,
  Receipt,
  Calendar,
  Utensils,
  ShieldCheck,
  ChevronLeft,
  Save,
  ExternalLink,
  CheckCircle2,
  Info,
  Loader2,
  CreditCard,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useGetCompanyQuery,
  useUpdateCompanyMutation,
  type BusinessType,
  type CompanyType,
  type FiscalYearType,
} from "@/services/company.service";
import {
  useGetProvincesQuery,
  useGetDistrictsByProvinceQuery,
  useGetLocalLevelsByDistrictQuery,
  type ProvinceListResponse,
  type DistrictListResponse,
  type LocalLevelListResponse,
} from "@/services/geography.service";
import { useAppSelector } from "@/store/hooks";
import PosRestaurantSettingsTab from "@/components/companies/pos-restaurant-settings-tab";

const COMPANY_TYPES: { value: CompanyType; label: string }[] = [
  { value: "PRIVATE_LIMITED", label: "Private Limited (Pvt. Ltd.)" },
  { value: "PUBLIC_LIMITED", label: "Public Limited" },
  { value: "SOLE_PROPRIETORSHIP", label: "Sole Proprietorship" },
  { value: "NGO_INGO", label: "NGO / INGO" },
  { value: "TRUST", label: "Trust" },
  { value: "COOPERATIVE", label: "Cooperative" },
  { value: "GOVERNMENT_ENTITY", label: "Government Entity" },
];

export default function CompanySettingsPage() {
  const params = useParams();
  const router = useRouter();
  const companyId = Number(params?.companyId);
  const tenantId = useAppSelector((s) => s.auth.tenantId);

  // Directly fetch target company
  const { data: company, isLoading, error: companyLoadError } = useGetCompanyQuery(companyId, {
    skip: !companyId || isNaN(companyId),
  });

  const [updateCompany, { isLoading: isSaving }] = useUpdateCompanyMutation();
  const [activeTab, setActiveTab] = useState("profile");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Form State
  const [companyName, setCompanyName] = useState("");
  const [legalName, setLegalName] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [companyType, setCompanyType] = useState<CompanyType | "">("");
  const [businessType, setBusinessType] = useState<BusinessType | "">("");
  const [fiscalYearType, setFiscalYearType] = useState<FiscalYearType | "">("");

  // Geography & Location
  const [provinceId, setProvinceId] = useState<number | null>(null);
  const [districtId, setDistrictId] = useState<number | null>(null);
  const [localLevelId, setLocalLevelId] = useState<number | null>(null);
  const [wardNo, setWardNo] = useState("");
  const [city, setCity] = useState("");
  const [toleStreet, setToleStreet] = useState("");
  const [address, setAddress] = useState("");

  // Contact
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [website, setWebsite] = useState("");

  // Tax & VAT
  const [vatRegistered, setVatRegistered] = useState(false);
  const [vatRegistrationNumber, setVatRegistrationNumber] = useState("");
  const [vatEffectiveFrom, setVatEffectiveFrom] = useState("");
  const [vatEffectiveTo, setVatEffectiveTo] = useState("");

  // Bank & Payment QR
  const [bankName, setBankName] = useState("");
  const [bankAccountName, setBankAccountName] = useState("");
  const [bankAccountNumber, setBankAccountNumber] = useState("");
  const [bankBranch, setBankBranch] = useState("");
  const [qrCodeUrl, setQrCodeUrl] = useState("");

  // Populate state when company data arrives
  useEffect(() => {
    if (company) {
      setCompanyName(company.companyName ?? "");
      setLegalName(company.legalName ?? "");
      setPanNumber(company.panNumber ?? "");
      setCompanyType(company.companyType ?? "");
      setBusinessType(company.businessType ?? "");
      setFiscalYearType(company.fiscalYearType ?? "");

      setProvinceId(company.provinceId != null ? Number(company.provinceId) : null);
      setDistrictId(company.districtId != null ? Number(company.districtId) : null);
      setLocalLevelId(company.localLevelId != null ? Number(company.localLevelId) : null);
      setWardNo(company.wardNo ?? "");
      setCity(company.city ?? "");
      setToleStreet(company.toleStreet ?? "");
      setAddress(company.address ?? "");

      setPhone(company.phone ?? "");
      setEmail(company.email ?? "");
      setWebsite(company.website ?? "");

      setVatRegistered(Boolean(company.vatRegistered));
      setVatRegistrationNumber(company.vatRegistrationNumber ?? "");
      setVatEffectiveFrom(company.vatEffectiveFrom ?? "");
      setVatEffectiveTo(company.vatEffectiveTo ?? "");

      setBankName(company.bankName ?? "");
      setBankAccountName(company.bankAccountName ?? "");
      setBankAccountNumber(company.bankAccountNumber ?? "");
      setBankBranch(company.bankBranch ?? "");
      setQrCodeUrl(company.qrCodeUrl ?? "");
    }
  }, [company]);

  // Cascading Geography Queries
  const { data: provinces = [], isLoading: provincesLoading } = useGetProvincesQuery();
  const { data: districts = [], isLoading: districtsLoading } = useGetDistrictsByProvinceQuery(
    provinceId ?? skipToken
  );
  const { data: localLevels = [], isLoading: localLevelsLoading } = useGetLocalLevelsByDistrictQuery(
    districtId ?? skipToken
  );

  const isRestaurantOrHospitality = useMemo(() => {
    return businessType === "RESTAURANT" || businessType === "CAFE" || businessType === "HOTEL";
  }, [businessType]);

  const navGroups = useMemo(() => {
    const groups: {
      label: string;
      items: { key: string; label: string; icon: React.ComponentType<{ className?: string }> }[];
    }[] = [
      {
        label: "GENERAL",
        items: [
          { key: "profile", label: "Company Profile", icon: Building2 },
          { key: "address", label: "Address & Geography", icon: MapPin },
          { key: "contact", label: "Contact Details", icon: Phone },
          { key: "regional", label: "Regional & Calendar", icon: Globe },
        ],
      },
      {
        label: "FINANCIAL & TAX",
        items: [
          { key: "tax", label: "Tax & VAT Registration", icon: Receipt },
          { key: "banking", label: "Bank Details & Payment QR", icon: CreditCard },
          { key: "fiscal", label: "Fiscal Years", icon: Calendar },
        ],
      },
    ];

    if (isRestaurantOrHospitality) {
      groups.push({
        label: "OPERATIONS",
        items: [{ key: "pos", label: "POS Company Setting", icon: Utensils }],
      });
    }

    groups.push({
      label: "INTEGRATIONS",
      items: [{ key: "ird", label: "IRD / CBMS Billing", icon: ShieldCheck }],
    });

    return groups;
  }, [isRestaurantOrHospitality]);

  const handleSave = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setErrorMessage(null);

    if (!companyName.trim()) {
      setErrorMessage("Company name is required.");
      toast.error("Company name is required.");
      return;
    }

    if (!city.trim()) {
      setErrorMessage("City / Town is required.");
      toast.error("City / Town is required.");
      return;
    }

    if (!phone.trim()) {
      setErrorMessage("Phone number is required.");
      toast.error("Phone number is required.");
      return;
    }

    if (!email.trim()) {
      setErrorMessage("Email address is required.");
      toast.error("Email address is required.");
      return;
    }

    if (panNumber.trim() && !/^\d{9}$/.test(panNumber.trim())) {
      setErrorMessage("Nepal PAN number must be exactly 9 numeric digits.");
      toast.error("Nepal PAN number must be exactly 9 numeric digits.");
      return;
    }

    try {
      await updateCompany({
        id: companyId,
        data: {
          companyName: companyName.trim(),
          legalName: legalName.trim() || undefined,
          panNumber: panNumber.trim() || undefined,
          city: city.trim(),
          wardNo: wardNo.trim() || undefined,
          toleStreet: toleStreet.trim() || undefined,
          address: address.trim() || undefined,
          phone: phone.trim(),
          email: email.trim(),
          website: website.trim() || undefined,
          companyType: companyType ? (companyType as CompanyType) : undefined,
          businessType: businessType ? (businessType as BusinessType) : undefined,
          fiscalYearType: fiscalYearType ? (fiscalYearType as FiscalYearType) : undefined,
          provinceId: provinceId ?? undefined,
          districtId: districtId ?? undefined,
          localLevelId: localLevelId ?? undefined,
          vatRegistered,
          vatRegistrationNumber: vatRegistered && vatRegistrationNumber.trim() ? vatRegistrationNumber.trim() : undefined,
          vatEffectiveFrom: vatRegistered && vatEffectiveFrom ? vatEffectiveFrom : undefined,
          vatEffectiveTo: vatRegistered && vatEffectiveTo ? vatEffectiveTo : undefined,
          bankName: bankName.trim() || undefined,
          bankAccountName: bankAccountName.trim() || undefined,
          bankAccountNumber: bankAccountNumber.trim() || undefined,
          bankBranch: bankBranch.trim() || undefined,
          qrCodeUrl: qrCodeUrl.trim() || undefined,
        },
      }).unwrap();

      toast.success("Company settings updated successfully!");
    } catch (cause) {
      const msg = getApiErrorMessage(cause);
      setErrorMessage(msg);
      toast.error(msg);
    }
  };

  const selectCls =
    "h-10 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:opacity-50 disabled:cursor-not-allowed";

  if (isLoading) {
    return (
      <main className="min-h-screen bg-muted/30">
        <div className="mx-auto flex max-w-6xl items-center justify-center px-4 py-32">
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="size-8 animate-spin text-primary" />
            <p className="text-sm font-medium text-muted-foreground">Loading company configuration…</p>
          </div>
        </div>
      </main>
    );
  }

  if (companyLoadError || !company) {
    return (
      <main className="min-h-screen bg-muted/30">
        <div className="mx-auto max-w-2xl px-4 py-20 text-center">
          <Card className="border-destructive/30">
            <CardContent className="pt-8 pb-8 space-y-4">
              <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
                <ShieldCheck className="size-6" />
              </div>
              <h2 className="text-xl font-semibold">Unable to Load Company</h2>
              <p className="text-sm text-muted-foreground">
                {companyLoadError ? getApiErrorMessage(companyLoadError) : "Company record not found or inaccessible."}
              </p>
              <Button variant="outline" onClick={() => router.push("/companies")}>
                Return to Companies
              </Button>
            </CardContent>
          </Card>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-muted/30 pb-24 md:pb-12">
      <div className="mx-auto max-w-6xl px-4 py-6 sm:px-8 sm:py-8">
        {/* Top Header */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b pb-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-muted-foreground hover:text-foreground"
                onClick={() => router.push("/companies")}
              >
                <ChevronLeft className="size-4 mr-0.5" />
                Companies
              </Button>
              <span className="text-muted-foreground/40">/</span>
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Settings
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-2.5 pt-1">
              <h1 className="text-2xl font-bold tracking-tight sm:text-3xl text-foreground">
                {companyName || company.companyName}
              </h1>
              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:text-emerald-400">
                <CheckCircle2 className="size-3" />
                {company.status}
              </span>
              {panNumber && (
                <span className="rounded-md border bg-background px-2 py-0.5 text-xs font-medium text-muted-foreground">
                  PAN: {panNumber}
                </span>
              )}
              {company.companyCode && (
                <span className="rounded-md border bg-background px-2 py-0.5 text-xs font-mono text-muted-foreground">
                  {company.companyCode}
                </span>
              )}
            </div>
          </div>

          <div className="hidden sm:flex items-center gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(`/companies/${companyId}/fiscal-years`)}
            >
              <Calendar className="size-4 mr-1.5" />
              Fiscal Years
            </Button>
            <Button onClick={handleSave} disabled={isSaving}>
              {isSaving ? (
                <>
                  <Loader2 className="size-4 mr-1.5 animate-spin" />
                  Saving…
                </>
              ) : (
                <>
                  <Save className="size-4 mr-1.5" />
                  Save Changes
                </>
              )}
            </Button>
          </div>
        </div>

        {errorMessage && (
          <div
            role="alert"
            className="mb-6 flex items-start gap-3 rounded-lg border border-destructive/20 bg-destructive/10 p-3.5 text-sm text-destructive"
          >
            <Info className="size-5 shrink-0 mt-0.5" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* Mobile Tab Scroller (< md) */}
        <div className="md:hidden mb-6 -mx-4 px-4 overflow-x-auto no-scrollbar">
          <div className="flex items-center gap-1.5 min-w-max pb-1">
            {navGroups.flatMap((group) => group.items).map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => setActiveTab(item.key)}
                  className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-medium transition-all ${
                    isActive
                      ? "bg-primary text-primary-foreground shadow-xs"
                      : "bg-background border text-muted-foreground hover:bg-muted"
                  }`}
                >
                  <Icon className="size-3.5" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Main Content Layout */}
        <div className="flex flex-col gap-6 md:flex-row md:items-start">
          {/* Desktop Navigation Sidebar (md+) */}
          <aside className="hidden md:block w-64 shrink-0">
            <Card className="sticky top-20 shadow-xs">
              <CardContent className="p-3">
                {navGroups.map((group) => (
                  <div key={group.label} className="mb-4 last:mb-0">
                    <p className="px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-muted-foreground/80">
                      {group.label}
                    </p>
                    <nav className="space-y-1 mt-1">
                      {group.items.map((item) => {
                        const Icon = item.icon;
                        const isActive = activeTab === item.key;
                        return (
                          <button
                            key={item.key}
                            type="button"
                            onClick={() => setActiveTab(item.key)}
                            className={`flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors ${
                              isActive
                                ? "bg-primary text-primary-foreground shadow-xs"
                                : "text-muted-foreground hover:bg-muted hover:text-foreground"
                            }`}
                          >
                            <Icon className="size-4 shrink-0" />
                            <span className="truncate">{item.label}</span>
                          </button>
                        );
                      })}
                    </nav>
                  </div>
                ))}
              </CardContent>
            </Card>
          </aside>

          {/* Settings Panel Content */}
          <section className="flex-1 min-w-0">
            {/* Tab: Company Profile */}
            {activeTab === "profile" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Building2 className="size-5 text-primary" />
                    Company Profile
                  </CardTitle>
                  <CardDescription>
                    Configure the legal entity, formal registration name, and primary business classification.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5 sm:col-span-2">
                      <Label htmlFor="companyName">
                        Display Company Name <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="companyName"
                        value={companyName}
                        onChange={(e) => setCompanyName(e.target.value)}
                        placeholder="e.g. Acme Himalayan Traders"
                        maxLength={200}
                        required
                        className="h-10 text-base sm:text-sm"
                      />
                      <p className="text-xs text-muted-foreground">
                        This name is displayed throughout invoices, workspace headers, and reports.
                      </p>
                    </div>

                    <div className="space-y-1.5 sm:col-span-2">
                      <Label htmlFor="legalName">Legal / Registered Name</Label>
                      <Input
                        id="legalName"
                        value={legalName}
                        onChange={(e) => setLegalName(e.target.value)}
                        placeholder="e.g. Acme Himalayan Traders Pvt. Ltd."
                        maxLength={200}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="panNumber">
                        PAN Number (9 Digits) <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="panNumber"
                        value={panNumber}
                        onChange={(e) => setPanNumber(e.target.value.replace(/\D/g, "").slice(0, 9))}
                        placeholder="e.g. 601234567"
                        maxLength={9}
                        className="h-10 font-mono text-base sm:text-sm tracking-wider"
                      />
                      {panNumber.length > 0 && panNumber.length < 9 ? (
                        <p className="text-[11px] text-amber-600 dark:text-amber-400">
                          Nepal PAN must be exactly 9 digits ({panNumber.length}/9 entered)
                        </p>
                      ) : (
                        <p className="text-xs text-muted-foreground">
                          Must match your Permanent Account Number issued by Nepal IRD.
                        </p>
                      )}
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="companyType">
                        Company Type <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="companyType"
                        className={selectCls}
                        value={companyType}
                        onChange={(e) => setCompanyType(e.target.value as CompanyType | "")}
                      >
                        <option value="" disabled>
                          Select company constitution
                        </option>
                        {COMPANY_TYPES.map((t) => (
                          <option key={t.value} value={t.value}>
                            {t.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="space-y-1.5 sm:col-span-2">
                      <Label htmlFor="businessType">
                        Business & Industry Type <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="businessType"
                        className={selectCls}
                        value={businessType}
                        onChange={(e) => setBusinessType(e.target.value as BusinessType | "")}
                      >
                        <option value="" disabled>
                          Select business industry
                        </option>
                        <optgroup label="Food & Hospitality">
                          <option value="RESTAURANT">Restaurant</option>
                          <option value="CAFE">Cafe</option>
                          <option value="HOTEL">Hotel & Hospitality</option>
                          <option value="SWEETS_RETAIL">Sweets Retail</option>
                        </optgroup>
                        <optgroup label="Retail & Wholesale">
                          <option value="GENERAL_RETAIL">General Retail</option>
                          <option value="GROCERY">Grocery / Supermarket</option>
                          <option value="HARDWARE">Hardware</option>
                          <option value="SANITARY">Sanitary</option>
                          <option value="CLOTHING">Clothing & Apparel</option>
                        </optgroup>
                        <optgroup label="Healthcare">
                          <option value="PHARMACY">Pharmacy</option>
                        </optgroup>
                        <optgroup label="Services & Other">
                          <option value="SERVICES">Professional Services</option>
                          <option value="OTHER">Other</option>
                        </optgroup>
                      </select>
                      {isRestaurantOrHospitality && (
                        <p className="text-xs text-primary font-medium flex items-center gap-1.5 mt-1.5">
                          <Utensils className="size-3.5" />
                          Enables POS, Table Management, KOT & KDS features in the sidebar navigation.
                        </p>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: Address & Geography */}
            {activeTab === "address" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <MapPin className="size-5 text-primary" />
                    Address & Administrative Geography
                  </CardTitle>
                  <CardDescription>
                    Official registered location according to Nepal administrative divisions (Province, District, Local Level, Ward).
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="grid gap-4 sm:grid-cols-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="province">
                        Province <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="province"
                        className={selectCls}
                        value={provinceId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value ? Number(e.target.value) : null;
                          setProvinceId(val);
                          setDistrictId(null);
                          setLocalLevelId(null);
                        }}
                        disabled={provincesLoading}
                      >
                        <option value="" disabled>
                          {provincesLoading ? "Loading provinces…" : "Select Province"}
                        </option>
                        {provinces.map((p: ProvinceListResponse) => (
                          <option key={p.id} value={p.id ?? ""}>
                            {p.provinceName} {p.nepaliName ? `(${p.nepaliName})` : ""}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="district">
                        District <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="district"
                        className={selectCls}
                        value={districtId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value ? Number(e.target.value) : null;
                          setDistrictId(val);
                          setLocalLevelId(null);
                        }}
                        disabled={!provinceId || districtsLoading}
                      >
                        <option value="" disabled>
                          {!provinceId
                            ? "Select province first"
                            : districtsLoading
                            ? "Loading districts…"
                            : "Select District"}
                        </option>
                        {districts.map((d: DistrictListResponse) => (
                          <option key={d.id} value={d.id ?? ""}>
                            {d.districtName} {d.nepaliName ? `(${d.nepaliName})` : ""}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="localLevel">
                        Local Level (Palika) <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="localLevel"
                        className={selectCls}
                        value={localLevelId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value ? Number(e.target.value) : null;
                          setLocalLevelId(val);
                        }}
                        disabled={!districtId || localLevelsLoading}
                      >
                        <option value="" disabled>
                          {!districtId
                            ? "Select district first"
                            : localLevelsLoading
                            ? "Loading palikas…"
                            : "Select Palika"}
                        </option>
                        {localLevels.map((l: LocalLevelListResponse) => {
                          const palikaId = l.id ?? Number(l.municipalityId);
                          return (
                            <option key={palikaId} value={palikaId}>
                              {l.name} {l.nepaliName ? `(${l.nepaliName})` : ""}
                            </option>
                          );
                        })}
                      </select>
                    </div>
                  </div>

                  <div className="grid gap-4 sm:grid-cols-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="wardNo">Ward No.</Label>
                      <Input
                        id="wardNo"
                        value={wardNo}
                        onChange={(e) => setWardNo(e.target.value)}
                        placeholder="e.g. 4"
                        maxLength={20}
                        className="h-10 text-base sm:text-sm"
                      />
                      <p className="text-[11px] text-muted-foreground">Required for IRD compliance</p>
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="city">
                        City / Town <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="city"
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        placeholder="e.g. Kathmandu"
                        maxLength={100}
                        required
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="toleStreet">Tole / Street</Label>
                      <Input
                        id="toleStreet"
                        value={toleStreet}
                        onChange={(e) => setToleStreet(e.target.value)}
                        placeholder="e.g. New Road"
                        maxLength={200}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="address">Full Street Address (Optional)</Label>
                    <Input
                      id="address"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      placeholder="e.g. Ward No 4, New Road, Kathmandu, Nepal"
                      maxLength={500}
                      className="h-10 text-base sm:text-sm"
                    />
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: Contact Details */}
            {activeTab === "contact" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Phone className="size-5 text-primary" />
                    Contact Information
                  </CardTitle>
                  <CardDescription>
                    Official communication channels printed on customer receipts and purchase orders.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <Label htmlFor="phone">
                        Primary Phone Number <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="phone"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="e.g. +977-1-4220000 / 9843000000"
                        maxLength={50}
                        required
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="email">
                        Official Email <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="e.g. info@company.com"
                        maxLength={255}
                        required
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5 sm:col-span-2">
                      <Label htmlFor="website">Website (Optional)</Label>
                      <Input
                        id="website"
                        type="url"
                        value={website}
                        onChange={(e) => setWebsite(e.target.value)}
                        placeholder="e.g. https://www.company.com"
                        maxLength={200}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: Regional & Calendar */}
            {activeTab === "regional" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Globe className="size-5 text-primary" />
                    Regional & Localization
                  </CardTitle>
                  <CardDescription>
                    Configure calendar format, currency symbols, and localization standards.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <Label htmlFor="fiscalYearType">
                        Calendar System <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="fiscalYearType"
                        className={selectCls}
                        value={fiscalYearType}
                        onChange={(e) => setFiscalYearType(e.target.value as FiscalYearType | "")}
                      >
                        <option value="NEPALI_BS">Nepali Bikram Sambat (BS)</option>
                        <option value="ENGLISH_AD">English Gregorian (AD)</option>
                      </select>
                      <p className="text-xs text-muted-foreground">
                        Default calendar used for fiscal periods and accounting vouchers.
                      </p>
                    </div>

                    <div className="space-y-1.5">
                      <Label>Standard Currency</Label>
                      <Input value="NPR (Rs. / Nepalese Rupee)" disabled className="h-10 bg-muted/60" />
                    </div>

                    <div className="space-y-1.5">
                      <Label>Timezone</Label>
                      <Input value="Asia/Kathmandu (UTC+05:45)" disabled className="h-10 bg-muted/60" />
                    </div>

                    <div className="space-y-1.5">
                      <Label>Country</Label>
                      <Input value="Nepal" disabled className="h-10 bg-muted/60" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: Tax & VAT Registration */}
            {activeTab === "tax" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Receipt className="size-5 text-primary" />
                    Tax & VAT Registration
                  </CardTitle>
                  <CardDescription>
                    Manage VAT registration status, effective periods, and tax invoice compliance.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="flex items-center justify-between rounded-lg border p-4 shadow-2xs">
                    <div className="space-y-0.5">
                      <Label htmlFor="vatRegisteredSwitch" className="text-base font-semibold cursor-pointer">
                        VAT Registered Entity
                      </Label>
                      <p className="text-xs text-muted-foreground">
                        Enable if this company is officially registered for Value Added Tax (VAT 13%) with IRD Nepal.
                      </p>
                    </div>
                    <Switch
                      id="vatRegisteredSwitch"
                      checked={vatRegistered}
                      onCheckedChange={(checked) => {
                        setVatRegistered(checked);
                        if (checked && !vatRegistrationNumber.trim()) {
                          setVatRegistrationNumber(panNumber.trim());
                        }
                      }}
                    />
                  </div>

                  {vatRegistered && (
                    <div className="grid gap-4 rounded-lg border bg-muted/20 p-4 sm:grid-cols-2">
                      <div className="space-y-1.5 sm:col-span-2">
                        <Label htmlFor="vatRegistrationNumber">
                          VAT / PAN Registration Number <span className="text-destructive">*</span>
                        </Label>
                        <Input
                          id="vatRegistrationNumber"
                          value={vatRegistrationNumber}
                          onChange={(e) => setVatRegistrationNumber(e.target.value.trim())}
                          placeholder="e.g. 601234567"
                          maxLength={30}
                          className="h-10 font-mono text-base sm:text-sm"
                        />
                      </div>

                      <div className="space-y-1.5">
                        <Label htmlFor="vatEffectiveFrom">VAT Effective From</Label>
                        <Input
                          id="vatEffectiveFrom"
                          type="date"
                          value={vatEffectiveFrom}
                          onChange={(e) => setVatEffectiveFrom(e.target.value)}
                          className="h-10 text-base sm:text-sm"
                        />
                      </div>

                      <div className="space-y-1.5">
                        <Label htmlFor="vatEffectiveTo">VAT Effective To (Optional)</Label>
                        <Input
                          id="vatEffectiveTo"
                          type="date"
                          value={vatEffectiveTo}
                          onChange={(e) => setVatEffectiveTo(e.target.value)}
                          className="h-10 text-base sm:text-sm"
                        />
                      </div>
                    </div>
                  )}

                  <div className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-3.5 text-xs text-amber-800 dark:text-amber-300 space-y-1">
                    <p className="font-semibold flex items-center gap-1.5">
                      <Info className="size-4 shrink-0" />
                      Inland Revenue Department (IRD) Note:
                    </p>
                    <p>
                      Tax invoices generated under VAT registered companies require valid 9-digit PAN/VAT numbers and must sync with the IRD Central Billing Monitoring System (CBMS).
                    </p>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: Bank Details & Payment QR */}
            {activeTab === "banking" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <CreditCard className="size-5 text-primary" />
                    Bank Accounts & Payment QR
                  </CardTitle>
                  <CardDescription>
                    Bank accounts and payment QR codes displayed on customer tax invoices and sales receipts for electronic settlement.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <Label htmlFor="bankName">Bank Name</Label>
                      <Input
                        id="bankName"
                        value={bankName}
                        onChange={(e) => setBankName(e.target.value)}
                        placeholder="e.g. Nabil Bank Ltd, NIC Asia Bank"
                        maxLength={100}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="bankBranch">Branch</Label>
                      <Input
                        id="bankBranch"
                        value={bankBranch}
                        onChange={(e) => setBankBranch(e.target.value)}
                        placeholder="e.g. New Road, Kathmandu"
                        maxLength={100}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="bankAccountName">Account Name</Label>
                      <Input
                        id="bankAccountName"
                        value={bankAccountName}
                        onChange={(e) => setBankAccountName(e.target.value)}
                        placeholder="e.g. Himalayan Enterprises Pvt. Ltd."
                        maxLength={150}
                        className="h-10 text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="bankAccountNumber">Account Number</Label>
                      <Input
                        id="bankAccountNumber"
                        value={bankAccountNumber}
                        onChange={(e) => setBankAccountNumber(e.target.value)}
                        placeholder="e.g. 01201017500012"
                        maxLength={50}
                        className="h-10 font-mono text-base sm:text-sm"
                      />
                    </div>

                    <div className="space-y-1.5 sm:col-span-2">
                      <Label htmlFor="qrCodeUrl">Fonepay / Payment QR Image URL</Label>
                      <Input
                        id="qrCodeUrl"
                        type="url"
                        value={qrCodeUrl}
                        onChange={(e) => setQrCodeUrl(e.target.value)}
                        placeholder="https://storage.yourdomain.com/qr/fonepay-qr.png"
                        maxLength={500}
                        className="h-10 text-base sm:text-sm"
                      />
                      <p className="text-xs text-muted-foreground">
                        URL to the company&apos;s dynamic or static payment QR code (Fonepay, NepalPay, SmartQR) printed on invoices.
                      </p>
                    </div>
                  </div>

                  {qrCodeUrl && (
                    <div className="rounded-lg border bg-muted/20 p-4 flex flex-col sm:flex-row items-center gap-4">
                      <div className="size-28 rounded-md border bg-white p-1 flex items-center justify-center shrink-0 overflow-hidden">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={qrCodeUrl} alt="Payment QR Preview" className="size-full object-contain" />
                      </div>
                      <div className="space-y-1 text-center sm:text-left">
                        <h4 className="text-sm font-semibold">Payment QR Code Preview</h4>
                        <p className="text-xs text-muted-foreground">
                          This QR code will be embedded into PDF tax invoices and POS thermal printouts so customers can scan and pay instantly.
                        </p>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Tab: POS & Restaurant Operations (Visible for RESTAURANT / CAFE / HOTEL) */}
            {activeTab === "pos" && isRestaurantOrHospitality && (
              <Card>
                <CardHeader>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <CardTitle className="flex items-center gap-2 text-xl">
                        <Utensils className="size-5 text-primary" />
                        POS & Restaurant Operations
                      </CardTitle>
                      <CardDescription>
                        Configure dine-in tables, kitchen order ticketing (KOT), kitchen display systems (KDS), and waiter routing.
                      </CardDescription>
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => router.push(`/administrations/company-settings/pos?companyId=${companyId}`)}
                    >
                      <ExternalLink className="size-3.5 mr-1.5" />
                      Full POS Setting
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <PosRestaurantSettingsTab companyId={companyId} tenantId={tenantId} />
                </CardContent>
              </Card>
            )}

            {/* Tab: Fiscal Years */}
            {activeTab === "fiscal" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Calendar className="size-5 text-primary" />
                    Fiscal Years Management
                  </CardTitle>
                  <CardDescription>
                    Create, view, and activate financial accounting periods for this company.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="rounded-lg border bg-muted/20 p-5 text-center space-y-3">
                    <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                      <Calendar className="size-6" />
                    </div>
                    <h3 className="text-base font-semibold">Company Fiscal Year Calendars</h3>
                    <p className="text-sm text-muted-foreground max-w-md mx-auto">
                      All general ledger transactions, vouchers, tax filings, and stock valuations are strictly scoped to the active fiscal year.
                    </p>
                    <div className="pt-2">
                      <Button
                        type="button"
                        onClick={() => router.push(`/companies/${companyId}/fiscal-years`)}
                      >
                        Manage Fiscal Years
                        <ExternalLink className="size-3.5 ml-1.5" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Tab: IRD / CBMS Configuration */}
            {activeTab === "ird" && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <ShieldCheck className="size-5 text-primary" />
                    IRD / CBMS Billing Configuration
                  </CardTitle>
                  <CardDescription>
                    Configure real-time tax invoice synchronization with the Nepal Inland Revenue Department (IRD) Central Billing Monitoring System.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="rounded-lg border bg-muted/20 p-5 text-center space-y-3">
                    <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                      <ShieldCheck className="size-6" />
                    </div>
                    <h3 className="text-base font-semibold">Central Billing Monitoring System (CBMS)</h3>
                    <p className="text-sm text-muted-foreground max-w-md mx-auto">
                      Manage API credentials, CBMS portal endpoints, sandbox testing, and digital invoice sign-offs.
                    </p>
                    <div className="pt-2">
                      <Button
                        type="button"
                        onClick={() =>
                          router.push(`/administrations/company-settings/ird?companyId=${companyId}`)
                        }
                      >
                        Open IRD Configuration
                        <ExternalLink className="size-3.5 ml-1.5" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Bottom Actions (Desktop & Tablet) */}
            <div className="mt-6 hidden sm:flex items-center justify-end gap-3 border-t pt-4">
              <Button type="button" variant="outline" onClick={() => router.push("/companies")}>
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
                {isSaving ? (
                  <>
                    <Loader2 className="size-4 mr-1.5 animate-spin" />
                    Saving…
                  </>
                ) : (
                  <>
                    <Save className="size-4 mr-1.5" />
                    Save Changes
                  </>
                )}
              </Button>
            </div>
          </section>
        </div>
      </div>

      {/* Sticky Bottom Action Bar for Mobile (< sm) */}
      <div className="sm:hidden fixed bottom-0 left-0 right-0 z-40 border-t bg-background/95 backdrop-blur-md p-3.5 shadow-lg flex items-center justify-between gap-3">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="flex-1"
          onClick={() => router.push("/companies")}
        >
          Cancel
        </Button>
        <Button
          size="sm"
          className="flex-1"
          onClick={handleSave}
          disabled={isSaving}
        >
          {isSaving ? (
            <>
              <Loader2 className="size-3.5 mr-1 animate-spin" />
              Saving…
            </>
          ) : (
            <>
              <Save className="size-3.5 mr-1" />
              Save
            </>
          )}
        </Button>
      </div>
    </main>
  );
}
