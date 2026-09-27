import { Badge } from "@/components/ui/badge";
import type { RoleType, RoleStatus } from "@/services/role.service";

interface RoleStatusBadgeProps {
  status: RoleStatus;
  roleType: RoleType;
}

export function RoleStatusBadge({ status, roleType }: RoleStatusBadgeProps) {
  const isActive = status === "ACTIVE";
  const isSystem = roleType === "SYSTEM";

  if (isSystem) {
    return (
      <Badge variant="outline" className="border-purple-500/30 bg-purple-50 text-purple-700 dark:bg-purple-950/30 dark:text-purple-300">
        System
      </Badge>
    );
  }

  if (isActive) {
    return (
      <Badge variant="outline" className="border-emerald-500/30 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300">
        Active
      </Badge>
    );
  }

  return (
    <Badge variant="outline" className="border-amber-500/30 bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300">
      Inactive
    </Badge>
  );
}
