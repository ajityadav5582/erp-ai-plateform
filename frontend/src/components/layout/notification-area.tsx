"use client";

import { useState, useEffect } from "react";
import { Bell, CheckCheck, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export interface Notification {
  id: string;
  title: string;
  description: string;
  timestamp: Date;
  read: boolean;
  type?: "info" | "success" | "warning" | "error";
}

interface NotificationAreaProps {
  className?: string;
}

const INITIAL_NOTIFICATIONS: Notification[] = [
  {
    id: "1",
    title: "System Update",
    description: "Platform maintenance scheduled for tonight at 2:00 AM UTC.",
    timestamp: new Date(Date.now() - 1000 * 60 * 30),
    read: false,
    type: "info",
  },
  {
    id: "2",
    title: "New Report Available",
    description: "Q3 financial report is ready for review.",
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2),
    read: false,
    type: "success",
  },
  {
    id: "3",
    title: "Inventory Alert",
    description: "Low stock warning for SKU-4821 in Warehouse B.",
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 5),
    read: true,
    type: "warning",
  },
];

function formatRelativeTime(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return "Just now";
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString();
}

function getBadgeVariant(type?: Notification["type"]) {
  switch (type) {
    case "success":
      return "default";
    case "warning":
      return "secondary";
    case "error":
      return "destructive";
    default:
      return "outline";
  }
}

export function NotificationArea({ className }: NotificationAreaProps) {
  const [notifications, setNotifications] = useState<Notification[]>(INITIAL_NOTIFICATIONS);
  const [open, setOpen] = useState(false);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    setNotifications((prev) =>
      prev.map((n) => ({ ...n, read: true }))
    );
  };

  const dismissNotification = (id: string) => {
    setNotifications((prev) =>
      prev.filter((n) => n.id !== id)
    );
  };

  // Close dropdown when pressing Escape
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className={cn("relative", className)}
          aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-1 -right-1 h-5 min-w-5 rounded-full px-1 text-xs"
            >
              {unreadCount > 9 ? "9+" : unreadCount}
            </Badge>
          )}
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-80">
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Notifications</span>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto p-1 text-xs text-muted-foreground hover:text-foreground"
              onClick={(e) => {
                e.stopPropagation();
                markAllAsRead();
              }}
            >
              <CheckCheck className="mr-1 h-3.5 w-3.5" />
              Mark all read
            </Button>
          )}
        </DropdownMenuLabel>

        <DropdownMenuSeparator />

        {notifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center text-sm text-muted-foreground">
            <Bell className="mb-2 h-8 w-8 opacity-50" />
            <p>No notifications</p>
          </div>
        ) : (
          <div className="max-h-80 overflow-y-auto">
            {notifications.map((notification) => (
              <DropdownMenuItem
                key={notification.id}
                className={cn(
                  "flex cursor-pointer flex-col items-start gap-1 p-3",
                  !notification.read && "bg-accent/50"
                )}
                onSelect={(e) => {
                  e.preventDefault();
                  dismissNotification(notification.id);
                }}
              >
                <div className="flex w-full items-start justify-between gap-2">
                  <div className="flex flex-col gap-0.5">
                    <div className="flex items-center gap-2">
                      <span
                        className={cn(
                          "text-sm font-medium",
                          !notification.read && "text-foreground"
                        )}
                      >
                        {notification.title}
                      </span>
                      {notification.type && (
                        <Badge
                          variant={getBadgeVariant(notification.type)}
                          className="h-4 px-1.5 text-[10px]"
                        >
                          {notification.type}
                        </Badge>
                      )}
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {notification.description}
                    </span>
                    <span className="text-xs text-muted-foreground/70">
                      {formatRelativeTime(notification.timestamp)}
                    </span>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6 shrink-0 opacity-0 transition-opacity group-hover:opacity-100"
                    onClick={(e) => {
                      e.stopPropagation();
                      dismissNotification(notification.id);
                    }}
                    aria-label={`Dismiss ${notification.title}`}
                  >
                    <X className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </DropdownMenuItem>
            ))}
          </div>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
