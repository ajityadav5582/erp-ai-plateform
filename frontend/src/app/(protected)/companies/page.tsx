"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Building2, Plus, X, MapPin, Briefcase, Settings } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { ACTIVE_FISCAL_SELECTION_STORAGE_KEY } from "@/config/company-context";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useCreateCompanyMutation,
  useGetCompaniesQuery,
  useLazyGetCompanyFiscalYearsQuery,
  type Company,
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

export default function CompaniesPage() {
  const router = useRouter();
  const { data: companies = [], isLoading, isError, refetch } = useGetCompaniesQuery();
  const [createCompany, { isLoading: isCreating }] = useCreateCompanyMutation();
  const [fetchFiscalYears] = useLazyGetCompanyFiscalYearsQuery();
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  // Basic Information
  const [companyName, setCompanyName] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");

  // Business Profile
  const [companyType, setCompanyType] = useState<CompanyType | "">("");
  const [businessType, setBusinessType] = useState<BusinessType | "">("");
  const [fiscalYearType, setFiscalYearType] = useState<FiscalYearType | "">("");
  const [vatRegistered, setVatRegistered] = useState(false);
  const [vatRegistrationNumber, setVatRegistrationNumber] = useState("");

  // Geography & Location
  const [provinceId, setProvinceId] = useState<number | null>(null);
  const [districtId, setDistrictId] = useState<number | null>(null);
  const [localLevelId, setLocalLevelId] = useState<number | null>(null);
  const [wardNo, setWardNo] = useState("");
  const [city, setCity] = useState("");
  const [toleStreet, setToleStreet] = useState("");
  const [address, setAddress] = useState("");

  const [error, setError] = useState<string | null>(null);

  // RTK Query hooks for Nepal administrative geography
  const { data: provinces = [], isLoading: provincesLoading } = useGetProvincesQuery();
  const { data: districts = [], isLoading: districtsLoading } = useGetDistrictsByProvinceQuery(
    provinceId ?? 0,
    { skip: !provinceId }
  );
  const { data: localLevels = [], isLoading: localLevelsLoading } = useGetLocalLevelsByDistrictQuery(
    districtId ?? 0,
    { skip: !districtId }
  );

  const resetForm = () => {
    setCompanyName("");
    setPanNumber("");
    setCity("");
    setPhone("");
    setEmail("");
    setCompanyType("");
    setBusinessType("");
    setFiscalYearType("");
    setVatRegistered(false);
    setVatRegistrationNumber("");
    setProvinceId(null);
    setDistrictId(null);
    setLocalLevelId(null);
    setWardNo("");
    setToleStreet("");
    setAddress("");
    setError(null);
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    const trimmedPan = panNumber.trim();
    if (!/^\d{9}$/.test(trimmedPan)) {
      setError("Nepal PAN number must be exactly 9 numeric digits.");
      return;
    }

    try {
      const createdCompany = await createCompany({
        companyName: companyName.trim(),
        panNumber: trimmedPan,
        city: city.trim(),
        phone: phone.trim(),
        email: email.trim(),
        companyType: companyType as CompanyType,
        businessType: businessType as BusinessType,
        fiscalYearType: fiscalYearType as FiscalYearType,
        vatRegistered,
        vatRegistrationNumber: vatRegistered
          ? (vatRegistrationNumber.trim() || trimmedPan)
          : undefined,
        provinceId: provinceId ?? undefined,
        districtId: districtId ?? undefined,
        localLevelId: localLevelId ?? undefined,
        wardNo: wardNo.trim() || undefined,
        toleStreet: toleStreet.trim() || undefined,
        address: address.trim() || undefined,
      }).unwrap();
      resetForm();
      setIsCreateOpen(false);
      refetch();
      toast.success("Company created! Head office, warehouse & active fiscal year configured.");

      try {
        const years = await fetchFiscalYears(createdCompany.id).unwrap();
        const activeYear = years.find((y) => y.active) || years[0];
        if (activeYear) {
          window.localStorage.setItem(
            ACTIVE_FISCAL_SELECTION_STORAGE_KEY,
            JSON.stringify({
              companyId: createdCompany.id,
              fiscalYearId: activeYear.id,
              companyName: createdCompany.companyName,
              fiscalYearName: activeYear.name,
              businessType: createdCompany.businessType ?? null,
            })
          );
          router.push("/");
          return;
        }
      } catch {
        // Fallback to fiscal years page if needed
      }
      router.push(`/companies/${createdCompany.id}/fiscal-years`);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  };

  const handleSelectCompany = async (targetCompany: Company) => {
    try {
      const years = await fetchFiscalYears(targetCompany.id).unwrap();
      const activeYear = years.find((y) => y.active) || years[0];
      if (activeYear) {
        window.localStorage.setItem(
          ACTIVE_FISCAL_SELECTION_STORAGE_KEY,
          JSON.stringify({
            companyId: targetCompany.id,
            fiscalYearId: activeYear.id,
            companyName: targetCompany.companyName,
            fiscalYearName: activeYear.name,
            businessType: targetCompany.businessType ?? null,
          })
        );
        toast.success(`Switched to ${targetCompany.companyName}`);
        router.push("/");
        return;
      }
    } catch {
      // Fallback
    }
    router.push(`/companies/${targetCompany.id}/fiscal-years`);
  };

  const selectCls =
    "h-11 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-9 disabled:opacity-50 disabled:cursor-not-allowed";

  return (
    <main className="min-h-screen bg-muted/30">
      <div className="mx-auto max-w-6xl px-5 py-12 sm:px-8">
        <div className="mb-10 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border bg-background px-3 py-1 text-xs font-medium text-muted-foreground">
              <Building2 className="size-3.5" /> Your workspace
            </div>
            <h1 className="text-3xl font-semibold tracking-tight">Your companies</h1>
            <p className="mt-2 max-w-xl text-sm text-muted-foreground">
              Create and manage the companies in your account. Only companies belonging to your account are shown here.
            </p>
          </div>
          <Button
            onClick={() => {
              resetForm();
              setIsCreateOpen(true);
            }}
          >
            <Plus /> Create company
          </Button>
        </div>

        {isCreateOpen && (
          <Card className="mb-8 border-primary/20 shadow-md">
            <CardContent className="pt-6">
              <div className="mb-6 flex items-start justify-between border-b pb-4">
                <div>
                  <h2 className="text-xl font-semibold text-foreground">Create a company</h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Configure company details, fiscal configuration, and geographic location.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label="Close"
                  onClick={() => setIsCreateOpen(false)}
                >
                  <X className="size-4" />
                </Button>
              </div>

              <form onSubmit={submit} className="space-y-6">
                {/* Section 1: Basic Information */}
                <div>
                  <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
                    <Building2 className="size-4 text-primary" />
                    <span>Basic Details</span>
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <Label htmlFor="companyName">
                        Company Name <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="companyName"
                        value={companyName}
                        onChange={(e) => setCompanyName(e.target.value)}
                        maxLength={200}
                        required
                        autoFocus
                        placeholder="e.g. Himalayan Enterprises Pvt. Ltd."
                      />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="panNumber">
                        PAN Number <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="panNumber"
                        value={panNumber}
                        onChange={(e) => {
                          const val = e.target.value.replace(/\D/g, "").slice(0, 9);
                          setPanNumber(val);
                          if (vatRegistered && (!vatRegistrationNumber || vatRegistrationNumber === panNumber)) {
                            setVatRegistrationNumber(val);
                          }
                        }}
                        maxLength={9}
                        required
                        placeholder="9-digit PAN (e.g. 601234567)"
                      />
                      {panNumber.length > 0 && panNumber.length < 9 && (
                        <p className="text-[11px] text-amber-600 dark:text-amber-400">
                          Nepal PAN must be exactly 9 digits ({panNumber.length}/9 entered)
                        </p>
                      )}
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="phone">
                        Phone <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="phone"
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        maxLength={50}
                        required
                        placeholder="+977-98..."
                      />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="email">
                        Email <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        maxLength={255}
                        required
                        placeholder="contact@company.com"
                      />
                    </div>
                  </div>
                </div>

                {/* Section 2: Business & Fiscal Profile */}
                <div>
                  <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
                    <Briefcase className="size-4 text-primary" />
                    <span>Business & Tax Profile</span>
                  </div>
                  <div className="grid gap-4 sm:grid-cols-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="companyType">
                        Company Type <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="companyType"
                        className={selectCls}
                        value={companyType}
                        onChange={(e) => setCompanyType(e.target.value as CompanyType | "")}
                        required
                      >
                        <option value="" disabled>Select company type</option>
                        <option value="PRIVATE_LIMITED">Private Limited</option>
                        <option value="PUBLIC_LIMITED">Public Limited</option>
                        <option value="SOLE_PROPRIETORSHIP">Sole Proprietorship</option>
                        <option value="NGO_INGO">NGO / INGO</option>
                        <option value="TRUST">Trust</option>
                        <option value="COOPERATIVE">Cooperative</option>
                        <option value="GOVERNMENT_ENTITY">Government Entity</option>
                      </select>
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="businessType">
                        Business Type <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="businessType"
                        className={selectCls}
                        value={businessType}
                        onChange={(e) => setBusinessType(e.target.value as BusinessType | "")}
                        required
                      >
                        <option value="" disabled>Select business type</option>
                        <optgroup label="Food & Hospitality">
                          <option value="RESTAURANT">Restaurant</option>
                          <option value="CAFE">Cafe</option>
                          <option value="HOTEL">Hotel</option>
                          <option value="SWEETS_RETAIL">Sweets Retail</option>
                        </optgroup>
                        <optgroup label="Retail & Wholesale">
                          <option value="GENERAL_RETAIL">General Retail</option>
                          <option value="GROCERY">Grocery</option>
                          <option value="HARDWARE">Hardware</option>
                          <option value="SANITARY">Sanitary</option>
                          <option value="CLOTHING">Clothing</option>
                        </optgroup>
                        <optgroup label="Healthcare">
                          <option value="PHARMACY">Pharmacy</option>
                        </optgroup>
                        <optgroup label="Services & Other">
                          <option value="SERVICES">Services</option>
                          <option value="OTHER">Other</option>
                        </optgroup>
                      </select>
                    </div>

                    <div className="space-y-1.5">
                      <Label htmlFor="fiscalYearType">
                        Fiscal Year Calendar <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="fiscalYearType"
                        className={selectCls}
                        value={fiscalYearType}
                        onChange={(e) => setFiscalYearType(e.target.value as FiscalYearType | "")}
                        required
                      >
                        <option value="" disabled>Select fiscal year</option>
                        <option value="NEPALI_BS">Nepali (Bikram Sambat)</option>
                        <option value="ENGLISH_AD">English (Gregorian AD)</option>
                      </select>
                    </div>
                  </div>

                  <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <label className="flex cursor-pointer items-center gap-3 rounded-lg border bg-card px-3 py-2.5 text-sm transition-colors hover:bg-muted/50">
                      <input
                        type="checkbox"
                        checked={vatRegistered}
                        onChange={(e) => {
                          const checked = e.target.checked;
                          setVatRegistered(checked);
                          if (checked && !vatRegistrationNumber.trim()) {
                            setVatRegistrationNumber(panNumber.trim());
                          }
                        }}
                        className="size-4 accent-primary"
                      />
                      <div>
                        <p className="font-medium">VAT Registered</p>
                        <p className="text-xs text-muted-foreground">Entity is registered under Nepal IRD VAT</p>
                      </div>
                    </label>

                    {vatRegistered && (
                      <div className="space-y-1.5">
                        <Label htmlFor="vatRegistrationNumber">VAT Registration Number</Label>
                        <Input
                          id="vatRegistrationNumber"
                          value={vatRegistrationNumber}
                          onChange={(e) => setVatRegistrationNumber(e.target.value)}
                          maxLength={30}
                          placeholder="Defaults to PAN if identical"
                        />
                      </div>
                    )}
                  </div>
                </div>

                {/* Section 3: Geographic Address & Bootstrap Branch */}
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                      <MapPin className="size-4 text-primary" />
                      <span>Location & Primary Head Office Branch</span>
                    </div>
                  </div>
                  <p className="mb-4 text-xs text-muted-foreground">
                    This location will be used to automatically configure your primary Head Office branch and default warehouse.
                  </p>

                  <div className="grid gap-4 sm:grid-cols-3">
                    {/* Province Cascading Dropdown */}
                    <div className="space-y-1.5">
                      <Label htmlFor="province">
                        Province <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="province"
                        className={selectCls}
                        value={provinceId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value;
                          setProvinceId(val ? Number(val) : null);
                          setDistrictId(null);
                          setLocalLevelId(null);
                        }}
                        required
                        disabled={provincesLoading}
                      >
                        <option value="" disabled>
                          {provincesLoading ? "Loading provinces..." : "Select Province"}
                        </option>
                        {provinces.map((p: ProvinceListResponse) => (
                          <option key={p.id} value={p.id}>
                            {p.provinceName} {p.nepaliName ? `(${p.nepaliName})` : ""}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* District Cascading Dropdown */}
                    <div className="space-y-1.5">
                      <Label htmlFor="district">
                        District <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="district"
                        className={selectCls}
                        value={districtId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value;
                          setDistrictId(val ? Number(val) : null);
                          setLocalLevelId(null);
                        }}
                        required
                        disabled={!provinceId || districtsLoading}
                      >
                        <option value="" disabled>
                          {!provinceId
                            ? "Select a province first"
                            : districtsLoading
                            ? "Loading districts..."
                            : "Select District"}
                        </option>
                        {districts.map((d: DistrictListResponse) => (
                          <option key={d.id} value={d.id}>
                            {d.districtName} {d.nepaliName ? `(${d.nepaliName})` : ""}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Local Level Cascading Dropdown */}
                    <div className="space-y-1.5">
                      <Label htmlFor="localLevel">
                        Local Level (Palika) <span className="text-destructive">*</span>
                      </Label>
                      <select
                        id="localLevel"
                        className={selectCls}
                        value={localLevelId ?? ""}
                        onChange={(e) => {
                          const val = e.target.value;
                          setLocalLevelId(val ? Number(val) : null);
                        }}
                        required
                        disabled={!districtId || localLevelsLoading}
                      >
                        <option value="" disabled>
                          {!districtId
                            ? "Select a district first"
                            : localLevelsLoading
                            ? "Loading palikas..."
                            : "Select Local Level (Palika)"}
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

                  <div className="mt-4 grid gap-4 sm:grid-cols-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="wardNo">Ward No.</Label>
                      <Input
                        id="wardNo"
                        value={wardNo}
                        onChange={(e) => setWardNo(e.target.value)}
                        maxLength={20}
                        placeholder="e.g. 4"
                      />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="city">
                        City / Town <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="city"
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        maxLength={100}
                        required
                        placeholder="e.g. Kathmandu"
                      />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="toleStreet">Tole / Street</Label>
                      <Input
                        id="toleStreet"
                        value={toleStreet}
                        onChange={(e) => setToleStreet(e.target.value)}
                        maxLength={200}
                        placeholder="e.g. New Road"
                      />
                    </div>
                  </div>

                  <div className="mt-4 space-y-1.5">
                    <Label htmlFor="address">Full Street Address (Optional)</Label>
                    <Input
                      id="address"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      maxLength={500}
                      placeholder="e.g. Ward No 4, New Road, Kathmandu"
                    />
                  </div>
                </div>

                {error && (
                  <div role="alert" className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-sm text-destructive">
                    {error}
                  </div>
                )}

                <div className="flex justify-end gap-3 border-t pt-4">
                  <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={isCreating}>
                    {isCreating ? "Creating company & bootstrapping branch…" : "Create Company"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        )}

        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-medium text-muted-foreground">Companies ({companies.length})</h2>
          {isError && <Button variant="outline" size="sm" onClick={() => void refetch()}>Try again</Button>}
        </div>

        {isLoading ? (
          <Card><CardContent className="py-14 text-center text-sm text-muted-foreground">Loading your companies…</CardContent></Card>
        ) : isError ? (
          <Card><CardContent className="py-14 text-center text-sm text-destructive">Could not load companies. Please try again.</CardContent></Card>
        ) : companies.length === 0 ? (
          <Card><CardContent className="flex flex-col items-center py-16 text-center">
            <div className="mb-4 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"><Building2 className="size-6" /></div>
            <h3 className="font-semibold">No companies yet</h3>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">Create your first company to start setting up your workspace.</p>
            <Button className="mt-5" onClick={() => setIsCreateOpen(true)}><Plus /> Create company</Button>
          </CardContent></Card>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {companies.map((company) => (
              <Card key={company.id} className="transition-shadow hover:shadow-md">
                <CardContent className="flex items-start gap-4 pt-6">
                  <div className="flex size-11 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary"><Building2 className="size-5" /></div>
                  <div className="min-w-0">
                    <h3 className="truncate font-semibold">{company.companyName}</h3>
                    <p className="mt-1 text-sm text-muted-foreground">PAN {company.panNumber || "not set"} · {company.city || "City not set"}</p>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {company.companyType?.replaceAll("_", " ") || "Company type not set"} · {company.businessType?.replaceAll("_", " ") || "Business type not set"} · {company.fiscalYearType === "NEPALI_BS" ? "Nepali BS" : company.fiscalYearType === "ENGLISH_AD" ? "English AD" : "Fiscal year not set"}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">VAT {company.vatRegistered ? "registered" : "not registered"}</p>
                    <span className="mt-3 inline-flex rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-300">{company.status}</span>
                    <div className="mt-4 flex flex-col gap-2 sm:flex-row">
                      <Button
                        className="flex-1 w-full"
                        variant="outline"
                        onClick={() => void handleSelectCompany(company)}
                      >
                        Select company
                      </Button>
                      <Button
                        variant="secondary"
                        className="w-full sm:w-auto"
                        onClick={() => router.push(`/companies/${company.id}/settings`)}
                      >
                        <Settings className="size-4 mr-1.5" />
                        Settings
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
