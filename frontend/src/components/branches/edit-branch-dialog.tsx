"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Building2, MapPin, Layers } from "lucide-react";
import { toast } from "sonner";
import { useUpdateBranchMutation, type Branch } from "@/services/branch.service";
import {
  useGetProvincesQuery,
  useGetDistrictsByProvinceQuery,
  useGetLocalLevelsByDistrictQuery,
  type ProvinceListResponse,
  type DistrictListResponse,
  type LocalLevelListResponse,
} from "@/services/geography.service";
import { getApiErrorMessage } from "@/services/error-handler";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

const selectCls =
  "h-11 w-full rounded-lg border border-input bg-card px-3 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-9 disabled:opacity-50 disabled:cursor-not-allowed transition-colors";

const editBranchSchema = z.object({
  branchCode: z
    .string()
    .min(1, "Branch code is required")
    .max(50, "Branch code must not exceed 50 characters")
    .regex(/^[A-Za-z0-9_-]+$/, "Branch code must only contain letters, numbers, hyphens, and underscores"),
  branchName: z
    .string()
    .min(1, "Branch name is required")
    .max(200, "Branch name must not exceed 200 characters"),
  provinceId: z.number().nullable().optional(),
  districtId: z.number().nullable().optional(),
  localLevelId: z.number().nullable().optional(),
  wardNo: z
    .string()
    .max(50, "Ward number must not exceed 50 characters")
    .optional()
    .or(z.literal("")),
  address: z
    .string()
    .max(500, "Address must not exceed 500 characters")
    .optional()
    .or(z.literal("")),
});

type EditBranchFormValues = z.infer<typeof editBranchSchema>;

interface EditBranchDialogProps {
  branch: Branch | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditBranchDialog({ branch, open, onOpenChange }: EditBranchDialogProps) {
  const [updateBranch, { isLoading }] = useUpdateBranchMutation();

  const form = useForm<EditBranchFormValues>({
    resolver: zodResolver(editBranchSchema),
    defaultValues: {
      branchCode: "",
      branchName: "",
      provinceId: null,
      districtId: null,
      localLevelId: null,
      wardNo: "",
      address: "",
    },
  });

  const provinceId = form.watch("provinceId");
  const districtId = form.watch("districtId");
  const localLevelId = form.watch("localLevelId");

  // RTK Query geography hooks
  const { data: provinces = [], isLoading: provincesLoading } = useGetProvincesQuery();
  const { data: districts = [], isLoading: districtsLoading } = useGetDistrictsByProvinceQuery(
    provinceId ?? 0,
    { skip: !provinceId }
  );
  const { data: localLevels = [], isLoading: localLevelsLoading } = useGetLocalLevelsByDistrictQuery(
    districtId ?? 0,
    { skip: !districtId }
  );

  useEffect(() => {
    if (branch && open) {
      form.reset({
        branchCode: branch.branchCode ?? "",
        branchName: branch.branchName ?? "",
        provinceId: branch.provinceId != null ? Number(branch.provinceId) : null,
        districtId: branch.districtId != null ? Number(branch.districtId) : null,
        localLevelId: branch.localLevelId != null ? Number(branch.localLevelId) : null,
        wardNo: branch.wardNo ?? "",
        address: branch.address ?? "",
      });
    }
  }, [branch, open, form]);

  const onSubmit = async (values: EditBranchFormValues) => {
    if (!branch) return;
    try {
      const payload = {
        branchCode: values.branchCode.trim().toUpperCase(),
        branchName: values.branchName.trim(),
        provinceId: values.provinceId || null,
        districtId: values.districtId || null,
        localLevelId: values.localLevelId || null,
        wardNo: values.wardNo?.trim() || undefined,
        address: values.address?.trim() || undefined,
      };

      const result = await updateBranch({
        branchId: branch.id,
        data: payload,
      }).unwrap();

      toast.success(`Branch "${result.branchName}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update branch", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl p-4 sm:p-6">
        <DialogHeader className="pb-2 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Building2 className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold sm:text-xl">
                Edit Branch: {branch?.branchName}
              </DialogTitle>
              <DialogDescription className="text-xs sm:text-sm">
                Update branch identifiers, administrative geography, and street address.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 pt-2">
            {/* Section 1: Basic Details */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                <Layers className="size-4 text-primary" />
                <span>Basic Details</span>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="branchCode"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs sm:text-sm">
                        Branch Code <span className="text-destructive">*</span>
                      </FormLabel>
                      <FormControl>
                        <Input
                          placeholder="e.g. BR-KTM-01"
                          disabled={isLoading}
                          className="h-11 sm:h-9 uppercase font-mono text-sm"
                          {...field}
                          onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="branchName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs sm:text-sm">
                        Branch Name <span className="text-destructive">*</span>
                      </FormLabel>
                      <FormControl>
                        <Input
                          placeholder="e.g. Kathmandu Main Branch"
                          disabled={isLoading}
                          className="h-11 sm:h-9 text-sm"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* Section 2: Geographic Location */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                <MapPin className="size-4 text-primary" />
                <span>Geographic Location (Administrative)</span>
              </div>
              <p className="text-xs text-muted-foreground">
                Administrative geography mapping for Nepal (Province, District, and Local Level).
              </p>

              {/* Province, District, Local Level Cascading Dropdowns */}
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                {/* Province */}
                <div className="space-y-1.5">
                  <label htmlFor="edit-province" className="text-xs font-medium sm:text-sm text-foreground">
                    Province
                  </label>
                  <select
                    id="edit-province"
                    className={selectCls}
                    value={provinceId ?? ""}
                    onChange={(e) => {
                      const val = e.target.value ? Number(e.target.value) : null;
                      form.setValue("provinceId", val);
                      form.setValue("districtId", null);
                      form.setValue("localLevelId", null);
                    }}
                    disabled={isLoading || provincesLoading}
                  >
                    <option value="">
                      {provincesLoading ? "Loading provinces..." : "Select Province"}
                    </option>
                    {provinces.map((p: ProvinceListResponse) => (
                      <option key={p.id} value={p.id}>
                        {p.provinceName} {p.nepaliName ? `(${p.nepaliName})` : ""}
                      </option>
                    ))}
                  </select>
                </div>

                {/* District */}
                <div className="space-y-1.5">
                  <label htmlFor="edit-district" className="text-xs font-medium sm:text-sm text-foreground">
                    District
                  </label>
                  <select
                    id="edit-district"
                    className={selectCls}
                    value={districtId ?? ""}
                    onChange={(e) => {
                      const val = e.target.value ? Number(e.target.value) : null;
                      form.setValue("districtId", val);
                      form.setValue("localLevelId", null);
                    }}
                    disabled={isLoading || !provinceId || districtsLoading}
                  >
                    <option value="">
                      {!provinceId
                        ? "Select province first"
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

                {/* Local Level (Palika) */}
                <div className="space-y-1.5">
                  <label htmlFor="edit-local-level" className="text-xs font-medium sm:text-sm text-foreground">
                    Local Level (Palika)
                  </label>
                  <select
                    id="edit-local-level"
                    className={selectCls}
                    value={localLevelId ?? ""}
                    onChange={(e) => {
                      const val = e.target.value ? Number(e.target.value) : null;
                      form.setValue("localLevelId", val);
                    }}
                    disabled={isLoading || !districtId || localLevelsLoading}
                  >
                    <option value="">
                      {!districtId
                        ? "Select district first"
                        : localLevelsLoading
                        ? "Loading palikas..."
                        : "Select Local Level"}
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

              {/* Ward No and Street / Address */}
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <FormField
                  control={form.control}
                  name="wardNo"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs sm:text-sm">Ward No.</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="e.g. 4"
                          disabled={isLoading}
                          className="h-11 sm:h-9 text-sm"
                          maxLength={20}
                          {...field}
                          value={field.value ?? ""}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="sm:col-span-2">
                  <FormField
                    control={form.control}
                    name="address"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-xs sm:text-sm">Street Address / Tole</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="e.g. Putalisadak, Ward-4"
                            disabled={isLoading}
                            className="h-11 sm:h-9 text-sm"
                            maxLength={500}
                            {...field}
                            value={field.value ?? ""}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </div>
            </div>

            <DialogFooter className="flex flex-col-reverse gap-2 pt-4 border-t border-border sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
                className="h-11 sm:h-9 w-full sm:w-auto"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isLoading}
                className="h-11 sm:h-9 w-full sm:w-auto shadow-sm"
              >
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Saving Changes..." : "Save Changes"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
