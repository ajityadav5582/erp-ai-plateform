import { axiosInstance } from "./http-client";

export interface PosTable {
  id: number;
  tableNumber: string;
  capacity: number;
  section: string | null;
  status: "FREE" | "OCCUPIED" | "ORDERED" | "RESERVED" | "BILLING" | "CLEANING";
  currentSessionId: number | null;
}

export interface TableSession {
  id: number;
  tableId: number;
  status: string;
}

export interface KitchenOrder {
  id: number;
  kotNumber?: string | null;
  tableId: number | null;
  sessionId: number | null;
  orderType?: string | null;
  tokenNumber?: string | null;
  deliveryPartner?: string | null;
  customerPhone?: string | null;
  status: "PENDING" | "PREPARING" | "READY" | "SERVED" | "CANCELLED";
  course: number;
  firedAt: string;
  notes: string | null;
  waiter: string | null;
}

export interface KitchenOrderLine {
  itemId: number;
  itemCode?: string | null;
  itemName?: string | null;
  quantity: number;
  notes?: string | null;
  course?: number;
  station?: string | null;
  modifiers?: string | null;
  modifierPrice?: number | null;
}

export interface KitchenTicketLine {
  itemId: number;
  itemCode: string | null;
  itemName: string;
  quantity: number;
  station: string | null;
  modifiers: string | null;
  modifierPrice: number | null;
  notes: string | null;
  course: number;
}

export interface KitchenTicketResponse {
  orderId: number;
  kotNumber: string;
  tableId: number | null;
  tableNumber: string | null;
  section: string | null;
  waiter: string | null;
  orderType: string;
  tokenNumber?: string | null;
  deliveryPartner?: string | null;
  customerPhone?: string | null;
  firedAt: string;
  status: KitchenOrder["status"];
  course: number;
  notes: string | null;
  lines: KitchenTicketLine[];
}

export interface RestaurantModifierOption {
  name: string;
  priceDelta: number;
}

export interface RestaurantModifierPreset {
  groupName: string;
  multiple: boolean;
  options: RestaurantModifierOption[];
}

export interface CreatePosTableRequest {
  tableNumber: string;
  capacity: number;
  section?: string | null;
}

export interface UpdatePosTableRequest {
  tableNumber?: string | null;
  capacity?: number | null;
  section?: string | null;
}

export interface TableBillLineItem {
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
  notes: string | null;
  station?: string | null;
  modifiers?: string | null;
  modifierPrice?: number | null;
}

export interface TableBillSummaryResponse {
  tableId: number;
  tableNumber: string;
  section: string | null;
  sessionId: number | null;
  waiter: string | null;
  openedAt: string | null;
  kotCount: number;
  lines: TableBillLineItem[];
  subtotal: number;
  serviceChargePercent?: number;
  serviceChargeAmount?: number;
  vatRate?: number;
  vatAmount?: number;
  totalAmount?: number;
}

export const posRestaurantService = {
  listTables: async () => {
    const res = await axiosInstance.get<PosTable[]>("/pos/restaurant/tables");
    return { data: res.data };
  },
  createTable: async (data: CreatePosTableRequest) => {
    const res = await axiosInstance.post<PosTable>("/pos/restaurant/tables", data);
    return { data: res.data };
  },
  updateTable: async (tableId: number, data: UpdatePosTableRequest) => {
    const res = await axiosInstance.put<PosTable>(`/pos/restaurant/tables/${tableId}`, data);
    return { data: res.data };
  },
  deleteTable: async (tableId: number) => {
    await axiosInstance.delete(`/pos/restaurant/tables/${tableId}`);
    return { data: undefined as void };
  },
  bootstrapTables: async () => {
    const res = await axiosInstance.post<PosTable[]>("/pos/restaurant/tables/bootstrap");
    return { data: res.data };
  },
  getTableCurrentBill: async (tableId: number) => {
    const res = await axiosInstance.get<TableBillSummaryResponse>(`/pos/restaurant/tables/${tableId}/current-bill`);
    return { data: res.data };
  },
  transferTable: async (fromTableId: number, toTableId: number) => {
    await axiosInstance.post(`/pos/restaurant/tables/${fromTableId}/transfer/${toTableId}`);
    return { data: undefined as void };
  },
  mergeTables: async (fromTableId: number, intoTableId: number) => {
    await axiosInstance.post(`/pos/restaurant/tables/${fromTableId}/merge/${intoTableId}`);
    return { data: undefined as void };
  },
  occupy: async (tableId: number) => {
    const res = await axiosInstance.post<TableSession>(`/pos/restaurant/tables/${tableId}/occupy`);
    return { data: res.data };
  },
  free: async (tableId: number) => {
    await axiosInstance.post(`/pos/restaurant/tables/${tableId}/free`);
    return { data: undefined as void };
  },
  openSession: async (tableId: number) => {
    const res = await axiosInstance.post<TableSession>(`/pos/restaurant/tables/${tableId}/sessions`);
    return { data: res.data };
  },
  fireKitchenOrder: async (tableId: number, waiter: string | null, lines: unknown[]) => {
    const res = await axiosInstance.post<KitchenOrder>(`/pos/restaurant/tables/${tableId}/kitchen-orders`, lines, { params: { waiter: waiter || undefined } });
    return { data: res.data };
  },
  fireGeneralKitchenOrder: async (
    waiter: string | null,
    lines: unknown[],
    details?: {
      tableId?: number | null;
      orderType?: string | null;
      tokenNumber?: string | null;
      deliveryPartner?: string | null;
      customerPhone?: string | null;
    }
  ) => {
    const res = await axiosInstance.post<KitchenOrder>(`/pos/restaurant/kitchen-orders`, lines, {
      params: {
        tableId: details?.tableId || undefined,
        waiter: waiter || undefined,
        orderType: details?.orderType || undefined,
        tokenNumber: details?.tokenNumber || undefined,
        deliveryPartner: details?.deliveryPartner || undefined,
        customerPhone: details?.customerPhone || undefined,
      },
    });
    return { data: res.data };
  },
  listAllKitchenOrders: async (station?: string) => {
    const res = await axiosInstance.get<KitchenOrder[]>("/pos/restaurant/kitchen-orders", {
      params: { station: station && station !== "ALL" ? station : undefined },
    });
    return { data: res.data };
  },
  listKitchenOrderLines: async (orderId: number, station?: string) => {
    const res = await axiosInstance.get<KitchenOrderLine[]>(`/pos/restaurant/kitchen-orders/${orderId}/lines`, {
      params: { station: station && station !== "ALL" ? station : undefined },
    });
    return { data: res.data };
  },
  getKitchenTicket: async (orderId: number) => {
    const res = await axiosInstance.get<KitchenTicketResponse>(`/pos/restaurant/kitchen-orders/${orderId}/ticket`);
    return { data: res.data };
  },
  getModifierPresets: async () => {
    const res = await axiosInstance.get<RestaurantModifierPreset[]>("/pos/restaurant/modifier-presets");
    return { data: res.data };
  },
  updateKitchenOrderStatus: async (orderId: number, status: KitchenOrder["status"]) => {
    const res = await axiosInstance.patch<KitchenOrder>(`/pos/restaurant/kitchen-orders/${orderId}/status`, null, { params: { status } });
    return { data: res.data };
  },
  listKitchenOrders: async (tableId: number) => {
    const res = await axiosInstance.get<KitchenOrder[]>(`/pos/restaurant/tables/${tableId}/kitchen-orders`);
    return { data: res.data };
  },
  markOrdered: async (tableId: number) => {
    const res = await axiosInstance.post<PosTable>(`/pos/restaurant/tables/${tableId}/ordered`);
    return { data: res.data };
  },
  markCleaning: async (tableId: number) => {
    const res = await axiosInstance.post<PosTable>(`/pos/restaurant/tables/${tableId}/cleaning`);
    return { data: res.data };
  },
};
