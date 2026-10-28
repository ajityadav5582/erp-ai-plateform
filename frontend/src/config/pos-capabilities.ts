import type { BusinessType } from "../services/company.service";
import { useCompanyContext } from "./company-context";

/**
 * Centralized POS capability flags derived from the active company's
 * business type. Shared cart, checkout, tax and payment behaviour stays
 * common; only these flags vary by business profile.
 */
export interface PosCapabilities {
  barcodeScanning: boolean;
  productGrid: boolean;
  tableManagement: boolean;
  kitchenTickets: boolean;
  orderTypes: boolean;
  productVariants: boolean;
  batchTracking: boolean;
  serviceSelection: boolean;
}

const DEFAULT_CAPABILITIES: PosCapabilities = {
  barcodeScanning: true,
  productGrid: true,
  tableManagement: false,
  kitchenTickets: false,
  orderTypes: false,
  productVariants: false,
  batchTracking: false,
  serviceSelection: false,
};

const CAPABILITIES_BY_BUSINESS_TYPE: Record<BusinessType, PosCapabilities> = {
  RESTAURANT: {
    barcodeScanning: false,
    productGrid: true,
    tableManagement: true,
    kitchenTickets: true,
    orderTypes: true,
    productVariants: true,
    batchTracking: false,
    serviceSelection: true,
  },
  CAFE: {
    barcodeScanning: false,
    productGrid: true,
    tableManagement: true,
    kitchenTickets: true,
    orderTypes: true,
    productVariants: true,
    batchTracking: false,
    serviceSelection: true,
  },
  HOTEL: {
    barcodeScanning: false,
    productGrid: true,
    tableManagement: true,
    kitchenTickets: true,
    orderTypes: true,
    productVariants: true,
    batchTracking: false,
    serviceSelection: true,
  },
  SWEETS_RETAIL: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: true,
    serviceSelection: false,
  },
  GENERAL_RETAIL: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: false,
    serviceSelection: false,
  },
  GROCERY: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: true,
    serviceSelection: false,
  },
  HARDWARE: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: false,
    serviceSelection: false,
  },
  SANITARY: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: false,
    serviceSelection: false,
  },
  PHARMACY: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: true,
    serviceSelection: false,
  },
  CLOTHING: {
    barcodeScanning: true,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: false,
    productVariants: true,
    batchTracking: false,
    serviceSelection: false,
  },
  SERVICES: {
    barcodeScanning: false,
    productGrid: true,
    tableManagement: false,
    kitchenTickets: false,
    orderTypes: true,
    productVariants: false,
    batchTracking: false,
    serviceSelection: true,
  },
  OTHER: DEFAULT_CAPABILITIES,
};

export function getPosCapabilities(
  businessType: BusinessType | null | undefined
): PosCapabilities {
  if (!businessType) return { ...DEFAULT_CAPABILITIES };
  return { ...(CAPABILITIES_BY_BUSINESS_TYPE[businessType] ?? DEFAULT_CAPABILITIES) };
}

export function usePosCapabilities(): PosCapabilities {
  const company = useCompanyContext().company;
  return getPosCapabilities(company?.businessType ?? null);
}
