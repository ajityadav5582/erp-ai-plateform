/* eslint-disable @typescript-eslint/no-explicit-any */
export interface ReceiptLine {
  name: string;
  qty: number;
  rate: number;
  amount: number;
}

export interface ReceiptData {
  companyName: string;
  tradeName?: string;
  address: string;
  pan: string;
  contact?: string;
  billNo: string;
  dateBs: string;
  dateAd: string;
  counterNo: string;
  cashier: string;
  lines: ReceiptLine[];
  subtotal: number;
  discount: number;
  nonTaxable: number;
  taxable: number;
  vat: number;
  grandTotal: number;
  amountInWords: string;
  qrPayload?: string;
}

const ESC = "\x1B";
const GS = "\x1D";

export class ThermalPrinterService {
  private port: any = null;
  private writer: any = null;

  async connect(): Promise<boolean> {
    if (!("serial" in navigator)) return false;
    try {
      this.port = await (navigator as any).serial.requestPort();
      await this.port.open({ baudRate: 9600 });
      this.writer = this.port.writable.getWriter();
      return true;
    } catch {
      return false;
    }
  }

  async disconnect() {
    try {
      await this.writer?.close();
      await this.port?.close();
    } catch {
      // ignore
    }
    this.writer = null;
    this.port = null;
  }

  private async write(data: string) {
    if (!this.writer) throw new Error("Printer not connected");
    const encoder = new TextEncoder();
    await this.writer.write(encoder.encode(data));
  }

  private cmd(s: string) {
    return s;
  }

  async printReceipt(data: ReceiptData) {
    const lines: string[] = [];
    const center = (s: string) => `${ESC}a\x01${s}\n${ESC}a\x00`;
    const bold = (s: string) => `${ESC}E\x01${s}${ESC}E\x00`;

    lines.push(center(bold(data.companyName)));
    if (data.tradeName) lines.push(center(data.tradeName));
    lines.push(center(data.address));
    lines.push(center(`PAN: ${data.pan}`));
    if (data.contact) lines.push(center(`Tel: ${data.contact}`));
    lines.push("--------------------------------");
    lines.push(`Bill No: ${data.billNo}`);
    lines.push(`Date BS: ${data.dateBs}  AD: ${data.dateAd}`);
    lines.push(`Counter: ${data.counterNo}  Cashier: ${data.cashier}`);
    lines.push("--------------------------------");
    lines.push("Qty  Item                Rate   Amt");
    for (const l of data.lines) {
      lines.push(`${l.qty}    ${l.name.slice(0, 18).padEnd(18)} ${l.rate.toFixed(2)} ${l.amount.toFixed(2)}`);
    }
    lines.push("--------------------------------");
    lines.push(`Subtotal: ${data.subtotal.toFixed(2)}`);
    lines.push(`Discount: ${data.discount.toFixed(2)}`);
    lines.push(`Non-Taxable: ${data.nonTaxable.toFixed(2)}`);
    lines.push(`Taxable: ${data.taxable.toFixed(2)}`);
    lines.push(`VAT: ${data.vat.toFixed(2)}`);
    lines.push(bold(`Grand Total: ${data.grandTotal.toFixed(2)}`));
    lines.push(`Amount in words: ${data.amountInWords}`);
    if (data.qrPayload) {
      lines.push(`${GS}(k\x04\x001A0${data.qrPayload}`);
    }
    lines.push("\n\n");
    // paper cut
    lines.push("\x1D\x56\x42\x00");
    // cash drawer kick
    lines.push("\x1B\x70\x00\x19\xFA");

    await this.write(lines.join("\n"));
  }

  async fallbackPrint(html: string) {
    const iframe = document.createElement("iframe");
    iframe.style.display = "none";
    document.body.appendChild(iframe);
    const doc = iframe.contentDocument;
    if (doc) {
      doc.open();
      doc.write(html);
      doc.close();
      iframe.contentWindow?.print();
    }
    setTimeout(() => document.body.removeChild(iframe), 1000);
  }
}

export const thermalPrinter = new ThermalPrinterService();
