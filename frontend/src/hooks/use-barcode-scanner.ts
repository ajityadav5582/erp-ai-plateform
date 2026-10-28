import * as React from "react";

/**
 * A barcode detected by the camera scanner.
 *
 * `source` tells the caller which reader produced it, so the UI can show the
 * right feedback ("Scanned" vs "Typed").
 */
export interface ScanResult {
  value: string;
  source: "camera" | "keyboard";
}

interface UseBarcodeScannerOptions {
  /** Called with the resolved code. Fires for camera scans and keyboard-wedge bursts. */
  onScan: (result: ScanResult) => void;
  /** Set false to stop listening (e.g. while a modal owns the keyboard). */
  enabled?: boolean;
}

/**
 * Timing that separates a hardware scanner's keystroke burst from a human
 * typing. Scanners emulate a keyboard and fire each character a couple of
 * milliseconds apart, then send Enter; a person types at 100ms+ per key.
 */
const KEYSTROKE_INTERVAL_MS = 50;
const BURST_TIMEOUT_MS = 200;
const MIN_SCANNER_LENGTH = 3;

/** Minimal shape of the experimental BarcodeDetector API (Chromium mobile). */
interface DetectedBarcode {
  rawValue: string;
}
interface BarcodeDetectorLike {
  detect: (source: CanvasImageSource) => Promise<DetectedBarcode[]>;
}
type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike;

function getBarcodeDetector(): BarcodeDetectorConstructor | null {
  if (typeof window === "undefined") return null;
  return (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor })
    .BarcodeDetector ?? null;
}

/**
 * Barcode scanning for the POS terminal, mobile-first.
 *
 * Two readers feed one callback:
 *
 * 1. **Camera** (phones and tablets). A "Scan" button opens the rear camera
 *    and decodes frames with the platform's `BarcodeDetector` where available.
 *    No native app or extra dependency: on browsers without the API the camera
 *    button is simply not offered and the keyboard reader carries the flow.
 * 2. **Keyboard wedge** (dedicated scanners and desktop). A burst of fast
 *    keystrokes terminated by Enter is recognised as a scan rather than
 *    typing, so the cashier never has to touch a button between items.
 *
 * The hook never steals focus: the terminal keeps its own always-focused input
 * and forwards keystrokes here, so manual entry and scanning share one field.
 */
export function useBarcodeScanner({ onScan, enabled = true }: UseBarcodeScannerOptions) {
  const onScanRef = React.useRef(onScan);
  onScanRef.current = onScan;

  const [isCameraSupported, setIsCameraSupported] = React.useState(false);
  const [isScanning, setIsScanning] = React.useState(false);
  const [cameraError, setCameraError] = React.useState<string | null>(null);

  const videoRef = React.useRef<HTMLVideoElement | null>(null);
  const streamRef = React.useRef<MediaStream | null>(null);
  const detectorRef = React.useRef<BarcodeDetectorLike | null>(null);
  const rafRef = React.useRef<number | null>(null);
  const lastDetectionRef = React.useRef<{ value: string; at: number }>({ value: "", at: 0 });

  React.useEffect(() => {
    setIsCameraSupported(getBarcodeDetector() !== null);
  }, []);

  /**
   * Feeds one raw keystroke stream into the burst detector. Exposed so the
   * terminal's always-focused input can forward every keypress, including the
   * terminating Enter.
   */
  const handleKeystroke = React.useRef(
    { buffer: "", lastKeyAt: 0, timeout: null as ReturnType<typeof setTimeout> | null }
  ).current;

  const flush = React.useCallback(() => {
    const value = handleKeystroke.buffer.trim();
    handleKeystroke.buffer = "";
    if (value.length >= MIN_SCANNER_LENGTH) {
      onScanRef.current({ value, source: "keyboard" });
    }
  }, [handleKeystroke]);

  const onKeyDown = React.useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (!enabled) return;

      if (event.key === "Enter") {
        // Enter ends a burst. A human pressing Enter on a short value is just
        // submitting the field, which the caller handles separately.
        if (handleKeystroke.timeout) {
          clearTimeout(handleKeystroke.timeout);
          handleKeystroke.timeout = null;
        }
        flush();
        return;
      }

      if (event.key.length !== 1) return; // ignore modifiers, arrows, etc.

      const now = Date.now();
      const isBurst = now - handleKeystroke.lastKeyAt <= KEYSTROKE_INTERVAL_MS;
      handleKeystroke.lastKeyAt = now;

      if (!isBurst) {
        handleKeystroke.buffer = "";
      }
      handleKeystroke.buffer += event.key;

      if (handleKeystroke.timeout) {
        clearTimeout(handleKeystroke.timeout);
      }
      // A pause longer than a scanner's inter-character gap means a human is
      // typing: drop the partial buffer instead of misreading it as a code.
      handleKeystroke.timeout = setTimeout(flush, BURST_TIMEOUT_MS);
    },
    [enabled, flush, handleKeystroke]
  );

  const stopCamera = React.useCallback(() => {
    if (rafRef.current !== null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    detectorRef.current = null;
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsScanning(false);
  }, []);

  const startCamera = React.useCallback(async () => {
    const Detector = getBarcodeDetector();
    if (!Detector) {
      setCameraError("Camera scanning is not supported on this device. Type or scan the code instead.");
      return;
    }
    try {
      setCameraError(null);
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });
      streamRef.current = stream;
      detectorRef.current = new Detector({
        formats: ["ean_13", "ean_8", "upc_a", "upc_e", "code_128", "code_39", "qr_code"],
      });
      setIsScanning(true);

      // The video element is rendered by the caller's scanner UI; attach the
      // stream once it mounts.
      requestAnimationFrame(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          void videoRef.current.play();
        }
      });

      const detectLoop = async () => {
        const video = videoRef.current;
        const detector = detectorRef.current;
        if (!video || !detector || video.readyState < 2) {
          rafRef.current = requestAnimationFrame(detectLoop);
          return;
        }
        try {
          const codes = await detector.detect(video);
          const now = Date.now();
          for (const code of codes) {
            // Debounce: the same code stays in frame for many frames.
            if (
              code.rawValue &&
              (code.rawValue !== lastDetectionRef.current.value ||
                now - lastDetectionRef.current.at > 1500)
            ) {
              lastDetectionRef.current = { value: code.rawValue, at: now };
              onScanRef.current({ value: code.rawValue, source: "camera" });
              stopCamera();
              return;
            }
          }
        } catch {
          // A failed frame is not fatal; keep scanning.
        }
        rafRef.current = requestAnimationFrame(detectLoop);
      };
      rafRef.current = requestAnimationFrame(detectLoop);
    } catch {
      setCameraError("Could not access the camera. Check the permission and try again.");
      setIsScanning(false);
    }
  }, [stopCamera]);

  React.useEffect(() => stopCamera, [stopCamera]);

  return {
    /** Attach to the always-focused scan input. */
    onKeyDown,
    /** Attach to the (hidden until scanning) video element. */
    videoRef,
    isCameraSupported,
    isScanning,
    cameraError,
    startCamera,
    stopCamera,
    clearCameraError: () => setCameraError(null),
  };
}
