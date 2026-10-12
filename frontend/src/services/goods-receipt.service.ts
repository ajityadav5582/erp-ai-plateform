import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `GoodsReceiptStatus` enum.
 *
 * There is no DRAFT: a receipt is created already posted, because receiving is the
 * point at which the goods physically arrive. A client that offered a "save as draft"
 * action would be inventing a state the server cannot store.
 */
export const GOODS_RECEIPT_STATUSES = ["POSTED", "CANCELLED"] as const;
export type GoodsReceiptStatus = (typeof GOODS_RECEIPT_STATUSES)[number];

export const GOODS_RECEIPT_STATUS_LABELS: Record<GoodsReceiptStatus, string> = {
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

export function formatGoodsReceiptStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return GOODS_RECEIPT_STATUS_LABELS[status as GoodsReceiptStatus] ?? status;
}

/** Mirrors the nested `GoodsReceiptResponse.Line` record. */
export interface GoodsReceiptLine {
  id: number;
  lineNumber: number;
  /** The order line this receipt satisfies. */
  purchaseOrderLineId: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  receivedQuantity: number;
  unitPrice: number;
  lineAmount: number;
  /**
   * Where this line's quantity actually landed. Equals the note's warehouse except
   * on a genuinely split consignment, where one challan was offloaded at two sites.
   * Non-null on every line, because stock cannot be posted against a line that does
   * not say where the goods went.
   */
  warehouseId: number | null;
  remarks: string | null;
}

/** Mirrors the backend `GoodsReceiptResponse` record. */
export interface GoodsReceipt {
  id: number;
  grnNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number;
  fiscalYearId: number;
  /** The order this receipt was raised against. */
  purchaseOrderId: number;
  poNumber: string;
  receiptDate: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  currency: string;
  status: GoodsReceiptStatus;
  /** Net value of the goods received on this note. */
  receivedAmount: number;
  /**
   * The site the note is filed under. Always resolved by the time the note is
   * posted - an explicit choice at the gate, the order's nomination, or refused.
   */
  warehouseId: number;
  warehouseCode: string | null;
  warehouseName: string | null;
  deliveryChallanNo: string | null;
  notes: string | null;
  postedAt: string | null;
  postedBy: string | null;
  cancellationReason: string | null;
  createdAt: string;
  lines: GoodsReceiptLine[];
}

/** One arriving order line, mirroring the backend `LineRequest` record. */
export interface GoodsReceiptLineRequest {
  lineNumber: number;
  /**
   * How much of this line physically arrived. Omit it to receive the whole
   * outstanding quantity, which is the common complete-delivery case.
   */
  receivedQuantity?: number;
  /**
   * Overrides the note's warehouse for this line only - the split-consignment case,
   * where part of a single challan was offloaded at a second site. Omit it and the
   * line takes the note's warehouse.
   */
  warehouseId?: number;
  remarks?: string;
}

/**
 * Mirrors the backend `CreateGoodsReceiptRequest` record.
 *
 * A quantity may now be sent per line. Partial receipt used to be refused outright,
 * on the reasoning that a caller-supplied quantity could let the receipt note and the
 * order disagree about what is still outstanding. That disagreement is prevented by
 * construction instead: the server bounds every quantity by the order line's
 * outstanding quantity, and the order is the only writer of `receivedQuantity`.
 *
 * Without it, recording a short delivery meant booking the ordered quantity and
 * correcting it afterwards, and `PARTIALLY_RECEIVED` was unreachable on both the
 * line and the header.
 *
 * `lines` takes precedence over `lineNumbers`; send one or the other. Omitting both
 * receives every outstanding line in full.
 */
export interface CreateGoodsReceiptRequest {
  purchaseOrderId: number;
  /** Defaults to today server-side when omitted. */
  receiptDate?: string;
  /**
   * Where the goods physically landed. Omit it when the goods went to the site's
   * nominated warehouse, which is the common case; state it when reality differs -
   * the nominated site is full, the driver was rerouted, the order nominated none.
   *
   * A receipt with neither this nor a nomination on the order is refused: a received
   * quantity that names no site cannot be posted as stock, and posting it against an
   * assumed site is how stock ends up on a floor nobody is counting.
   */
  warehouseId?: number;
  /** Frozen onto the note alongside the id so it keeps printing what it received. */
  warehouseCode?: string;
  warehouseName?: string;
  /** Short deliveries. Wins over `lineNumbers` when both are sent. */
  lines?: GoodsReceiptLineRequest[];
  /** Compact complete-delivery form: these lines, in full. */
  lineNumbers?: number[];
  deliveryChallanNo?: string;
  notes?: string;
}

export interface GetGoodsReceiptsParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/procurement/goods-receipts";

/**
 * Goods receipt API endpoints with RTK Query caching and invalidation.
 *
 * Recording a receipt invalidates `PurchaseOrder` too: it moves the order's received
 * quantities and can advance the header to PARTIALLY_RECEIVED or RECEIVED, so an order
 * screen holding a pre-receipt copy would keep showing outstanding value that no longer
 * exists.
 */
export const goodsReceiptApi = api.injectEndpoints({
  endpoints: (build) => ({
    getGoodsReceipts: build.query<
      PageResponse<GoodsReceipt>,
      GetGoodsReceiptsParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["GoodsReceipt"],
    }),

    getGoodsReceipt: build.query<GoodsReceipt, number>({
      query: (goodsReceiptId) => ({
        url: `${BASE_URL}/${goodsReceiptId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, goodsReceiptId) => [
        { type: "GoodsReceipt", id: goodsReceiptId },
        "GoodsReceipt",
      ],
    }),

    /** The receipts raised against one purchase order. */
    getGoodsReceiptsByOrder: build.query<
      PageResponse<GoodsReceipt>,
      { purchaseOrderId: number } & GetGoodsReceiptsParams
    >({
      query: ({ purchaseOrderId, ...params }) => ({
        url: `${BASE_URL}/by-order/${purchaseOrderId}`,
        method: "GET",
        params,
      }),
      providesTags: (_result, _error, { purchaseOrderId }) => [
        { type: "GoodsReceipt", id: `order-${purchaseOrderId}` },
        "GoodsReceipt",
      ],
    }),

    recordGoodsReceipt: build.mutation<GoodsReceipt, CreateGoodsReceiptRequest>({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["GoodsReceipt", "PurchaseOrder"],
    }),

    /**
     * Voids a receipt and hands its quantities back to the order.
     *
     * Both documents move together, which is why this invalidates `PurchaseOrder`
     * as well: a receipt posted against the wrong order leaves the order holding
     * quantities that never arrived, and cancelling is what puts them back.
     */
    cancelGoodsReceipt: build.mutation<
      GoodsReceipt,
      { goodsReceiptId: number; reason: string }
    >({
      query: ({ goodsReceiptId, ...body }) => ({
        url: `${BASE_URL}/${goodsReceiptId}/cancel`,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["GoodsReceipt", "PurchaseOrder"],
    }),
  }),
});

export const {
  useGetGoodsReceiptsQuery,
  useGetGoodsReceiptQuery,
  useGetGoodsReceiptsByOrderQuery,
  useRecordGoodsReceiptMutation,
  useCancelGoodsReceiptMutation,
} = goodsReceiptApi;
