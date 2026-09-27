"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

function Checkbox({
  className,
  checked,
  onCheckedChange,
  disabled,
  ...props
}: React.ComponentProps<"input"> & {
  checked?: boolean
  onCheckedChange?: (checked: boolean) => void
}) {
  return (
    <input
      type="checkbox"
      role="checkbox"
      aria-checked={checked}
      data-slot="checkbox"
      checked={checked}
      disabled={disabled}
      onChange={(e) => onCheckedChange?.(e.target.checked)}
      className={cn(
        "peer size-4 shrink-0 rounded-[4px] border border-primary shadow-xs transition-all outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50",
        "data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground data-[state=checked]:border-primary",
        "dark:data-[state=checked]:bg-primary dark:data-[state=checked]:text-primary-foreground",
        className
      )}
      {...props}
    />
  )
}

export { Checkbox }
