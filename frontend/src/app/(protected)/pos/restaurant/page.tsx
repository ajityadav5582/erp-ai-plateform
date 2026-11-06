"use client";

import * as React from "react";
import { Search, ShoppingCart, Plus, Minus, Trash2, Users, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { posRestaurantService, type PosTable, type KitchenOrder, type KitchenOrderLine } from "@/services/pos-restaurant.service";
import { useGetPosQuickItemGridQuery } from "@/services/pos-quick-item.service";
import { useLazyScanPosItemQuery } from "@/services/pos-scan.service";
import { usePosCapabilities } from "@/config/pos-capabilities";
import { useSelectedCompany } from "@/config/company-context";
import { useAppSelector } from "@/store/hooks";
import { axiosInstance } from "@/services/http-client";
import { useGetMyOpenPosSessionsQuery } from "@/services/pos-session.service";
import { useCheckoutMutation, type PosPaymentMethod } from "@/services/pos-transaction.service";
import { SettlementDialog } from "@/components/pos/terminal/settlement-dialog";
import { toast } from "sonner";

type OrderLine = {
  itemId: number;
  name: string;
  price: number;
  qty: number;
};

const statusColor = (status: PosTable["status"]) => {
  switch (status) {
    case "FREE": return "bg-emerald-500/20 text-emerald-300 border-emerald-500/40";
    case "OCCUPIED": return "bg-amber-500/20 text-amber-300 border-amber-500/40";
    case "ORDERED": return "bg-violet-500/20 text-violet-300 border-violet-500/40";
    case "RESERVED": return "bg-blue-500/20 text-blue-300 border-blue-500/40";
    case "BILLING": return "bg-red-500/20 text-red-300 border-red-500/40";
    case "CLEANING": return "bg-slate-500/20 text-slate-300 border-slate-500/40";
    default: return "bg-slate-500/20 text-slate-300 border-slate-500/40";
  }
};

const statusLabel = (status: PosTable["status"]) => {
  switch (status) {
    case "FREE": return "Available";
    case "OCCUPIED": return "Occupied";
    case "ORDERED": return "Ordered";
    case "RESERVED": return "Reserved";
    case "BILLING": return "Billing";
    case "CLEANING": return "Cleaning";
    default: return status;
  }
};

export default function RestaurantPosTerminalPage() {
  const capabilities = usePosCapabilities();
  const selectedCompany = useSelectedCompany();
  const tenantId = useAppSelector((s) => s.auth.tenantId);
  const [settings, setSettings] = React.useState<Record<string, boolean>>({
    dineIn: true,
    takeaway: true,
    delivery: false,
    tableManagement: true,
    kot: true,
    kds: true,
    kitchenPrinter: false,
    waiterAssignment: true,
    orderNotes: true,
  });
  const [activeTab, setActiveTab] = React.useState<"floor" | "menu" | "kds">("floor");
  const [tables, setTables] = React.useState<PosTable[]>([]);
  const [loadingTables, setLoadingTables] = React.useState(false);
  const [selectedTable, setSelectedTable] = React.useState<PosTable | null>(null);
  const [kitchenOrders, setKitchenOrders] = React.useState<KitchenOrder[]>([]);
  const [kdsOrders, setKdsOrders] = React.useState<KitchenOrder[]>([]);
  const [kdsLines, setKdsLines] = React.useState<Record<number, KitchenOrderLine[]>>({});
  const [kdsFilter, setKdsFilter] = React.useState<"ALL" | "PENDING" | "PREPARING" | "READY">("ALL");
  const [search, setSearch] = React.useState("");
  const [orderLines, setOrderLines] = React.useState<OrderLine[]>([]);
  const [orderMode, setOrderMode] = React.useState<"dine-in" | "takeaway">("dine-in");
  const [sendingKot, setSendingKot] = React.useState(false);
  const [guestCount, setGuestCount] = React.useState<number>(2);
  const [waiter, setWaiter] = React.useState<string>("");
  const [orderNotes, setOrderNotes] = React.useState<string>("");
  const [isPaymentOpen, setIsPaymentOpen] = React.useState(false);
  const [idempotencyKey, setIdempotencyKey] = React.useState<string | null>(null);
  const [checkingOut, setCheckingOut] = React.useState(false);
  const { data: openSessions } = useGetMyOpenPosSessionsQuery();
  const session = openSessions?.[0] ?? null;
  const [checkout, checkoutState] = useCheckoutMutation();
  const [floor, setFloor] = React.useState<string>("all");

  const { data: quickItems } = useGetPosQuickItemGridQuery(0);
  const [scanItem] = useLazyScanPosItemQuery();

  const loadTables = React.useCallback(async () => {
    setLoadingTables(true);
    try {
      const res = await posRestaurantService.listTables();
      setTables(res.data ?? []);
    } catch {
      toast.error("Failed to load tables");
    } finally {
      setLoadingTables(false);
    }
  }, []);

  const loadKitchenOrders = React.useCallback(async (tableId: number) => {
    try {
      const res = await posRestaurantService.listKitchenOrders(tableId);
      setKitchenOrders(res.data ?? []);
    } catch {
      toast.error("Failed to load kitchen orders");
    }
  }, []);

  const loadKds = React.useCallback(async () => {
    try {
      const res = await posRestaurantService.listAllKitchenOrders();
      const orders = res.data ?? [];
      setKdsOrders(orders);
      const linesMap: Record<number, KitchenOrderLine[]> = {};
      await Promise.all(
        orders.map(async (o) => {
          try {
            const lr = await posRestaurantService.listKitchenOrderLines(o.id);
            linesMap[o.id] = lr.data ?? [];
          } catch {
            linesMap[o.id] = [];
          }
        })
      );
      setKdsLines(linesMap);
    } catch {
      toast.error("Failed to load kitchen orders");
    }
  }, []);

  React.useEffect(() => {
    if (capabilities.orderTypes && capabilities.serviceSelection) {
      loadTables();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [capabilities.orderTypes, capabilities.serviceSelection, loadTables]);

  React.useEffect(() => {
    const companyId = selectedCompany?.id;
    if (!companyId || !tenantId) return;
    axiosInstance
      .get("/pos/company-config", { params: { tenantId, companyId } })
      .then((res) => {
        if (res.data?.restaurantSettings) {
          try {
            setSettings(JSON.parse(res.data.restaurantSettings));
          } catch {
            // keep defaults
          }
        }
      })
      .catch(() => {
        // keep defaults
      });
  }, [selectedCompany?.id, tenantId]);

  React.useEffect(() => {
    if (selectedTable) {
      loadKitchenOrders(selectedTable.id);
    }
  }, [selectedTable, loadKitchenOrders]);

  React.useEffect(() => {
    if (activeTab === "kds") {
      loadKds();
    }
  }, [activeTab, loadKds]);

  React.useEffect(() => {
    if (!settings.tableManagement) {
      setActiveTab("menu");
    }
  }, [settings.tableManagement]);

  const floors = React.useMemo(() => {
    const sections = new Set<string>();
    tables.forEach((t) => sections.add(t.section || "Main Floor"));
    if (sections.size === 0) sections.add("Main Floor");
    return ["all", ...Array.from(sections)];
  }, [tables]);

  const filteredTables = tables.filter((t) => floor === "all" || (t.section || "Main Floor") === floor);

  const filteredItems = (quickItems ?? []).filter((q) =>
    q.itemName.toLowerCase().includes(search.toLowerCase())
  );

  if (!capabilities.orderTypes || !capabilities.serviceSelection) {
    return (
      <div className="container mx-auto p-6">
        <Card>
          <CardHeader>
            <CardTitle>Restaurant POS not available</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">
              This company is not configured for restaurant operations.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const addToOrder = async (item: { itemId: number; itemName: string; itemCode: string; defaultPrice: number | null }) => {
    let price = item.defaultPrice;
    if (price == null) {
      try {
        const scanned = await scanItem(item.itemCode).unwrap();
        price = scanned.unitPrice;
      } catch {
        price = 0;
      }
    }
    setOrderLines((prev) => {
      const existing = prev.find((l) => l.itemId === item.itemId);
      if (existing) {
        return prev.map((l) => (l.itemId === item.itemId ? { ...l, qty: l.qty + 1 } : l));
      }
      return [...prev, { itemId: item.itemId, name: item.itemName, price: price ?? 0, qty: 1 }];
    });
  };

  const updateQty = (itemId: number, delta: number) => {
    setOrderLines((prev) =>
      prev
        .map((l) => (l.itemId === itemId ? { ...l, qty: Math.max(0, l.qty + delta) } : l))
        .filter((l) => l.qty > 0)
    );
  };

  const removeLine = (itemId: number) => {
    setOrderLines((prev) => prev.filter((l) => l.itemId !== itemId));
  };

  const subtotal = orderLines.reduce((s, l) => s + l.price * l.qty, 0);
  const tax = 0;
  const total = subtotal + tax;

  const handleOccupy = async () => {
    if (!selectedTable) return;
    try {
      await posRestaurantService.occupy(selectedTable.id);
      toast.success("Table occupied");
      await loadTables();
      const updated = tables.find((t) => t.id === selectedTable.id);
      if (updated) setSelectedTable(updated);
    } catch {
      toast.error("Failed to occupy table");
    }
  };

  const handleFree = async () => {
    if (!selectedTable) return;
    try {
      await posRestaurantService.free(selectedTable.id);
      toast.success("Table freed");
      setSelectedTable(null);
      setOrderLines([]);
      await loadTables();
    } catch {
      toast.error("Failed to free table");
    }
  };

  const handleMarkOrdered = async () => {
    if (!selectedTable) return;
    try {
      await posRestaurantService.markOrdered(selectedTable.id);
      toast.success("Table marked ordered");
      await loadTables();
    } catch {
      toast.error("Failed to update table");
    }
  };

  const handleMarkCleaning = async () => {
    if (!selectedTable) return;
    try {
      await posRestaurantService.markCleaning(selectedTable.id);
      toast.success("Table marked cleaning");
      await loadTables();
    } catch {
      toast.error("Failed to update table");
    }
  };

  const handleCheckout = async (payment: {
    paymentMethod: string;
    amountTendered?: number;
    paymentReference?: string;
    customerId?: number;
    customerName?: string;
    customerPanNumber?: string;
  }) => {
    if (checkingOut) return;
    if (!session) {
      toast.error("No open POS session. Please open a session first.");
      return;
    }
    if (orderLines.length === 0) {
      toast.error("Add at least one item before checkout.");
      return;
    }
    setCheckingOut(true);
    // Reuse the existing key for retries; generate + persist one on first attempt.
    const key = idempotencyKey ?? crypto.randomUUID();
    if (idempotencyKey === null) {
      setIdempotencyKey(key);
    }
    try {
      await checkout({
        request: {
          sessionId: session.id,
          lines: orderLines.map((l) => ({
            itemId: l.itemId,
            quantity: l.qty,
            discountPercentage: 0,
          })),
          paymentMethod: payment.paymentMethod as PosPaymentMethod,
          amountTendered: payment.amountTendered,
          paymentReference: payment.paymentReference,
          customerId: payment.customerId,
          customerName: payment.customerName,
          customerPanNumber: payment.customerPanNumber,
          notes: orderNotes.trim() || undefined,
        },
        idempotencyKey: key,
      }).unwrap();
      if (selectedTable) {
        await posRestaurantService.markCleaning(selectedTable.id);
      }
      setOrderLines([]);
      setIsPaymentOpen(false);
      // Reset so the next order gets a fresh key.
      setIdempotencyKey(null);
      toast.success("Bill generated");
      await loadTables();
    } catch (error) {
      const msg = (error as { data?: { message?: string }; message?: string })?.data?.message
        || (error as { message?: string })?.message
        || "Checkout failed";
      toast.error(msg);
      // Keep idempotencyKey so a retry reuses the same key.
    } finally {
      setCheckingOut(false);
    }
  };

  return (
    <div className="flex h-[calc(100dvh-4rem)] flex-col bg-slate-950 text-slate-100">
      <div className="flex gap-2 border-b border-slate-800 bg-slate-900 px-4 py-2">
        {settings.tableManagement && (
          <button
            onClick={() => setActiveTab("floor")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activeTab === "floor" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
            }`}
          >
            Floor View
          </button>
        )}
        <button
          onClick={() => setActiveTab("menu")}
          className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
            activeTab === "menu" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
          }`}
        >
          Menu
        </button>
        {settings.kds && (
          <button
            onClick={() => setActiveTab("kds")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activeTab === "kds" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
            }`}
          >
            Kitchen Display (KDS)
          </button>
        )}
      </div>

      {settings.tableManagement && activeTab === "floor" && (
        <div className="flex flex-1 flex-col lg:flex-row">
          {/* Left: Floor & Tables */}
          <aside className="w-full shrink-0 overflow-y-auto border-b border-slate-800 bg-slate-900 p-4 lg:w-72 lg:border-b-0 lg:border-r">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">Floor</h2>
              <Select value={floor} onValueChange={setFloor}>
                <SelectTrigger className="h-8 w-36 bg-slate-800 border-slate-700 text-slate-100">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {floors.map((f) => (
                    <SelectItem key={f} value={f}>
                      {f === "all" ? "All Floors" : f}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {loadingTables ? (
              <p className="text-sm text-slate-400">Loading tables…</p>
            ) : (
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-2">
                {filteredTables.map((t) => (
                  <button
                    key={t.id}
                    onClick={() => setSelectedTable(t)}
                    className={`rounded-lg border p-3 text-left transition-colors ${statusColor(t.status)} ${selectedTable?.id === t.id ? "ring-2 ring-blue-400" : ""}`}
                  >
                    <div className="font-semibold">{t.tableNumber}</div>
                    <div className="text-xs opacity-80">Seats: {t.capacity}</div>
                    <div className="mt-1 text-xs font-medium">{statusLabel(t.status)}</div>
                  </button>
                ))}
                {filteredTables.length === 0 && (
                  <p className="col-span-full text-sm text-slate-400">No tables found.</p>
                )}
              </div>
            )}

            {selectedTable && (
              <div className="mt-4 rounded-lg border border-slate-700 bg-slate-800 p-3 text-xs">
                <p className="font-semibold">{selectedTable.tableNumber}</p>
                <p className="text-slate-400">Status: {statusLabel(selectedTable.status)}</p>
                <p className="text-slate-400">Capacity: {selectedTable.capacity}</p>
                <p className="text-slate-400">Floor: {selectedTable.section || "Main Floor"}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {selectedTable.status === "FREE" && (
                    <Button size="sm" onClick={handleOccupy}>Occupy</Button>
                  )}
                  {selectedTable.status === "OCCUPIED" && (
                    <>
                      <Button size="sm" onClick={handleMarkOrdered}>Mark Ordered</Button>
                      <Button size="sm" variant="outline" onClick={handleFree}>Free</Button>
                    </>
                  )}
                  {selectedTable.status === "ORDERED" && (
                    <>
                      <Button size="sm" onClick={handleMarkCleaning}>Cleaning</Button>
                      <Button size="sm" variant="outline" onClick={handleFree}>Free</Button>
                    </>
                  )}
                  {selectedTable.status === "CLEANING" && (
                    <Button size="sm" onClick={handleFree}>Mark Available</Button>
                  )}
                </div>
              </div>
            )}
          </aside>

          {/* Center: Menu */}
          <main className="flex-1 overflow-y-auto p-4">
            <div className="mb-4 flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search menu items…"
                  className="pl-9 bg-slate-800 border-slate-700 text-slate-100"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
              {filteredItems.map((q) => (
                <button
                  key={q.id}
                  onClick={() => addToOrder({ itemId: q.itemId, itemName: q.itemName, itemCode: q.itemCode, defaultPrice: q.defaultPrice })}
                  className="rounded-lg border border-slate-700 bg-slate-800 p-3 text-left transition-colors hover:bg-slate-700"
                >
                  <div className="font-medium text-sm">{q.itemName}</div>
                  <div className="mt-1 text-xs text-slate-400">{q.uomCode}</div>
                  <div className="mt-2 text-sm font-bold text-emerald-300">
                    {q.defaultPrice != null ? `Rs. ${q.defaultPrice.toFixed(2)}` : "Rs. (market)"}
                  </div>
                </button>
              ))}
              {filteredItems.length === 0 && (
                <p className="col-span-full text-sm text-slate-400">No menu items found.</p>
              )}
            </div>

            {selectedTable && kitchenOrders.length > 0 && (
              <div className="mt-6">
                <h3 className="mb-2 text-sm font-semibold text-slate-400">Kitchen Orders</h3>
                <div className="space-y-2">
                  {kitchenOrders.map((ko) => (
                    <div key={ko.id} className="rounded-md border border-slate-700 bg-slate-800 p-3 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">Order #{ko.id}</span>
                        <Badge variant="outline">{ko.status}</Badge>
                      </div>
                      <p className="mt-1 text-slate-400">Course {ko.course}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </main>

          {/* Right: Current Order */}
          <aside className="w-full shrink-0 overflow-y-auto border-t border-slate-800 bg-slate-900 p-4 lg:w-80 lg:border-t-0 lg:border-l">
            <div className="mb-3 flex items-center gap-2">
              <ShoppingCart className="h-4 w-4 text-slate-400" />
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">Current Order</h2>
            </div>
            <div className="mb-3 flex gap-2">
              <button
                onClick={() => setOrderMode("dine-in")}
                className={`flex-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${orderMode === "dine-in" ? "bg-blue-600 text-white" : "bg-slate-800 text-slate-400 hover:bg-slate-700"}`}
              >
                Dine-in
              </button>
              <button
                onClick={() => setOrderMode("takeaway")}
                className={`flex-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${orderMode === "takeaway" ? "bg-blue-600 text-white" : "bg-slate-800 text-slate-400 hover:bg-slate-700"}`}
              >
                Takeaway
              </button>
            </div>
            {selectedTable && (
              <div className="mb-3 space-y-2 rounded-lg border border-slate-700 bg-slate-800 p-3 text-xs">
                <p className="font-semibold">{selectedTable.tableNumber}</p>
                <div className="flex items-center gap-2">
                  <Users className="h-3.5 w-3.5 text-slate-400" />
                  <Input
                    type="number"
                    min={1}
                    value={guestCount}
                    onChange={(e) => setGuestCount(Number(e.target.value) || 1)}
                    className="h-7 w-16 bg-slate-900 border-slate-700 text-slate-100"
                  />
                  <span className="text-slate-400">guests</span>
                </div>
                {settings.waiterAssignment && (
                  <div className="flex items-center gap-2">
                    <User className="h-3.5 w-3.5 text-slate-400" />
                    <Input
                      value={waiter}
                      onChange={(e) => setWaiter(e.target.value)}
                      placeholder="Waiter"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100"
                    />
                  </div>
                )}
                {settings.orderNotes && (
                  <div className="flex items-center gap-2">
                    <Input
                      value={orderNotes}
                      onChange={(e) => setOrderNotes(e.target.value)}
                      placeholder="Order notes"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100"
                    />
                  </div>
                )}
              </div>
            )}
            <div className="space-y-2">
              {orderLines.map((line) => (
                <div key={line.itemId} className="rounded-md border border-slate-700 bg-slate-800 p-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium">{line.name}</span>
                    <button onClick={() => removeLine(line.itemId)} className="text-slate-400 hover:text-red-400">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                  <div className="mt-1 flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <Button size="sm" variant="outline" className="h-6 w-6 p-0" onClick={() => updateQty(line.itemId, -1)}>
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="w-6 text-center text-sm">{line.qty}</span>
                      <Button size="sm" variant="outline" className="h-6 w-6 p-0" onClick={() => updateQty(line.itemId, 1)}>
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                    <span className="text-sm font-semibold">Rs. {(line.price * line.qty).toFixed(2)}</span>
                  </div>
                </div>
              ))}
              {orderLines.length === 0 && (
                <p className="text-sm text-slate-400">No items added.</p>
              )}
            </div>

            <div className="mt-4 space-y-1 border-t border-slate-700 pt-3 text-sm">
              <div className="flex justify-between text-slate-400">
                <span>Subtotal</span>
                <span>Rs. {subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Tax (at billing)</span>
                <span>Rs. {tax.toFixed(2)}</span>
              </div>
              <div className="flex justify-between font-bold text-slate-100">
                <span>Total</span>
                <span>Rs. {total.toFixed(2)}</span>
              </div>
            </div>

            <div className="mt-4 space-y-2">
              <Button
                className="w-full"
                disabled={orderLines.length === 0 || !session || checkingOut}
                onClick={() => {
                  setIdempotencyKey(crypto.randomUUID());
                  setIsPaymentOpen(true);
                }}
              >
                Generate Bill
              </Button>
               {settings.kot && (
                 <Button
                   className="w-full"
                   disabled={orderLines.length === 0 || sendingKot || (orderMode === "dine-in" && !selectedTable)}
                   onClick={async () => {
                     if (sendingKot) return;
                     if (orderMode === "dine-in" && !selectedTable) return;
                     setSendingKot(true);
                     const lines = orderLines.map((l) => ({
                       itemId: l.itemId,
                       itemName: l.name,
                       quantity: l.qty,
                       notes: orderNotes.trim() || null,
                       course: 1,
                     }));
                     try {
                       if (orderMode === "takeaway") {
                         await posRestaurantService.fireGeneralKitchenOrder(waiter || null, lines);
                       } else if (selectedTable) {
                         await posRestaurantService.fireKitchenOrder(selectedTable.id, waiter || null, lines);
                         await posRestaurantService.markOrdered(selectedTable.id);
                         loadKitchenOrders(selectedTable.id);
                       }
                       toast.success("Sent to kitchen");
                       setOrderLines([]);
                       setOrderNotes("");
                       loadTables();
                     } catch {
                       toast.error("Failed to send to kitchen");
                     } finally {
                       setSendingKot(false);
                     }
                   }}
                 >
                   {sendingKot ? "Sending…" : "Send to Kitchen"}
                 </Button>
               )}
              <Button variant="outline" className="w-full" disabled={orderLines.length === 0} onClick={() => setOrderLines([])}>
                Clear Order
              </Button>
            </div>
          </aside>
        </div>
      )}

      {activeTab === "kds" && (
        <div className="flex flex-1 flex-col p-4">
          <div className="mb-4 flex flex-wrap items-center gap-2">
            {(["ALL", "PENDING", "PREPARING", "READY"] as const).map((f) => (
              <button
                key={f}
                onClick={() => setKdsFilter(f)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  kdsFilter === f ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
                }`}
              >
                {f === "ALL" ? "All" : f === "PENDING" ? "New" : f}
              </button>
            ))}
            <Button variant="outline" size="sm" onClick={loadKds} className="ml-auto">
              Refresh
            </Button>
          </div>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {kdsOrders
              .filter((o) => kdsFilter === "ALL" || o.status === kdsFilter)
              .map((o) => {
                const table = tables.find((t) => t.id === o.tableId);
                const lines = kdsLines[o.id] ?? [];
                const station = (name: string) => {
                  const n = name.toLowerCase();
                  if (n.includes("tea") || n.includes("coffee") || n.includes("beverage") || n.includes("drink")) return "Beverage";
                  if (n.includes("momo") || n.includes("snack") || n.includes("fries")) return "Snacks";
                  return "Hot Kitchen";
                };
                return (
                  <Card key={o.id} className="bg-slate-900 border-slate-700">
                    <CardHeader className="pb-2">
                      <div className="flex items-center justify-between">
                        <CardTitle className="text-sm">KOT #{o.id}</CardTitle>
                        <Badge variant="outline">{o.status === "PENDING" ? "New" : o.status}</Badge>
                      </div>
                      <p className="text-xs text-slate-400">
                         {o.tableId ? `Table ${table?.tableNumber ?? o.tableId}` : "Takeaway"} · {o.waiter || "—"} · {new Date(o.firedAt).toLocaleTimeString()}
                      </p>
                    </CardHeader>
                    <CardContent className="space-y-2 text-xs">
                      {lines.map((l, idx) => (
                        <div key={idx} className="flex items-center justify-between border-b border-slate-800 pb-1">
                          <span>
                            {l.itemName} <span className="text-slate-500">({station(l.itemName || "")})</span>
                          </span>
                          <span className="font-semibold">x{l.quantity}</span>
                        </div>
                      ))}
                      {lines.length === 0 && <p className="text-slate-500">No items</p>}
                      {lines.some((l) => l.notes) && (
                        <p className="text-amber-300">Notes: {lines.map((l) => l.notes).filter(Boolean).join(", ")}</p>
                      )}
                      <div className="flex gap-2 pt-2">
                        {o.status === "PENDING" && (
                          <Button size="sm" onClick={async () => { await posRestaurantService.updateKitchenOrderStatus(o.id, "PREPARING"); toast.success("Preparing"); loadKds(); }}>
                            Start
                          </Button>
                        )}
                        {o.status === "PREPARING" && (
                          <Button size="sm" onClick={async () => { await posRestaurantService.updateKitchenOrderStatus(o.id, "READY"); toast.success(`Order #${o.id} ready`); loadKds(); }}>
                            Ready
                          </Button>
                        )}
                        {o.status === "READY" && (
                          <Button size="sm" onClick={async () => { await posRestaurantService.updateKitchenOrderStatus(o.id, "SERVED"); toast.success("Served"); loadKds(); }}>
                            Served
                          </Button>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            {kdsOrders.filter((o) => kdsFilter === "ALL" || o.status === kdsFilter).length === 0 && (
              <p className="text-sm text-slate-400">No kitchen orders.</p>
            )}
          </div>
        </div>
      )}

      {activeTab === "menu" && (
        <div className="flex flex-1 items-center justify-center p-6">
          <Card>
            <CardHeader>
              <CardTitle>Menu</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">Use Floor View to add items to an order.</p>
            </CardContent>
          </Card>
        </div>
      )}

      <SettlementDialog
        open={isPaymentOpen}
        onOpenChange={setIsPaymentOpen}
        total={total}
        isSubmitting={checkoutState.isLoading}
        onConfirm={handleCheckout}
      />
    </div>
  );
}
