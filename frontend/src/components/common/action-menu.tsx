"use client";

import * as React from "react";
import { MoreHorizontal } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface ActionMenuItem {
  label: string;
  onClick: () => void;
  destructive?: boolean;
  disabled?: boolean;
}

interface ActionMenuProps {
  items: ActionMenuItem[];
  className?: string;
  iconClassName?: string;
}

export function ActionMenu({ items, className, iconClassName }: ActionMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className={cn("h-8 w-8", className)}
          aria-label="Actions"
        >
          <MoreHorizontal className={cn("h-4 w-4", iconClassName)} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        {items.map((item, index) => {
          if (item.label === "separator") {
            return <DropdownMenuSeparator key={`sep-${index}`} />;
          }
          return (
            <DropdownMenuItem
              key={index}
              onClick={item.onClick}
              variant={item.destructive ? "destructive" : "default"}
              disabled={item.disabled}
            >
              {item.label}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
