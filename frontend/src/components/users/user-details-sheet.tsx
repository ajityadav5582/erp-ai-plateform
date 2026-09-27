"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { UserStatusBadge } from "./user-status-badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Mail,
  Phone,
  Shield,
  Building,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetUserQuery,
  useActivateUserMutation,
  useDeactivateUserMutation,
} from "@/services/user.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface UserDetailsSheetProps {
  userId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function UserDetailsSheet({
  userId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: UserDetailsSheetProps) {
  const { data: user, isLoading, isError } = useGetUserQuery(userId ?? 0, {
    skip: !userId || !open,
  });

  const [activateUser, { isLoading: isActivating }] = useActivateUserMutation();
  const [deactivateUser, { isLoading: isDeactivating }] = useDeactivateUserMutation();

  const handleActivate = async () => {
    if (!user) return;
    try {
      await activateUser(user.userId).unwrap();
      toast.success(`User ${user.fullName} activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate user", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!user) return;
    try {
      await deactivateUser(user.userId).unwrap();
      toast.success(`User ${user.fullName} deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate user", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const initials = user
    ? `${user.firstName?.[0] ?? ""}${user.lastName?.[0] ?? ""}`.toUpperCase()
    : "U";

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>User Details</SheetTitle>
          <SheetDescription>
            Comprehensive profile and account metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load user details.
          </div>
        )}

        {user && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <Avatar className="size-14 border border-border">
                <AvatarImage src={user.profileImageUrl ?? undefined} alt={user.fullName} />
                <AvatarFallback className="bg-primary/10 text-lg font-bold text-primary">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {user.fullName}
                </h3>
                <p className="truncate text-sm text-muted-foreground">@{user.username}</p>
                <div className="mt-2">
                  <UserStatusBadge status={user.status} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {user.status === "INACTIVE" || user.status === "PENDING_ACTIVATION" ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30"
                  onClick={handleActivate}
                  disabled={isActivating}
                >
                  {isActivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <CheckCircle2 className="mr-1.5 size-4" />}
                  Activate
                </Button>
              ) : user.status === "ACTIVE" ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <PauseCircle className="mr-1.5 size-4" />}
                  Deactivate
                </Button>
              ) : null}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete}>
                <Trash2 className="size-4" />
              </Button>
            </div>

            <Separator />

            {/* Profile fields */}
            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3">
                <Mail className="size-4 shrink-0 text-muted-foreground" />
                <span className="font-medium text-foreground">{user.email}</span>
              </div>

              {user.phoneNumber && (
                <div className="flex items-center gap-3">
                  <Phone className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{user.phoneNumber}</span>
                </div>
              )}

              {(user.roleName || user.roleCode) && (
                <div className="flex items-center gap-3">
                  <Shield className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground font-medium">
                    Role: {user.roleName || user.roleCode}
                  </span>
                </div>
              )}

              {user.departmentId && (
                <div className="flex items-center gap-3">
                  <Building className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Department ID: {user.departmentId}</span>
                </div>
              )}
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>User UUID:</span>
                <span className="font-mono">{user.userId}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{user.tenantId}</span>
              </div>
              {user.lastLoginAt && (
                <div className="flex justify-between">
                  <span>Last Login:</span>
                  <span>{new Date(user.lastLoginAt).toLocaleString()}</span>
                </div>
              )}
              {user.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(user.createdAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
