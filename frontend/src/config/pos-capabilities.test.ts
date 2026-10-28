import { getPosCapabilities } from "./pos-capabilities";
import type { BusinessType } from "../services/company.service";

const BUSINESS_TYPES: BusinessType[] = [
  "RESTAURANT",
  "CAFE",
  "HOTEL",
  "SWEETS_RETAIL",
  "GENERAL_RETAIL",
  "GROCERY",
  "HARDWARE",
  "SANITARY",
  "PHARMACY",
  "CLOTHING",
  "SERVICES",
  "OTHER",
];

const CAPABILITY_KEYS: (keyof ReturnType<typeof getPosCapabilities>)[] = [
  "barcodeScanning",
  "productGrid",
  "tableManagement",
  "kitchenTickets",
  "orderTypes",
  "productVariants",
  "batchTracking",
  "serviceSelection",
];

function assert(condition: boolean, message: string): void {
  if (!condition) throw new Error(message);
}

export function runPosCapabilityTests(): void {
  for (const businessType of BUSINESS_TYPES) {
    const capabilities = getPosCapabilities(businessType);
    for (const key of CAPABILITY_KEYS) {
      assert(
        typeof capabilities[key] === "boolean",
        `${businessType}: ${key} must be a boolean`
      );
    }
  }

  // Profile-specific expectations
  assert(getPosCapabilities("RESTAURANT").tableManagement, "RESTAURANT should enable tableManagement");
  assert(getPosCapabilities("RESTAURANT").kitchenTickets, "RESTAURANT should enable kitchenTickets");
  assert(!getPosCapabilities("RESTAURANT").barcodeScanning, "RESTAURANT should disable barcodeScanning");

  assert(getPosCapabilities("CAFE").tableManagement, "CAFE should enable tableManagement");
  assert(getPosCapabilities("HOTEL").serviceSelection, "HOTEL should enable serviceSelection");

  assert(getPosCapabilities("SWEETS_RETAIL").barcodeScanning, "SWEETS_RETAIL should enable barcodeScanning");
  assert(getPosCapabilities("SWEETS_RETAIL").batchTracking, "SWEETS_RETAIL should enable batchTracking");

  assert(getPosCapabilities("GENERAL_RETAIL").barcodeScanning, "GENERAL_RETAIL should enable barcodeScanning");
  assert(!getPosCapabilities("GENERAL_RETAIL").tableManagement, "GENERAL_RETAIL should disable tableManagement");

  assert(getPosCapabilities("GROCERY").batchTracking, "GROCERY should enable batchTracking");
  assert(getPosCapabilities("PHARMACY").batchTracking, "PHARMACY should enable batchTracking");

  assert(getPosCapabilities("CLOTHING").productVariants, "CLOTHING should enable productVariants");
  assert(getPosCapabilities("HARDWARE").productVariants, "HARDWARE should enable productVariants");
  assert(getPosCapabilities("SANITARY").productVariants, "SANITARY should enable productVariants");

  assert(getPosCapabilities("SERVICES").serviceSelection, "SERVICES should enable serviceSelection");
  assert(getPosCapabilities("SERVICES").orderTypes, "SERVICES should enable orderTypes");

  // Unknown/null falls back to defaults
  assert(getPosCapabilities(null).barcodeScanning, "null should fall back to defaults");
  assert(getPosCapabilities(undefined).productGrid, "undefined should fall back to defaults");
}

// Intentionally no auto-run: this module is type-checked by `tsc --noEmit`
// and can be invoked via `runPosCapabilityTests()` in a test runner.
