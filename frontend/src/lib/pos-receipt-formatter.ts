export interface PosReceiptLine {
  itemName: string;
  quantity: number;
  unitRate: number;
  lineAmount: number;
}

export interface PosReceiptData {
  companyTradeName: string;
  companyLegalName: string;
  address: string;
  panNumber: string;
  billNo: string;
  dateBs: string;
  dateAd: string;
  counterName: string;
  cashierName: string;
  buyerPan?: string | null;
  buyerName?: string | null;
  lines: PosReceiptLine[];
  taxableAmount: number;
  vatAmount: number;
  exemptAmount: number;
  grandTotal: number;
  cbmsSynced: boolean;
  sellerPan: string;
  isCopyOfOriginal: boolean;
  copyNumber: number;
}

const ESC = "\x1B";
const CENTER = ESC + "a" + "\x01";
const LEFT = ESC + "a" + "\x00";
const BOLD_ON = ESC + "E" + "\x01";
const BOLD_OFF = ESC + "E" + "\x00";
const DOUBLE_HEIGHT = ESC + "!" + "\x10";
const NORMAL = ESC + "!" + "\x00";

function padLine(left: string, right: string, width = 32): string {
  const space = Math.max(1, width - left.length - right.length);
  return left + " ".repeat(space) + right;
}

export function formatPosReceipt(data: PosReceiptData): string {
  const lines: string[] = [];
  lines.push(CENTER + BOLD_ON + DOUBLE_HEIGHT + data.companyTradeName + "\n");
  lines.push(NORMAL + data.companyLegalName + "\n");
  lines.push(data.address + "\n");
  lines.push("PAN: " + data.panNumber + "\n");
  lines.push(BOLD_ON + "TAX INVOICE" + BOLD_OFF + "\n");

  if (data.isCopyOfOriginal) {
    lines.push(BOLD_ON + `*** COPY OF ORIGINAL (Copy #${data.copyNumber}) ***` + BOLD_OFF + "\n");
  }

  lines.push(LEFT);
  lines.push(padLine("Bill No:", data.billNo) + "\n");
  lines.push(padLine("Date (BS):", data.dateBs) + "\n");
  lines.push(padLine("Date (AD):", data.dateAd) + "\n");
  lines.push(padLine("Counter:", data.counterName) + "\n");
  lines.push(padLine("Cashier:", data.cashierName) + "\n");
  if (data.buyerPan) lines.push(padLine("Buyer PAN:", data.buyerPan) + "\n");
  if (data.buyerName) lines.push(padLine("Buyer:", data.buyerName) + "\n");

  lines.push("-".repeat(32) + "\n");
  for (const l of data.lines) {
    lines.push(l.itemName + "\n");
    lines.push(padLine(`${l.quantity} x ${l.unitRate.toFixed(2)}`, l.lineAmount.toFixed(2)) + "\n");
  }
  lines.push("-".repeat(32) + "\n");

  lines.push(padLine("Taxable:", data.taxableAmount.toFixed(2)) + "\n");
  lines.push(padLine("VAT:", data.vatAmount.toFixed(2)) + "\n");
  lines.push(padLine("Exempt:", data.exemptAmount.toFixed(2)) + "\n");
  lines.push(BOLD_ON + padLine("Grand Total:", data.grandTotal.toFixed(2)) + BOLD_OFF + "\n");

  lines.push(padLine("CBMS:", data.cbmsSynced ? "Synced" : "Offline Queue") + "\n");

  const qr = `${data.sellerPan}|${data.billNo}|${data.grandTotal}|${data.dateBs}`;
  lines.push("QR: " + qr + "\n");

  lines.push(CENTER + "Thank you for your business\n");
  return lines.join("");
}
