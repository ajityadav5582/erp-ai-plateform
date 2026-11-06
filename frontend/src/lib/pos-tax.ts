export interface TaxBreakdown {
  grossTotal: number;
  discount: number;
  nonTaxable: number;
  taxable: number;
  vat: number;
  roundOff: number;
  grandTotal: number;
}

export function computeNepalTax(
  grossTotal: number,
  discount: number,
  nonTaxable: number,
  vatRate: number,
): TaxBreakdown {
  const taxable = Math.max(grossTotal - discount - nonTaxable, 0);
  const vat = Math.round(taxable * vatRate * 100) / 100;
  const rawTotal = taxable + vat + nonTaxable;
  const grandTotal = Math.round(rawTotal);
  const roundOff = Math.round((grandTotal - rawTotal) * 100) / 100;
  return { grossTotal, discount, nonTaxable, taxable, vat, roundOff, grandTotal };
}
