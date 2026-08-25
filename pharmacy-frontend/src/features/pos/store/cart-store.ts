import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

import type { PaymentMethod } from "@/features/workspace/types";

export interface CartLine {
  lineId: string;
  medicineId: string;
  quantity: number;
  /** Percentage discount applied at the till (0-100). */
  discountPercent?: number;
}

interface CartStore {
  cashTendered: string;
  checkoutKey: string | null;
  customerId: string | null;
  lines: CartLine[];
  mpesaMode: "STK" | "MANUAL";
  mpesaPhone: string;
  mpesaReference: string;
  paymentMethod: PaymentMethod;
  pharmacistApproved: boolean;
  prescriptionReferenceId: string;
  addItem(medicineId: string): void;
  clear(): void;
  prepareCheckoutKey(): string;
  removeItem(medicineId: string): void;
  resetCheckoutKey(): void;
  setMpesaMode(mode: "STK" | "MANUAL"): void;
  setMpesaPhone(phone: string): void;
  setMpesaReference(reference: string): void;
  setPaymentMethod(method: PaymentMethod): void;
  setPharmacistApproved(approved: boolean): void;
  setCashTendered(amount: string): void;
  setCustomerId(customerId: string | null): void;
  setPrescriptionReferenceId(reference: string): void;
  setQuantity(medicineId: string, quantity: number): void;
  setLineDiscount(medicineId: string, discountPercent: number): void;
}

export const useCartStore = create<CartStore>()(
  persist(
    (set, get) => ({
      cashTendered: "",
      checkoutKey: null,
      customerId: null,
      lines: [],
      mpesaMode: "STK",
      mpesaPhone: "",
      mpesaReference: "",
      paymentMethod: "CASH",
      pharmacistApproved: false,
      prescriptionReferenceId: "",
      addItem(medicineId) {
        const existing = get().lines.find((line) => line.medicineId === medicineId);
        set({
          checkoutKey: null,
          lines: existing
            ? get().lines.map((line) =>
                line.medicineId === medicineId
                  ? { ...line, quantity: line.quantity + 1 }
                  : line,
              )
            : [
                ...get().lines,
                { lineId: crypto.randomUUID(), medicineId, quantity: 1 },
              ],
        });
      },
      clear() {
        set({
          checkoutKey: null,
          cashTendered: "",
          customerId: null,
          lines: [],
          mpesaMode: "STK",
          mpesaPhone: "",
          mpesaReference: "",
          paymentMethod: "CASH",
          pharmacistApproved: false,
          prescriptionReferenceId: "",
        });
      },
      prepareCheckoutKey() {
        const current = get().checkoutKey;
        if (current) {
          return current;
        }
        const next = crypto.randomUUID();
        set({ checkoutKey: next });
        return next;
      },
      removeItem(medicineId) {
        set({
          checkoutKey: null,
          lines: get().lines.filter((line) => line.medicineId !== medicineId),
        });
      },
      setMpesaReference(mpesaReference) {
        set({ checkoutKey: null, mpesaReference });
      },
      setPaymentMethod(paymentMethod) {
        set({ checkoutKey: null, paymentMethod });
      },
      setPharmacistApproved(pharmacistApproved) {
        set({ checkoutKey: null, pharmacistApproved });
      },
      setCashTendered(cashTendered) {
        set({ checkoutKey: null, cashTendered });
      },
      resetCheckoutKey() {
        set({ checkoutKey: null });
      },
      setMpesaMode(mpesaMode) {
        set({ checkoutKey: null, mpesaMode });
      },
      setMpesaPhone(mpesaPhone) {
        set({ checkoutKey: null, mpesaPhone });
      },
      setCustomerId(customerId) {
        set({ checkoutKey: null, customerId });
      },
      setPrescriptionReferenceId(prescriptionReferenceId) {
        set({ checkoutKey: null, prescriptionReferenceId });
      },
      setQuantity(medicineId, quantity) {
        const normalized = Math.max(0, Math.min(999, Math.floor(quantity)));
        set({
          checkoutKey: null,
          lines:
            normalized === 0
              ? get().lines.filter((line) => line.medicineId !== medicineId)
              : get().lines.map((line) =>
                  line.medicineId === medicineId
                    ? { ...line, quantity: normalized }
                    : line,
                ),
        });
      },
      setLineDiscount(medicineId, discountPercent) {
        const normalized = Math.max(0, Math.min(100, discountPercent));
        set({
          checkoutKey: null,
          lines: get().lines.map((line) =>
            line.medicineId === medicineId ? { ...line, discountPercent: normalized } : line,
          ),
        });
      },
    }),
    {
      name: "pharmacy-pos:cart-draft",
      storage: createJSONStorage(() => window.localStorage),
      partialize: (state) => ({
        cashTendered: state.cashTendered,
        checkoutKey: state.checkoutKey,
        customerId: state.customerId,
        lines: state.lines,
        mpesaMode: state.mpesaMode,
        mpesaPhone: state.mpesaPhone,
        mpesaReference: state.mpesaReference,
        paymentMethod: state.paymentMethod,
        prescriptionReferenceId: state.prescriptionReferenceId,
      }),
      skipHydration: true,
      migrate(persistedState, version) {
        const state = persistedState as Partial<CartStore>;
        if (version >= 4) return state as CartStore;
        return {
          ...state,
          customerId: state.customerId ?? null,
          lines: (state.lines ?? []).map((line) => ({
            ...line,
            lineId: line.lineId ?? crypto.randomUUID(),
          })),
        } as CartStore;
      },
      version: 4,
    },
  ),
);
