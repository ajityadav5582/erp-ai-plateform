/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { QRCodeSVG } from "qrcode.react";

export default function A4Invoice({ data }: { data: any }) {
  return (
    <div className="print:block hidden print:p-8">
      <div className="text-center font-bold text-lg">{data.companyName}</div>
      <div className="text-center">PAN: {data.pan}</div>
      <div className="text-center font-bold">TAX INVOICE</div>
      {data.isCopy && (
        <div className="absolute inset-0 flex items-center justify-center opacity-20 text-4xl font-bold rotate-[-30deg]">
          COPY OF ORIGINAL (Reprint #{data.copyNumber})
        </div>
      )}
      <div className="mt-4">
        <div>Invoice No: {data.invoiceNo}</div>
        <div>Buyer: {data.buyerName} PAN: {data.buyerPan}</div>
      </div>
      <table className="w-full border mt-4 text-sm">
        <thead>
          <tr>
            <th>S.N.</th><th>Item</th><th>Qty</th><th>Rate</th><th>Taxable</th><th>VAT</th><th>Total</th>
          </tr>
        </thead>
        <tbody>
          {data.lines?.map((l: any, i: number) => (
            <tr key={i}>
              <td>{i + 1}</td><td>{l.itemName}</td><td>{l.quantity}</td><td>{l.rate}</td><td>{l.taxable}</td><td>{l.vat}</td><td>{l.total}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="mt-4 flex justify-between">
        <div>Amount in Words: {data.amountInWords}</div>
        <QRCodeSVG value={data.qrPayload} size={96} />
      </div>
      <div className="mt-8 flex justify-between">
        <div>Authorized Signatory</div>
        <div>Stamp</div>
      </div>
    </div>
  );
}
