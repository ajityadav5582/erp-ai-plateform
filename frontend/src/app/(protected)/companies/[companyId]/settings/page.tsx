"use client";

import { useEffect, useState } from "react";
import { skipToken } from "@reduxjs/toolkit/query";
import { useParams, useRouter } from "next/navigation";
import { ChevronLeft, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { getApiErrorMessage } from "@/services/error-handler";
import { useGetCompaniesQuery, useUpdateCompanyMutation, type BusinessType, type CompanyType, type FiscalYearType } from "@/services/company.service";
import { useGetProvincesQuery, useGetDistrictsByProvinceQuery, useGetLocalLevelsByDistrictQuery, type ProvinceListResponse, type DistrictListResponse, type LocalLevelListResponse } from "@/services/geography.service";

const NAV_GROUPS = [
  { label: "GENERAL", items: [{ key: "profile", label: "Company Profile" }, { key: "address", label: "Address & Contact" }, { key: "regional", label: "Regional Settings" }] },
  { label: "FINANCIAL & TAX", items: [{ key: "tax", label: "Tax Registration" }, { key: "fiscal", label: "Fiscal Years" }, { key: "invoice", label: "Invoice Settings" }] },
  { label: "INTEGRATIONS", items: [{ key: "ird", label: "IRD / CBMS Configuration" }] },
];

const COMPANY_TYPES = [
  { value: "PRIVATE_LIMITED", label: "Private limited" },
  { value: "PUBLIC_LIMITED", label: "Public limited" },
  { value: "SOLE_PROPRIETORSHIP", label: "Sole proprietorship" },
  { value: "NGO_INGO", label: "NGO / INGO" },
  { value: "TRUST", label: "Trust" },
  { value: "COOPERATIVE", label: "Cooperative" },
  { value: "GOVERNMENT_ENTITY", label: "Government entity" },
];

function Field({ label, htmlFor, children, hint, disabled }: { label: string; htmlFor: string; children: React.ReactNode; hint?: string; disabled?: boolean }) {
  return (
    <div className="space-y-1.5" aria-disabled={disabled || undefined}>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}

const BUSINESS_TYPES = [
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

export default function CompanySettingsPage() {
  const params = useParams();
  const router = useRouter();
  const companyId = Number(params?.companyId);
  const { data: companies = [], isLoading: companiesLoading } = useGetCompaniesQuery();
  const company = companies.find((c) => c.id === companyId);
  const isLoading = companiesLoading;
  const [updateCompany, { isLoading: isSaving }] = useUpdateCompanyMutation();
  const [active, setActive] = useState("profile");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Form state
  const [companyName, setCompanyName] = useState("");
  const [legalName, setLegalName] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [companyType, setCompanyType] = useState<CompanyType | "">("");
  const [businessType, setBusinessType] = useState<BusinessType | "">("");
  const [city, setCity] = useState("");
  const [province, setProvince] = useState<number | null>(null);
  const [district, setDistrict] = useState<number | null>(null);
  const [localLevel, setLocalLevel] = useState<string | null>(null);
  const [wardNo, setWardNo] = useState("");
  const [toleStreet, setToleStreet] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [website, setWebsite] = useState("");
  const [address, setAddress] = useState("");
  const [fiscalYearType, setFiscalYearType] = useState<FiscalYearType | "">("");
  const [vatRegistered, setVatRegistered] = useState(false);
  const [vatRegistrationNumber, setVatRegistrationNumber] = useState("");
  const [vatEffectiveFrom, setVatEffectiveFrom] = useState("");
  const [vatEffectiveTo, setVatEffectiveTo] = useState("");

  useEffect(() => {
    if (company) {
      setCompanyName(company.companyName ?? "");
      setLegalName(company.legalName ?? "");
      setPanNumber(company.panNumber ?? "");
      setCompanyType(company.companyType ?? "");
      setBusinessType(company.businessType ?? "");
      setCity(company.city ?? "");
      setWardNo(company.wardNo ?? "");
      setToleStreet(company.toleStreet ?? "");
      setPhone(company.phone ?? "");
      setEmail(company.email ?? "");
      setWebsite(company.website ?? "");
      setAddress(company.address ?? "");
      setFiscalYearType(company.fiscalYearType ?? "");
      setVatRegistered(Boolean(company.vatRegistered));
      setVatRegistrationNumber(company.vatRegistrationNumber ?? "");
      setVatEffectiveFrom(company.vatEffectiveFrom ?? "");
      setVatEffectiveTo(company.vatEffectiveTo ?? "");
    }
  }, [company]);

  const { data: provinces = [], isLoading: _provincesLoading } = useGetProvincesQuery();
  const { data: districts = [], isLoading: _districtsLoading } = useGetDistrictsByProvinceQuery(province ?? skipToken);
  const { data: localLevels = [], isLoading: _localLevelsLoading } = useGetLocalLevelsByDistrictQuery(district ?? skipToken);

  const save = async () => {
    setError(null);
    setSuccess(null);
    try {
      await updateCompany({
        id: companyId,
        data: {
          companyName: companyName.trim() || undefined,
          legalName: legalName.trim() || undefined,
          panNumber: panNumber.trim() || undefined,
          city: city.trim() || undefined,
          wardNo: wardNo.trim() || undefined,
          toleStreet: toleStreet.trim() || undefined,
          phone: phone.trim() || undefined,
          email: email.trim() || undefined,
          website: website.trim() || undefined,
          address: address.trim() || undefined,
          companyType: companyType ? (companyType as CompanyType) : undefined,
          businessType: businessType ? (businessType as BusinessType) : undefined,
          fiscalYearType: fiscalYearType ? (fiscalYearType as FiscalYearType) : undefined,
          vatRegistered,
          vatRegistrationNumber: vatRegistered ? vatRegistrationNumber.trim() : undefined,
          vatEffectiveFrom: vatRegistered && vatEffectiveFrom ? vatEffectiveFrom : undefined,
          vatEffectiveTo: vatRegistered && vatEffectiveTo ? vatEffectiveTo : undefined,
        },
      }).unwrap();
      setSuccess("Saved successfully.");
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  };

  const inputCls = "h-9 w-full rounded-md border bg-background px-3 text-sm";

  return (
    <main className="min-h-screen bg-muted/30">
      <div className="mx-auto max-w-6xl px-5 py-8 sm:px-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => router.push("/companies")}>
            <ChevronLeft className="size-4" /> Back
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">{company?.companyName ?? "Company Settings"}</h1>
            <p className="text-sm text-muted-foreground">Manage company configuration</p>
          </div>
        </div>

        {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
        {success && <p className="mb-4 text-sm text-emerald-600">{success}</p>}

        <div className="flex flex-col gap-6 md:flex-row">
          <aside className="w-full md:w-60 shrink-0">
            <Card>
              <CardContent className="p-2">
                {NAV_GROUPS.map((group) => (
                  <div key={group.label} className="mb-4">
                    <p className="px-3 py-2 text-xs font-semibold text-muted-foreground">{group.label}</p>
                    <nav className="space-y-1">
                      {group.items.map((item) => (
                        <button
                          key={item.key}
                          onClick={() => setActive(item.key)}
                          className={`w-full rounded-md px-3 py-2 text-left text-sm transition-colors ${active === item.key ? "bg-primary/10 text-primary font-medium" : "hover:bg-muted"}`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </nav>
                  </div>
                ))}
              </CardContent>
            </Card>
          </aside>

          <section className="flex-1">
            <Card>
              <CardContent className="p-6 space-y-4">
                {isLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
                {!isLoading && company && (
                  <>
                    {active === "profile" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Company Profile</h2>
                        <div className="grid gap-4 sm:grid-cols-2">
                          <div className="space-y-1.5"><Label>Company Name</Label><Input value={companyName} onChange={(e) => setCompanyName(e.target.value)} /></div>
                          <div className="space-y-1.5"><Label>Legal Name</Label><Input value={legalName} onChange={(e) => setLegalName(e.target.value)} /></div>
                          <div className="space-y-1.5"><Label>PAN Number</Label><Input value={panNumber} onChange={(e) => setPanNumber(e.target.value.replace(/\D/g, "").slice(0, 9))} /></div>
                          <div className="space-y-1.5"><Label>Company Type</Label><select className={inputCls} value={companyType} onChange={(e) => setCompanyType(e.target.value as CompanyType | "")}><option value="">Select</option>{COMPANY_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}</select></div>
                          <div className="space-y-1.5"><Label>Business Type</Label><select className={inputCls} value={businessType} onChange={(e) => setBusinessType(e.target.value as BusinessType | "")}><option value="">Select</option>{BUSINESS_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}</select></div>
                          <div className="space-y-1.5"><Label>Status</Label><p className="text-sm">{company.status}</p></div>
                        </div>
                      </div>
                    )}
                    {active === "address" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Address & Contact</h2>
                        <div className="grid gap-4 sm:grid-cols-2">
                          <div className="space-y-1.5"><Label>City</Label><Input value={city} onChange={(e) => setCity(e.target.value)} /></div>
                          <Field label="Province" htmlFor="province">
                            <select id="province" className="h-9 w-full rounded-md border bg-background px-3 text-sm" value={province ?? ""} onChange={(e) => { const v = e.target.value; setProvince(v ? Number(v) : null); setDistrict(null); setLocalLevel(null); setWardNo(""); setToleStreet(""); }}>
                              <option value="" disabled>Select province</option>
                              {provinces.map((p: ProvinceListResponse) => <option key={p.id} value={p.id ?? ""}>{p.provinceName}</option>)}
                            </select>
                          </Field>
                          <Field label="District" htmlFor="district" disabled={!province}>
                            <select id="district" className="h-9 w-full rounded-md border bg-background px-3 text-sm" value={district ?? ""} onChange={(e) => { const v = e.target.value; setDistrict(v ? Number(v) : null); setLocalLevel(null); setWardNo(""); setToleStreet(""); }} disabled={!province}>
                              <option value="" disabled>Select district</option>
                              {districts.map((d: DistrictListResponse) => <option key={d.id} value={d.id ?? ""}>{d.districtName}</option>)}
                            </select>
                            {!province && <p className="text-xs text-muted-foreground">Select a province first</p>}
                          </Field>
                          <Field label="Municipality / Rural Municipality" htmlFor="localLevel" disabled={!district}>
                            <select id="localLevel" className="h-9 w-full rounded-md border bg-background px-3 text-sm" value={localLevel ?? ""} onChange={(e) => { const v = e.target.value; setLocalLevel(v ? v : null); }} disabled={!district}>
                              <option value="" disabled>Select local level</option>
                              {localLevels.map((l: LocalLevelListResponse) => <option key={l.municipalityId} value={l.municipalityId ?? ""}>{l.name}</option>)}
                            </select>
                            {!district && <p className="text-xs text-muted-foreground">Select a district first</p>}
                          </Field>
                          <Field label="Ward No." htmlFor="wardNo" hint="Mandatory for IRD tax audit certificates.">
                            <Input id="wardNo" type="text" value={wardNo} onChange={(e) => setWardNo(e.target.value)} maxLength={20} placeholder="e.g. 5" />
                          </Field>
                          <Field label="Tole / Street" htmlFor="toleStreet">
                            <Input id="toleStreet" value={toleStreet} onChange={(e) => setToleStreet(e.target.value)} maxLength={200} placeholder="Tole name or street" />
                          </Field>
                          <div className="space-y-1.5"><Label>Phone</Label><Input value={phone} onChange={(e) => setPhone(e.target.value)} /></div>
                          <div className="space-y-1.5"><Label>Email</Label><Input value={email} onChange={(e) => setEmail(e.target.value)} /></div>
                          <div className="space-y-1.5"><Label>Website</Label><Input value={website} onChange={(e) => setWebsite(e.target.value)} /></div>
                          <div className="space-y-1.5 sm:col-span-2"><Label>Address</Label><Input value={address} onChange={(e) => setAddress(e.target.value)} /></div>
                        </div>
                      </div>
                    )}
                    {active === "regional" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Regional Settings</h2>
                        <div className="grid gap-4 sm:grid-cols-2">
                          <div className="space-y-1.5"><Label>Calendar System</Label><select className={inputCls} value={fiscalYearType} onChange={(e) => setFiscalYearType(e.target.value as FiscalYearType | "")}><option value="NEPALI_BS">Nepali BS</option><option value="ENGLISH_AD">English AD</option></select></div>
                          <div className="space-y-1.5"><Label>Currency</Label><Input value="NPR" disabled /></div>
                          <div className="space-y-1.5"><Label>Timezone</Label><Input value="Asia/Kathmandu" disabled /></div>
                        </div>
                      </div>
                    )}
                    {active === "tax" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Tax Registration</h2>
                        <label className="flex cursor-pointer items-center gap-3 rounded-md border px-3 py-2.5 text-sm">
                          <input type="checkbox" checked={vatRegistered} onChange={(e) => setVatRegistered(e.target.checked)} className="size-4 accent-primary" />
                          VAT registered
                        </label>
                        {vatRegistered && (
                          <div className="grid gap-4 sm:grid-cols-2">
                            <div className="space-y-1.5"><Label>VAT Registration Number</Label><Input value={vatRegistrationNumber} onChange={(e) => setVatRegistrationNumber(e.target.value)} /></div>
                            <div className="space-y-1.5"><Label>VAT Effective From</Label><Input type="date" value={vatEffectiveFrom} onChange={(e) => setVatEffectiveFrom(e.target.value)} /></div>
                            <div className="space-y-1.5"><Label>VAT Effective To</Label><Input type="date" value={vatEffectiveTo} onChange={(e) => setVatEffectiveTo(e.target.value)} /></div>
                          </div>
                        )}
                      </div>
                    )}
                    {active === "fiscal" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Fiscal Years</h2>
                        <p className="text-sm text-muted-foreground">Manage fiscal years for this company.</p>
                        <Button variant="outline" onClick={() => router.push(`/companies/${companyId}/fiscal-years`)}>Open Fiscal Years</Button>
                      </div>
                    )}
                    {active === "invoice" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">Invoice Settings</h2>
                        <p className="text-sm text-muted-foreground">No additional invoice settings available.</p>
                      </div>
                    )}
                    {active === "ird" && (
                      <div className="space-y-4">
                        <h2 className="text-lg font-semibold">IRD / CBMS Configuration</h2>
                        <p className="text-sm text-muted-foreground">Configure IRD integration.</p>
                        <Button variant="outline" onClick={() => router.push(`/administrations/company-settings/ird?companyId=${companyId}`)}>Open IRD Settings</Button>
                      </div>
                    )}

                    <div className="flex justify-end gap-3 pt-4">
                      <Button variant="outline" onClick={() => router.push("/companies")}>Cancel</Button>
                      <Button disabled={isSaving} onClick={save}>
                        <Save className="size-4" /> {isSaving ? "Saving…" : "Save Changes"}
                      </Button>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>
          </section>
        </div>
      </div>
    </main>
  );
}
