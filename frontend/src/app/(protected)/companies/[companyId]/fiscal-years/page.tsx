"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, CalendarDays, Check, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { ACTIVE_FISCAL_SELECTION_STORAGE_KEY } from "@/config/company-context";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useActivateCompanyFiscalYearMutation,
  useCreateCompanyFiscalYearMutation,
  useGetCompaniesQuery,
  useGetCompanyFiscalYearsQuery,
  useGetMasterFiscalYearsQuery,
} from "@/services/company.service";

export default function CompanyFiscalYearsPage() {
  const router = useRouter();
  const params = useParams<{ companyId: string }>();
  const companyId = Number(params.companyId);
  const { data: companies = [] } = useGetCompaniesQuery();
  const company = useMemo(() => companies.find((item) => item.id === companyId), [companies, companyId]);
  const { data: years = [], isLoading, isError, refetch } = useGetCompanyFiscalYearsQuery(companyId, { skip: !Number.isFinite(companyId) });
  const { data: masterFiscalYears = [], isLoading: isLoadingMasterYears, isError: isMasterFiscalYearsError, refetch: refetchMasterFiscalYears } = useGetMasterFiscalYearsQuery();
  const [createFiscalYear, { isLoading: isCreating }] = useCreateCompanyFiscalYearMutation();
  const [activateFiscalYear, { isLoading: isActivating }] = useActivateCompanyFiscalYearMutation();
  const [showForm, setShowForm] = useState(false);
  const [masterFiscalYearId, setMasterFiscalYearId] = useState("");
  const [name, setName] = useState("");
  const [startDateBs, setStartDateBs] = useState("");
  const [endDateBs, setEndDateBs] = useState("");
  const [startDateAd, setStartDateAd] = useState("");
  const [endDateAd, setEndDateAd] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const currentYear = years.find((year) => year.active);
    if (currentYear && company) {
      window.localStorage.setItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY, JSON.stringify({
        companyId,
        fiscalYearId: currentYear.id,
        companyName: company.companyName,
        fiscalYearName: currentYear.name,
      }));
    }
  }, [company, companyId, years]);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (!company?.fiscalYearType) {
      setError("This company has no fiscal year calendar configured.");
      return;
    }
    try {
      await createFiscalYear({
        companyId,
        data: {
          masterFiscalYearId: Number(masterFiscalYearId),
          name: name.trim(),
          startDateBs: startDateBs.trim(),
          endDateBs: endDateBs.trim(),
          startDateAd: startDateAd.trim(),
          endDateAd: endDateAd.trim(),
          calendarType: company.fiscalYearType,
        },
      }).unwrap();
      setMasterFiscalYearId(""); setName(""); setStartDateBs(""); setEndDateBs(""); setStartDateAd(""); setEndDateAd(""); setShowForm(false);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  };

  const selectMasterFiscalYear = (value: string) => {
    setMasterFiscalYearId(value);
    const selected = masterFiscalYears.find((item) => item.id === Number(value));
    if (!selected) return;
    setName(selected.name);
    setStartDateBs(selected.startDateBs);
    setEndDateBs(selected.endDateBs);
    setStartDateAd(selected.startDateAd);
    setEndDateAd(selected.endDateAd);
  };

  const activate = async (fiscalYearId: number) => {
    setError(null);
    try {
      const activated = await activateFiscalYear({ companyId, fiscalYearId }).unwrap();
      window.localStorage.setItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY, JSON.stringify({
        companyId,
        fiscalYearId,
        companyName: company?.companyName,
        fiscalYearName: activated.name,
      }));
      router.push("/");
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  };

  return (
    <main className="min-h-screen bg-muted/30">
      <div className="mx-auto max-w-5xl px-5 py-12 sm:px-8">
        <Button variant="ghost" className="mb-6 -ml-3" onClick={() => router.push("/companies")}><ArrowLeft /> Companies</Button>
        <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border bg-background px-3 py-1 text-xs font-medium text-muted-foreground"><CalendarDays className="size-3.5" /> Company setup</div>
            <h1 className="text-3xl font-semibold tracking-tight">Fiscal years</h1>
            <p className="mt-2 text-sm text-muted-foreground">{company?.companyName ?? "Selected company"} · {company?.fiscalYearType === "NEPALI_BS" ? "Nepali BS" : "English AD"}</p>
          </div>
          <Button onClick={() => { setError(null); setShowForm(true); }}><Plus /> Create fiscal year</Button>
        </div>

        {showForm && <Card className="mb-8 border-primary/20 shadow-sm"><CardContent className="pt-6">
          <h2 className="mb-1 text-lg font-semibold">New fiscal year</h2>
          <p className="mb-5 text-sm text-muted-foreground">Choose a master fiscal year to fill its BS and AD dates. You can adjust the values before saving.</p>
          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2 sm:col-span-2"><Label htmlFor="master-fy">Fiscal year</Label><select id="master-fy" className="h-11 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-9" required value={masterFiscalYearId} onChange={(e) => selectMasterFiscalYear(e.target.value)} disabled={isLoadingMasterYears}>
              <option value="" disabled>{isLoadingMasterYears ? "Loading master fiscal years…" : "Select a fiscal year"}</option>
              {masterFiscalYears.map((item) => <option key={item.id} value={item.id}>{item.name} ({item.startDateBs} to {item.endDateBs} BS)</option>)}
            </select><p className="text-xs text-muted-foreground">Selecting a year fills the dates below. You can still adjust them.</p></div>
            {isMasterFiscalYearsError && <div className="flex items-center justify-between text-sm text-destructive sm:col-span-2"><span>Could not load master fiscal years.</span><Button type="button" variant="outline" size="sm" onClick={() => void refetchMasterFiscalYears()}>Try again</Button></div>}
            <div className="space-y-2 sm:col-span-2"><Label htmlFor="fy-name">Name</Label><Input id="fy-name" value={name} onChange={(e) => setName(e.target.value)} required maxLength={50} placeholder="FY 2084/85" /></div>
            <div className="space-y-2"><Label htmlFor="fy-start-bs">Start date (BS)</Label><Input id="fy-start-bs" value={startDateBs} onChange={(e) => setStartDateBs(e.target.value)} required pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}" placeholder="2084-04-01" /></div>
            <div className="space-y-2"><Label htmlFor="fy-end-bs">End date (BS)</Label><Input id="fy-end-bs" value={endDateBs} onChange={(e) => setEndDateBs(e.target.value)} required pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}" placeholder="2085-03-31" /></div>
            <div className="space-y-2"><Label htmlFor="fy-start-ad">Start date (AD)</Label><Input id="fy-start-ad" type="date" value={startDateAd} onChange={(e) => setStartDateAd(e.target.value)} required /></div>
            <div className="space-y-2"><Label htmlFor="fy-end-ad">End date (AD)</Label><Input id="fy-end-ad" type="date" value={endDateAd} onChange={(e) => setEndDateAd(e.target.value)} required /></div>
            {error && <p role="alert" className="text-sm text-destructive sm:col-span-2">{error}</p>}
            <div className="flex justify-end gap-3 sm:col-span-2"><Button type="button" variant="outline" onClick={() => setShowForm(false)}>Cancel</Button><Button type="submit" disabled={isCreating}>{isCreating ? "Creating…" : "Create fiscal year"}</Button></div>
          </form>
        </CardContent></Card>}

        {error && !showForm && <p role="alert" className="mb-4 text-sm text-destructive">{error}</p>}
        <div className="mb-4 flex items-center justify-between"><h2 className="text-sm font-medium text-muted-foreground">Fiscal years ({years.length})</h2>{isError && <Button variant="outline" size="sm" onClick={() => void refetch()}>Try again</Button>}</div>
        {isLoading ? <Card><CardContent className="py-14 text-center text-sm text-muted-foreground">Loading fiscal years…</CardContent></Card>
          : isError ? <Card><CardContent className="py-14 text-center text-sm text-destructive">Could not load fiscal years.</CardContent></Card>
          : years.length === 0 ? <Card><CardContent className="py-14 text-center text-sm text-muted-foreground">No fiscal years yet. Create one to continue setting up this company.</CardContent></Card>
          : <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{years.map((year) => <Card key={year.id} className={year.active ? "border-primary/50" : ""}>
            <CardContent className="pt-6"><div className="flex items-start justify-between gap-3"><div><h3 className="font-semibold">{year.name}</h3><p className="mt-1 text-xs text-muted-foreground">{year.code}</p></div>{year.active && <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-300"><Check className="size-3" /> Active</span>}</div>
              <p className="mt-5 text-sm text-muted-foreground">{year.startDateBs ?? "—"} – {year.endDateBs ?? "—"} <span className="text-xs">(BS)</span></p>
              <p className="mt-1 text-sm text-muted-foreground">{year.startDateAd ?? "—"} – {year.endDateAd ?? "—"} <span className="text-xs">(AD)</span></p>
              {year.active ? <Button className="mt-5 w-full" onClick={() => router.push("/")}>Continue to dashboard</Button> : <Button className="mt-5 w-full" variant="outline" disabled={isActivating} onClick={() => void activate(year.id)}>Set as active</Button>}
            </CardContent>
          </Card>)}</div>}
      </div>
    </main>
  );
}
