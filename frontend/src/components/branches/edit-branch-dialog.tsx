"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Building2 } from "lucide-react";
import { toast } from "sonner";
import { useUpdateBranchMutation, type Branch } from "@/services/branch.service";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useGetProvincesQuery,
  useGetDistrictsByProvinceQuery,
  useGetDistrictByIdQuery,
  useGetLocalLevelsByDistrictQuery,
  useGetLocalLevelByMunicipalityIdQuery,
} from "@/services/geography.service";

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";

const editBranchSchema = z.object({
  branchCode: z
    .string()
    .min(1, "Branch code is required")
    .max(50, "Branch code must not exceed 50 characters"),
  branchName: z
    .string()
    .min(1, "Branch name is required")
    .max(200, "Branch name must not exceed 200 characters"),
  email: z
    .string()
    .email("Invalid email address")
    .max(255, "Email must not exceed 255 characters")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .max(50, "Phone must not exceed 50 characters")
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

  const [selectedProvinceId, setSelectedProvinceId] = useState<number | undefined>(undefined);
  const [selectedDistrictId, setSelectedDistrictId] = useState<number | undefined>(undefined);
  const [selectedLocalLevelId, setSelectedLocalLevelId] = useState<string | undefined>(undefined);

  const { data: provincesData } = useGetProvincesQuery();
  const { data: districtsData } = useGetDistrictsByProvinceQuery(selectedProvinceId ?? -1, {
    skip: !selectedProvinceId,
  });
  const { data: localLevelsData } = useGetLocalLevelsByDistrictQuery(selectedDistrictId ?? -1, {
    skip: !selectedDistrictId,
  });

  // Resolve the stored local level (province -> district -> local level) so the
  // cascading dropdowns can be pre-populated when editing an existing branch.
  const storedLocalLevelId = branch?.localLevelId ?? undefined;
  const { data: localLevelData } = useGetLocalLevelByMunicipalityIdQuery(
    storedLocalLevelId ?? "",
    { skip: !storedLocalLevelId }
  );
  const { data: districtData } = useGetDistrictByIdQuery(localLevelData?.districtId ?? -1, {
    skip: !localLevelData?.districtId,
  });

  const form = useForm<EditBranchFormValues>({
    resolver: zodResolver(editBranchSchema),
    defaultValues: {
      branchCode: "",
      branchName: "",
      email: "",
      phone: "",
      address: "",
    },
  });

  useEffect(() => {
    if (branch) {
      form.reset({
        branchCode: branch.branchCode,
        branchName: branch.branchName,
        email: branch.email || "",
        phone: branch.phone || "",
        address: branch.address || "",
      });
      // Reset any previously-selected geography before resolving the new branch.
      setSelectedProvinceId(undefined);
      setSelectedDistrictId(undefined);
      setSelectedLocalLevelId(undefined);
    }
  }, [branch, form]);

  // Once the stored local level and its district have been resolved, seed the
  // cascading dropdowns so province / district / local level are shown.
  useEffect(() => {
    if (!branch || !localLevelData || !districtData) return;
    if (selectedLocalLevelId === localLevelData.municipalityId) return;

    setSelectedProvinceId(districtData.provinceId);
    setSelectedDistrictId(districtData.id);
    setSelectedLocalLevelId(localLevelData.municipalityId);
  }, [branch, localLevelData, districtData, selectedLocalLevelId]);

  const onSubmit = async (values: EditBranchFormValues) => {
    if (!branch) return;
    try {
      const payload = {
        ...values,
        email: values.email || undefined,
        phone: values.phone || undefined,
        address: values.address || undefined,
        localLevelId: selectedLocalLevelId || undefined,
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
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Building2 className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Branch</DialogTitle>
              <DialogDescription>
                Update details for {branch?.branchName} ({branch?.branchCode})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="branchCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Branch Code *</FormLabel>
                    <FormControl>
                      <Input placeholder="NYC-01" disabled={isLoading} {...field} />
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
                    <FormLabel>Branch Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="New York Downtown" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input placeholder="branch@example.com" type="email" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Phone</FormLabel>
                    <FormControl>
                      <Input placeholder="+1 555-0100" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Address</FormLabel>
                  <FormControl>
                    <Input placeholder="123 Main Street" disabled={isLoading} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Province Dropdown */}
            <FormItem>
              <FormLabel>Province</FormLabel>
              <Select
                value={selectedProvinceId?.toString() ?? ""}
                onValueChange={(value) => {
                  const id = Number(value);
                  setSelectedProvinceId(id);
                  setSelectedDistrictId(undefined);
                  setSelectedLocalLevelId(undefined);
                }}
                disabled={isLoading}
              >
                <FormControl>
                  <SelectTrigger disabled={isLoading}>
                    <SelectValue placeholder="Select province" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {provincesData?.map((province) => (
                    <SelectItem key={province.id} value={province.id.toString()}>
                      {province.provinceName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>

            {/* District Dropdown */}
            <FormItem>
              <FormLabel>District</FormLabel>
              <Select
                value={selectedDistrictId?.toString() ?? ""}
                onValueChange={(value) => {
                  const id = Number(value);
                  setSelectedDistrictId(id);
                  setSelectedLocalLevelId(undefined);
                }}
                disabled={!selectedProvinceId || isLoading}
              >
                <FormControl>
                  <SelectTrigger disabled={!selectedProvinceId || isLoading}>
                    <SelectValue placeholder="Select district" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {districtsData?.map((district) => (
                    <SelectItem key={district.id} value={district.id.toString()}>
                      {district.districtName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>

            {/* Local Level Dropdown */}
            <FormItem>
              <FormLabel>Local Level</FormLabel>
              <Select
                value={selectedLocalLevelId ?? ""}
                onValueChange={(value) => {
                  setSelectedLocalLevelId(value);
                }}
                disabled={!selectedDistrictId || isLoading}
              >
                <FormControl>
                  <SelectTrigger disabled={!selectedDistrictId || isLoading}>
                    <SelectValue placeholder="Select local level" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {localLevelsData?.map((localLevel) => (
                    <SelectItem key={localLevel.municipalityId} value={localLevel.municipalityId}>
                      {localLevel.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Saving..." : "Save Changes"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
