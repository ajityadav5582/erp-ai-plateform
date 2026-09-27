import { Badge } from "@/components/ui/badge";

interface PermissionStatusBadgeProps {
  status: string;
}

export function PermissionStatusBadge({ status }: PermissionStatusBadgeProps) {
  const variant = status === "ACTIVE" ? "default" : "secondary";

  return (
    <Badge variant={variant}>
      {status}
    </Badge>
  );
}
