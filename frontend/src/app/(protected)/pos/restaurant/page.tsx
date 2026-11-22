"use client";

import * as React from "react";
import {
  Search,
  ShoppingCart,
  Plus,
  Minus,
  Trash2,
  Users,
  User,
  ArrowRightLeft,
  Merge,
  Sparkles,
  Receipt,
  UtensilsCrossed,
  RefreshCw,
  Edit,
  Printer,
  SlidersHorizontal,
  Flame,
  Wine,
  ChefHat,
  Cake,
  Clock,
  Banknote,
  CreditCard,
  Wallet,
  Building2,
  Split,
  CheckCircle2,
  Bike,
  ShoppingBag,
  LayoutGrid,
  Store,
  DoorOpen,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  posRestaurantService,
  type PosTable,
  type KitchenOrder,
  type KitchenOrderLine,
  type TableBillSummaryResponse,
  type KitchenTicketResponse,
  type RestaurantModifierPreset,
} from "@/services/pos-restaurant.service";
import { useGetPosQuickItemGridQuery } from "@/services/pos-quick-item.service";
import { useLazyScanPosItemQuery } from "@/services/pos-scan.service";
import { usePosCapabilities } from "@/config/pos-capabilities";
import { useSelectedCompany } from "@/config/company-context";
import { useAppSelector } from "@/store/hooks";
import { axiosInstance } from "@/services/http-client";
import { useGetMyOpenPosSessionsQuery, useOpenPosSessionMutation } from "@/services/pos-session.service";
import { useGetPosRegistersQuery } from "@/services/pos-register.service";
import { useCheckoutMutation, type PosPaymentMethod, type CheckoutRequest } from "@/services/pos-transaction.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { useGetActiveItemsQuery } from "@/services/item.service";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

type OrderLine = {
  itemId: number;
  name: string;
  price: number;
  qty: number;
  isExistingKot?: boolean;
  station?: string;
  modifiers?: string;
  modifierPrice?: number;
  course?: number;
};

type CompletedReceiptState = {
  isTaxInvoice?: boolean;
  transactionNumber?: string | null;
  transactionDateNp?: string | null;
  tableNumber?: string | null;
  section?: string | null;
  waiter?: string | null;
  customerName?: string | null;
  customerPanNumber?: string | null;
  subtotalAmount?: number | null;
  discountAmount?: number | null;
  discountReason?: string | null;
  serviceChargeAmount?: number | null;
  vatAmount?: number | null;
  totalAmount?: number | null;
  paymentMethod?: string | null;
  amountTendered?: number | null;
  changeAmount?: number | null;
  paymentReference?: string | null;
  roomNumber?: string | null;
  guestName?: string | null;
  splitPaymentDetails?: string | null;
  splitCashAmount?: number | null;
  splitDigitalAmount?: number | null;
  orderType?: string | null;
  tokenNumber?: string | null;
  deliveryPartner?: string | null;
  deliveryAddress?: string | null;
  customerPhone?: string | null;
  deliveryCharge?: number | null;
  orderedLinesSnapshot?: OrderLine[];
  printCount?: number | null;
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
  const [mobileSection, setMobileSection] = React.useState<"tables" | "menu" | "cart">("menu");
  const [tables, setTables] = React.useState<PosTable[]>([]);
  const [loadingTables, setLoadingTables] = React.useState(false);
  const [selectedTable, setSelectedTable] = React.useState<PosTable | null>(null);
  const [kitchenOrders, setKitchenOrders] = React.useState<KitchenOrder[]>([]);
  const [currentBill, setCurrentBill] = React.useState<TableBillSummaryResponse | null>(null);
  const [loadingBill, setLoadingBill] = React.useState(false);

  // KDS & Station Routing State
  const [kdsOrders, setKdsOrders] = React.useState<KitchenOrder[]>([]);
  const [kdsLines, setKdsLines] = React.useState<Record<number, KitchenOrderLine[]>>({});
  const [kdsFilter, setKdsFilter] = React.useState<"ALL" | "PENDING" | "PREPARING" | "READY">("ALL");
  const [kdsStationFilter, setKdsStationFilter] = React.useState<string>("ALL");

  const [search, setSearch] = React.useState("");
  const [orderLines, setOrderLines] = React.useState<OrderLine[]>([]);
  const [orderMode, setOrderMode] = React.useState<"dine-in" | "takeaway" | "delivery">("dine-in");
  const [tokenNumber, setTokenNumber] = React.useState<string>("");
  const [customerPhone, setCustomerPhone] = React.useState<string>("");
  const [deliveryPartner, setDeliveryPartner] = React.useState<string>("Pathao Food");
  const [deliveryAddress, setDeliveryAddress] = React.useState<string>("");
  const [deliveryCharge, setDeliveryCharge] = React.useState<number>(0);
  const [isOnline, setIsOnline] = React.useState<boolean>(true);
  const [selectedCourse, setSelectedCourse] = React.useState<number>(1);
  const [sendingKot, setSendingKot] = React.useState(false);
  const [guestCount, setGuestCount] = React.useState<number>(2);
  const [waiter, setWaiter] = React.useState<string>("");
  const [orderNotes, setOrderNotes] = React.useState<string>("");
  const [isPaymentOpen, setIsPaymentOpen] = React.useState(false);
  const [idempotencyKey, setIdempotencyKey] = React.useState<string | null>(null);
  const [checkingOut, setCheckingOut] = React.useState(false);
  const { data: openSessions } = useGetMyOpenPosSessionsQuery();
  const session = openSessions?.[0] ?? null;
  const [isSessionModalOpen, setIsSessionModalOpen] = React.useState(false);
  const [sessionRegisterId, setSessionRegisterId] = React.useState<string>("");
  const [openingCashAmount, setOpeningCashAmount] = React.useState<string>("0");
  const { data: registersData } = useGetPosRegistersQuery({ status: "ACTIVE", size: 50 });
  const [openPosSession, { isLoading: isOpeningSession }] = useOpenPosSessionMutation();

  const handleOpenSessionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!sessionRegisterId) {
      toast.error("Please select a POS register");
      return;
    }
    try {
      await openPosSession({
        registerId: Number(sessionRegisterId),
        openingCashAmount: Number(openingCashAmount) || 0,
      }).unwrap();
      toast.success("POS Register session opened! Billing is now enabled.");
      setIsSessionModalOpen(false);
    } catch (err: unknown) {
      const msg =
        (err as { data?: { message?: string }; message?: string })?.data?.message ||
        "Failed to open session";
      toast.error(msg);
    }
  };
  const [checkout] = useCheckoutMutation();
  const [floor, setFloor] = React.useState<string>("all");

  // Table Management Dialogs State
  const [isTableModalOpen, setIsTableModalOpen] = React.useState(false);
  const [editingTableId, setEditingTableId] = React.useState<number | null>(null);
  const [tableForm, setTableForm] = React.useState({ tableNumber: "", capacity: 4, section: "Main Floor" });
  const [savingTable, setSavingTable] = React.useState(false);

  // Table Transfer & Merge State
  const [isTransferOpen, setIsTransferOpen] = React.useState(false);
  const [targetTransferTableId, setTargetTransferTableId] = React.useState<string>("");
  const [transferring, setTransferring] = React.useState(false);

  const [isMergeOpen, setIsMergeOpen] = React.useState(false);
  const [targetMergeTableId, setTargetMergeTableId] = React.useState<string>("");
  const [merging, setMerging] = React.useState(false);

  // Modifiers & Add-ons Dialog State
  const [modifierPresets, setModifierPresets] = React.useState<RestaurantModifierPreset[]>([]);
  const [isModifierDialogOpen, setIsModifierDialogOpen] = React.useState(false);
  const [customizingLineIndex, setCustomizingLineIndex] = React.useState<number | null>(null);
  const [selectedModifierTags, setSelectedModifierTags] = React.useState<string[]>([]);
  const [customInstruction, setCustomInstruction] = React.useState<string>("");

  // KOT Ticket Print Dialog State
  const [isTicketDialogOpen, setIsTicketDialogOpen] = React.useState(false);
  const [currentTicket, setCurrentTicket] = React.useState<KitchenTicketResponse | null>(null);
  const [loadingTicket, setLoadingTicket] = React.useState(false);

  // Customers Query for Hospitality Billing
  const { data: customerList } = useGetActiveCustomersQuery();

  // Hospitality Settlement & Bill State
  const [discountType, setDiscountType] = React.useState<"NONE" | "5%" | "10%" | "15%" | "CUSTOM">("NONE");
  const [customDiscountValue, setCustomDiscountValue] = React.useState<string>("");
  const [discountReason, setDiscountReason] = React.useState<string>("");
  const [applyServiceCharge, setApplyServiceCharge] = React.useState<boolean>(true);
  const [applyVat, setApplyVat] = React.useState<boolean>(true);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = React.useState<PosPaymentMethod>("CASH");
  const [amountTenderedInput, setAmountTenderedInput] = React.useState<string>("");
  const [settlementRef, setSettlementRef] = React.useState<string>("");
  const [roomNumber, setRoomNumber] = React.useState<string>("");
  const [guestName, setGuestName] = React.useState<string>("");
  const [splitCash, setSplitCash] = React.useState<string>("");
  const [splitDigital, setSplitDigital] = React.useState<string>("");
  const [settlementCustomerId, setSettlementCustomerId] = React.useState<string>("walk-in");
  const [customerPan, setCustomerPan] = React.useState<string>("");

  // Customer Receipt Print State
  const [completedTx, setCompletedTx] = React.useState<CompletedReceiptState | null>(null);
  const [isReceiptDialogOpen, setIsReceiptDialogOpen] = React.useState<boolean>(false);

  const { data: quickItems } = useGetPosQuickItemGridQuery(0);
  const { data: catalogData } = useGetActiveItemsQuery({ size: 100 });
  const [selectedCategory, setSelectedCategory] = React.useState<string>("ALL");
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

  const loadCurrentBill = React.useCallback(async (tableId: number) => {
    setLoadingBill(true);
    try {
      const res = await posRestaurantService.getTableCurrentBill(tableId);
      const bill = res.data ?? null;
      setCurrentBill(bill);
      if (bill && bill.lines.length > 0) {
        setOrderLines(
          bill.lines.map((l) => ({
            itemId: l.itemId,
            name: l.itemName,
            price: l.unitPrice - (l.modifierPrice || 0),
            qty: l.quantity,
            isExistingKot: true,
            station: l.station || undefined,
            modifiers: l.modifiers || undefined,
            modifierPrice: l.modifierPrice || 0,
          }))
        );
        if (bill.waiter) {
          setWaiter(bill.waiter);
        }
      } else {
        setCurrentBill(null);
        setOrderLines([]);
      }
    } catch {
      setCurrentBill(null);
    } finally {
      setLoadingBill(false);
    }
  }, []);

  const handleSelectTable = React.useCallback(
    (table: PosTable) => {
      setSelectedTable(table);
      setOrderMode("dine-in");
      loadKitchenOrders(table.id);
      loadCurrentBill(table.id);
      if (typeof window !== "undefined" && window.innerWidth < 1024) {
        setMobileSection("menu");
      }
    },
    [loadKitchenOrders, loadCurrentBill]
  );

  const loadKds = React.useCallback(async () => {
    try {
      const res = await posRestaurantService.listAllKitchenOrders(kdsStationFilter);
      const orders = res.data ?? [];
      setKdsOrders(orders);
      const linesMap: Record<number, KitchenOrderLine[]> = {};
      await Promise.all(
        orders.map(async (o) => {
          try {
            const lr = await posRestaurantService.listKitchenOrderLines(o.id, kdsStationFilter);
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
  }, [kdsStationFilter]);

  const loadModifierPresets = React.useCallback(async () => {
    try {
      const res = await posRestaurantService.getModifierPresets();
      setModifierPresets(res.data ?? []);
    } catch {
      // ignore
    }
  }, []);

  React.useEffect(() => {
    if (capabilities.orderTypes && capabilities.serviceSelection) {
      loadTables();
      loadModifierPresets();
    }
  }, [capabilities.orderTypes, capabilities.serviceSelection, loadTables, loadModifierPresets]);

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
    if (activeTab === "kds") {
      loadKds();
      const interval = setInterval(() => {
        loadKds();
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [activeTab, loadKds]);

  React.useEffect(() => {
    if (!settings.tableManagement) {
      setActiveTab("menu");
    }
  }, [settings.tableManagement]);

  // Online / Offline Status Detection
  React.useEffect(() => {
    setIsOnline(typeof window !== "undefined" ? window.navigator.onLine : true);
    const handleOnline = () => {
      setIsOnline(true);
      toast.success("Network connection restored");
    };
    const handleOffline = () => {
      setIsOnline(false);
      toast.warning("Offline mode active. Your draft cart is safely saved locally.");
    };
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  // Restore local draft cart if present on load / company switch
  React.useEffect(() => {
    if (!selectedCompany?.id) return;
    const storageKey = `pos_restaurant_draft_${selectedCompany.id}`;
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed.orderLines) && parsed.orderLines.length > 0 && orderLines.length === 0 && !selectedTable) {
          setOrderLines(parsed.orderLines);
          if (parsed.orderMode) setOrderMode(parsed.orderMode);
          if (parsed.tokenNumber) setTokenNumber(parsed.tokenNumber);
          if (parsed.customerPhone) setCustomerPhone(parsed.customerPhone);
          if (parsed.deliveryPartner) setDeliveryPartner(parsed.deliveryPartner);
          if (parsed.deliveryAddress) setDeliveryAddress(parsed.deliveryAddress);
          if (parsed.deliveryCharge) setDeliveryCharge(parsed.deliveryCharge);
          toast.info("Restored uncommitted draft cart from local backup");
        }
      }
    } catch {
      // ignore
    }
  }, [selectedCompany?.id]);

  // Persist local draft cart on change
  React.useEffect(() => {
    if (!selectedCompany?.id) return;
    const storageKey = `pos_restaurant_draft_${selectedCompany.id}`;
    try {
      const pendingUnsaved = orderLines.filter((l) => !l.isExistingKot);
      if (pendingUnsaved.length > 0) {
        localStorage.setItem(
          storageKey,
          JSON.stringify({
            orderLines: pendingUnsaved,
            orderMode,
            tokenNumber,
            customerPhone,
            deliveryPartner,
            deliveryAddress,
            deliveryCharge,
          })
        );
      } else {
        localStorage.removeItem(storageKey);
      }
    } catch {
      // ignore
    }
  }, [orderLines, orderMode, tokenNumber, customerPhone, deliveryPartner, deliveryAddress, deliveryCharge, selectedCompany?.id]);

  const floors = React.useMemo(() => {
    const sections = new Set<string>();
    tables.forEach((t) => sections.add(t.section || "Main Floor"));
    if (sections.size === 0) sections.add("Main Floor");
    return ["all", ...Array.from(sections)];
  }, [tables]);

  const filteredTables = tables.filter((t) => floor === "all" || (t.section || "Main Floor") === floor);

  const displayItems = React.useMemo(() => {
    const list: Array<{
      id: string | number;
      itemId: number;
      itemName: string;
      itemCode: string;
      uomCode?: string | null;
      defaultPrice: number | null;
      category: string;
      isQuickItem: boolean;
    }> = [];
    const seenIds = new Set<number>();

    (quickItems ?? []).forEach((q) => {
      seenIds.add(q.itemId);
      list.push({
        id: `quick-${q.id}`,
        itemId: q.itemId,
        itemName: q.itemName,
        itemCode: q.itemCode,
        uomCode: q.uomCode,
        defaultPrice: q.defaultPrice,
        category: "Quick Picks",
        isQuickItem: true,
      });
    });

    (catalogData?.content ?? []).forEach((it) => {
      if (!seenIds.has(it.id)) {
        list.push({
          id: `catalog-${it.id}`,
          itemId: it.id,
          itemName: it.nameEn,
          itemCode: it.sku,
          uomCode: it.uomCode,
          defaultPrice: it.sellingPrice,
          category: it.categoryName || "General",
          isQuickItem: false,
        });
      }
    });

    return list;
  }, [quickItems, catalogData?.content]);

  const categories = React.useMemo(() => {
    const cats = new Set<string>();
    if (quickItems && quickItems.length > 0) {
      cats.add("Quick Picks");
    }
    displayItems.forEach((it) => {
      if (it.category && it.category !== "Quick Picks") {
        cats.add(it.category);
      }
    });
    return ["ALL", ...Array.from(cats)];
  }, [quickItems, displayItems]);

  const filteredItems = React.useMemo(() => {
    const term = search.trim().toLowerCase();
    return displayItems.filter((item) => {
      const matchesSearch =
        !term ||
        item.itemName.toLowerCase().includes(term) ||
        item.itemCode.toLowerCase().includes(term);
      const matchesCategory =
        selectedCategory === "ALL" ||
        (selectedCategory === "Quick Picks" && item.isQuickItem) ||
        item.category === selectedCategory;
      return matchesSearch && matchesCategory;
    });
  }, [displayItems, search, selectedCategory]);

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
      const existing = prev.find((l) => l.itemId === item.itemId && !l.isExistingKot && !l.modifiers);
      if (existing) {
        return prev.map((l) => (l === existing ? { ...l, qty: l.qty + 1 } : l));
      }
      return [
        ...prev,
        {
          itemId: item.itemId,
          name: item.itemName,
          price: price ?? 0,
          qty: 1,
          isExistingKot: false,
          course: selectedCourse,
          modifierPrice: 0,
        },
      ];
    });
  };

  const updateQty = (index: number, delta: number) => {
    setOrderLines((prev) =>
      prev
        .map((l, idx) => (idx === index ? { ...l, qty: Math.max(0, l.qty + delta) } : l))
        .filter((l) => l.qty > 0)
    );
  };

  const removeLine = (index: number) => {
    setOrderLines((prev) => prev.filter((_, idx) => idx !== index));
  };

  // Subtotal incorporates modifierPrice add-ons
  const rawSubtotal = orderLines.reduce((s, l) => s + (l.price + (l.modifierPrice || 0)) * l.qty, 0);

  // Live Hospitality Calculation
  let computedDiscount = 0;
  if (discountType === "5%") computedDiscount = rawSubtotal * 0.05;
  else if (discountType === "10%") computedDiscount = rawSubtotal * 0.10;
  else if (discountType === "15%") computedDiscount = rawSubtotal * 0.15;
  else if (discountType === "CUSTOM") computedDiscount = Number(customDiscountValue) || 0;
  computedDiscount = Math.min(computedDiscount, rawSubtotal);

  const netAfterDiscount = Math.max(0, rawSubtotal - computedDiscount);
  const serviceChargeAmount = applyServiceCharge ? netAfterDiscount * 0.10 : 0;
  const taxableBase = netAfterDiscount + serviceChargeAmount;
  const vatAmount = applyVat ? taxableBase * 0.13 : 0;
  const effectiveDeliveryCharge = orderMode === "delivery" ? Number(deliveryCharge) || 0 : 0;
  const finalPayableTotal = Math.round((taxableBase + vatAmount + effectiveDeliveryCharge) * 100) / 100;

  // Tender & change calculations
  const tenderedVal = Number(amountTenderedInput);
  const hasTendered = amountTenderedInput.trim() !== "" && !isNaN(tenderedVal);
  const cashChange = hasTendered ? Math.max(0, tenderedVal - finalPayableTotal) : 0;
  const isCashShort = selectedPaymentMethod === "CASH" && hasTendered && tenderedVal < finalPayableTotal;

  const pendingLines = orderLines.filter((l) => !l.isExistingKot);

  // Modifiers Dialog Handling
  const handleOpenModifierDialog = (index: number) => {
    const line = orderLines[index];
    if (!line || line.isExistingKot) return;
    setCustomizingLineIndex(index);
    const existingTags = line.modifiers ? line.modifiers.split(",").map((s) => s.trim()).filter(Boolean) : [];
    setSelectedModifierTags(existingTags);
    setCustomInstruction("");
    setIsModifierDialogOpen(true);
  };

  const toggleModifierOption = (option: { name: string; priceDelta: number }, multiple: boolean) => {
    const formatted = option.priceDelta > 0 ? `${option.name} (+Rs. ${option.priceDelta})` : option.name;
    setSelectedModifierTags((prev) => {
      if (prev.includes(formatted)) {
        return prev.filter((t) => t !== formatted);
      } else {
        if (!multiple) {
          // Find if another option from this preset was already selected, and swap it
          const cleanName = option.name;
          const filtered = prev.filter((t) => !t.startsWith(cleanName));
          return [...filtered, formatted];
        }
        return [...prev, formatted];
      }
    });
  };

  const handleApplyModifiers = () => {
    if (customizingLineIndex == null) return;
    const allTags = [...selectedModifierTags];
    if (customInstruction.trim()) {
      allTags.push(`Note: ${customInstruction.trim()}`);
    }

    // Compute total modifier price
    let totalModifierPrice = 0;
    modifierPresets.forEach((group) => {
      group.options.forEach((opt) => {
        const formatted = opt.priceDelta > 0 ? `${opt.name} (+Rs. ${opt.priceDelta})` : opt.name;
        if (allTags.includes(formatted)) {
          totalModifierPrice += Number(opt.priceDelta) || 0;
        }
      });
    });

    setOrderLines((prev) =>
      prev.map((l, idx) =>
        idx === customizingLineIndex
          ? {
              ...l,
              modifiers: allTags.length > 0 ? allTags.join(", ") : undefined,
              modifierPrice: totalModifierPrice,
            }
          : l
      )
    );
    setIsModifierDialogOpen(false);
    setCustomizingLineIndex(null);
  };

  // KOT Ticket Print Dialog Handling
  const handleViewTicket = async (orderId: number) => {
    setLoadingTicket(true);
    setIsTicketDialogOpen(true);
    try {
      const res = await posRestaurantService.getKitchenTicket(orderId);
      setCurrentTicket(res.data);
    } catch {
      toast.error("Failed to load KOT ticket");
      setIsTicketDialogOpen(false);
    } finally {
      setLoadingTicket(false);
    }
  };

  const handleOccupy = async () => {
    if (!selectedTable) return;
    try {
      await posRestaurantService.occupy(selectedTable.id);
      toast.success(`Table ${selectedTable.tableNumber} occupied`);
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
      toast.success(`Table ${selectedTable.tableNumber} freed`);
      setSelectedTable(null);
      setCurrentBill(null);
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

  const handleBootstrapTables = async () => {
    try {
      const res = await posRestaurantService.bootstrapTables();
      toast.success(`Bootstrapped ${res.data?.length ?? 10} default tables`);
      await loadTables();
    } catch {
      toast.error("Failed to bootstrap tables");
    }
  };

  const handleOpenAddTable = () => {
    setEditingTableId(null);
    setTableForm({ tableNumber: `T-${tables.length + 1}`, capacity: 4, section: "Main Floor" });
    setIsTableModalOpen(true);
  };

  const handleOpenEditTable = (table: PosTable) => {
    setEditingTableId(table.id);
    setTableForm({
      tableNumber: table.tableNumber,
      capacity: table.capacity,
      section: table.section || "Main Floor",
    });
    setIsTableModalOpen(true);
  };

  const handleSaveTable = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tableForm.tableNumber.trim()) {
      toast.error("Table number is required");
      return;
    }
    setSavingTable(true);
    try {
      if (editingTableId != null) {
        await posRestaurantService.updateTable(editingTableId, tableForm);
        toast.success("Table updated");
      } else {
        await posRestaurantService.createTable(tableForm);
        toast.success("Table created");
      }
      setIsTableModalOpen(false);
      await loadTables();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to save table";
      toast.error(msg);
    } finally {
      setSavingTable(false);
    }
  };

  const handleDeleteTable = async (tableId: number) => {
    if (!confirm("Are you sure you want to delete this table?")) return;
    try {
      await posRestaurantService.deleteTable(tableId);
      toast.success("Table deleted");
      if (selectedTable?.id === tableId) {
        setSelectedTable(null);
        setCurrentBill(null);
        setOrderLines([]);
      }
      await loadTables();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to delete table";
      toast.error(msg);
    }
  };

  const handleTransferTable = async () => {
    if (!selectedTable || !targetTransferTableId) return;
    const toId = Number(targetTransferTableId);
    setTransferring(true);
    try {
      await posRestaurantService.transferTable(selectedTable.id, toId);
      toast.success(`Table ${selectedTable.tableNumber} shifted successfully`);
      setIsTransferOpen(false);
      setTargetTransferTableId("");
      await loadTables();
      const updatedTarget = tables.find((t) => t.id === toId);
      if (updatedTarget) {
        handleSelectTable(updatedTarget);
      } else {
        setSelectedTable(null);
        setCurrentBill(null);
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to transfer table";
      toast.error(msg);
    } finally {
      setTransferring(false);
    }
  };

  const handleMergeTable = async () => {
    if (!selectedTable || !targetMergeTableId) return;
    const intoId = Number(targetMergeTableId);
    setMerging(true);
    try {
      await posRestaurantService.mergeTables(selectedTable.id, intoId);
      toast.success(`Table ${selectedTable.tableNumber} merged successfully`);
      setIsMergeOpen(false);
      setTargetMergeTableId("");
      await loadTables();
      const updatedTarget = tables.find((t) => t.id === intoId);
      if (updatedTarget) {
        handleSelectTable(updatedTarget);
      } else {
        setSelectedTable(null);
        setCurrentBill(null);
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to merge table";
      toast.error(msg);
    } finally {
      setMerging(false);
    }
  };

  const handleOpenSettlement = () => {
    if (orderLines.length === 0) {
      toast.error("Add at least one item before checkout");
      return;
    }
    setDiscountType("NONE");
    setCustomDiscountValue("");
    setDiscountReason("");
    setApplyServiceCharge(true);
    setApplyVat(true);
    setSelectedPaymentMethod("CASH");
    setAmountTenderedInput("");
    setSettlementRef("");
    setRoomNumber("");
    setGuestName("");
    setSplitCash("");
    setSplitDigital("");
    setSettlementCustomerId("walk-in");
    setCustomerPan("");
    setIdempotencyKey(crypto.randomUUID());
    setIsPaymentOpen(true);
  };

  const handleExecuteCheckout = async () => {
    if (checkingOut) return;
    if (!session) {
      toast.error("No open POS session. Please open a session first.");
      return;
    }
    if (orderLines.length === 0) {
      toast.error("Add at least one item before checkout.");
      return;
    }
    if (selectedPaymentMethod === "CASH" && isCashShort) {
      toast.error("Tendered cash is less than the total due amount.");
      return;
    }
    if (selectedPaymentMethod === "ROOM_CHARGE" && !roomNumber.trim()) {
      toast.error("Please enter the hotel guest room number.");
      return;
    }

    setCheckingOut(true);
    const key = idempotencyKey ?? crypto.randomUUID();
    const selectedCust = (customerList ?? []).find((c) => String(c.id) === settlementCustomerId);
    const splitDetails =
      selectedPaymentMethod === "SPLIT"
        ? `Cash: Rs. ${splitCash || 0}, Digital/QR: Rs. ${splitDigital || 0}`
        : undefined;
    const splitCashNum = parseFloat(splitCash) || 0;
    const splitDigitalNum = parseFloat(splitDigital) || 0;

    try {
      const payload: CheckoutRequest = {
        sessionId: session.id,
        tableId: selectedTable ? selectedTable.id : undefined,
        customerId: settlementCustomerId !== "walk-in" && selectedCust ? selectedCust.id : undefined,
        customerName: selectedCust
          ? selectedCust.name
          : selectedPaymentMethod === "ROOM_CHARGE" && guestName.trim()
          ? `Room ${roomNumber.trim()} - ${guestName.trim()}`
          : "Walk-in Customer",
        customerPanNumber: customerPan.trim() || undefined,
        lines: orderLines.map((l) => ({
          itemId: l.itemId,
          quantity: l.qty,
          discountPercentage: 0,
        })),
        paymentMethod: selectedPaymentMethod,
        amountTendered: selectedPaymentMethod === "CASH" && hasTendered ? tenderedVal : finalPayableTotal,
        paymentReference:
          settlementRef.trim() ||
          (selectedPaymentMethod === "ROOM_CHARGE"
            ? `Room ${roomNumber.trim()}${guestName.trim() ? ` (${guestName.trim()})` : ""}`
            : undefined),
        notes: orderNotes.trim() || undefined,
        discountAmount: computedDiscount > 0 ? computedDiscount : undefined,
        discountReason: computedDiscount > 0 ? (discountReason.trim() || `${discountType} discount`) : undefined,
        serviceChargeAmount: serviceChargeAmount > 0 ? serviceChargeAmount : undefined,
        roomNumber: selectedPaymentMethod === "ROOM_CHARGE" ? roomNumber.trim() : undefined,
        guestName: selectedPaymentMethod === "ROOM_CHARGE" ? guestName.trim() : undefined,
        splitPaymentDetails: splitDetails,
        splitCashAmount: selectedPaymentMethod === "SPLIT" && splitCashNum > 0 ? splitCashNum : undefined,
        splitDigitalAmount: selectedPaymentMethod === "SPLIT" && splitDigitalNum > 0 ? splitDigitalNum : undefined,
        orderType: orderMode === "takeaway" ? "TAKEAWAY" : orderMode === "delivery" ? "DELIVERY" : "DINE_IN",
        tokenNumber: tokenNumber.trim() || undefined,
        deliveryPartner: orderMode === "delivery" ? deliveryPartner : undefined,
        deliveryAddress: orderMode === "delivery" ? (deliveryAddress.trim() || undefined) : undefined,
        customerPhone: customerPhone.trim() || undefined,
        deliveryCharge: effectiveDeliveryCharge > 0 ? effectiveDeliveryCharge : undefined,
      };

      const res = await checkout({
        request: payload,
        idempotencyKey: key,
      }).unwrap();

      toast.success(
        selectedTable
          ? `Bill settled! Table ${selectedTable.tableNumber} is settled and marked for cleaning.`
          : "Bill settled successfully"
      );

      setCompletedTx({
        ...res,
        tableNumber: selectedTable?.tableNumber,
        section: selectedTable?.section,
        waiter: waiter || currentBill?.waiter,
        discountAmount: computedDiscount,
        discountReason: discountReason.trim() || (computedDiscount > 0 ? `${discountType} discount` : undefined),
        serviceChargeAmount: serviceChargeAmount,
        vatAmount: vatAmount,
        totalAmount: finalPayableTotal,
        paymentMethod: selectedPaymentMethod,
        amountTendered: selectedPaymentMethod === "CASH" && hasTendered ? tenderedVal : finalPayableTotal,
        changeAmount: cashChange,
        paymentReference: payload.paymentReference,
        roomNumber: roomNumber.trim(),
        guestName: guestName.trim(),
        splitPaymentDetails: splitDetails,
        splitCashAmount: selectedPaymentMethod === "SPLIT" && splitCashNum > 0 ? splitCashNum : undefined,
        splitDigitalAmount: selectedPaymentMethod === "SPLIT" && splitDigitalNum > 0 ? splitDigitalNum : undefined,
        customerName: payload.customerName,
        customerPanNumber: payload.customerPanNumber,
        orderType: orderMode === "takeaway" ? "TAKEAWAY" : orderMode === "delivery" ? "DELIVERY" : "DINE_IN",
        tokenNumber: tokenNumber.trim() || undefined,
        deliveryPartner: orderMode === "delivery" ? deliveryPartner : undefined,
        deliveryAddress: orderMode === "delivery" ? (deliveryAddress.trim() || undefined) : undefined,
        customerPhone: customerPhone.trim() || undefined,
        deliveryCharge: effectiveDeliveryCharge,
        orderedLinesSnapshot: [...orderLines],
      });

      setIsPaymentOpen(false);
      setIsReceiptDialogOpen(true);
      setOrderLines([]);
      setCurrentBill(null);
      setSelectedTable(null);
      setIdempotencyKey(null);
      if (selectedCompany?.id) {
        localStorage.removeItem(`pos_restaurant_draft_${selectedCompany.id}`);
      }
      setTokenNumber("");
      setCustomerPhone("");
      setDeliveryAddress("");
      setDeliveryCharge(0);
      await loadTables();
    } catch (error) {
      const msg =
        (error as { data?: { message?: string }; message?: string })?.data?.message ||
        (error as { message?: string })?.message ||
        "Checkout failed";
      toast.error(msg);
    } finally {
      setCheckingOut(false);
    }
  };

  return (
    <div className="dark flex h-[calc(100dvh-4rem)] flex-col bg-slate-950 text-slate-100 selection:bg-blue-600 selection:text-white">
      {/* Top Navbar */}
      <div className="flex items-center justify-between border-b border-slate-800 bg-slate-900 px-4 py-2">
        <div className="flex gap-2">
          {settings.tableManagement && (
            <button
              onClick={() => {
                setActiveTab("floor");
                setMobileSection("tables");
              }}
              className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                activeTab === "floor" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
              }`}
            >
              <LayoutGrid className="h-4 w-4" />
              Floor View
            </button>
          )}
          <button
            onClick={() => {
              setActiveTab("menu");
              setMobileSection("menu");
            }}
            className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activeTab === "menu" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
            }`}
          >
            <Store className="h-4 w-4" />
            Menu & Counter
          </button>
          {settings.kds && (
            <button
              onClick={() => setActiveTab("kds")}
              className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                activeTab === "kds" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
              }`}
            >
              <ChefHat className="h-4 w-4" />
              Kitchen Display (KDS)
            </button>
          )}
        </div>
        <div className="flex items-center gap-2">
          {/* POS Session Status Indicator */}
          {session ? (
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-emerald-950/40 border-emerald-500/40 text-emerald-400" title={`Register #${session.registerId} - Active Shift`}>
              <span className="flex h-2 w-2 rounded-full bg-emerald-400" />
              <span>Shift #{session.id} (Open)</span>
            </div>
          ) : (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setIsSessionModalOpen(true)}
              className="h-7 text-xs border-amber-500/60 bg-amber-500/10 text-amber-300 hover:bg-amber-500/20 font-medium"
            >
              <DoorOpen className="mr-1 h-3.5 w-3.5 text-amber-400" />
              Open Shift / Session
            </Button>
          )}

          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-slate-900 border-slate-700">
            {isOnline ? (
              <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            ) : (
              <span className="flex h-2 w-2 rounded-full bg-amber-400" />
            )}
            <span className={isOnline ? "text-emerald-400" : "text-amber-400"}>
              {isOnline ? "Online" : "Offline (Local Draft)"}
            </span>
          </div>
          {tables.length === 0 && (
            <Button
              size="sm"
              variant="outline"
              onClick={handleBootstrapTables}
              className="h-8 text-xs border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              <Sparkles className="mr-1.5 h-3.5 w-3.5 text-amber-400" />
              Bootstrap Tables
            </Button>
          )}
          <Button
            size="sm"
            variant="default"
            onClick={handleOpenAddTable}
            className="h-8 text-xs bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-xs"
          >
            <Plus className="mr-1 h-3.5 w-3.5" />
            Add Table
          </Button>
        </div>
      </div>

      {/* Mobile Segmented Navigation Tab Bar */}
      {(activeTab === "floor" || activeTab === "menu") && (
        <div className="flex lg:hidden items-center border-b border-slate-800 bg-slate-900/95 px-3 py-1.5 gap-1.5 shrink-0 z-10">
          {settings.tableManagement && activeTab === "floor" && (
            <button
              type="button"
              onClick={() => setMobileSection("tables")}
              className={cn(
                "flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2 rounded-md text-xs font-semibold transition-all",
                mobileSection === "tables"
                  ? "bg-blue-600 text-white shadow-xs"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/80"
              )}
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              <span className="truncate">Tables {selectedTable ? `(${selectedTable.tableNumber})` : ""}</span>
            </button>
          )}
          <button
            type="button"
            onClick={() => setMobileSection("menu")}
            className={cn(
              "flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2 rounded-md text-xs font-semibold transition-all",
              mobileSection === "menu"
                ? "bg-blue-600 text-white shadow-xs"
                : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/80"
            )}
          >
            <Store className="h-3.5 w-3.5" />
            <span>Menu Items</span>
          </button>
          <button
            type="button"
            onClick={() => setMobileSection("cart")}
            className={cn(
              "flex-1 flex items-center justify-center gap-1.5 py-1.5 px-2 rounded-md text-xs font-semibold transition-all",
              mobileSection === "cart"
                ? "bg-blue-600 text-white shadow-xs"
                : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/80"
            )}
          >
            <ShoppingCart className="h-3.5 w-3.5" />
            <span>Cart {orderLines.length > 0 ? `(${orderLines.length})` : ""}</span>
          </button>
        </div>
      )}

      {(activeTab === "floor" || activeTab === "menu") && (
        <div className="flex flex-1 flex-col lg:flex-row overflow-hidden">
          {/* Left: Floor & Tables */}
          {settings.tableManagement && activeTab === "floor" && (
            <aside className={cn(
              "w-full shrink-0 overflow-y-auto border-b border-slate-800 bg-slate-900 p-4 lg:w-72 lg:border-b-0 lg:border-r",
              mobileSection !== "tables" && "hidden lg:block"
            )}>
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
                    onClick={() => handleSelectTable(t)}
                    className={`rounded-lg border p-3 text-left transition-all ${statusColor(t.status)} ${
                      selectedTable?.id === t.id ? "ring-2 ring-blue-400 ring-offset-1 ring-offset-slate-950" : ""
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-sm">{t.tableNumber}</span>
                      <span className="text-[10px] opacity-75">{t.capacity}P</span>
                    </div>
                    <div className="mt-1 flex items-center justify-between text-xs font-medium">
                      <span>{statusLabel(t.status)}</span>
                      {t.section && <span className="text-[10px] opacity-60 truncate max-w-[65px]">{t.section}</span>}
                    </div>
                  </button>
                ))}
                {filteredTables.length === 0 && (
                  <div className="col-span-full py-6 text-center">
                    <p className="text-sm text-slate-400">No tables configured.</p>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleBootstrapTables}
                      className="mt-3 text-xs border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
                    >
                      <Sparkles className="mr-1.5 h-3.5 w-3.5 text-amber-400" />
                      Seed Default Tables
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* Selected Table Actions Card */}
            {selectedTable && (
              <div className="mt-4 rounded-lg border border-slate-700 bg-slate-800/90 p-3 text-xs shadow-md">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-slate-100">{selectedTable.tableNumber}</span>
                  <div className="flex items-center gap-1">
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-6 w-6 text-slate-400 hover:text-slate-100"
                      onClick={() => handleOpenEditTable(selectedTable)}
                    >
                      <Edit className="h-3 w-3" />
                    </Button>
                    {selectedTable.status === "FREE" && (
                      <Button
                        size="icon"
                        variant="ghost"
                        className="h-6 w-6 text-slate-400 hover:text-red-400"
                        onClick={() => handleDeleteTable(selectedTable.id)}
                      >
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    )}
                  </div>
                </div>
                <div className="mt-1 grid grid-cols-2 gap-1 text-[11px] text-slate-400">
                  <p>Status: <span className="font-semibold text-slate-200">{statusLabel(selectedTable.status)}</span></p>
                  <p>Capacity: <span className="font-semibold text-slate-200">{selectedTable.capacity} Seats</span></p>
                  <p className="col-span-2">Section: <span className="font-semibold text-slate-200">{selectedTable.section || "Main Floor"}</span></p>
                </div>

                <div className="mt-3 flex flex-wrap gap-1.5 pt-2 border-t border-slate-700">
                  {selectedTable.status === "FREE" && (
                    <Button size="sm" className="h-7 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-medium" onClick={handleOccupy}>
                      Occupy
                    </Button>
                  )}
                  {(selectedTable.status === "OCCUPIED" || selectedTable.status === "ORDERED" || selectedTable.status === "BILLING") && (
                    <>
                      {selectedTable.status === "OCCUPIED" && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-7 text-xs border border-violet-500/60 bg-violet-950/40 text-violet-200 hover:bg-violet-900/60 hover:text-white font-medium"
                          onClick={handleMarkOrdered}
                        >
                          Mark Ordered
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-7 text-xs border border-blue-500/60 bg-blue-950/40 text-blue-200 hover:bg-blue-900/60 hover:text-white font-medium"
                        onClick={() => setIsTransferOpen(true)}
                      >
                        <ArrowRightLeft className="mr-1 h-3 w-3" />
                        Shift
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-7 text-xs border border-purple-500/60 bg-purple-950/40 text-purple-200 hover:bg-purple-900/60 hover:text-white font-medium"
                        onClick={() => setIsMergeOpen(true)}
                      >
                        <Merge className="mr-1 h-3 w-3" />
                        Merge
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-7 text-xs border border-amber-500/60 bg-amber-950/40 text-amber-200 hover:bg-amber-900/60 hover:text-white font-medium"
                        onClick={handleMarkCleaning}
                      >
                        Cleaning
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="h-7 text-xs border border-red-500/40 bg-red-950/20 text-red-300 hover:bg-red-900/40 hover:text-red-100 font-medium"
                        onClick={handleFree}
                      >
                        Free
                      </Button>
                    </>
                  )}
                  {selectedTable.status === "CLEANING" && (
                    <Button size="sm" className="h-7 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-medium" onClick={handleFree}>
                      Mark Available
                    </Button>
                  )}
                </div>
              </div>
            )}
          </aside>
          )}

          {/* Center: Menu Catalogue */}
          <main className={cn("flex-1 overflow-y-auto p-4", mobileSection !== "menu" && "hidden lg:block")}>
            <div className="mb-3 flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search food, beverages & bar items…"
                  className="pl-9 bg-slate-800 border-slate-700 text-slate-100"
                />
              </div>
            </div>

            {/* Category Filter Pills */}
            {categories.length > 1 && (
              <div className="mb-4 flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-thin">
                {categories.map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className={`shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                      selectedCategory === cat
                        ? "bg-blue-600 text-white shadow-sm font-semibold"
                        : "bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200 border border-slate-700"
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
              {filteredItems.map((q) => (
                <button
                  key={q.id}
                  onClick={() =>
                    addToOrder({
                      itemId: q.itemId,
                      itemName: q.itemName,
                      itemCode: q.itemCode,
                      defaultPrice: q.defaultPrice,
                    })
                  }
                  className="group rounded-lg border border-slate-700 bg-slate-800/80 p-3 text-left transition-all hover:border-blue-500 hover:bg-slate-800 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-1">
                      <span className="font-semibold text-sm group-hover:text-blue-300 line-clamp-2">
                        {q.itemName}
                      </span>
                      {q.isQuickItem && (
                        <span className="shrink-0 text-[10px] text-amber-400 font-semibold" title="Quick Pick">★</span>
                      )}
                    </div>
                    <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-400">
                      {q.uomCode && <span>{q.uomCode}</span>}
                      {q.category && q.category !== "Quick Picks" && (
                        <span className="truncate max-w-[90px] text-[10px] text-slate-500">• {q.category}</span>
                      )}
                    </div>
                  </div>
                  <div className="mt-3 text-sm font-bold text-emerald-400">
                    {q.defaultPrice != null && q.defaultPrice > 0 ? `Rs. ${q.defaultPrice.toFixed(2)}` : "Rs. (catalog)"}
                  </div>
                </button>
              ))}
              {filteredItems.length === 0 && (
                <p className="col-span-full py-8 text-center text-sm text-slate-400">No items match your search.</p>
              )}
            </div>

            {/* Active KOT Orders Display */}
            {selectedTable && kitchenOrders.length > 0 && (
              <div className="mt-6 border-t border-slate-800 pt-4">
                <div className="mb-2 flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-300">
                    Kitchen Orders for Table {selectedTable.tableNumber}
                  </h3>
                  <Badge variant="outline">{kitchenOrders.length} KOT(s)</Badge>
                </div>
                <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {kitchenOrders.map((ko) => (
                    <div key={ko.id} className="rounded-md border border-slate-700 bg-slate-800/60 p-2.5 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-200">{ko.kotNumber || `KOT #${ko.id}`}</span>
                        <div className="flex items-center gap-1.5">
                          <Button
                            size="icon"
                            variant="ghost"
                            className="h-5 w-5 text-slate-400 hover:text-slate-100"
                            onClick={() => handleViewTicket(ko.id)}
                            title="Print KOT Slip"
                          >
                            <Printer className="h-3 w-3" />
                          </Button>
                          <Badge
                            variant="outline"
                            className={
                              ko.status === "SERVED"
                                ? "text-emerald-400 border-emerald-500/30"
                                : ko.status === "READY"
                                ? "text-blue-400 border-blue-500/30"
                                : "text-amber-400 border-amber-500/30"
                            }
                          >
                            {ko.status}
                          </Badge>
                        </div>
                      </div>
                      <p className="mt-1 text-[11px] text-slate-400">
                        Waiter: {ko.waiter || "—"} · Course {ko.course}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </main>

          {/* Right: Current Order / Live Table Bill */}
          <aside className={cn(
            "w-full shrink-0 overflow-y-auto border-t border-slate-800 bg-slate-900 p-4 lg:w-80 lg:border-t-0 lg:border-l",
            mobileSection !== "cart" && "hidden lg:block"
          )}>
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShoppingCart className="h-4 w-4 text-slate-400" />
                <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">
                  {selectedTable ? `Bill: Table ${selectedTable.tableNumber}` : "Order Cart"}
                </h2>
              </div>
              <div className="flex items-center gap-1">
                {orderLines.length > 0 && (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-6 px-1.5 text-[11px] text-slate-400 hover:text-red-400"
                    onClick={() => {
                      setOrderLines([]);
                      setCurrentBill(null);
                      if (selectedCompany?.id) {
                        localStorage.removeItem(`pos_restaurant_draft_${selectedCompany.id}`);
                      }
                      setTokenNumber("");
                      setCustomerPhone("");
                      setDeliveryAddress("");
                      setDeliveryCharge(0);
                    }}
                    title="Clear Cart"
                  >
                    Clear
                  </Button>
                )}
                {selectedTable && (
                  <Button
                    size="icon"
                    variant="ghost"
                    className="h-6 w-6 text-slate-400 hover:text-slate-100"
                    onClick={() => loadCurrentBill(selectedTable.id)}
                    title="Refresh Table Bill"
                  >
                    <RefreshCw className={`h-3 w-3 ${loadingBill ? "animate-spin" : ""}`} />
                  </Button>
                )}
              </div>
            </div>

            {/* Order Mode & Course Selector */}
            <div className="mb-3 space-y-1.5">
              <div className="flex gap-1.5 bg-slate-900 p-1 rounded-lg border border-slate-800">
                <button
                  onClick={() => setOrderMode("dine-in")}
                  className={`flex-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${
                    orderMode === "dine-in" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
                  }`}
                >
                  Dine-in
                </button>
                <button
                  onClick={() => {
                    setOrderMode("takeaway");
                    setSelectedTable(null);
                    setCurrentBill(null);
                  }}
                  className={`flex-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${
                    orderMode === "takeaway" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
                  }`}
                >
                  Takeaway
                </button>
                <button
                  onClick={() => {
                    setOrderMode("delivery");
                    setSelectedTable(null);
                    setCurrentBill(null);
                  }}
                  className={`flex-1 rounded-md px-2 py-1 text-xs font-medium transition-colors ${
                    orderMode === "delivery" ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
                  }`}
                >
                  Delivery
                </button>
              </div>

              {/* Course Selection */}
              <div className="flex items-center gap-1 bg-slate-800/80 p-1 rounded-md text-[11px]">
                <Clock className="h-3 w-3 text-slate-400 ml-1" />
                <span className="text-slate-400 font-medium mr-1">Course:</span>
                {[
                  { num: 1, label: "1: Starters" },
                  { num: 2, label: "2: Mains" },
                  { num: 3, label: "3: Desserts" },
                ].map((c) => (
                  <button
                    key={c.num}
                    onClick={() => setSelectedCourse(c.num)}
                    className={`flex-1 rounded px-1.5 py-0.5 text-center font-medium transition-colors ${
                      selectedCourse === c.num ? "bg-blue-500 text-white" : "text-slate-400 hover:text-slate-200"
                    }`}
                  >
                    {c.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Takeaway Quick Counter Card */}
            {orderMode === "takeaway" && (
              <div className="mb-3 space-y-2 rounded-lg border border-slate-700 bg-slate-800/90 p-2.5 text-xs">
                <div className="flex items-center justify-between text-slate-200 font-semibold">
                  <span className="flex items-center gap-1.5">
                    <ShoppingBag className="h-3.5 w-3.5 text-blue-400" />
                    Takeaway Counter Pickup
                  </span>
                  <Badge variant="outline" className="border-blue-500/40 bg-blue-500/10 text-blue-300 text-[10px]">
                    Quick Token
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1">
                    <Label className="text-[10px] text-slate-400">Token # (Auto/Manual)</Label>
                    <Input
                      value={tokenNumber}
                      onChange={(e) => setTokenNumber(e.target.value)}
                      placeholder="e.g. TK-012"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100 text-xs font-mono"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-[10px] text-slate-400">Customer Phone</Label>
                    <Input
                      value={customerPhone}
                      onChange={(e) => setCustomerPhone(e.target.value)}
                      placeholder="98xxxxxxxx"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100 text-xs font-mono"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Delivery Platform & Rider Card */}
            {orderMode === "delivery" && (
              <div className="mb-3 space-y-2 rounded-lg border border-slate-700 bg-slate-800/90 p-2.5 text-xs">
                <div className="flex items-center justify-between text-slate-200 font-semibold">
                  <span className="flex items-center gap-1.5">
                    <Bike className="h-3.5 w-3.5 text-emerald-400" />
                    Delivery Order Details
                  </span>
                  <Badge variant="outline" className="border-emerald-500/40 bg-emerald-500/10 text-emerald-300 text-[10px]">
                    Platform Dispatch
                  </Badge>
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px] text-slate-400">Platform / Rider</Label>
                  <div className="flex flex-wrap gap-1">
                    {["Pathao Food", "Foodmandu", "Bhojdeals", "In-House Rider", "Other"].map((partner) => (
                      <button
                        key={partner}
                        type="button"
                        onClick={() => setDeliveryPartner(partner)}
                        className={`rounded px-2 py-0.5 text-[10px] font-medium transition-colors ${
                          deliveryPartner === partner
                            ? "bg-emerald-600 text-white"
                            : "bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-700"
                        }`}
                      >
                        {partner}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1">
                    <Label className="text-[10px] text-slate-400">Customer Phone</Label>
                    <Input
                      value={customerPhone}
                      onChange={(e) => setCustomerPhone(e.target.value)}
                      placeholder="98xxxxxxxx"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100 text-xs font-mono"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-[10px] text-slate-400">Delivery Fee (Rs.)</Label>
                    <Input
                      type="number"
                      min={0}
                      value={deliveryCharge || ""}
                      onChange={(e) => setDeliveryCharge(Number(e.target.value) || 0)}
                      placeholder="0"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100 text-xs font-mono"
                    />
                  </div>
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px] text-slate-400">Drop Address / Landmark</Label>
                  <Input
                    value={deliveryAddress}
                    onChange={(e) => setDeliveryAddress(e.target.value)}
                    placeholder="e.g. Jhamsikhel, near St. Mary's School"
                    className="h-7 bg-slate-900 border-slate-700 text-slate-100 text-xs"
                  />
                </div>
              </div>
            )}

            {/* Live Active KOT Accumulation Banner */}
            {currentBill && currentBill.lines.length > 0 && (
              <div className="mb-3 rounded-lg border border-amber-500/40 bg-amber-500/10 p-2.5 text-xs text-amber-200">
                <div className="flex items-center justify-between font-semibold">
                  <span className="flex items-center gap-1.5">
                    <Receipt className="h-3.5 w-3.5 text-amber-400" />
                    Accumulated Table Bill
                  </span>
                  <span>Rs. {currentBill.subtotal.toFixed(2)}</span>
                </div>
                <p className="mt-0.5 text-[11px] text-slate-300">
                  {currentBill.kotCount} KOT(s) active · Waiter: {currentBill.waiter || "—"}
                </p>
              </div>
            )}

            {orderMode === "dine-in" && selectedTable && (
              <div className="mb-3 space-y-2 rounded-lg border border-slate-700 bg-slate-800 p-3 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-slate-200">Table {selectedTable.tableNumber}</span>
                  <Badge variant="outline" className={statusColor(selectedTable.status)}>
                    {statusLabel(selectedTable.status)}
                  </Badge>
                </div>
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
                      placeholder="Waiter Name"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100"
                    />
                  </div>
                )}
                {settings.orderNotes && (
                  <div className="flex items-center gap-2">
                    <Input
                      value={orderNotes}
                      onChange={(e) => setOrderNotes(e.target.value)}
                      placeholder="Order / Kitchen notes"
                      className="h-7 bg-slate-900 border-slate-700 text-slate-100"
                    />
                  </div>
                )}
              </div>
            )}

            {/* Cart Order Lines */}
            <div className="space-y-2 max-h-[38vh] overflow-y-auto pr-1">
              {orderLines.map((line, idx) => (
                <div key={`${line.itemId}-${idx}`} className="rounded-md border border-slate-700 bg-slate-800 p-2 text-xs">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-sm font-medium text-slate-200">{line.name}</span>
                        {line.isExistingKot ? (
                          <Badge variant="outline" className="text-[9px] px-1 py-0 h-4 border-amber-500/50 text-amber-300">
                            KOT
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="text-[9px] px-1 py-0 h-4 border-emerald-500/50 text-emerald-300">
                            New
                          </Badge>
                        )}
                        {line.station && (
                          <span className="text-[9px] text-slate-400 font-mono">[{line.station}]</span>
                        )}
                      </div>
                      {line.modifiers && (
                        <p className="mt-0.5 text-[11px] text-amber-300/90 font-medium">
                          {line.modifiers}
                        </p>
                      )}
                    </div>
                    <div className="flex items-center gap-1">
                      {!line.isExistingKot && (
                        <Button
                          size="icon"
                          variant="ghost"
                          className="h-5 w-5 text-slate-400 hover:text-blue-300"
                          onClick={() => handleOpenModifierDialog(idx)}
                          title="Modifiers & Add-ons"
                        >
                          <SlidersHorizontal className="h-3 w-3" />
                        </Button>
                      )}
                      <button onClick={() => removeLine(idx)} className="text-slate-400 hover:text-red-400">
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-6 w-6 p-0 border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-bold shrink-0"
                        onClick={() => updateQty(idx, -1)}
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="w-6 text-center text-sm font-semibold">{line.qty}</span>
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-6 w-6 p-0 border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-bold shrink-0"
                        onClick={() => updateQty(idx, 1)}
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                    <div className="text-right">
                      <span className="text-sm font-semibold text-slate-100">
                        Rs. {((line.price + (line.modifierPrice || 0)) * line.qty).toFixed(2)}
                      </span>
                      {line.modifierPrice != null && line.modifierPrice > 0 && (
                        <span className="block text-[10px] text-emerald-400">
                          (incl. +Rs. {line.modifierPrice} add-on)
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
              {orderLines.length === 0 && (
                <p className="text-center py-6 text-xs text-slate-400">No items on this bill.</p>
              )}
            </div>

            {/* Hospitality Summary */}
            <div className="mt-4 space-y-1.5 border-t border-slate-700 pt-3 text-xs">
              <div className="flex justify-between text-slate-400">
                <span>Food & Drink Subtotal</span>
                <span>Rs. {rawSubtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Service Charge (10%)</span>
                <span>Rs. {serviceChargeAmount.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Nepal VAT (13%)</span>
                <span>Rs. {vatAmount.toFixed(2)}</span>
              </div>
              {orderMode === "delivery" && effectiveDeliveryCharge > 0 && (
                <div className="flex justify-between text-slate-400">
                  <span>Delivery Fee ({deliveryPartner})</span>
                  <span>Rs. {effectiveDeliveryCharge.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between font-bold text-slate-100 text-sm pt-1.5 border-t border-slate-800">
                <span>Total Amount</span>
                <span className="text-emerald-400 font-mono text-base">Rs. {finalPayableTotal.toFixed(2)}</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="mt-4 space-y-2">
              {!session && (
                <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 p-2.5 text-xs text-amber-200 space-y-1.5">
                  <div className="flex items-center justify-between font-semibold">
                    <span className="flex items-center gap-1.5">
                      <DoorOpen className="h-4 w-4 text-amber-400" />
                      Register Session Closed
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-300 leading-relaxed">
                    POS transactions require an active shift session to balance the cash drawer.
                  </p>
                  <Button
                    size="sm"
                    className="w-full h-7 text-xs bg-amber-600 hover:bg-amber-700 text-white font-medium"
                    onClick={() => setIsSessionModalOpen(true)}
                  >
                    Open Session to Enable Checkout
                  </Button>
                </div>
              )}

              <Button
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
                disabled={orderLines.length === 0 || !session || checkingOut}
                onClick={handleOpenSettlement}
                title={
                  !session
                    ? "Open a register shift session first"
                    : orderLines.length === 0
                    ? "Add items or select an active table to checkout"
                    : "Settle Bill & Print Tax Invoice"
                }
              >
                <Receipt className="mr-1.5 h-4 w-4" />
                Settle Bill / Checkout
              </Button>

              {settings.kot && (
                <Button
                  className="w-full bg-blue-600 hover:bg-blue-700"
                  disabled={
                    pendingLines.length === 0 ||
                    sendingKot ||
                    (orderMode === "dine-in" && !selectedTable)
                  }
                  onClick={async () => {
                    if (sendingKot || pendingLines.length === 0) return;
                    if (orderMode === "dine-in" && !selectedTable) return;
                    setSendingKot(true);
                    const lines = pendingLines.map((l) => ({
                      itemId: l.itemId,
                      itemName: l.name,
                      quantity: l.qty,
                      notes: orderNotes.trim() || null,
                      course: l.course || selectedCourse,
                      station: l.station || undefined,
                      modifiers: l.modifiers || null,
                      modifierPrice: l.modifierPrice || 0,
                    }));
                    try {
                      if (orderMode === "takeaway" || orderMode === "delivery") {
                        const res = await posRestaurantService.fireGeneralKitchenOrder(waiter || null, lines, {
                          orderType: orderMode.toUpperCase(),
                          tokenNumber: tokenNumber.trim() || undefined,
                          deliveryPartner: orderMode === "delivery" ? deliveryPartner : undefined,
                          customerPhone: customerPhone.trim() || undefined,
                        });
                        if (res.data?.tokenNumber) {
                          setTokenNumber(res.data.tokenNumber);
                        }
                        setOrderLines((prev) => prev.map((l) => ({ ...l, isExistingKot: true })));
                        if (res.data?.id) {
                          handleViewTicket(res.data.id);
                        }
                      } else if (selectedTable) {
                        const res = await posRestaurantService.fireKitchenOrder(selectedTable.id, waiter || null, lines);
                        await posRestaurantService.markOrdered(selectedTable.id);
                        await loadKitchenOrders(selectedTable.id);
                        await loadCurrentBill(selectedTable.id);
                        if (res.data?.id) {
                          handleViewTicket(res.data.id);
                        }
                      }
                      toast.success(`Fired KOT with ${pendingLines.length} item(s)`);
                      setOrderNotes("");
                      await loadTables();
                    } catch {
                      toast.error("Failed to send order to kitchen");
                    } finally {
                      setSendingKot(false);
                    }
                  }}
                >
                  <UtensilsCrossed className="mr-1.5 h-4 w-4" />
                  {sendingKot
                    ? "Firing KOT…"
                    : pendingLines.length > 0
                    ? `Send to Kitchen (${pendingLines.length} new)`
                    : "Send to Kitchen"}
                </Button>
              )}

              <Button
                variant="outline"
                className="w-full h-8 text-xs border border-slate-700 bg-slate-800/80 text-slate-300 hover:bg-slate-800 hover:text-red-400 font-medium"
                disabled={orderLines.length === 0}
                onClick={() => {
                  setOrderLines([]);
                  setCurrentBill(null);
                  if (selectedCompany?.id) {
                    localStorage.removeItem(`pos_restaurant_draft_${selectedCompany.id}`);
                  }
                  setTokenNumber("");
                  setCustomerPhone("");
                  setDeliveryAddress("");
                  setDeliveryCharge(0);
                }}
              >
                Clear Cart
              </Button>
            </div>
          </aside>
        </div>
      )}

      {/* Mobile Floating Cart Summary Bar */}
      {(activeTab === "floor" || activeTab === "menu") && (
        <div className="lg:hidden p-3 bg-slate-900/95 backdrop-blur border-t border-slate-800 flex items-center justify-between z-20 shrink-0">
          <div className="min-w-0 pr-2">
            <p className="text-[11px] text-slate-400 font-medium truncate">
              {selectedTable
                ? `Table ${selectedTable.tableNumber} (${statusLabel(selectedTable.status)})`
                : orderMode === "takeaway"
                ? "Takeaway Counter"
                : "Delivery Order"}
              {orderLines.length > 0 ? ` · ${orderLines.length} item(s)` : ""}
            </p>
            <p className="text-sm font-bold text-emerald-400 font-mono">
              Rs. {finalPayableTotal.toFixed(2)}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Button
              size="sm"
              variant="outline"
              onClick={() => setMobileSection(mobileSection === "cart" ? "menu" : "cart")}
              className="h-8 text-xs border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              {mobileSection === "cart" ? "View Menu" : `View Cart (${orderLines.length})`}
            </Button>
            <Button
              size="sm"
              disabled={orderLines.length === 0 || !session || checkingOut}
              onClick={handleOpenSettlement}
              className="h-8 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-semibold shadow-xs"
            >
              <Receipt className="mr-1 h-3.5 w-3.5" />
              Checkout
            </Button>
          </div>
        </div>
      )}

      {/* Kitchen Display System (KDS) Tab */}
      {activeTab === "kds" && (
        <div className="flex flex-1 flex-col p-4 overflow-y-auto">
          {/* Station Selector Bar */}
          <div className="mb-3 flex flex-wrap items-center gap-2 border-b border-slate-800 pb-3">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider mr-2">
              Kitchen Station:
            </span>
            {[
              { id: "ALL", label: "All Stations", icon: ChefHat },
              { id: "KITCHEN", label: "Main Kitchen", icon: ChefHat },
              { id: "BAR", label: "Bar & Beverage", icon: Wine },
              { id: "TANDOOR", label: "Tandoor & Grill", icon: Flame },
              { id: "DESSERT", label: "Dessert & Bakery", icon: Cake },
            ].map((st) => {
              const Icon = st.icon;
              return (
                <button
                  key={st.id}
                  onClick={() => setKdsStationFilter(st.id)}
                  className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
                    kdsStationFilter === st.id
                      ? "bg-amber-600 text-white shadow-sm"
                      : "bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200"
                  }`}
                >
                  <Icon className="h-3.5 w-3.5" />
                  {st.label}
                </button>
              );
            })}
          </div>

          {/* Status Filters */}
          <div className="mb-4 flex flex-wrap items-center gap-2">
            {(["ALL", "PENDING", "PREPARING", "READY"] as const).map((f) => (
              <button
                key={f}
                onClick={() => setKdsFilter(f)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  kdsFilter === f ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800"
                }`}
              >
                {f === "ALL" ? "All Orders" : f === "PENDING" ? "New KOT" : f}
              </button>
            ))}
            <Button
              variant="outline"
              size="sm"
              onClick={loadKds}
              className="ml-auto text-xs border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
              Refresh KDS
            </Button>
          </div>

          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {kdsOrders
              .filter((o) => kdsFilter === "ALL" || o.status === kdsFilter)
              .map((o) => {
                const table = tables.find((t) => t.id === o.tableId);
                const lines = kdsLines[o.id] ?? [];
                return (
                  <Card key={o.id} className="bg-slate-900 border-slate-700 shadow-md">
                    <CardHeader className="pb-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <CardTitle className="text-sm font-bold">{o.kotNumber || `KOT #${o.id}`}</CardTitle>
                          <Badge variant="outline" className="text-[10px] bg-slate-800">
                            Course {o.course}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Button
                            size="icon"
                            variant="ghost"
                            className="h-6 w-6 text-slate-400 hover:text-slate-100"
                            onClick={() => handleViewTicket(o.id)}
                            title="Print KOT Slip"
                          >
                            <Printer className="h-3.5 w-3.5" />
                          </Button>
                          <Badge
                            variant="outline"
                            className={
                              o.status === "PENDING"
                                ? "bg-amber-500/20 text-amber-300 border-amber-500/40"
                                : o.status === "PREPARING"
                                ? "bg-blue-500/20 text-blue-300 border-blue-500/40"
                                : "bg-emerald-500/20 text-emerald-300 border-emerald-500/40"
                            }
                          >
                            {o.status === "PENDING" ? "New" : o.status}
                          </Badge>
                        </div>
                      </div>
                      <p className="text-xs text-slate-400">
                        {o.tableId ? `Table ${table?.tableNumber ?? o.tableId}` : "Takeaway"} · Waiter: {o.waiter || "—"} ·{" "}
                        {new Date(o.firedAt).toLocaleTimeString()}
                      </p>
                    </CardHeader>
                    <CardContent className="space-y-2 text-xs">
                      {lines.map((l, idx) => (
                        <div key={idx} className="border-b border-slate-800 pb-1.5">
                          <div className="flex items-center justify-between">
                            <span className="font-medium text-slate-200">
                              {l.itemName}
                              <span className="ml-1.5 text-[10px] font-mono text-slate-400">
                                [{l.station || "KITCHEN"}]
                              </span>
                            </span>
                            <span className="font-bold text-slate-100">x{l.quantity}</span>
                          </div>
                          {l.modifiers && (
                            <p className="text-[11px] text-amber-300 font-medium mt-0.5">
                              Modifiers: {l.modifiers}
                            </p>
                          )}
                          {l.notes && (
                            <p className="text-[11px] text-slate-400 italic">
                              Note: {l.notes}
                            </p>
                          )}
                        </div>
                      ))}
                      {lines.length === 0 && <p className="text-slate-500">No items for this station</p>}
                      {o.notes && (
                        <p className="text-amber-300 font-medium">Table Note: {o.notes}</p>
                      )}
                      <div className="flex gap-2 pt-2">
                        {o.status === "PENDING" && (
                          <Button
                            size="sm"
                            className="bg-blue-600 hover:bg-blue-700"
                            onClick={async () => {
                              await posRestaurantService.updateKitchenOrderStatus(o.id, "PREPARING");
                              toast.success(`Preparing ${o.kotNumber || `KOT #${o.id}`}`);
                              loadKds();
                            }}
                          >
                            Start Cooking
                          </Button>
                        )}
                        {o.status === "PREPARING" && (
                          <Button
                            size="sm"
                            className="bg-emerald-600 hover:bg-emerald-700"
                            onClick={async () => {
                              await posRestaurantService.updateKitchenOrderStatus(o.id, "READY");
                              toast.success(`${o.kotNumber || `KOT #${o.id}`} is Ready!`);
                              loadKds();
                            }}
                          >
                            Mark Ready
                          </Button>
                        )}
                        {o.status === "READY" && (
                          <Button
                            size="sm"
                            variant="outline"
                            className="border border-emerald-500/60 bg-emerald-950/30 text-emerald-300 hover:bg-emerald-900/50 hover:text-white font-medium"
                            onClick={async () => {
                              await posRestaurantService.updateKitchenOrderStatus(o.id, "SERVED");
                              toast.success(`${o.kotNumber || `KOT #${o.id}`} Served`);
                              loadKds();
                            }}
                          >
                            Mark Served
                          </Button>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            {kdsOrders.filter((o) => kdsFilter === "ALL" || o.status === kdsFilter).length === 0 && (
              <p className="col-span-full py-12 text-center text-sm text-slate-400">No active kitchen orders for this station.</p>
            )}
          </div>
        </div>
      )}


      {/* Hospitality Bill Settlement Dialog */}
      <Dialog open={isPaymentOpen} onOpenChange={setIsPaymentOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-lg">
              <Receipt className="h-5 w-5 text-emerald-400" />
              Settle Bill {selectedTable ? `- Table ${selectedTable.tableNumber}` : "(Takeaway / Quick Order)"}
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              {customerPan.trim().length === 9 ? "Official B2B Tax Invoice (कर विजक)" : "Abbreviated Restaurant Cash Bill"}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2 text-sm">
            {/* Customer & PAN Selection */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-slate-800/60 p-3 rounded-lg border border-slate-750">
              <div className="space-y-1.5">
                <Label className="text-xs text-slate-300">Customer</Label>
                <Select
                  value={settlementCustomerId}
                  onValueChange={(val) => {
                    setSettlementCustomerId(val);
                    const cust = (customerList ?? []).find((c) => String(c.id) === val);
                    setCustomerPan(cust?.panNumber ?? "");
                  }}
                >
                  <SelectTrigger className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs">
                    <SelectValue placeholder="Walk-in Customer" />
                  </SelectTrigger>
                  <SelectContent className="bg-slate-900 border-slate-700 text-slate-100">
                    <SelectItem value="walk-in">Walk-in Customer</SelectItem>
                    {(customerList ?? []).map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name} {c.panNumber ? `(${c.panNumber})` : ""}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="cust-pan" className="text-xs text-slate-300">Buyer PAN (9 Digits for Tax Invoice)</Label>
                <Input
                  id="cust-pan"
                  value={customerPan}
                  onChange={(e) => setCustomerPan(e.target.value)}
                  placeholder="e.g. 601234567"
                  maxLength={9}
                  className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs"
                />
              </div>
            </div>

            {/* Bill Calculation Breakdown Card */}
            <div className="rounded-lg border border-slate-750 bg-slate-800/40 p-3 space-y-2 text-xs">
              <div className="flex justify-between text-slate-300">
                <span>Food & Drink Subtotal</span>
                <span className="font-mono">Rs. {rawSubtotal.toFixed(2)}</span>
              </div>

              {/* Discount Selector */}
              <div className="pt-2 border-t border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-slate-300 font-semibold">Hospitality Discount:</span>
                  <div className="flex gap-1.5">
                    {(["NONE", "5%", "10%", "15%", "CUSTOM"] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setDiscountType(t)}
                        className={`rounded px-2 py-0.5 text-[11px] font-medium transition-colors ${
                          discountType === t
                            ? "bg-blue-600 text-white font-bold"
                            : "bg-slate-800 text-slate-400 border border-slate-700 hover:bg-slate-700"
                        }`}
                      >
                        {t === "NONE" ? "0%" : t}
                      </button>
                    ))}
                  </div>
                </div>

                {discountType === "CUSTOM" && (
                  <div className="flex items-center gap-2">
                    <Label className="text-[11px] text-slate-400 shrink-0">Discount Rs:</Label>
                    <Input
                      type="number"
                      min={0}
                      max={rawSubtotal}
                      value={customDiscountValue}
                      onChange={(e) => setCustomDiscountValue(e.target.value)}
                      placeholder="Amount"
                      className="bg-slate-900 border-slate-700 text-slate-100 h-7 text-xs w-28"
                    />
                  </div>
                )}

                {computedDiscount > 0 && (
                  <div className="flex items-center gap-2">
                    <Label className="text-[11px] text-slate-400 shrink-0">Discount Reason:</Label>
                    <Input
                      value={discountReason}
                      onChange={(e) => setDiscountReason(e.target.value)}
                      placeholder="e.g. Staff Courtesy, VIP Guest, Manager Discretion"
                      className="bg-slate-900 border-slate-700 text-slate-100 h-7 text-xs flex-1"
                    />
                  </div>
                )}

                {computedDiscount > 0 && (
                  <div className="flex justify-between text-amber-400 font-medium pt-1">
                    <span>Discount Applied ({discountType})</span>
                    <span className="font-mono">- Rs. {computedDiscount.toFixed(2)}</span>
                  </div>
                )}
              </div>

              {/* Service Charge & VAT Checkboxes */}
              <div className="pt-2 border-t border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="sc-toggle"
                      checked={applyServiceCharge}
                      onCheckedChange={(c) => setApplyServiceCharge(!!c)}
                    />
                    <Label htmlFor="sc-toggle" className="text-xs text-slate-300 cursor-pointer">
                      Nepal Hospitality Service Charge (10%)
                    </Label>
                  </div>
                  <span className="font-mono text-slate-300">
                    + Rs. {serviceChargeAmount.toFixed(2)}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="vat-toggle"
                      checked={applyVat}
                      onCheckedChange={(c) => setApplyVat(!!c)}
                    />
                    <Label htmlFor="vat-toggle" className="text-xs text-slate-300 cursor-pointer">
                      Nepal Value Added Tax (13% VAT)
                    </Label>
                  </div>
                  <span className="font-mono text-slate-300">
                    + Rs. {vatAmount.toFixed(2)}
                  </span>
                </div>
              </div>

              {orderMode === "delivery" && effectiveDeliveryCharge > 0 && (
                <div className="flex justify-between text-xs text-slate-300 pt-1 border-t border-slate-800">
                  <span>Delivery Charge ({deliveryPartner})</span>
                  <span className="font-mono text-slate-300">+ Rs. {effectiveDeliveryCharge.toFixed(2)}</span>
                </div>
              )}

              {/* Grand Total */}
              <div className="flex justify-between text-sm font-bold text-slate-100 pt-2 border-t border-slate-700">
                <span className="text-emerald-400 text-base">Grand Total Due</span>
                <span className="text-emerald-400 text-lg font-mono">Rs. {finalPayableTotal.toFixed(2)}</span>
              </div>
            </div>

            {/* Payment Mode Selector Tabs */}
            <div className="space-y-2">
              <Label className="text-xs text-slate-300 font-semibold uppercase tracking-wider">
                Select Payment Mode
              </Label>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                {[
                  { id: "CASH" as const, label: "Cash", icon: Banknote },
                  { id: "DIGITAL_WALLET" as const, label: "Fonepay / QR", icon: Wallet },
                  { id: "CARD" as const, label: "Card", icon: CreditCard },
                  { id: "ROOM_CHARGE" as const, label: "Room Charge", icon: Building2 },
                  { id: "SPLIT" as const, label: "Split", icon: Split },
                ].map((m) => {
                  const Icon = m.icon;
                  const isSelected = selectedPaymentMethod === m.id;
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => setSelectedPaymentMethod(m.id)}
                      className={`flex flex-col items-center justify-center p-2.5 rounded-lg border transition-all text-center ${
                        isSelected
                          ? "bg-blue-600 text-white border-blue-400 shadow-md ring-1 ring-blue-400"
                          : "bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-700"
                      }`}
                    >
                      <Icon className="h-5 w-5 mb-1" />
                      <span className="text-[11px] font-semibold">{m.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Cash Tender & Change Mode */}
            {selectedPaymentMethod === "CASH" && (
              <div className="rounded-lg bg-slate-800/60 border border-slate-750 p-3 space-y-2.5">
                <div className="flex items-center justify-between">
                  <Label htmlFor="tender-input" className="text-xs text-slate-300 font-medium">
                    Cash Tendered from Customer (Rs.)
                  </Label>
                  <div className="flex gap-1.5">
                    {[
                      Math.ceil(finalPayableTotal),
                      500,
                      1000,
                      2000,
                    ].map((amt) => (
                      <button
                        key={amt}
                        type="button"
                        onClick={() => setAmountTenderedInput(String(amt))}
                        className="rounded px-2 py-0.5 text-[10px] bg-slate-700 text-slate-200 hover:bg-slate-600 font-mono"
                      >
                        {amt === Math.ceil(finalPayableTotal) ? "Exact" : `Rs. ${amt}`}
                      </button>
                    ))}
                  </div>
                </div>

                <Input
                  id="tender-input"
                  type="number"
                  step="0.01"
                  min={0}
                  value={amountTenderedInput}
                  onChange={(e) => setAmountTenderedInput(e.target.value)}
                  placeholder={`Exact: Rs. ${finalPayableTotal.toFixed(2)}`}
                  className="bg-slate-900 border-slate-700 text-slate-100 text-lg font-mono h-11"
                />

                {hasTendered && (
                  <div className={`p-2 rounded text-xs font-semibold flex justify-between ${
                    isCashShort ? "bg-red-500/20 text-red-300 border border-red-500/30" : "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                  }`}>
                    <span>{isCashShort ? "⚠ Short Cash Tendered:" : "Change to Return to Guest:"}</span>
                    <span className="font-mono text-sm">
                      Rs. {isCashShort ? (finalPayableTotal - tenderedVal).toFixed(2) : cashChange.toFixed(2)}
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Digital Wallet / QR Mode */}
            {selectedPaymentMethod === "DIGITAL_WALLET" && (
              <div className="rounded-lg bg-slate-800/60 border border-slate-750 p-3 space-y-2 text-xs">
                <div className="p-3 bg-slate-900 rounded-md border border-slate-700 text-center">
                  <p className="font-semibold text-emerald-400">Fonepay / Dynamic QR Ready</p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Scan to Pay Exactly <span className="font-bold text-white">Rs. {finalPayableTotal.toFixed(2)}</span>
                  </p>
                </div>
                <div className="space-y-1">
                  <Label htmlFor="qr-ref" className="text-xs text-slate-300">
                    Payment Reference / Trace ID (Optional)
                  </Label>
                  <Input
                    id="qr-ref"
                    value={settlementRef}
                    onChange={(e) => setSettlementRef(e.target.value)}
                    placeholder="e.g. Fonepay Txn / Trace #12948"
                    className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs"
                  />
                </div>
              </div>
            )}

            {/* Card Swipe Mode */}
            {selectedPaymentMethod === "CARD" && (
              <div className="rounded-lg bg-slate-800/60 border border-slate-750 p-3 space-y-2 text-xs">
                <div className="space-y-1">
                  <Label htmlFor="card-ref" className="text-xs text-slate-300">
                    POS Machine Auth / Slip Reference
                  </Label>
                  <Input
                    id="card-ref"
                    value={settlementRef}
                    onChange={(e) => setSettlementRef(e.target.value)}
                    placeholder="e.g. Visa/Mastercard Auth #847291"
                    className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs"
                  />
                </div>
              </div>
            )}

            {/* Hotel Room Charge Mode */}
            {selectedPaymentMethod === "ROOM_CHARGE" && (
              <div className="rounded-lg bg-slate-800/60 border border-amber-500/30 p-3 space-y-3 text-xs">
                <div className="flex items-center gap-2 text-amber-300 font-semibold">
                  <Building2 className="h-4 w-4" />
                  <span>Hotel Guest Room Charge</span>
                </div>
                <p className="text-[11px] text-slate-400">
                  This bill will be charged to the guest room folio and settled upon room checkout.
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1">
                    <Label htmlFor="room-no" className="text-xs text-slate-300">Room Number *</Label>
                    <Input
                      id="room-no"
                      value={roomNumber}
                      onChange={(e) => setRoomNumber(e.target.value)}
                      placeholder="e.g. 204, Suite-1"
                      required
                      className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs font-semibold"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="guest-name" className="text-xs text-slate-300">Guest Name</Label>
                    <Input
                      id="guest-name"
                      value={guestName}
                      onChange={(e) => setGuestName(e.target.value)}
                      placeholder="e.g. Ramesh Thapa"
                      className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Split Payment Mode */}
            {selectedPaymentMethod === "SPLIT" && (
              <div className="rounded-lg bg-slate-800/60 border border-slate-750 p-3 space-y-3 text-xs">
                <div className="flex items-center gap-2 text-blue-300 font-semibold">
                  <Split className="h-4 w-4" />
                  <span>Split Tender Payment</span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1">
                    <Label htmlFor="split-cash" className="text-xs text-slate-300">Cash Amount (Rs.)</Label>
                    <Input
                      id="split-cash"
                      type="number"
                      step="0.01"
                      min={0}
                      value={splitCash}
                      onChange={(e) => {
                        const val = e.target.value;
                        setSplitCash(val);
                        const num = Number(val) || 0;
                        setSplitDigital(Math.max(0, finalPayableTotal - num).toFixed(2));
                      }}
                      placeholder="e.g. 500"
                      className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs font-mono"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="split-digital" className="text-xs text-slate-300">Digital / Card (Rs.)</Label>
                    <Input
                      id="split-digital"
                      type="number"
                      step="0.01"
                      min={0}
                      value={splitDigital}
                      onChange={(e) => setSplitDigital(e.target.value)}
                      placeholder="e.g. 500"
                      className="bg-slate-900 border-slate-700 text-slate-100 h-9 text-xs font-mono"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>

          <DialogFooter className="flex items-center justify-between pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsPaymentOpen(false)}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Cancel
            </Button>
            <Button
              type="button"
              disabled={checkingOut || isCashShort || (selectedPaymentMethod === "ROOM_CHARGE" && !roomNumber.trim())}
              onClick={handleExecuteCheckout}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
            >
              {checkingOut ? "Settling Bill…" : "Settle & Print Tax Invoice"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Customer 80mm Printable Tax Invoice Receipt Dialog */}
      <Dialog open={isReceiptDialogOpen} onOpenChange={setIsReceiptDialogOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-emerald-400" />
              Settlement Completed
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              Printable 80mm customer tax invoice receipt.
            </DialogDescription>
          </DialogHeader>

          {completedTx && (
            <div className="rounded-lg bg-white p-4 font-mono text-slate-950 text-xs shadow-inner">
              <div className="text-center border-b border-dashed border-slate-400 pb-2 mb-2">
                <h3 className="font-bold text-sm tracking-wider uppercase">
                  {selectedCompany?.name || "RESTAURANT & CAFE"}
                </h3>
                <p className="text-[10px] text-slate-600">
                  Kathmandu, Nepal
                </p>
                <p className="text-[10px] text-slate-600 font-bold">
                  Hospitality POS · Nepal IRD Compliant
                </p>
                <p className="text-[11px] font-bold text-slate-800 mt-1">
                  {completedTx.isTaxInvoice ? "*** TAX INVOICE (कर विजक) ***" : "*** CASH BILL ***"}
                </p>
                {completedTx.printCount && completedTx.printCount > 1 && (
                  <p className="text-[10px] font-bold text-red-600 print:text-black uppercase tracking-wider mt-0.5">
                    *** प्रतिलिपि / COPY (Copy #{completedTx.printCount - 1}) ***
                  </p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-1 text-[10px] border-b border-dashed border-slate-400 pb-2 mb-2">
                <p>Invoice #: <span className="font-bold">{completedTx.transactionNumber}</span></p>
                <p>Date: <span>{new Date().toLocaleDateString()}</span></p>
                <p>Order Type: <span className="font-bold">{completedTx.orderType || (completedTx.tableNumber ? "DINE_IN" : "TAKEAWAY")}</span></p>
                <p>Time: <span>{new Date().toLocaleTimeString()}</span></p>
                <p>Table: <span className="font-bold">{completedTx.tableNumber || "—"}</span></p>
                <p>Waiter: <span>{completedTx.waiter || "—"}</span></p>
                <p>BS Date: <span>{completedTx.transactionDateNp || "—"}</span></p>
                {completedTx.tokenNumber && (
                  <p className="col-span-2 text-center text-xs font-black bg-slate-100 p-1 my-0.5 rounded border border-slate-300">
                    TOKEN #: {completedTx.tokenNumber}
                  </p>
                )}
                {completedTx.orderType === "DELIVERY" && (
                  <div className="col-span-2 space-y-0.5 bg-slate-50 p-1 my-0.5 rounded border border-slate-200">
                    <p>Platform / Rider: <span className="font-bold">{completedTx.deliveryPartner || "In-House Rider"}</span></p>
                    {completedTx.customerPhone && <p>Customer Phone: <span className="font-bold">{completedTx.customerPhone}</span></p>}
                    {completedTx.deliveryAddress && <p>Drop Address: <span className="font-bold">{completedTx.deliveryAddress}</span></p>}
                  </div>
                )}
                {completedTx.customerName && completedTx.customerName !== "Walk-in Customer" && (
                  <p className="col-span-2">Customer: <span className="font-bold">{completedTx.customerName}</span></p>
                )}
                {completedTx.customerPanNumber && (
                  <p className="col-span-2">Buyer PAN: <span className="font-bold">{completedTx.customerPanNumber}</span></p>
                )}
              </div>

              {/* Line Items */}
              <div className="space-y-1 border-b border-dashed border-slate-400 pb-2 mb-2">
                <div className="flex justify-between font-bold text-[10px] pb-1 border-b border-slate-300">
                  <span>QTY  ITEM</span>
                  <span>AMOUNT</span>
                </div>
                {(completedTx.orderedLinesSnapshot ?? []).map((l: OrderLine, i: number) => {
                  const effectivePrice = l.price + (l.modifierPrice || 0);
                  const lineTotal = effectivePrice * l.qty;
                  return (
                    <div key={i}>
                      <div className="flex justify-between font-semibold">
                        <span>{l.qty}x  {l.name}</span>
                        <span>Rs. {lineTotal.toFixed(2)}</span>
                      </div>
                      {l.modifiers && (
                        <p className="pl-4 text-[9px] text-slate-600 font-sans italic">
                          ↳ {l.modifiers}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Totals Breakdown */}
              <div className="space-y-1 text-[11px] border-b border-dashed border-slate-400 pb-2 mb-2">
                <div className="flex justify-between">
                  <span>Subtotal:</span>
                  <span>Rs. {(completedTx.subtotalAmount ?? rawSubtotal).toFixed(2)}</span>
                </div>
                {(completedTx.discountAmount ?? 0) > 0 && (
                  <div className="flex justify-between text-slate-700">
                    <span>Discount ({completedTx.discountReason || "Special"}):</span>
                    <span>- Rs. {Number(completedTx.discountAmount).toFixed(2)}</span>
                  </div>
                )}
                {(completedTx.serviceChargeAmount ?? 0) > 0 && (
                  <div className="flex justify-between">
                    <span>Service Charge (10%):</span>
                    <span>+ Rs. {Number(completedTx.serviceChargeAmount).toFixed(2)}</span>
                  </div>
                )}
                {(completedTx.vatAmount ?? 0) > 0 && (
                  <div className="flex justify-between">
                    <span>VAT (13%):</span>
                    <span>+ Rs. {Number(completedTx.vatAmount).toFixed(2)}</span>
                  </div>
                )}
                {(completedTx.deliveryCharge ?? 0) > 0 && (
                  <div className="flex justify-between">
                    <span>Delivery Charge ({completedTx.deliveryPartner || "Rider"}):</span>
                    <span>+ Rs. {Number(completedTx.deliveryCharge).toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between font-bold text-xs pt-1 border-t border-slate-300">
                  <span>GRAND TOTAL:</span>
                  <span>Rs. {Number(completedTx.totalAmount ?? finalPayableTotal).toFixed(2)}</span>
                </div>
              </div>

              {/* Payment Details */}
              <div className="text-[10px] border-b border-dashed border-slate-400 pb-2 mb-2">
                <p className="font-semibold uppercase">
                  Payment Mode: {completedTx.paymentMethod}
                </p>
                {completedTx.paymentMethod === "CASH" && (
                  <p>
                    Tendered: Rs. {Number(completedTx.amountTendered).toFixed(2)} | Change: Rs. {Number(completedTx.changeAmount).toFixed(2)}
                  </p>
                )}
                {completedTx.paymentMethod === "ROOM_CHARGE" && (
                  <p className="text-amber-800 font-bold">
                    Charged to Room #{completedTx.roomNumber} ({completedTx.guestName || "Guest"})
                  </p>
                )}
                {completedTx.paymentMethod === "DIGITAL_WALLET" && completedTx.paymentReference && (
                  <p>Ref: {completedTx.paymentReference}</p>
                )}
                {completedTx.paymentMethod === "SPLIT" && (
                  <p>{completedTx.splitPaymentDetails}</p>
                )}
              </div>

              <div className="text-center text-[10px] text-slate-500 pt-1">
                *** THANK YOU! PLEASE VISIT AGAIN ***
              </div>
            </div>
          )}

          <DialogFooter className="flex items-center justify-between">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsReceiptDialogOpen(false);
                setCompletedTx(null);
              }}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Done / Return
            </Button>
            <Button
              type="button"
              className="bg-emerald-600 hover:bg-emerald-700"
              onClick={() => window.print()}
            >
              <Printer className="mr-1.5 h-3.5 w-3.5" />
              Print Invoice Slip
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Food Modifiers & Add-ons Dialog */}
      <Dialog open={isModifierDialogOpen} onOpenChange={setIsModifierDialogOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-lg max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <SlidersHorizontal className="h-5 w-5 text-blue-400" />
              Customize: {customizingLineIndex != null ? orderLines[customizingLineIndex]?.name : ""}
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              Select spice levels, cooking preparations, and billable add-ons.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-2">
            {modifierPresets.map((group) => (
              <div key={group.groupName} className="space-y-1.5">
                <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                  {group.groupName} {group.multiple ? "(Multiple)" : "(Choose One)"}
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {group.options.map((opt) => {
                    const formatted = opt.priceDelta > 0 ? `${opt.name} (+Rs. ${opt.priceDelta})` : opt.name;
                    const isSelected = selectedModifierTags.includes(formatted);
                    return (
                      <button
                        key={opt.name}
                        type="button"
                        onClick={() => toggleModifierOption(opt, group.multiple)}
                        className={`rounded-md px-2.5 py-1 text-xs font-medium transition-all ${
                          isSelected
                            ? "bg-blue-600 text-white shadow-sm ring-1 ring-blue-400"
                            : "bg-slate-800 text-slate-300 border border-slate-700 hover:bg-slate-700"
                        }`}
                      >
                        {opt.name}
                        {opt.priceDelta > 0 && (
                          <span className="ml-1 text-[10px] text-emerald-300 font-bold">
                            +Rs. {opt.priceDelta}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}

            <div className="space-y-1.5 pt-2 border-t border-slate-800">
              <Label className="text-xs text-slate-300">Custom Kitchen Note / Instruction</Label>
              <Input
                value={customInstruction}
                onChange={(e) => setCustomInstruction(e.target.value)}
                placeholder="e.g. Pack extra chutney, crispier crust, no salt..."
                className="bg-slate-800 border-slate-700 text-slate-100 h-8 text-xs"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsModifierDialogOpen(false)}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Cancel
            </Button>
            <Button type="button" onClick={handleApplyModifiers} className="bg-blue-600 hover:bg-blue-700">
              Apply Modifiers
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Printable Thermal KOT Ticket Dialog */}
      <Dialog open={isTicketDialogOpen} onOpenChange={setIsTicketDialogOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Printer className="h-5 w-5 text-amber-400" />
              Kitchen Order Ticket (KOT)
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              Printable 80mm thermal receipt slip for kitchen & bar preparation.
            </DialogDescription>
          </DialogHeader>

          {loadingTicket || !currentTicket ? (
            <p className="py-8 text-center text-xs text-slate-400">Loading KOT ticket…</p>
          ) : (
            <div className="rounded-lg bg-white p-4 font-mono text-slate-950 text-xs shadow-inner">
              <div className="text-center border-b border-dashed border-slate-400 pb-2 mb-2">
                <h3 className="font-bold text-sm tracking-wider uppercase">
                  {selectedCompany?.name || "RESTAURANT KITCHEN"}
                </h3>
                <p className="text-[11px] font-semibold text-slate-700">
                  *** KITCHEN ORDER TICKET ***
                </p>
                <p className="text-[10px] text-slate-500">
                  {currentTicket.orderType} · Course {currentTicket.course}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-1 text-[11px] border-b border-dashed border-slate-400 pb-2 mb-2">
                <p>KOT #: <span className="font-bold">{currentTicket.kotNumber}</span></p>
                <p>Table: <span className="font-bold">{currentTicket.tableNumber || currentTicket.orderType || "Takeaway"}</span></p>
                <p>Waiter: <span>{currentTicket.waiter || "—"}</span></p>
                <p>Time: <span>{new Date(currentTicket.firedAt).toLocaleTimeString()}</span></p>
                {currentTicket.tokenNumber && (
                  <p className="col-span-2 text-center text-xs font-black bg-slate-100 p-1 my-0.5 rounded border border-slate-300">
                    TOKEN #: {currentTicket.tokenNumber}
                  </p>
                )}
                {currentTicket.deliveryPartner && (
                  <p className="col-span-2">
                    Delivery: <span className="font-bold">{currentTicket.deliveryPartner}</span> {currentTicket.customerPhone ? `(${currentTicket.customerPhone})` : ""}
                  </p>
                )}
                {currentTicket.section && (
                  <p className="col-span-2">Section: <span>{currentTicket.section}</span></p>
                )}
              </div>

              <div className="space-y-1.5 border-b border-dashed border-slate-400 pb-2 mb-2">
                <div className="flex justify-between font-bold text-[11px] pb-1 border-b border-slate-300">
                  <span>QTY  ITEM</span>
                  <span>STATION</span>
                </div>
                {currentTicket.lines.map((l, i) => (
                  <div key={i}>
                    <div className="flex justify-between font-semibold">
                      <span>{l.quantity}x  {l.itemName}</span>
                      <span className="font-normal text-[10px]">[{l.station || "KITCHEN"}]</span>
                    </div>
                    {l.modifiers && (
                      <p className="pl-4 text-[10px] text-slate-600 font-sans italic">
                        ↳ {l.modifiers}
                      </p>
                    )}
                    {l.notes && (
                      <p className="pl-4 text-[10px] text-slate-600 font-sans italic">
                        ↳ Note: {l.notes}
                      </p>
                    )}
                  </div>
                ))}
              </div>

              {currentTicket.notes && (
                <div className="text-[10px] italic text-slate-700 border-b border-dashed border-slate-400 pb-1 mb-2">
                  Special Instructions: {currentTicket.notes}
                </div>
              )}

              <div className="text-center text-[10px] text-slate-500 pt-1">
                -- Print Time: {new Date().toLocaleTimeString()} --
              </div>
            </div>
          )}

          <DialogFooter className="flex items-center justify-between">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsTicketDialogOpen(false)}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Close
            </Button>
            <Button
              type="button"
              className="bg-emerald-600 hover:bg-emerald-700"
              onClick={() => window.print()}
            >
              <Printer className="mr-1.5 h-3.5 w-3.5" />
              Print Ticket
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Table Create / Edit Dialog */}
      <Dialog open={isTableModalOpen} onOpenChange={setIsTableModalOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <form onSubmit={handleSaveTable}>
            <DialogHeader>
              <DialogTitle>{editingTableId != null ? "Edit Table" : "Add Restaurant Table"}</DialogTitle>
              <DialogDescription className="text-slate-400">
                Configure table seating capacity, identifier and floor location.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4 text-sm">
              <div className="space-y-2">
                <Label htmlFor="tableNumber">Table Number / Code *</Label>
                <Input
                  id="tableNumber"
                  value={tableForm.tableNumber}
                  onChange={(e) => setTableForm({ ...tableForm, tableNumber: e.target.value })}
                  placeholder="e.g. T-1, Cabin-1, Balcony-1"
                  required
                  className="bg-slate-800 border-slate-700 text-slate-100"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="capacity">Seating Capacity (Guests) *</Label>
                <Input
                  id="capacity"
                  type="number"
                  min={1}
                  max={50}
                  value={tableForm.capacity}
                  onChange={(e) => setTableForm({ ...tableForm, capacity: Number(e.target.value) || 2 })}
                  required
                  className="bg-slate-800 border-slate-700 text-slate-100"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="section">Section / Floor Layout</Label>
                <Input
                  id="section"
                  value={tableForm.section}
                  onChange={(e) => setTableForm({ ...tableForm, section: e.target.value })}
                  placeholder="e.g. Main Floor, Garden & Balcony, Rooftop, VIP Cabin"
                  className="bg-slate-800 border-slate-700 text-slate-100"
                />
              </div>
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsTableModalOpen(false)}
                className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
              >
                Cancel
              </Button>
              <Button type="submit" disabled={savingTable} className="bg-blue-600 hover:bg-blue-700">
                {savingTable ? "Saving…" : "Save Table"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Table Transfer Dialog */}
      <Dialog open={isTransferOpen} onOpenChange={setIsTransferOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <ArrowRightLeft className="h-5 w-5 text-blue-400" />
              Transfer Table {selectedTable?.tableNumber}
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              Move all active kitchen orders and guests from Table {selectedTable?.tableNumber} to an available table.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4 text-sm">
            <div className="space-y-2">
              <Label>Select Destination Available Table</Label>
              <Select value={targetTransferTableId} onValueChange={setTargetTransferTableId}>
                <SelectTrigger className="bg-slate-800 border-slate-700 text-slate-100">
                  <SelectValue placeholder="Choose a free table…" />
                </SelectTrigger>
                <SelectContent>
                  {tables
                    .filter((t) => t.id !== selectedTable?.id && t.status === "FREE")
                    .map((t) => (
                      <SelectItem key={t.id} value={String(t.id)}>
                        {t.tableNumber} ({t.capacity} seats - {t.section || "Main Floor"})
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsTransferOpen(false)}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Cancel
            </Button>
            <Button
              type="button"
              disabled={!targetTransferTableId || transferring}
              onClick={handleTransferTable}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {transferring ? "Transferring…" : "Confirm Shift"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Table Merge Dialog */}
      <Dialog open={isMergeOpen} onOpenChange={setIsMergeOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Merge className="h-5 w-5 text-purple-400" />
              Merge Table {selectedTable?.tableNumber}
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              Combine orders from Table {selectedTable?.tableNumber} into another occupied table.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4 text-sm">
            <div className="space-y-2">
              <Label>Select Target Table to Merge Into</Label>
              <Select value={targetMergeTableId} onValueChange={setTargetMergeTableId}>
                <SelectTrigger className="bg-slate-800 border-slate-700 text-slate-100">
                  <SelectValue placeholder="Choose target table…" />
                </SelectTrigger>
                <SelectContent>
                  {tables
                    .filter((t) => t.id !== selectedTable?.id && t.status !== "FREE")
                    .map((t) => (
                      <SelectItem key={t.id} value={String(t.id)}>
                        {t.tableNumber} ({statusLabel(t.status)} - {t.section || "Main Floor"})
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsMergeOpen(false)}
              className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
            >
              Cancel
            </Button>
            <Button
              type="button"
              disabled={!targetMergeTableId || merging}
              onClick={handleMergeTable}
              className="bg-purple-600 hover:bg-purple-700"
            >
              {merging ? "Merging…" : "Confirm Merge"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Open POS Register Session Dialog */}
      <Dialog open={isSessionModalOpen} onOpenChange={setIsSessionModalOpen}>
        <DialogContent className="bg-slate-900 border-slate-700 text-slate-100 sm:max-w-md">
          <form onSubmit={handleOpenSessionSubmit}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <DoorOpen className="h-5 w-5 text-amber-400" />
                Open POS Register Shift
              </DialogTitle>
              <DialogDescription className="text-slate-400">
                Select your billing register counter and declare opening cash float to enable checkout.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4 text-sm">
              <div className="space-y-2">
                <Label>Select Billing Register *</Label>
                <Select value={sessionRegisterId} onValueChange={setSessionRegisterId}>
                  <SelectTrigger className="bg-slate-800 border-slate-700 text-slate-100">
                    <SelectValue placeholder="Choose counter register…" />
                  </SelectTrigger>
                  <SelectContent className="bg-slate-900 border-slate-700 text-slate-100">
                    {(registersData?.content ?? []).map((r) => (
                      <SelectItem key={r.id} value={String(r.id)}>
                        {r.registerName} ({r.registerCode})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="openingCash">Opening Cash Drawer Float (Rs.)</Label>
                <Input
                  id="openingCash"
                  type="number"
                  min={0}
                  value={openingCashAmount}
                  onChange={(e) => setOpeningCashAmount(e.target.value)}
                  placeholder="0"
                  className="bg-slate-800 border-slate-700 text-slate-100 font-mono"
                />
              </div>
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsSessionModalOpen(false)}
                className="border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700 hover:text-white font-medium"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={!sessionRegisterId || isOpeningSession}
                className="bg-emerald-600 hover:bg-emerald-700"
              >
                {isOpeningSession ? "Opening…" : "Start Shift & Enable Billing"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
