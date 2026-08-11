"use client";

import { Banknote, Minus, PackageSearch, Plus, Search, ShieldCheck, Smartphone, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";

import { PrimaryButton } from "@/components/ui/buttons";
import { FormError, Input } from "@/components/ui/form-controls";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { useCartStore } from "@/features/pos/store/cart-store";
import {
  addMoney,
  centsToMoney,
  formatKes,
  moneyToCents,
  multiplyMoney,
} from "@/features/workspace/lib/money";
import { stockForMedicine } from "@/features/workspace/lib/workspace-helpers";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { cn } from "@/lib/cn";

export function PosPage() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const batches = useWorkspaceQuery((state) => state.batches);
  const categories = useWorkspaceQuery((state) => state.categories);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const cashierName = useAuthStore((state) => state.session?.user.displayName ?? "Cashier");
  const canApprovePrescription = usePermission(
    PERMISSIONS.PRESCRIPTION_APPROVE,
  );
  const lines = useCartStore((state) => state.lines);
  const cashTendered = useCartStore((state) => state.cashTendered);
  const paymentMethod = useCartStore((state) => state.paymentMethod);
  const mpesaReference = useCartStore((state) => state.mpesaReference);
  const prescriptionReferenceId = useCartStore(
    (state) => state.prescriptionReferenceId,
  );
  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const setPaymentMethod = useCartStore((state) => state.setPaymentMethod);
  const setMpesaReference = useCartStore((state) => state.setMpesaReference);
  const setCashTendered = useCartStore((state) => state.setCashTendered);
  const setPrescriptionReferenceId = useCartStore(
    (state) => state.setPrescriptionReferenceId,
  );
  const prepareCheckoutKey = useCartStore((state) => state.prepareCheckoutKey);
  const clear = useCartStore((state) => state.clear);
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("ALL");
  const [mobileView, setMobileView] = useState<"products" | "cart">("products");
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [scanStatus, setScanStatus] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    function handleGlobalKeyDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      const isEditing = target?.matches(
        "input, textarea, select, [contenteditable='true']",
      );
      if (event.key !== "/" || isEditing) return;

      event.preventDefault();
      setMobileView("products");
      requestAnimationFrame(() => searchInputRef.current?.focus());
    }

    window.addEventListener("keydown", handleGlobalKeyDown);
    return () => window.removeEventListener("keydown", handleGlobalKeyDown);
  }, []);

  const activeMedicines = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return medicines.filter((medicine) =>
      medicine.status === "ACTIVE" &&
      (categoryId === "ALL" || medicine.categoryId === categoryId) &&
      (!normalized || [medicine.brandName, medicine.genericName, medicine.sku, medicine.barcode].some((value) => value.toLowerCase().includes(normalized))),
    );
  }, [categoryId, medicines, query]);
  const detailedLines = lines.flatMap((line) => {
    const medicine = medicines.find((item) => item.id === line.medicineId);
    return medicine ? [{ ...line, medicine, stock: stockForMedicine(batches, medicine.id) }] : [];
  });
  const total = addMoney(...detailedLines.map((line) => multiplyMoney(line.medicine.sellingPrice, line.quantity)));
  const itemCount = detailedLines.reduce((sum, line) => sum + line.quantity, 0);
  const requiresApproval = detailedLines.some((line) => line.medicine.prescriptionRequired);
  const validCashTendered =
    cashTendered === "" || /^\d+(\.\d{1,2})?$/.test(cashTendered);
  const cashCoversTotal =
    cashTendered === "" || moneyToCents(cashTendered) >= moneyToCents(total);
  const changeDue =
    paymentMethod === "CASH" && cashTendered && validCashTendered
      ? centsToMoney(
          Math.max(0, moneyToCents(cashTendered) - moneyToCents(total)),
        )
      : "0.00";

  function addMedicine(medicineId: string) {
    const medicine = medicines.find((item) => item.id === medicineId);
    const existing = lines.find((line) => line.medicineId === medicineId)?.quantity ?? 0;
    const stock = stockForMedicine(batches, medicineId);
    if (existing < stock) {
      addItem(medicineId);
      setScanStatus(`${medicine?.brandName ?? "Product"} added to the cart.`);
      return true;
    }
    setScanStatus(
      stock === 0
        ? `${medicine?.brandName ?? "Product"} is out of stock.`
        : `All available ${medicine?.brandName ?? "product"} units are already in the cart.`,
    );
    return false;
  }

  function handleSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key !== "Enter") return;
    const searchValue = query.trim();
    if (!searchValue) return;
    const exact = medicines.find((medicine) => medicine.status === "ACTIVE" && (medicine.barcode === searchValue || medicine.sku.toLowerCase() === searchValue.toLowerCase()));
    event.preventDefault();
    if (exact) {
      addMedicine(exact.id);
      setQuery("");
    } else {
      setScanStatus("No exact SKU or barcode was found.");
    }
  }

  async function handleCheckout() {
    setCheckoutError(null);
    setSubmitting(true);
    try {
      const saleId = await workspaceGateway.completeSale({
        idempotencyKey: prepareCheckoutKey(),
        cashierName,
        items: detailedLines.map(({ lineId, medicineId, quantity }) => ({
          lineId,
          medicineId,
          quantity,
        })),
        paymentMethod,
        mpesaReference,
        pharmacistApproved: canApprovePrescription,
        cashTendered: cashTendered || total,
        prescriptionReferenceId: requiresApproval
          ? prescriptionReferenceId.trim()
          : undefined,
      });
      clear();
      router.push(`/sales/${saleId}?completed=1`);
    } catch (error) {
      setCheckoutError(
        getWorkspaceErrorMessage(error, "Checkout could not be completed."),
      );
      setSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-[calc(100vh-4rem)] content-start xl:grid-cols-[minmax(0,1fr)_430px]">
      <div className="sticky top-16 z-20 col-span-full grid grid-cols-2 gap-1 border-b border-[var(--border)] bg-white p-2 xl:hidden">
        <button
          type="button"
          onClick={() => setMobileView("products")}
          className={cn(
            "h-10 rounded-md text-sm font-semibold",
            mobileView === "products"
              ? "bg-[var(--brand-soft)] text-[var(--brand-strong)]"
              : "text-[var(--text-muted)]",
          )}
        >
          Products
        </button>
        <button
          type="button"
          onClick={() => setMobileView("cart")}
          className={cn(
            "h-10 rounded-md text-sm font-semibold",
            mobileView === "cart"
              ? "bg-[var(--brand-soft)] text-[var(--brand-strong)]"
              : "text-[var(--text-muted)]",
          )}
        >
          Cart ({itemCount})
        </button>
      </div>
      <section className={cn("min-w-0 border-r border-[var(--border)] p-4 sm:p-5 xl:block", mobileView !== "products" && "hidden")}>
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <label className="relative min-w-0 flex-1">
            <span className="sr-only">Search or scan a product</span>
            <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]" size={18} />
            <Input ref={searchInputRef} autoFocus className="h-12 pl-10 text-base" placeholder="Search medicine, SKU, or scan barcode" value={query} onChange={(event) => { setQuery(event.target.value); setScanStatus(""); }} onKeyDown={handleSearchKeyDown} />
          </label>
          <div className="no-scrollbar flex max-w-full gap-1 overflow-x-auto" role="tablist" aria-label="Product categories">
            <button type="button" onClick={() => setCategoryId("ALL")} className={cn("h-10 shrink-0 rounded-md px-3 text-sm font-medium", categoryId === "ALL" ? "bg-[var(--brand)] text-white" : "bg-white text-[var(--text-muted)] hover:bg-[var(--surface-muted)]")}>All</button>
            {categories.map((category) => <button type="button" key={category.id} onClick={() => setCategoryId(category.id)} className={cn("h-10 shrink-0 rounded-md px-3 text-sm font-medium", categoryId === category.id ? "bg-[var(--brand)] text-white" : "bg-white text-[var(--text-muted)] hover:bg-[var(--surface-muted)]")}>{category.name}</button>)}
          </div>
          <p className="sr-only" role="status" aria-live="polite">{scanStatus}</p>
        </div>

        <div className="mt-5 flex items-center justify-between"><h1 className="text-lg font-semibold">Products</h1><span className="text-xs text-[var(--text-muted)]">{activeMedicines.length} results</span></div>
        {activeMedicines.length ? (
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
            {activeMedicines.map((medicine) => {
              const stock = stockForMedicine(batches, medicine.id);
              const inCart = lines.find((line) => line.medicineId === medicine.id)?.quantity ?? 0;
              return (
                <button type="button" disabled={stock === 0 || inCart >= stock} onClick={() => addMedicine(medicine.id)} key={medicine.id} className="flex min-h-32 flex-col rounded-md border border-[var(--border)] bg-white p-3 text-left transition hover:border-[var(--brand)] hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-55">
                  <div className="flex w-full items-start justify-between gap-2"><span className="text-xs font-medium text-[var(--text-muted)]">{medicine.sku}</span>{medicine.prescriptionRequired ? <span className="rounded bg-[var(--accent-soft)] px-1.5 py-0.5 text-xs font-semibold text-[var(--accent)]">Rx</span> : null}</div>
                  <span className="mt-2 line-clamp-2 text-sm font-semibold">{medicine.brandName}</span>
                  <span className="mt-0.5 line-clamp-1 text-xs text-[var(--text-muted)]">{medicine.genericName}</span>
                  <div className="mt-auto flex w-full items-end justify-between gap-2 pt-3"><span className="font-semibold text-[var(--brand-strong)]">{formatKes(medicine.sellingPrice)}</span><span className={cn("text-xs", stock <= medicine.reorderLevel ? "text-[var(--danger)]" : "text-[var(--text-muted)]")}>{stock} left</span></div>
                </button>
              );
            })}
          </div>
        ) : <div className="mt-16 flex flex-col items-center text-center"><PackageSearch aria-hidden="true" className="text-[var(--text-subtle)]" size={32} /><p className="mt-3 text-sm font-semibold">No products found</p><p className="mt-1 text-xs text-[var(--text-muted)]">Adjust the search or category.</p></div>}
      </section>

      <aside className={cn("min-h-[620px] flex-col bg-white xl:sticky xl:top-16 xl:flex xl:h-[calc(100vh-4rem)]", mobileView === "cart" ? "flex" : "hidden")}>
        <div className="border-b border-[var(--border)] px-4 py-4 sm:px-5"><div className="flex items-center justify-between"><h2 className="text-base font-semibold">Current sale</h2><span className="text-xs text-[var(--text-muted)]">{itemCount} {itemCount === 1 ? "item" : "items"}</span></div></div>
        <div className="min-h-48 flex-1 overflow-y-auto">
          {detailedLines.length ? <div className="divide-y divide-[var(--border)]">{detailedLines.map(({ medicine, quantity, stock }) => (
            <div className="p-4" key={medicine.id}>
              <div className="flex items-start gap-3"><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">{medicine.brandName}</p><p className="mt-0.5 text-xs text-[var(--text-muted)]">{formatKes(medicine.sellingPrice)} each · {stock} available</p></div><p className="shrink-0 text-sm font-semibold">{formatKes(multiplyMoney(medicine.sellingPrice, quantity))}</p></div>
              <div className="mt-3 flex items-center justify-between"><div className="flex h-9 items-center rounded-md border border-[var(--border-strong)]"><button type="button" title="Decrease quantity" aria-label={`Decrease ${medicine.brandName} quantity`} onClick={() => setQuantity(medicine.id, quantity - 1)} className="flex size-8 items-center justify-center hover:bg-[var(--surface-muted)]"><Minus aria-hidden="true" size={15} /></button><span className="w-10 text-center text-sm font-semibold">{quantity}</span><button type="button" title="Increase quantity" aria-label={`Increase ${medicine.brandName} quantity`} disabled={quantity >= stock} onClick={() => setQuantity(medicine.id, Math.min(stock, quantity + 1))} className="flex size-8 items-center justify-center hover:bg-[var(--surface-muted)] disabled:opacity-40"><Plus aria-hidden="true" size={15} /></button></div><button type="button" title="Remove item" aria-label={`Remove ${medicine.brandName}`} onClick={() => removeItem(medicine.id)} className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"><Trash2 aria-hidden="true" size={17} /></button></div>
            </div>
          ))}</div> : <div className="flex h-full min-h-56 flex-col items-center justify-center px-5 text-center"><PackageSearch aria-hidden="true" className="text-[var(--text-subtle)]" size={30} /><p className="mt-3 text-sm font-semibold">Cart is empty</p><p className="mt-1 text-xs text-[var(--text-muted)]">Select a product to begin the sale.</p></div>}
        </div>

        <div className="border-t border-[var(--border)] p-4 sm:p-5">
          {!currentShiftId ? <div className="mb-4 rounded-md border border-[var(--border)] bg-[var(--warning-soft)] p-3 text-sm text-[var(--warning)]">Checkout is locked. <Link href="/shifts/current" className="font-semibold underline">Open a shift</Link> to continue.</div> : null}
          <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Payment method">
            <button type="button" role="radio" aria-checked={paymentMethod === "CASH"} onClick={() => setPaymentMethod("CASH")} className={cn("flex h-11 items-center justify-center gap-2 rounded-md border text-sm font-semibold", paymentMethod === "CASH" ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand-strong)]" : "border-[var(--border-strong)] text-[var(--text-muted)]")}><Banknote aria-hidden="true" size={17} /> Cash</button>
            <button type="button" role="radio" aria-checked={paymentMethod === "MPESA"} onClick={() => setPaymentMethod("MPESA")} className={cn("flex h-11 items-center justify-center gap-2 rounded-md border text-sm font-semibold", paymentMethod === "MPESA" ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand-strong)]" : "border-[var(--border-strong)] text-[var(--text-muted)]")}><Smartphone aria-hidden="true" size={17} /> M-Pesa</button>
          </div>
          {paymentMethod === "MPESA" ? <label className="mt-3 block"><span className="mb-1.5 block text-xs font-semibold">Confirmed M-Pesa reference</span><Input autoCapitalize="characters" placeholder="e.g. SGA12ABC34" value={mpesaReference} onChange={(event) => setMpesaReference(event.target.value.toUpperCase())} /></label> : null}
          {paymentMethod === "CASH" ? <label className="mt-3 block"><span className="mb-1.5 block text-xs font-semibold">Cash tendered</span><Input inputMode="decimal" placeholder={total} value={cashTendered} onChange={(event) => setCashTendered(event.target.value)} />{cashTendered && validCashTendered && cashCoversTotal ? <span className="mt-1.5 block text-xs text-[var(--text-muted)]">Change due: {formatKes(changeDue)}</span> : null}</label> : null}
          {requiresApproval && canApprovePrescription ? <label className="mt-3 block rounded-md bg-[var(--accent-soft)] p-3 text-sm"><span className="mb-1.5 flex items-center gap-1.5 font-semibold"><ShieldCheck aria-hidden="true" size={16} /> Prescription reference</span><Input placeholder="Prescription UUID" value={prescriptionReferenceId} onChange={(event) => setPrescriptionReferenceId(event.target.value.trim())} /></label> : null}
          {requiresApproval && !canApprovePrescription ? <div className="mt-3 flex items-start gap-2.5 rounded-md bg-[var(--warning-soft)] p-3 text-sm text-[var(--warning)]"><ShieldCheck aria-hidden="true" className="mt-0.5 shrink-0" size={16} /><span><span className="block font-semibold">Pharmacist approval required</span><span className="mt-0.5 block text-xs">A staff member with prescription approval permission must complete this sale.</span></span></div> : null}
          <div className="my-4 flex items-center justify-between"><span className="text-sm text-[var(--text-muted)]">Total due</span><span className="text-2xl font-semibold">{formatKes(total)}</span></div>
          <FormError message={checkoutError} />
          <PrimaryButton type="button" onClick={() => void handleCheckout()} disabled={submitting || !currentShiftId || detailedLines.length === 0 || (paymentMethod === "MPESA" && !mpesaReference.trim()) || (paymentMethod === "CASH" && (!validCashTendered || !cashCoversTotal)) || (requiresApproval && (!canApprovePrescription || !prescriptionReferenceId.trim()))} className="mt-3 h-12 w-full text-base">{submitting ? "Completing sale..." : `Complete sale - ${formatKes(total)}`}</PrimaryButton>
        </div>
      </aside>
    </div>
  );
}
