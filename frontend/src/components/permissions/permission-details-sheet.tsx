"use client";

import { Key, Calendar, Shield } from "lucide-react";
import type { PermissionResponse } from "@/services/permission.service";

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";

interface PermissionDetailsSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  permission: PermissionResponse | null;
}

function formatDate(dateString: string) {
  try {
    const date = new Date(dateString);
    return date.toLocaleString();
  } catch {
    return dateString;
  }
}

export function PermissionDetailsSheet({ open, onOpenChange, permission }: PermissionDetailsSheetProps) {
  if (!permission) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Key className="size-5" />
            </div>
            <div>
              <SheetTitle>Permission Details</SheetTitle>
              <SheetDescription>
                View detailed information about this permission.
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="mt-6 space-y-6">
          <div className="flex items-center gap-3">
            <Badge variant="outline" className="font-mono text-sm">
              {permission.permissionCode}
            </Badge>
            <Badge variant={permission.status === "ACTIVE" ? "default" : "secondary"}>
              {permission.status}
            </Badge>
          </div>

          <Separator />

          <div className="space-y-4">
            <h3 className="text-sm font-medium text-muted-foreground">Permission Information</h3>

            <div className="grid gap-3">
              <div className="flex items-center gap-3 rounded-lg border p-3">
                <Shield className="size-4 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Resource</p>
                  <p className="text-sm text-muted-foreground">{permission.resource}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 rounded-lg border p-3">
                <Key className="size-4 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Action</p>
                  <p className="text-sm text-muted-foreground">{permission.action}</p>
                </div>
              </div>

              {permission.description && (
                <div className="flex items-start gap-3 rounded-lg border p-3">
                  <Shield className="size-4 mt-0.5 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">Description</p>
                    <p className="text-sm text-muted-foreground">{permission.description}</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          <Separator />

          <div className="space-y-4">
            <h3 className="text-sm font-medium text-muted-foreground">Audit Information</h3>

            <div className="grid gap-3">
              <div className="flex items-center gap-3 rounded-lg border p-3">
                <Calendar className="size-4 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Created At</p>
                  <p className="text-sm text-muted-foreground">
                    {formatDate(permission.createdAt)}
                  </p>
                </div>
              </div>

              {permission.updatedAt && (
                <div className="flex items-center gap-3 rounded-lg border p-3">
                  <Calendar className="size-4 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">Last Updated</p>
                    <p className="text-sm text-muted-foreground">
                      {formatDate(permission.updatedAt)}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}
