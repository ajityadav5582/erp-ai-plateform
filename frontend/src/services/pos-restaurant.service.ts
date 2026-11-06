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
  tableId: number | null;
  sessionId: number | null;
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
}

export const posRestaurantService = {
  listTables: async () => {
    const res = await axiosInstance.get<PosTable[]>("/pos/restaurant/tables");
    return { data: res.data };
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
  fireGeneralKitchenOrder: async (waiter: string | null, lines: unknown[]) => {
    const res = await axiosInstance.post<KitchenOrder>(`/pos/restaurant/kitchen-orders`, lines, { params: { waiter: waiter || undefined } });
    return { data: res.data };
  },
  listAllKitchenOrders: async () => {
    const res = await axiosInstance.get<KitchenOrder[]>("/pos/restaurant/kitchen-orders");
    return { data: res.data };
  },
  listKitchenOrderLines: async (orderId: number) => {
    const res = await axiosInstance.get<KitchenOrderLine[]>(`/pos/restaurant/kitchen-orders/${orderId}/lines`);
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
