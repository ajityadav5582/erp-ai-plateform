"use client";

import { Check, ChevronDown, Building2 } from "lucide-react";
import { useGetCompaniesQuery, type Company } from "@/services/company.service";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface CompanySelectProps {
  value?: number;
  onChange: (value: number | undefined) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export function CompanySelect({
  value,
  onChange,
  placeholder = "Select company",
  disabled = false,
  className,
}: CompanySelectProps) {
  const { data: companies, isLoading } = useGetCompaniesQuery();

  const selectedCompany = companies?.find((c) => c.id === value);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          disabled={disabled || isLoading}
          className={cn("justify-between gap-2", className)}
        >
          <span className="flex items-center gap-2 truncate">
            <Building2 className="size-4 text-muted-foreground" />
            {selectedCompany ? (
              <span className="truncate">
                {selectedCompany.companyName} ({selectedCompany.companyCode})
              </span>
            ) : (
              <span className="text-muted-foreground">{placeholder}</span>
            )}
          </span>
          <ChevronDown className="size-4 text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-72">
        <DropdownMenuItem
          onSelect={() => onChange(undefined)}
          className="cursor-pointer"
        >
          <span className={cn(!value ? "font-medium" : "")}>All Companies</span>
          {!value && <Check className="ml-auto size-4" />}
        </DropdownMenuItem>
        {companies?.map((company: Company) => (
          <DropdownMenuItem
            key={company.id}
            onSelect={() => onChange(company.id)}
            className="cursor-pointer"
          >
            <span className={cn(value === company.id ? "font-medium" : "")}>
              {company.companyName}
            </span>
            <span className="text-muted-foreground ml-1">({company.companyCode})</span>
            {value === company.id && <Check className="ml-auto size-4" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
