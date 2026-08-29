"use client";

import { Banknote, CreditCard, Minus, PackageSearch, Plus, ScanLine, Search, ShieldCheck, Smartphone, Trash2, UserRound } from "lucide-react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";

import { PrimaryButton } from "@/components/ui/buttons";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  type Customer,
  operationsGateway,
} from "@/features/operations/operations-gateway";
import { recordOperationalEvent } from "@/features/operations/operational-metrics";
import { useCartStore } from "@/features/pos/store/cart-store";
import { medicineImage } from "@/features/medicines/lib/medicine-image";
import { BarcodeScanner } from "@/features/pos/components/barcode-scanner";
import {
  type Prescription,
  prescriptionGateway,
} from "@/features/prescriptions/prescription-gateway";
import {
  type CashRegisterConfig,
  getLocalTerminalId,
  setLocalTerminalId,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import { getLastConnectorBarcode } from "@/features/terminals/local-hardware-connector";
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
import { WorkspaceError } from "@/features/workspace/store/workspace-store";
import type { CheckoutInput, PaymentCapabilities } from "@/features/workspace/types";
import type { PosLookupItem } from "@/features/workspace/types";
import { cn } from "@/lib/cn";

export function PosPage() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const batches = useWorkspaceQuery((state) => state.batches);
  const categories = useWorkspaceQuery((state) => state.categories);
  const units = useWorkspaceQuery((state) => state.units);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const canApprovePrescription = usePermission(
    PERMISSIONS.PRESCRIPTION_APPROVE,
  );
  const lines = useCartStore((state) => state.lines);
  const customerId = useCartStore((state) => state.customerId);
  const cashTendered = useCartStore((state) => state.cashTendered);
  const creditAmount = useCartStore((state) => state.creditAmount);
  const paymentMethod = useCartStore((state) => state.paymentMethod);
  const mpesaMode = useCartStore((state) => state.mpesaMode);
  const mpesaPhone = useCartStore((state) => state.mpesaPhone);
  const mpesaReference = useCartStore((state) => state.mpesaReference);
  const prescriptionReferenceId = useCartStore(
    (state) => state.prescriptionReferenceId,
  );
  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const setLineDiscount = useCartStore((state) => state.setLineDiscount);
  const setLineUnit = useCartStore((state) => state.setLineUnit);
  const setPaymentMethod = useCartStore((state) => state.setPaymentMethod);
  const setMpesaMode = useCartStore((state) => state.setMpesaMode);
  const setMpesaPhone = useCartStore((state) => state.setMpesaPhone);
  const setMpesaReference = useCartStore((state) => state.setMpesaReference);
  const setCashTendered = useCartStore((state) => state.setCashTendered);
  const setCreditAmount = useCartStore((state) => state.setCreditAmount);
  const setCustomerId = useCartStore((state) => state.setCustomerId);
  const payments = useCartStore((state) => state.payments);
  const addPayment = useCartStore((state) => state.addPayment);
  const removePayment = useCartStore((state) => state.removePayment);
  const setPrescriptionReferenceId = useCartStore(
    (state) => state.setPrescriptionReferenceId,
  );
  const prepareCheckoutKey = useCartStore((state) => state.prepareCheckoutKey);
  const resetCheckoutKey = useCartStore((state) => state.resetCheckoutKey);
  const clear = useCartStore((state) => state.clear);
  const [query, setQuery] = useState("");
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [paymentCapabilities, setPaymentCapabilities] =
    useState<PaymentCapabilities | null>(null);
  const [registerConfig, setRegisterConfig] =
    useState<CashRegisterConfig | null>(null);
  const [categoryId, setCategoryId] = useState("ALL");
  const [mobileView, setMobileView] = useState<"products" | "cart">("products");
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [lookupResults, setLookupResults] = useState<PosLookupItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [scanStatus, setScanStatus] = useState("");
  const [showScanner, setShowScanner] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [pendingOfflineSales, setPendingOfflineSales] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let active = true;
    void operationsGateway
      .listCustomers()
      .then((rows) => {
        if (active) setCustomers(rows);
      })
      .catch(() => {
        if (active) setCustomers([]);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const handleOnline = async () => {
      setIsOnline(true);
      const raw = localStorage.getItem("pharmacy-pos:offline-queue");
      if (!raw) return;
      let queue: CheckoutInput[];
      try { queue = JSON.parse(raw) as CheckoutInput[]; } catch { return; }
      if (!queue.length) return;
      await recordOperationalEvent({
        eventType: "OFFLINE_QUEUE",
        status: "ATTEMPTED",
        reasonCode: "REPLAY_STARTED",
        source: "pos-offline-queue",
        details: `Replaying ${queue.length} queued sale(s).`,
      });
      const remaining: CheckoutInput[] = [];
      for (const sale of queue) {
        try {
          await workspaceGateway.completeSale(sale);
          await recordOperationalEvent({
            eventType: "OFFLINE_QUEUE",
            status: "SUCCESS",
            reasonCode: "SALE_REPLAYED",
            source: "pos-offline-queue",
            idempotencyKey: sale.idempotencyKey,
          });
        } catch (error) {
          remaining.push(sale);
          await recordOperationalEvent({
            eventType: "OFFLINE_QUEUE",
            status: "FAILED",
            reasonCode: error instanceof WorkspaceError ? error.code : "REPLAY_FAILED",
            source: "pos-offline-queue",
            idempotencyKey: sale.idempotencyKey,
            details: error instanceof Error ? error.message : "Offline sale replay failed.",
          });
        }
      }
      if (remaining.length > 0) {
        localStorage.setItem("pharmacy-pos:offline-queue", JSON.stringify(remaining));
        setPendingOfflineSales(remaining.length);
      } else {
        localStorage.removeItem("pharmacy-pos:offline-queue");
        setPendingOfflineSales(0);
      }
    };
    window.addEventListener("online", handleOnline);
    setPendingOfflineSales(() => {
      try { return JSON.parse(localStorage.getItem("pharmacy-pos:offline-queue") ?? "[]").length; } catch { return 0; }
    });
    return () => window.removeEventListener("online", handleOnline);
  }, []);

  useEffect(() => {
    let active = true;
    void workspaceGateway
      .getPaymentCapabilities()
      .then((capabilities) => {
        if (!active) return;
        setPaymentCapabilities(capabilities);
        if (!capabilities.mpesaStkConfigured) setMpesaMode("MANUAL");
      })
      .catch(() => {
        if (!active) return;
        setPaymentCapabilities({
          mpesaEnvironment: "unavailable",
          mpesaStkConfigured: false,
          pollingSupported: false,
        });
        setMpesaMode("MANUAL");
      });
    return () => {
      active = false;
    };
  }, [setMpesaMode]);

  useEffect(() => {
    if (!canApprovePrescription) return;
    let active = true;
    void prescriptionGateway
      .list()
      .then((items) => {
        if (active) setPrescriptions(items.filter((item) => item.status === "ACTIVE"));
      })
      .catch(() => {
        if (active) setPrescriptions([]);
      });
    return () => {
      active = false;
    };
  }, [canApprovePrescription]);

  useEffect(() => {
    const terminalId = getLocalTerminalId();
    if (!terminalId) return;
    let active = true;
    void terminalGateway.getCashRegisterConfig(terminalId)
      .then((config) => {
        if (active) setRegisterConfig(config);
      })
      .catch(() => {
        if (active) setRegisterConfig(null);
      });
    return () => {
      active = false;
    };
  }, []);

  const [pairCode, setPairCode] = useState("");
  const [pairing, setPairing] = useState(false);
  const [pairError, setPairError] = useState<string | null>(null);
  const [assignedTerminalName, setAssignedTerminalName] = useState<string | null>(null);

  async function activateRegister() {
    if (!pairCode.trim() || pairing) return;
    setPairing(true);
    setPairError(null);
    try {
      const terminal = await terminalGateway.pairByCode(pairCode.trim());
      setLocalTerminalId(terminal.terminalId);
      setAssignedTerminalName(terminal.name);
      setPairCode("");
      try {
        setRegisterConfig(
          await terminalGateway.getCashRegisterConfig(terminal.terminalId),
        );
      } catch {
        setRegisterConfig(null);
      }
    } catch (caught) {
      setPairError(
        caught instanceof Error ? caught.message : "This code could not be activated.",
      );
    } finally {
      setPairing(false);
    }
  }


  useEffect(() => {
    if (!registerConfig) return;
    if (paymentMethod === "CASH" && !registerConfig.cashEnabled && registerConfig.mpesaEnabled) {
      setPaymentMethod("MPESA");
    } else if (paymentMethod === "MPESA" && !registerConfig.mpesaEnabled && registerConfig.cashEnabled) {
      setPaymentMethod("CASH");
    }
  }, [paymentMethod, registerConfig, setPaymentMethod]);

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

  useEffect(() => {
    const normalized = query.trim();
    if (!normalized) return;

    let active = true;
    const timer = window.setTimeout(() => {
      if (active) setSearching(true);
      void workspaceGateway
        .lookupPos(normalized)
        .then((results) => {
          if (!active) return;
          setLookupResults(results);
          setLookupError(null);
        })
        .catch((error) => {
          if (!active) return;
          setLookupResults([]);
          setLookupError(
            getWorkspaceErrorMessage(error, "Product search could not be completed."),
          );
        })
        .finally(() => {
          if (active) setSearching(false);
        });
    }, 200);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [query]);

  const products = useMemo(() => {
    if (query.trim()) {
      return lookupResults.flatMap((result) => {
        const medicine = medicines.find((candidate) => candidate.id === result.id);
        if (!medicine || (categoryId !== "ALL" && result.categoryId !== categoryId)) {
          return [];
        }
        return [{ medicine, stock: result.stockAvailable }];
      });
    }
    return medicines
      .filter(
        (medicine) =>
          medicine.status === "ACTIVE" &&
          (categoryId === "ALL" || medicine.categoryId === categoryId),
      )
      .map((medicine) => ({
        medicine,
        stock: stockForMedicine(batches, medicine.id),
      }));
  }, [batches, categoryId, lookupResults, medicines, query]);
  const detailedLines = lines.flatMap((line) => {
    const medicine = medicines.find((item) => item.id === line.medicineId);
    return medicine ? [{ ...line, medicine, stock: stockForMedicine(batches, medicine.id) }] : [];
  });
  function getMedicineUnits(med: { unitId: string }) {
    const baseUnit = units.find((u) => u.id === med.unitId);
    if (!baseUnit) return [];
    const chain: Array<typeof units[number] & { cumulativeFactor: number }> = [];
    let current: typeof units[number] | undefined = baseUnit;
    let cumulativeFactor = 1;
    while (current) {
      chain.push({ ...current, cumulativeFactor });
      const childFactor = current.conversionFactor ?? 1;
      cumulativeFactor *= childFactor;
      current = current.parentUnitId ? units.find((u) => u.id === current!.parentUnitId) : undefined;
    }
    return chain;
  }
  /** Effective price for a cart line based on selected unit. */
  const effectiveLinePrice = (line: { medicine: (typeof medicines)[number]; unitConversion?: number }) => {
    const conversion = line.unitConversion ?? 1;
    return centsToMoney(Math.round(moneyToCents(multiplyMoney(line.medicine.sellingPrice, conversion))));
  };
  const lineGross = (line: (typeof detailedLines)[number]) =>
    multiplyMoney(effectiveLinePrice(line), line.quantity);
  const total = addMoney(
    ...detailedLines.map((line) => {
      const gross = lineGross(line);
      const discount = line.discountPercent ?? 0;
      return discount > 0
        ? centsToMoney(
            Math.round(moneyToCents(gross) * (1 - discount / 100)),
          )
        : gross;
    }),
  );
  const totalDiscount = addMoney(
    ...detailedLines.map((line) => {
      const discount = line.discountPercent ?? 0;
      if (discount <= 0) return "0.00";
      return centsToMoney(
        Math.round(moneyToCents(lineGross(line)) * (discount / 100)),
      );
    }),
  );
  const itemCount = detailedLines.reduce((sum, line) => sum + line.quantity, 0);
  const requiresApproval = detailedLines.some((line) => line.medicine.prescriptionRequired);
  const validCashTendered =
    cashTendered === "" || /^\d+(\.\d{1,2})?$/.test(cashTendered);
  const cashCoversTotal =
    cashTendered === "" || (validCashTendered && moneyToCents(cashTendered) >= moneyToCents(total));
  const validCreditAmount =
    creditAmount === "" || /^\d+(\.\d{1,2})?$/.test(creditAmount);
  const creditAmountValid =
    paymentMethod !== "CREDIT" ||
    (validCreditAmount && creditAmount !== "" && moneyToCents(creditAmount) > 0 && moneyToCents(creditAmount) <= moneyToCents(total));
  const changeDue =
    paymentMethod === "CASH" && cashTendered && validCashTendered
      ? centsToMoney(
          Math.max(0, moneyToCents(cashTendered) - moneyToCents(total)),
        )
      : "0.00";
  const validMpesaPhone = /^(?:\+?254|0)(?:7|1)\d{8}$/.test(
    mpesaPhone.replace(/[\s-]/g, ""),
  );
  // The backend enforces the configured sale.max_discount_percent; the till
  // caps input at the common default so cashiers get instant feedback.
  const maxDiscountPercent = 20;

  function addMedicine(medicineId: string, availableStock?: number) {
    const medicine = medicines.find((item) => item.id === medicineId);
    const existing = lines.find((line) => line.medicineId === medicineId)?.quantity ?? 0;
    const stock = availableStock ?? stockForMedicine(batches, medicineId);
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

  async function handleCameraBarcode(barcode: string) {
    setShowScanner(false);
    setSearching(true);
    setLookupError(null);
    try {
      const matches = await workspaceGateway.lookupPos(barcode);
      const exact = matches.find(
        (item) =>
          item.barcode === barcode ||
          item.sku.toLowerCase() === barcode.toLowerCase(),
      );
      if (exact) {
        addMedicine(exact.id, exact.stockAvailable);
      } else {
        setScanStatus(`No product found for barcode ${barcode}.`);
      }
    } catch (error) {
      setLookupError(getWorkspaceErrorMessage(error, "Product lookup failed."));
    } finally {
      setSearching(false);
    }
  }

  async function handleSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    const submitKey = registerConfig?.barcodeSubmitKey === "TAB" ? "Tab" : "Enter";
    if (event.key !== submitKey) return;
    const searchValue = query.trim();
    if (!searchValue) return;
    event.preventDefault();
    setSearching(true);
    setLookupError(null);
    let matches: PosLookupItem[];
    try {
      matches = await workspaceGateway.lookupPos(searchValue);
    } catch (error) {
      setLookupError(
        getWorkspaceErrorMessage(error, "Product search could not be completed."),
      );
      setSearching(false);
      return;
    }
    setSearching(false);
    const exact = matches.find(
      (item) =>
        item.barcode === searchValue ||
        item.sku.toLowerCase() === searchValue.toLowerCase(),
    );
    if (exact) {
      addMedicine(exact.id, exact.stockAvailable);
      setQuery("");
    } else {
      setScanStatus("No exact SKU or barcode was found.");
    }
  }

  useEffect(() => {
    if (registerConfig?.scannerMode !== "LOCAL_CONNECTOR") return;
    let active = true;
    let timer: number | null = null;
    let polling = false;

    async function poll(connectorUrl: string) {
      if (!active || polling) return;
      polling = true;
      try {
        const { barcode } = await getLastConnectorBarcode(connectorUrl);
        if (!active || !barcode) return;
        const matches = await workspaceGateway.lookupPos(barcode);
        const exact = matches.find(
          (item) =>
            item.barcode === barcode ||
            item.sku.toLowerCase() === barcode.toLowerCase(),
        );
        if (!exact) {
          setScanStatus(`No exact product was found for ${barcode}.`);
          return;
        }
        const medicine = medicines.find((item) => item.id === exact.id);
        const existing = lines.find((line) => line.medicineId === exact.id)?.quantity ?? 0;
        if (existing >= exact.stockAvailable) {
          setScanStatus(`${medicine?.brandName ?? "Product"} has no more sellable stock.`);
          return;
        }
        addItem(exact.id);
        setScanStatus(`${medicine?.brandName ?? "Product"} added to the cart.`);
      } catch {
        // The health bar reports connector failures without interrupting checkout.
      } finally {
        polling = false;
        if (active) timer = window.setTimeout(() => void poll(connectorUrl), 750);
      }
    }

    void terminalGateway.getHardwareConfig()
      .then((bridge) => poll(bridge.connectorUrl))
      .catch(() => undefined);
    return () => {
      active = false;
      if (timer !== null) window.clearTimeout(timer);
    };
  }, [addItem, lines, medicines, registerConfig?.scannerMode]);

  async function handleCheckout() {
    setCheckoutError(null);
    setSubmitting(true);
    try {
      if (!isOnline) {
        if (paymentMethod === "MPESA" && mpesaMode === "STK") {
          throw new WorkspaceError(
            "OFFLINE_STK_UNAVAILABLE",
            "M-Pesa STK cannot be queued while offline. Use cash, credit, or manual M-Pesa reference.",
          );
        }
        const offlineSale: CheckoutInput = {
          idempotencyKey: prepareCheckoutKey(),
          customerId: customerId ?? undefined,
          items: detailedLines.map(({ lineId, medicineId, quantity, discountPercent, sellingUnitId, unitConversion }) => ({
            lineId, medicineId, quantity,
            discountPercent: discountPercent ?? 0,
            sellingUnitId: sellingUnitId || undefined,
            unitConversion: unitConversion ?? 1,
          })),
          paymentMethod,
          mpesaMode,
          mpesaPhone,
          mpesaReference,
          pharmacistApproved: canApprovePrescription,
          cashTendered: cashTendered || total,
          prescriptionReferenceId: requiresApproval ? prescriptionReferenceId.trim() : undefined,
          creditAmount: paymentMethod === "CREDIT" ? creditAmount || total : undefined,
        };
        const raw = localStorage.getItem("pharmacy-pos:offline-queue") ?? "[]";
        const queue = JSON.parse(raw);
        queue.push(offlineSale);
        localStorage.setItem("pharmacy-pos:offline-queue", JSON.stringify(queue));
        setPendingOfflineSales(queue.length);
        void recordOperationalEvent({
          eventType: "OFFLINE_QUEUE",
          status: "PENDING",
          reasonCode: "SALE_QUEUED",
          source: "pos-offline-queue",
          idempotencyKey: offlineSale.idempotencyKey,
          details: `Pending offline sales: ${queue.length}`,
        });
        clear();
        setCheckoutError("You're offline — sale queued and will sync when connected.");
        setSubmitting(false);
        return;
      }
      const saleId = await workspaceGateway.completeSale({
        idempotencyKey: prepareCheckoutKey(),
        customerId: customerId ?? undefined,
        items: detailedLines.map(({ lineId, medicineId, quantity, discountPercent, sellingUnitId, unitConversion }) => ({
          lineId,
          medicineId,
          quantity,
          discountPercent: discountPercent ?? 0,
          sellingUnitId: sellingUnitId || undefined,
          unitConversion: unitConversion ?? 1,
        })),
        paymentMethod,
        mpesaMode,
        mpesaPhone,
        mpesaReference,
        pharmacistApproved: canApprovePrescription,
        cashTendered: cashTendered || total,
        prescriptionReferenceId: requiresApproval
          ? prescriptionReferenceId.trim()
          : undefined,
        creditAmount: paymentMethod === "CREDIT" ? creditAmount || total : undefined,
        payments: payments.length > 0 ? payments : undefined,
      });
      if (
        paymentMethod === "CASH" &&
        registerConfig?.openDrawerOnCashSale
      ) {
        void terminalGateway.getHardwareConfig().then((bridge) =>
          fetch(`${bridge.connectorUrl}/cash-drawer/open`, { method: "POST" }),
        ).catch(() => undefined);
      }
      clear();
      router.push(`/sales/${saleId}?completed=1${registerConfig?.autoPrintReceipt ? "&autoprint=1" : ""}`);
    } catch (error) {
      if (
        error instanceof WorkspaceError &&
        (error.code.startsWith("MPESA_FINAL_") ||
          error.code === "MPESA_FAILED" ||
          error.code === "MPESA_CANCELLED")
      ) {
        resetCheckoutKey();
      }
      setCheckoutError(
        getWorkspaceErrorMessage(error, "Checkout could not be completed."),
      );
      setSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-[calc(100vh-6.25rem)] content-start xl:grid-cols-[minmax(0,1fr)_430px]">
      {!isOnline && (
        <div className="col-span-full rounded-md bg-[var(--danger-soft)] px-4 py-2.5 text-sm text-[var(--danger)] flex items-center justify-between">
          <span><strong>Offline mode</strong> — sales are queued and will sync automatically when you reconnect. Pending: {pendingOfflineSales}</span>
        </div>
      )}
      <div className="sticky top-[6.25rem] z-20 col-span-full grid grid-cols-2 gap-1 border-b border-[var(--border)] bg-white p-2 xl:hidden">
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
          <div className="flex items-center gap-2">
            <label className="relative min-w-0 flex-1">
              <span className="sr-only">Search or scan a product</span>
              <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]" size={18} />
              <Input ref={searchInputRef} autoFocus className="h-12 pl-10 pr-12 text-base" placeholder="Search medicine, SKU, or scan barcode" value={query} onChange={(event) => { const value = event.target.value; setQuery(value); setScanStatus(""); if (!value.trim()) { setLookupResults([]); setLookupError(null); setSearching(false); } }} onKeyDown={(event) => void handleSearchKeyDown(event)} />
              <button type="button" aria-label="Scan barcode with camera" title="Scan barcode" onClick={() => setShowScanner(true)} className="absolute right-2 top-1/2 -translate-y-1/2 flex size-9 items-center justify-center rounded-md text-[var(--brand)] hover:bg-[var(--brand-soft)]">
                <ScanLine aria-hidden="true" size={20} />
              </button>
            </label>
          </div>
          <div className="no-scrollbar flex max-w-full gap-1 overflow-x-auto" role="tablist" aria-label="Product categories">
            <button type="button" onClick={() => setCategoryId("ALL")} className={cn("h-10 shrink-0 rounded-md px-3 text-sm font-medium", categoryId === "ALL" ? "bg-[var(--brand)] text-white" : "bg-white text-[var(--text-muted)] hover:bg-[var(--surface-muted)]")}>All</button>
            {categories.map((category) => <button type="button" key={category.id} onClick={() => setCategoryId(category.id)} className={cn("h-10 shrink-0 rounded-md px-3 text-sm font-medium", categoryId === category.id ? "bg-[var(--brand)] text-white" : "bg-white text-[var(--text-muted)] hover:bg-[var(--surface-muted)]")}>{category.name}</button>)}
          </div>
          <p className="sr-only" role="status" aria-live="polite">{scanStatus}</p>
        </div>
        <FormError message={query.trim() ? lookupError : null} />

        <div className="mt-5 flex items-center justify-between"><h1 className="text-lg font-semibold">Products</h1><span className="text-xs text-[var(--text-muted)]">{query.trim() && searching ? "Searching..." : `${products.length} results`}</span></div>
        {products.length ? (
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
            {products.map(({ medicine, stock }) => {
              const inCart = lines.find((line) => line.medicineId === medicine.id)?.quantity ?? 0;
              return (
                <button type="button" disabled={stock === 0 || inCart >= stock} onClick={() => addMedicine(medicine.id, stock)} key={medicine.id} className="flex min-h-48 flex-col rounded-md border border-[var(--border)] bg-white p-3 text-left transition hover:border-[var(--brand)] hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-55">
                  <div className="flex w-full items-start justify-between gap-2"><span className="text-xs font-medium text-[var(--text-muted)]">{medicine.sku}</span>{medicine.prescriptionRequired ? <span className="rounded bg-[var(--accent-soft)] px-1.5 py-0.5 text-xs font-semibold text-[var(--accent)]">Rx</span> : null}</div>
                  <Image src={medicineImage(medicine)} alt="" width={160} height={96} className="mx-auto my-2 h-16 w-full object-contain" />
                  <span className="mt-2 line-clamp-2 text-sm font-semibold">{medicine.brandName}</span>
                  <span className="mt-0.5 line-clamp-1 text-xs text-[var(--text-muted)]">{medicine.genericName}</span>
                   <div className="mt-auto flex w-full items-end justify-between gap-2 pt-3"><span className="font-semibold text-[var(--brand-strong)]">{formatKes(medicine.sellingPrice)}</span><span className={cn("text-xs", stock <= medicine.reorderLevel ? "text-[var(--danger)]" : "text-[var(--text-muted)]")}>{stock} left</span></div>
                   {(() => { const base = getMedicineUnits(medicine)[0]; return base ? <span className="text-[10px] text-[var(--text-muted)]">per {base.name}</span> : null; })()}
                </button>
              );
            })}
          </div>
        ) : <div className="mt-16 flex flex-col items-center text-center"><PackageSearch aria-hidden="true" className="text-[var(--text-subtle)]" size={32} /><p className="mt-3 text-sm font-semibold">No products found</p><p className="mt-1 text-xs text-[var(--text-muted)]">Adjust the search or category.</p></div>}
      </section>

      <aside className={cn("min-h-[620px] flex-col bg-white xl:sticky xl:top-[6.25rem] xl:flex xl:h-[calc(100vh-6.25rem)]", mobileView === "cart" ? "flex" : "hidden")}>
        <div className="border-b border-[var(--border)] px-4 py-4 sm:px-5"><div className="flex items-center justify-between"><h2 className="text-base font-semibold">Current sale</h2><span className="text-xs text-[var(--text-muted)]">{itemCount} {itemCount === 1 ? "item" : "items"}</span></div></div>
        <div className="min-h-48 flex-1 overflow-y-auto">
          {detailedLines.length ? (
            <div className="divide-y divide-[var(--border)]">
              {detailedLines.map(({ medicine, quantity, stock, discountPercent, lineId, sellingUnitId, unitConversion }) => {
                const availUnits = getMedicineUnits(medicine);
                const currentUnit = availUnits.find((u) => u.id === sellingUnitId) ?? availUnits[0];
                const conv = unitConversion ?? 1;
                const effPrice = effectiveLinePrice({ medicine, unitConversion: conv });
                const displayName = conv > 1 ? `${medicine.brandName} (${currentUnit?.name ?? "ea"})` : medicine.brandName;
                return (
                  <div className="p-4" key={lineId}>
                    <div className="flex items-start gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold">{displayName}</p>
                        <p className="mt-0.5 text-xs text-[var(--text-muted)]">{formatKes(effPrice)} each · {stock} available</p>
                      </div>
                      <p className="shrink-0 text-sm font-semibold">{formatKes(centsToMoney(Math.round(moneyToCents(multiplyMoney(effPrice, quantity)) * (1 - (discountPercent ?? 0) * 0.01))))}</p>
                    </div>
                    <div className="mt-3 flex items-center justify-between">
                      {availUnits.length > 1 ? (
                        <select
                          aria-label={`Selling unit for ${medicine.brandName}`}
                          className="h-9 rounded-md border border-[var(--border-strong)] px-2 text-xs font-medium"
                          value={sellingUnitId ?? medicine.unitId}
                          onChange={(e) => {
                            const unit = availUnits.find((u) => u.id === e.target.value);
                            if (unit) setLineUnit(medicine.id, unit.id, (unit as any).cumulativeFactor ?? unit.conversionFactor ?? 1, multiplyMoney(medicine.sellingPrice, (unit as any).cumulativeFactor ?? unit.conversionFactor ?? 1));
                          }}
                        >
                          {availUnits.map((u) => (
                            <option key={u.id} value={u.id}>{u.name}{(u as any).cumulativeFactor > 1 ? ` (${(u as any).cumulativeFactor}x)` : ""}</option>
                          ))}
                        </select>
                      ) : null}
                      <div className="flex h-9 items-center rounded-md border border-[var(--border-strong)]">
                        <button type="button" title="Decrease quantity" aria-label={`Decrease ${medicine.brandName} quantity`} onClick={() => setQuantity(medicine.id, quantity - 1)} className="flex size-8 items-center justify-center hover:bg-[var(--surface-muted)]"><Minus aria-hidden="true" size={15} /></button>
                        <span className="w-10 text-center text-sm font-semibold">{quantity}</span>
                        <button type="button" title="Increase quantity" aria-label={`Increase ${medicine.brandName} quantity`} disabled={quantity >= stock} onClick={() => setQuantity(medicine.id, Math.min(stock, quantity + 1))} className="flex size-8 items-center justify-center hover:bg-[var(--surface-muted)] disabled:opacity-40"><Plus aria-hidden="true" size={15} /></button>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <label className="flex items-center gap-1 text-xs text-[var(--text-muted)]" title={`Line discount % (max ${maxDiscountPercent})`}>
                        Disc %
                        <input
                          inputMode="numeric"
                          aria-label={`Discount percent for ${medicine.brandName}`}
                          className="h-8 w-12 rounded-md border border-[var(--border-strong)] px-2 text-right text-sm"
                          value={discountPercent ?? 0}
                          onChange={(event) => {
                            const value = Math.max(0, Math.min(maxDiscountPercent, Number(event.target.value) || 0));
                            setLineDiscount(medicine.id, value);
                          }}
                        />
                      </label>
                      <button type="button" title="Remove item" aria-label={`Remove ${medicine.brandName}`} onClick={() => removeItem(medicine.id)} className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"><Trash2 aria-hidden="true" size={17} /></button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex h-full min-h-56 flex-col items-center justify-center px-5 text-center">
              <PackageSearch aria-hidden="true" className="text-[var(--text-subtle)]" size={30} />
              <p className="mt-3 text-sm font-semibold">Cart is empty</p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">Select a product to begin the sale.</p>
            </div>
          )}
        </div>

        <div className="border-t border-[var(--border)] p-4 sm:p-5">
          {assignedTerminalName ? (
            <div className="mb-4 rounded-md border border-[var(--success)]/30 bg-[var(--success-soft)] p-3 text-sm text-[var(--success)]">
              This device is now assigned to <strong>{assignedTerminalName}</strong>.
            </div>
          ) : null}
          {!getLocalTerminalId() && !assignedTerminalName ? (
            <form
              className="mb-4 rounded-md border border-[var(--border)] bg-white p-3"
              onSubmit={(event) => {
                event.preventDefault();
                void activateRegister();
              }}
            >
              <p className="text-sm font-semibold">Activate this register</p>
              <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                Enter the one-time pairing code from the terminals page.
              </p>
              <div className="mt-2 flex gap-2">
                <Input
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="e.g. 481902"
                  value={pairCode}
                  onChange={(event) => setPairCode(event.target.value)}
                />
                <PrimaryButton type="submit" disabled={pairing || pairCode.trim().length === 0}>
                  {pairing ? "Activating..." : "Activate"}
                </PrimaryButton>
              </div>
              {pairError ? (
                <p className="mt-2 text-xs text-[var(--danger)]">{pairError}</p>
              ) : null}
            </form>
          ) : null}
          {!currentShiftId ? <div className="mb-4 rounded-md border border-[var(--border)] bg-[var(--warning-soft)] p-3 text-sm text-[var(--warning)]">Checkout is locked. <Link href="/shifts/current" className="font-semibold underline">Open a shift</Link> to continue.</div> : null}
          <label className="mb-3 block">
            <span className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold">
              <UserRound aria-hidden="true" size={15} /> Customer
            </span>
            <Select
              value={customerId ?? ""}
              onChange={(event) => setCustomerId(event.target.value || null)}
            >
              <option value="">Walk-in customer</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {[customer.firstName, customer.lastName].filter(Boolean).join(" ")}
                  {customer.phoneNumber ? ` - ${customer.phoneNumber}` : ""}
                </option>
              ))}
            </Select>
          </label>
          <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Payment method">
            <button type="button" role="radio" aria-checked={paymentMethod === "CASH"} disabled={registerConfig?.cashEnabled === false} onClick={() => setPaymentMethod("CASH")} className={cn("flex h-11 items-center justify-center gap-2 rounded-md border text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40", paymentMethod === "CASH" ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand-strong)]" : "border-[var(--border-strong)] text-[var(--text-muted)]")}><Banknote aria-hidden="true" size={17} /> Cash</button>
            <button type="button" role="radio" aria-checked={paymentMethod === "MPESA"} disabled={registerConfig?.mpesaEnabled === false} onClick={() => setPaymentMethod("MPESA")} className={cn("flex h-11 items-center justify-center gap-2 rounded-md border text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40", paymentMethod === "MPESA" ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand-strong)]" : "border-[var(--border-strong)] text-[var(--text-muted)]")}><Smartphone aria-hidden="true" size={17} /> M-Pesa</button>
            <button type="button" role="radio" aria-checked={paymentMethod === "CREDIT"} disabled={!customerId} onClick={() => setPaymentMethod("CREDIT")} title={!customerId ? "Select a customer first" : undefined} className={cn("flex h-11 items-center justify-center gap-2 rounded-md border text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40", paymentMethod === "CREDIT" ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand-strong)]" : "border-[var(--border-strong)] text-[var(--text-muted)]")}><CreditCard aria-hidden="true" size={17} /> Credit</button>
          </div>
          {paymentMethod === "MPESA" ? (
            <div className="mt-3 space-y-3">
              <div className="grid grid-cols-2 gap-1 rounded-md bg-[var(--surface-muted)] p-1" role="radiogroup" aria-label="M-Pesa payment mode">
                <button
                  type="button"
                  role="radio"
                  aria-checked={mpesaMode === "STK"}
                  disabled={!paymentCapabilities?.mpesaStkConfigured}
                  title={paymentCapabilities?.mpesaStkConfigured ? undefined : "Configure Daraja credentials to enable STK Push"}
                  onClick={() => setMpesaMode("STK")}
                  className={cn(
                    "h-9 rounded text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-45",
                    mpesaMode === "STK"
                      ? "bg-white text-[var(--brand-strong)] shadow-sm"
                      : "text-[var(--text-muted)]",
                  )}
                >
                  STK Push
                </button>
                <button
                  type="button"
                  role="radio"
                  aria-checked={mpesaMode === "MANUAL"}
                  onClick={() => setMpesaMode("MANUAL")}
                  className={cn(
                    "h-9 rounded text-xs font-semibold",
                    mpesaMode === "MANUAL"
                      ? "bg-white text-[var(--brand-strong)] shadow-sm"
                      : "text-[var(--text-muted)]",
                  )}
                >
                  Manual code
                </button>
              </div>
              {mpesaMode === "STK" && paymentCapabilities?.mpesaStkConfigured ? (
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold">M-Pesa phone number</span>
                  <Input inputMode="tel" autoComplete="tel" placeholder="0712 345 678" value={mpesaPhone} onChange={(event) => setMpesaPhone(event.target.value)} />
                  {mpesaPhone && !validMpesaPhone ? <span className="mt-1.5 block text-xs text-[var(--danger)]">Enter a valid Kenyan mobile number.</span> : null}
                </label>
              ) : (
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold">Confirmed M-Pesa reference</span>
                  <Input autoCapitalize="characters" placeholder="e.g. SGA12ABC34" value={mpesaReference} onChange={(event) => setMpesaReference(event.target.value.toUpperCase())} />
                </label>
              )}
            </div>
          ) : null}
          {paymentMethod === "CASH" ? <label className="mt-3 block"><span className="mb-1.5 block text-xs font-semibold">Cash tendered</span><Input inputMode="decimal" placeholder={total} value={cashTendered} onChange={(event) => setCashTendered(event.target.value.replace(/[^\d.]/g, ""))} />{cashTendered && validCashTendered && cashCoversTotal ? <span className="mt-1.5 block text-xs text-[var(--text-muted)]">Change due: {formatKes(changeDue)}</span> : null}</label> : null}
          {paymentMethod === "CREDIT" ? <label className="mt-3 block"><span className="mb-1.5 block text-xs font-semibold">Credit amount (max {formatKes(total)})</span><Input inputMode="decimal" placeholder={total} value={creditAmount} onChange={(event) => setCreditAmount(event.target.value.replace(/[^\d.]/g, ""))} />{creditAmount && validCreditAmount ? <span className="mt-1.5 block text-xs text-[var(--text-muted)]">Amount owed: {formatKes(centsToMoney(moneyToCents(total) - moneyToCents(creditAmount || "0")))}</span> : null}</label> : null}
          {/* Split payment section */}
          {payments.length > 0 ? (
            <div className="mt-3 space-y-2">
              <p className="text-xs font-semibold text-[var(--text-muted)]">Additional payments</p>
              {payments.map((entry) => (
                <div key={entry.id} className="flex items-center gap-2 rounded-md border border-[var(--border)] px-3 py-2 text-sm">
                  <span className="font-medium">{entry.method === "CASH" ? "Cash" : entry.method === "CREDIT" ? "Credit" : "M-Pesa"}</span>
                  <span className="text-[var(--text-muted)]">{formatKes(entry.amount)}</span>
                  {entry.reference ? <span className="text-xs text-[var(--text-muted)]">({entry.reference})</span> : null}
                  <button type="button" onClick={() => removePayment(entry.id)} className="ml-auto text-[var(--danger)] hover:underline text-xs">Remove</button>
                </div>
              ))}
            </div>
          ) : null}
          <button
            type="button"
            onClick={() => {
              const remaining = centsToMoney(Math.max(0, moneyToCents(total) - moneyToCents(cashTendered || "0") - moneyToCents(creditAmount || "0") - payments.reduce((sum, p) => sum + moneyToCents(p.amount), 0)));
              if (moneyToCents(remaining) > 0) {
                addPayment(paymentMethod === "CASH" ? "MPESA" : "CASH", remaining);
              }
            }}
            className="mt-2 text-xs text-[var(--brand)] hover:underline"
          >
            + Split payment
          </button>
          {requiresApproval && canApprovePrescription ? <label className="mt-3 block rounded-md bg-[var(--accent-soft)] p-3 text-sm"><span className="mb-1.5 flex items-center gap-1.5 font-semibold"><ShieldCheck aria-hidden="true" size={16} /> Prescription</span><Select value={prescriptionReferenceId} onChange={(event) => setPrescriptionReferenceId(event.target.value)}><option value="">Select an active prescription</option>{prescriptionReferenceId && !prescriptions.some((item) => item.id === prescriptionReferenceId) ? <option value={prescriptionReferenceId}>Selected prescription</option> : null}{prescriptions.map((prescription) => <option key={prescription.id} value={prescription.id}>{prescription.prescriptionNumber} - {prescription.customerName}</option>)}</Select></label> : null}
          {requiresApproval && !canApprovePrescription ? <div className="mt-3 flex items-start gap-2.5 rounded-md bg-[var(--warning-soft)] p-3 text-sm text-[var(--warning)]"><ShieldCheck aria-hidden="true" className="mt-0.5 shrink-0" size={16} /><span><span className="block font-semibold">Pharmacist approval required</span><span className="mt-0.5 block text-xs">A staff member with prescription approval permission must complete this sale.</span></span></div> : null}
                      {moneyToCents(totalDiscount) > 0 ? (
              <div className="my-2 flex items-center justify-between text-xs text-[var(--success)]">
                <span>Discount applied</span>
                <span>-{formatKes(totalDiscount)}</span>
              </div>
            ) : null}
            <div className="my-4 flex items-center justify-between"><span className="text-sm text-[var(--text-muted)]">Total due</span><span className="text-2xl font-semibold">{formatKes(total)}</span></div>
          <FormError message={checkoutError} />
          <PrimaryButton type="button" onClick={() => void handleCheckout()} disabled={submitting || !currentShiftId || detailedLines.length === 0 || (paymentMethod === "MPESA" && (mpesaMode === "STK" ? !paymentCapabilities?.mpesaStkConfigured || !validMpesaPhone : !mpesaReference.trim())) || (paymentMethod === "CASH" && (!validCashTendered || !cashCoversTotal)) || (paymentMethod === "CREDIT" && !creditAmountValid) || (requiresApproval && (!canApprovePrescription || !prescriptionReferenceId.trim()))} className="mt-3 h-12 w-full text-base">{submitting ? paymentMethod === "MPESA" && mpesaMode === "STK" ? "Waiting for M-Pesa..." : "Completing sale..." : paymentMethod === "MPESA" && mpesaMode === "STK" ? `Send STK Push - ${formatKes(total)}` : `Complete sale - ${formatKes(total)}`}</PrimaryButton>
        </div>
      </aside>

      {showScanner ? (
        <BarcodeScanner
          onDetected={(barcode) => void handleCameraBarcode(barcode)}
          onClose={() => setShowScanner(false)}
        />
      ) : null}
    </div>
  );
}
