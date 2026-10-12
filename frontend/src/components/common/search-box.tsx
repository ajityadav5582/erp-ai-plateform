"use client";

import * as React from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface SearchBoxProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  debounceMs?: number;
}

export function SearchBox({
  value,
  onChange,
  placeholder = "Search...",
  className,
  debounceMs = 300,
}: SearchBoxProps) {
  const [localValue, setLocalValue] = React.useState(value);
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  // Keep the latest callback in a ref so the debounce effect never depends on
  // `onChange`. Callers routinely pass an inline arrow
  // (`onChange={(v) => setTerm(v)}`), which is a new function on every parent
  // render. Depending on it made the effect re-run, call `onChange` again,
  // re-render the parent, and loop forever.
  const onChangeRef = React.useRef(onChange);
  React.useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  // Mirror external value changes into the input without clobbering typing.
  const isFirstRender = React.useRef(true);
  React.useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    setLocalValue(value);
  }, [value]);

  React.useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    if (debounceMs > 0) {
      timerRef.current = setTimeout(() => {
        onChangeRef.current(localValue);
      }, debounceMs);
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [localValue, debounceMs]);

  const handleClear = () => {
    setLocalValue("");
    onChangeRef.current("");
  };

  return (
    <div className={cn("relative", className)}>
      <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" aria-hidden="true" />
      <Input
        type="search"
        value={localValue}
        onChange={(e) => setLocalValue(e.target.value)}
        placeholder={placeholder}
        className="pl-9 pr-9"
      />
      {localValue && (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="absolute right-1 top-1 h-7 w-7"
          onClick={handleClear}
          aria-label="Clear search"
        >
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
