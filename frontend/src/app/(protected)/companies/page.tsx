"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Building2, Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { getApiErrorMessage } from "@/services/error-handler";
import { useCreateCompanyMutation, useGetCompaniesQuery, type CompanyType, type FiscalYearType } from "@/services/company.service";

export default function CompaniesPage() {
  const router = useRouter();
  const { data: companies = [], isLoading, isError, refetch } = useGetCompaniesQuery();
  const [createCompany, { isLoading: isCreating }] = useCreateCompanyMutation();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [companyName, setCompanyName] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [city, setCity] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [companyType, setCompanyType] = useState<CompanyType | "">("");
  const [fiscalYearType, setFiscalYearType] = useState<FiscalYearType | "">("");
  const [vatRegistered, setVatRegistered] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    try {
      const createdCompany = await createCompany({
        companyName: companyName.trim(),
        panNumber: panNumber.trim(),
        city: city.trim(),
        phone: phone.trim(),
        email: email.trim(),
        companyType: companyType as CompanyType,
        fiscalYearType: fiscalYearType as FiscalYearType,
        vatRegistered,
      }).unwrap();
      setCompanyName("");
      setPanNumber("");
      setCity("");
      setPhone("");
      setEmail("");
      setCompanyType("");
      setFiscalYearType("");
      setVatRegistered(false);
      setIsCreateOpen(false);
      router.push(`/companies/${createdCompany.id}/fiscal-years`);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  };

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
          <Button onClick={() => { setError(null); setIsCreateOpen(true); }}>
            <Plus /> Create company
          </Button>
        </div>

        {isCreateOpen && (
          <Card className="mb-8 border-primary/20 shadow-sm">
            <CardContent className="pt-6">
              <div className="mb-5 flex items-start justify-between">
                <div>
                  <h2 className="text-lg font-semibold">Create a company</h2>
                  <p className="mt-1 text-sm text-muted-foreground">Add a company to your account.</p>
                </div>
                <Button type="button" variant="ghost" size="icon" aria-label="Close" onClick={() => setIsCreateOpen(false)}><X /></Button>
              </div>
              <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="companyName">Company name</Label>
                  <Input id="companyName" value={companyName} onChange={(event) => setCompanyName(event.target.value)} maxLength={200} required autoFocus placeholder="ABC Sweets Pvt. Ltd." />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="panNumber">PAN number</Label>
                  <Input id="panNumber" value={panNumber} onChange={(event) => setPanNumber(event.target.value)} maxLength={30} required placeholder="Enter PAN number" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="city">City</Label>
                  <Input id="city" value={city} onChange={(event) => setCity(event.target.value)} maxLength={100} required placeholder="Kathmandu" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Phone</Label>
                  <Input id="phone" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={50} required placeholder="+977..." />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={255} required placeholder="company@example.com" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="companyType">Company type</Label>
                  <select id="companyType" className="h-11 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-9" value={companyType} onChange={(event) => setCompanyType(event.target.value as CompanyType | "")} required>
                    <option value="" disabled>Select company type</option>
                    <option value="PRIVATE_LIMITED">Private limited</option>
                    <option value="PUBLIC_LIMITED">Public limited</option>
                    <option value="SOLE_PROPRIETORSHIP">Sole proprietorship</option>
                    <option value="NGO_INGO">NGO / INGO</option>
                    <option value="TRUST">Trust</option>
                    <option value="COOPERATIVE">Cooperative</option>
                    <option value="GOVERNMENT_ENTITY">Government entity</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="fiscalYearType">Fiscal year</Label>
                  <select id="fiscalYearType" className="h-11 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-9" value={fiscalYearType} onChange={(event) => setFiscalYearType(event.target.value as FiscalYearType | "")} required>
                    <option value="" disabled>Select fiscal year</option>
                    <option value="NEPALI_BS">Nepali BS</option>
                    <option value="ENGLISH_AD">English AD</option>
                  </select>
                </div>
                <label className="flex cursor-pointer items-center gap-3 rounded-md border px-3 py-2.5 text-sm sm:col-span-2">
                  <input type="checkbox" checked={vatRegistered} onChange={(event) => setVatRegistered(event.target.checked)} className="size-4 accent-primary" />
                  VAT registered
                </label>
                {error && <p role="alert" className="text-sm text-destructive sm:col-span-2">{error}</p>}
                <div className="flex justify-end gap-3 sm:col-span-2">
                  <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>Cancel</Button>
                  <Button type="submit" disabled={isCreating}>{isCreating ? "Creating…" : "Create company"}</Button>
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
                    <p className="mt-2 text-xs text-muted-foreground">{company.companyType?.replaceAll("_", " ") || "Company type not set"} · {company.fiscalYearType === "NEPALI_BS" ? "Nepali BS" : company.fiscalYearType === "ENGLISH_AD" ? "English AD" : "Fiscal year not set"}</p>
                    <p className="mt-1 text-xs text-muted-foreground">VAT {company.vatRegistered ? "registered" : "not registered"}</p>
                    <span className="mt-3 inline-flex rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-300">{company.status}</span>
                    <Button className="mt-4 w-full" variant="outline" onClick={() => router.push(`/companies/${company.id}/fiscal-years`)}>Select company</Button>
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
