"use client";

import * as React from "react";
import { Loader2, ParkingMeter, Play, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { toast } from "sonner";
import { useIsMobile } from "@/hooks/use-mobile";
import { usePosCart, type CartLine } from "@/hooks/use-pos-cart";
import { useLazyScanPosItemQuery } from "@/services/pos-scan.service";
import { useGetMyOpenPosSessionsQuery } from "@/services/pos-session.service";
import {
  useGetParkedPosTransactionsQuery,
  useCheckoutMutation,
  useParkTransactionMutation,
  useGetPosReceiptQuery,
  type PosPaymentMethod,
  type PosReceipt,
} from "@/services/pos-transaction.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { SessionGate } from "@/components/pos/terminal/session-gate";
import { ScanField } from "@/components/pos/terminal/scan-field";
import { QuickItemGrid } from "@/components/pos/terminal/quick-item-grid";
import { CartPanel } from "@/components/pos/terminal/cart-panel";
import { PaymentDialog } from "@/components/pos/terminal/payment-dialog";
import { ReceiptDialog } from "@/components/pos/terminal/receipt-dialog";
import type { PosQuickItem } from "@/services/pos-quick-item.service";
import { formatPosCurrency as currency } from "@/lib/pos-format";

/**
 * The POS terminal — mobile first.
 *
 * On a phone the layout is a single column: session bar, scan field, quick-item
 * grid, and a sticky total bar that opens the cart as a bottom sheet. On a
 * desktop the cart becomes a permanent side panel. Both share one cart hook and
 * one set of dialogs, so the flow is identical on every device.
 *
 * The server is the single writer of money: every scan resolves the price
 * server-side, the cart re-prices through the preview endpoint on each change,
 * and checkout re-prices again before persisting.
 */
export default function PosTerminalPage() {
  const isMobile = useIsMobile();
  const { data: openSessions, isLoading: isLoadingSessions } = useGetMyOpenPosSessionsQuery();
  const session = openSessions?.[0] ?? null;

  const cart = usePosCart();
  const [scanItem, scanState] = useLazyScanPosItemQuery();
  const [checkout, checkoutState] = useCheckoutMutation();
  const [parkTransaction, parkState] = useParkTransactionMutation();

  const [isCartOpen, setIsCartOpen] = React.useState(false);
  const [isPaymentOpen, setIsPaymentOpen] = React.useState(false);
  const [receiptId, setReceiptId] = React.useState<number | null>(null);
  const { data: receipt } = useGetPosReceiptQuery(receiptId ?? 0, { skip: receiptId === null });

  const { data: parked } = useGetParkedPosTransactionsQuery(undefined, { skip: !session });

  /** Resolves a code to a priced item and adds it to the cart. */
  const handleScan = React.useCallback(
    async (code: string) => {
      try {
        const item = await scanItem(code).unwrap();
        cart.addLine({
          itemId: item.id,
          itemCode: item.sku,
          itemName: item.name,
          uomCode: item.uomCode,
          quantity: 1,
          discountPercentage: 0,
          unitPrice: item.unitPrice,
          vatType: item.vatType,
          vatRate: item.vatRate,
        });
      } catch (error) {
        toast.error(getApiErrorMessage(error) || `Could not resolve '${code}'.`);
      }
    },
    [cart, scanItem]
  );

  /** Tapping a quick item resolves its price through the same scan path. */
  const handleQuickItem = React.useCallback(
    (quickItem: PosQuickItem) => {
      void handleScan(quickItem.itemCode);
    },
    [handleScan]
  );

  const handleCheckout = async (payment: {
    paymentMethod: PosPaymentMethod;
    amountTendered?: number;
    paymentReference?: string;
    customerId?: number;
    customerName?: string;
    customerPanNumber?: string;
  }) => {
    if (!session) return;
    try {
      const transaction = await checkout({
        sessionId: session.id,
        lines: cart.lines.map((line) => ({
          itemId: line.itemId,
          quantity: line.quantity,
          discountPercentage: line.discountPercentage,
        })),
        ...payment,
      }).unwrap();
      cart.clear();
      setIsPaymentOpen(false);
      setIsCartOpen(false);
      setReceiptId(transaction.id);
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Checkout failed.");
    }
  };

  const handlePark = async () => {
    if (!session) return;
    try {
      await parkTransaction({
        sessionId: session.id,
        lines: cart.lines.map((line) => ({
          itemId: line.itemId,
          quantity: line.quantity,
          discountPercentage: line.discountPercentage,
        })),
      }).unwrap();
      cart.clear();
      setIsCartOpen(false);
      toast.success("Cart parked. Resume it any time from the parked list.");
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not park the cart.");
    }
  };

  /** Loads a parked cart back into the working cart (resume). */
  const handleResume = (transactionId: number) => {
    const target = parked?.content.find((t) => t.id === transactionId);
    if (!target) return;
    const lines: CartLine[] = target.lines.map((line) => ({
      itemId: line.itemId,
      itemCode: line.itemCode,
      itemName: line.itemName,
      uomCode: line.uomCode,
      quantity: line.quantity,
      discountPercentage: line.discountPercentage,
      unitPrice: line.unitPrice,
      vatType: line.vatType,
      vatRate: line.vatRate,
    }));
    cart.loadLines(lines);
    toast.success(`Resumed cart with ${lines.length} item(s).`);
  };

  // Cashier hotkeys: F2 pay, F4 park, F8 clear. Function keys never collide
  // with typing and the scan field keeps focus, so these fire mid-scan. The
  // latest park/clear handlers are read through a ref so the listener binds
  // once per session instead of re-subscribing on every cart change.
  const hotkeyActions = React.useRef({ park: handlePark, clear: cart.clear });
  React.useEffect(() => {
    hotkeyActions.current = { park: handlePark, clear: cart.clear };
  });

  React.useEffect(() => {
    if (!session) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (isPaymentOpen || receiptId !== null || cart.lines.length === 0) return;
      if (event.key === "F2") {
        event.preventDefault();
        setIsPaymentOpen(true);
      } else if (event.key === "F4") {
        event.preventDefault();
        void hotkeyActions.current.park();
      } else if (event.key === "F8") {
        event.preventDefault();
        hotkeyActions.current.clear();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [session, isPaymentOpen, receiptId, cart.lines.length]);

  if (isLoadingSessions) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!session) {
    return (
      <div className="p-4">
        <SessionGate />
      </div>
    );
  }

  const cartActions = (
    <>
      <Button
        className="h-12 w-full"
        disabled={cart.lines.length === 0 || checkoutState.isLoading}
        onClick={() => setIsPaymentOpen(true)}
        title="Pay (F2)"
      >
        {checkoutState.isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        Charge {currency(cart.preview.total)}
      </Button>
      <Button
        variant="outline"
        className="h-12 w-full"
        disabled={cart.lines.length === 0 || parkState.isLoading}
        onClick={handlePark}
        title="Park cart (F4)"
      >
        <ParkingMeter className="mr-2 h-4 w-4" />
        Park cart
      </Button>
    </>
  );

  return (
    <div className="flex h-[calc(100dvh-4rem)] flex-col lg:h-[calc(100dvh-4rem)] lg:flex-row">
      {/* Main column: session bar, scan, quick grid */}
      <div className="flex-1 space-y-4 overflow-y-auto p-4 pb-28 lg:pb-4">
        <header className="rounded-lg border bg-card p-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-sm font-semibold">{session.registerName}</p>
              <p className="text-xs text-muted-foreground">
                Session {session.sessionNumber} · {session.cashierName}
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted-foreground">Session sales</p>
              <p className="text-sm font-bold">{currency(session.grossSales)}</p>
            </div>
          </div>
        </header>

        <ScanField onScan={(code) => void handleScan(code)} />

        <QuickItemGrid
          registerId={session.registerId}
          onSelect={handleQuickItem}
          disabled={scanState.isLoading}
        />

        {parked && parked.content.length > 0 && (
          <section className="rounded-lg border p-3">
            <h2 className="mb-2 text-sm font-semibold">Parked carts</h2>
            <ul className="space-y-1">
              {parked.content.map((transaction) => (
                <li key={transaction.id} className="flex items-center justify-between gap-2">
                  <span className="text-sm text-muted-foreground">
                    {transaction.lines.length} item(s) · {currency(transaction.totalAmount)}
                  </span>
                  <Button size="sm" variant="secondary" onClick={() => handleResume(transaction.id)}>
                    <Play className="mr-1 h-3 w-3" />
                    Resume
                  </Button>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>

      {/* Desktop: permanent cart panel */}
      {!isMobile && (
        <aside className="w-96 shrink-0 overflow-y-auto border-l bg-card p-4">
          <CartPanel
            lines={cart.lines}
            preview={cart.preview}
            isPreviewing={cart.isPreviewing}
            onQuantityChange={cart.setQuantity}
            onDiscountChange={cart.setDiscount}
            onRemove={cart.removeLine}
            onClear={cart.clear}
            actions={cartActions}
          />
        </aside>
      )}

      {/* Mobile: sticky total bar + cart bottom sheet */}
      {isMobile && (
        <>
          <div className="fixed inset-x-0 bottom-0 z-30 border-t bg-card p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
            <Button
              className="h-12 w-full"
              disabled={cart.lines.length === 0}
              onClick={() => setIsCartOpen(true)}
            >
              <ShoppingCart className="mr-2 h-5 w-5" />
              View cart ({cart.lines.length}) · {currency(cart.preview.total)}
            </Button>
          </div>

          <Sheet open={isCartOpen} onOpenChange={setIsCartOpen}>
            <SheetContent side="bottom" className="max-h-[85dvh] overflow-y-auto">
              <SheetHeader>
                <SheetTitle>Cart</SheetTitle>
              </SheetHeader>
              <div className="mt-4">
                <CartPanel
                  lines={cart.lines}
                  preview={cart.preview}
                  isPreviewing={cart.isPreviewing}
                  onQuantityChange={cart.setQuantity}
                  onDiscountChange={cart.setDiscount}
                  onRemove={cart.removeLine}
                  onClear={cart.clear}
                  actions={cartActions}
                />
              </div>
            </SheetContent>
          </Sheet>
        </>
      )}

      <PaymentDialog
        open={isPaymentOpen}
        onOpenChange={setIsPaymentOpen}
        total={cart.preview.total}
        isSubmitting={checkoutState.isLoading}
        onConfirm={(payment) => void handleCheckout(payment)}
      />

      <ReceiptDialog
        open={receiptId !== null}
        onOpenChange={(open) => !open && setReceiptId(null)}
        receipt={(receipt as PosReceipt | undefined) ?? null}
      />
    </div>
  );
}
