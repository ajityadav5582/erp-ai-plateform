"use client";

import { Check, ChevronDown, Building2 } from "lucide-react";
import { useGetBranchesQuery, type BranchListResponse } from "@/services/branch.service";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface BranchSelectProps {
  value?: number;
  onChange: (value: number | undefined) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export function BranchSelect({
  value,
  onChange,
  placeholder = "Select branch",
  disabled = false,
  className,
}: BranchSelectProps) {
  const { data: branchesData, isLoading } = useGetBranchesQuery({
    page: 0,
    size: 100,
    sort: "branchName,asc",
  });

  const branches = branchesData?.content ?? [];
  const selectedBranch = branches.find((b) => b.id === value);

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
            {selectedBranch ? (
              <span className="truncate">
                {selectedBranch.branchName} ({selectedBranch.branchCode})
              </span>
            ) : (
              <span className="text-muted-foreground">{placeholder}</span>
            )}
          </span>
          <ChevronDown className="size-4 text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-64">
        <DropdownMenuItem
          onSelect={() => onChange(undefined)}
          className="cursor-pointer"
        >
          <span className={cn(!value ? "font-medium" : "")}>All Branches</span>
          {!value && <Check className="ml-auto size-4" />}
        </DropdownMenuItem>
        {branches.map((branch: BranchListResponse) => (
          <DropdownMenuItem
            key={branch.id}
            onSelect={() => onChange(branch.id)}
            className="cursor-pointer"
          >
            <span className={cn(value === branch.id ? "font-medium" : "")}>
              {branch.branchName}
            </span>
            <span className="text-muted-foreground ml-1">({branch.branchCode})</span>
            {value === branch.id && <Check className="ml-auto size-4" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
