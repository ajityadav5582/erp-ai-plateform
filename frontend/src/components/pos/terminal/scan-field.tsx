"use client";

import * as React from "react";
import { Camera, ScanLine, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useBarcodeScanner } from "@/hooks/use-barcode-scanner";

interface ScanFieldProps {
  /** Called with the resolved code, from either the camera or a scanner burst. */
  onScan: (code: string) => void;
  /** Set false while a dialog owns the keyboard. */
  enabled?: boolean;
}

/**
 * The terminal's always-focused scan input.
 *
 * Mobile-first: the field is full-width and thumb-reachable, with a camera
 * button beside it for phones and tablets. Hardware scanners (keyboard wedge)
 * need no button at all — the burst detector fires as they type.
 */
export function ScanField({ onScan, enabled = true }: ScanFieldProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  const [value, setValue] = React.useState("");

  const { onKeyDown, videoRef, isCameraSupported, isScanning, cameraError, startCamera, stopCamera } =
    useBarcodeScanner({
      onScan: (result) => {
        onScan(result.value);
        setValue("");
      },
      enabled,
    });

  // Keep the field focused so a hardware scanner can fire at any moment
  // without the cashier touching the screen between items.
  React.useEffect(() => {
    if (!enabled) return;
    const refocus = () => {
      if (document.activeElement instanceof HTMLInputElement) return;
      inputRef.current?.focus();
    };
    const interval = setInterval(refocus, 1000);
    refocus();
    return () => clearInterval(interval);
  }, [enabled]);

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const code = value.trim();
    if (!code) return;
    onScan(code);
    setValue("");
  };

  return (
    <div className="space-y-2">
      <form onSubmit={submit} className="flex items-center gap-2">
        <div className="relative flex-1">
          <ScanLine className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={inputRef}
            value={value}
            onChange={(event) => setValue(event.target.value)}
            onKeyDown={onKeyDown}
            disabled={!enabled}
            autoComplete="off"
            enterKeyHint="go"
            placeholder="Scan barcode or type item code…"
            className="h-12 pl-10 text-base"
            aria-label="Scan barcode or item code"
          />
        </div>
        {isCameraSupported && (
          <Button
            type="button"
            size="icon"
            variant={isScanning ? "destructive" : "outline"}
            className="h-12 w-12 shrink-0"
            onClick={() => (isScanning ? stopCamera() : void startCamera())}
            aria-label={isScanning ? "Stop camera scanning" : "Scan with camera"}
          >
            {isScanning ? <X className="h-5 w-5" /> : <Camera className="h-5 w-5" />}
          </Button>
        )}
      </form>

      {isScanning && (
        <div className="relative overflow-hidden rounded-lg border bg-black">
          {/* The camera preview. Muted + playsInline are required on iOS. */}
          <video
            ref={videoRef}
            muted
            playsInline
            className="aspect-video w-full object-cover"
            aria-label="Camera preview"
          />
          {/* Reticle: tells the cashier where to aim. */}
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
            <div className="h-1/2 w-2/3 rounded-lg border-2 border-white/70" />
          </div>
        </div>
      )}

      {cameraError && (
        <p role="alert" className="text-sm text-destructive">
          {cameraError}
        </p>
      )}
    </div>
  );
}
