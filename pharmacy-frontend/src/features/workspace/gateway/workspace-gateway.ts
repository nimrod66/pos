"use client";

import { useSyncExternalStore } from "react";

import { useAuthStore } from "@/features/auth/store/auth-store";
import type { WorkspaceSeed } from "@/features/workspace/data/seed-workspace";
import {
  useWorkspaceStore,
  WorkspaceError,
} from "@/features/workspace/store/workspace-store";
import type {
  CheckoutInput,
  DashboardReport,
  InventoryReport,
  MedicineInput,
  PaymentCapabilities,
  PharmacySettings,
  PluRow,
  PosLookupItem,
  ProfitReport,
  ReceiveStockInput,
  ReorderSuggestionReport,
  ReturnInput,
  SalesReport,
  SlowStockReport,
  FinancialSummary,
  SupplierPriceComparison,
  StaffInput,
  StaffUser,
  Supplier,
  SupplierInput,
} from "@/features/workspace/types";
import { ApiClientError } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";
import { createLiveWorkspaceGateway } from "@/features/workspace/gateway/live-workspace-gateway";
import { todayIsoDate } from "@/features/workspace/lib/workspace-helpers";

const reportableSaleStatuses = new Set(["COMPLETED", "PARTIALLY_RETURNED", "RETURNED"]);

function money(value: number) {
  return value.toFixed(2);
}

function previewInventoryReport(state: WorkspaceSeed, asOf = todayIsoDate()): InventoryReport {
  const sellableByMedicine = new Map<string, number>();
  let stockValue = 0;
  let batchCount = 0;
  let nearExpiryCount = 0;
  let expiredCount = 0;
  const nearExpiryDate = new Date(`${asOf}T00:00:00Z`);
  nearExpiryDate.setUTCDate(nearExpiryDate.getUTCDate() + state.settings.nearExpiryDays);
  const nearExpiryIso = nearExpiryDate.toISOString().slice(0, 10);

  for (const batch of state.batches) {
    if (batch.quantity <= 0) continue;
    batchCount += 1;
    stockValue += Number(batch.unitCost) * batch.quantity;
    if (batch.expiryDate <= asOf) {
      expiredCount += 1;
    } else {
      sellableByMedicine.set(
        batch.medicineId,
        (sellableByMedicine.get(batch.medicineId) ?? 0) + batch.quantity,
      );
      if (batch.expiryDate <= nearExpiryIso) nearExpiryCount += 1;
    }
  }

  const lowStockItems = state.medicines
    .filter((medicine) => medicine.status === "ACTIVE")
    .map((medicine) => ({
      available: sellableByMedicine.get(medicine.id) ?? 0,
      medicineId: medicine.id,
      medicineName: medicine.brandName,
      branchId: "preview-main",
      branchName: state.settings.branchName,
      reorderLevel: medicine.reorderLevel,
      sku: medicine.sku,
    }))
    .filter((item) => item.available <= item.reorderLevel)
    .sort((left, right) => left.available - right.available);

  return {
    asOf,
    batchCount,
    expiredCount,
    lowStockCount: lowStockItems.length,
    lowStockItems,
    nearExpiryCount,
    nearExpiryDays: state.settings.nearExpiryDays,
    pharmacyWide: false,
    stockValue: money(stockValue),
  };
}

export function getWorkspaceErrorMessage(error: unknown, fallback: string) {
  return error instanceof WorkspaceError || error instanceof ApiClientError
    ? error.message
    : fallback;
}

function previewActor() {
  const user = useAuthStore.getState().session?.user;
  if (!user) {
    throw new WorkspaceError(
      "AUTH_REQUIRED",
      "Sign in again before changing pharmacy data.",
    );
  }
  return user;
}

export interface WorkspaceGateway {
  addMedicine(input: MedicineInput): Promise<string>;
  addStaff(input: StaffInput): Promise<string>;
  addSupplier(input: SupplierInput): Promise<string>;
  closeShift(actualCash: string): Promise<void>;
  completeSale(input: CheckoutInput): Promise<string>;
  deleteMedicine(id: string): Promise<void>;
  deleteSupplier(id: string): Promise<void>;
  getSnapshot(): WorkspaceSeed;
  getDashboardReport(date?: string): Promise<DashboardReport>;
  getInventoryReport(asOf?: string): Promise<InventoryReport>;
  getPaymentCapabilities(): Promise<PaymentCapabilities>;
  getPluReport(from?: string, to?: string): Promise<PluRow[]>;
  getSalesReport(from: string, to: string): Promise<SalesReport>;
  getFinancialSummary(from: string, to: string): Promise<FinancialSummary>;
  getProfitReport(from: string, to: string): Promise<ProfitReport>;
  getSlowStockReport(): Promise<SlowStockReport>;
  getReorderSuggestionReport(): Promise<ReorderSuggestionReport>;
  getSupplierPriceComparison(): Promise<SupplierPriceComparison[]>;
  hydrate(): Promise<void>;
  lookupPos(query: string): Promise<PosLookupItem[]>;
  openShift(openingFloat: string): Promise<string>;
  receiveStock(input: ReceiveStockInput): Promise<string>;
  resetWorkspace(): Promise<void>;
  returnSaleItem(input: ReturnInput): Promise<void>;
  setMedicineStatus(
    id: string,
    status: MedicineInput["status"],
  ): Promise<void>;
  setStaffStatus(
    id: string,
    status: StaffUser["status"],
  ): Promise<void>;
  setSupplierStatus(id: string, status: Supplier["status"]): Promise<void>;
  subscribe(listener: () => void): () => void;
  updateMedicine(id: string, input: MedicineInput): Promise<void>;
  updateStaff(
    id: string,
    input: StaffInput,
  ): Promise<void>;
  updateSettings(settings: PharmacySettings): Promise<void>;
  updateSupplier(id: string, input: SupplierInput): Promise<void>;
}

class PreviewWorkspaceGateway implements WorkspaceGateway {
  async addMedicine(input: MedicineInput) {
    return useWorkspaceStore.getState().addMedicine(input);
  }

  async addStaff(input: StaffInput) {
    return useWorkspaceStore.getState().addStaff(input);
  }

  async addSupplier(input: SupplierInput) {
    return useWorkspaceStore.getState().addSupplier(input);
  }

  async closeShift(actualCash: string) {
    useWorkspaceStore.getState().closeShift(actualCash);
  }

  async completeSale(input: CheckoutInput) {
    return useWorkspaceStore.getState().completeSale({
      ...input,
      cashierName: previewActor().displayName,
    });
  }

  async deleteMedicine(id: string) {
    useWorkspaceStore.getState().deleteMedicine(id);
  }

  async deleteSupplier(id: string) {
    useWorkspaceStore.getState().deleteSupplier(id);
  }

  getSnapshot() {
    return useWorkspaceStore.getState();
  }

  async getDashboardReport(date?: string) {
    const state = useWorkspaceStore.getState();
    const reportDate = date ?? todayIsoDate();
    const sales = state.sales.filter(
      (sale) =>
        reportableSaleStatuses.has(sale.status) &&
        sale.completedAt.slice(0, 10) === reportDate,
    );
    const grossSales = sales.reduce((total, sale) => total + Number(sale.total), 0);
    const refunds = sales.reduce(
      (total, sale) => total + Number(sale.refundTotal),
      0,
    );
    const inventory = previewInventoryReport(state, reportDate);
    return {
      completedSalesCount: sales.length,
      date: reportDate,
      expiredCount: inventory.expiredCount,
      grossSales: money(grossSales),
      lowStockCount: inventory.lowStockCount,
      nearExpiryCount: inventory.nearExpiryCount,
      nearExpiryDays: inventory.nearExpiryDays,
      netSales: money(grossSales - refunds),
      pharmacyWide: false,
      refunds: money(refunds),
      totalStockItems: inventory.batchCount,
    };
  }

  async getInventoryReport(asOf?: string) {
    return previewInventoryReport(useWorkspaceStore.getState(), asOf);
  }

  async getPaymentCapabilities() {
    return {
      mpesaEnvironment: "preview",
      mpesaStkConfigured: true,
      pollingSupported: true,
    };
  }

  async getPluReport() {
    return [];
  }

  async getSalesReport(from: string, to: string) {
    const sales = useWorkspaceStore.getState().sales.filter(
      (sale) =>
        reportableSaleStatuses.has(sale.status) &&
        sale.completedAt.slice(0, 10) >= from &&
        sale.completedAt.slice(0, 10) <= to,
    );
    let cashPayments = 0;
    let mpesaPayments = 0;
    let cashRefunds = 0;
    let mpesaRefunds = 0;
    const products = new Map<
      string,
      { medicineId: string; medicineName: string; quantity: number; netRevenue: number }
    >();
    for (const sale of sales) {
      for (const payment of sale.payments) {
        if (payment.method === "MPESA") mpesaPayments += Number(payment.amount);
        else cashPayments += Number(payment.amount);
      }
      if (sale.payments[0]?.method === "MPESA") mpesaRefunds += Number(sale.refundTotal);
      else cashRefunds += Number(sale.refundTotal);
      for (const item of sale.items) {
        const current = products.get(item.medicineId) ?? {
          medicineId: item.medicineId,
          medicineName: item.medicineName,
          netRevenue: 0,
          quantity: 0,
        };
        const quantity = item.quantity - item.returnedQuantity;
        current.quantity += quantity;
        current.netRevenue += Number(item.unitPrice) * quantity;
        products.set(item.medicineId, current);
      }
    }
    const grossSales = sales.reduce((total, sale) => total + Number(sale.total), 0);
    const refunds = sales.reduce(
      (total, sale) => total + Number(sale.refundTotal),
      0,
    );
    return {
      cashPayments: money(cashPayments),
      cashRefunds: money(cashRefunds),
      completedSalesCount: sales.length,
      from,
      grossSales: money(grossSales),
      mpesaPayments: money(mpesaPayments),
      mpesaRefunds: money(mpesaRefunds),
      netSales: money(grossSales - refunds),
      otherPayments: "0.00",
      otherRefunds: "0.00",
      pharmacyWide: false,
      refunds: money(refunds),
      to,
      topProducts: [...products.values()]
        .sort((left, right) => right.netRevenue - left.netRevenue)
        .slice(0, 6)
        .map((product) => ({ ...product, netRevenue: money(product.netRevenue) })),
    };
  }

  async getFinancialSummary(from: string, to: string) {
    return {
      branchId: null,
      pharmacyWide: false,
      from,
      to,
      grossSales: "0.00",
      salesReturns: "0.00",
      netSales: "0.00",
      totalCostOfGoodsSold: "0.00",
      grossProfit: "0.00",
      grossMarginPercent: 0,
      totalExpenses: "0.00",
      expensesByCategory: [],
      netProfit: "0.00",
      netProfitMarginPercent: 0,
      cashCollected: "0.00",
      mpesaCollected: "0.00",
      creditCollected: "0.00",
    };
  }

  async getProfitReport(from: string, to: string): Promise<ProfitReport> {
    return { from, to, totalRevenue: "0.00", totalCostOfGoodsSold: "0.00", grossProfit: "0.00", grossMarginPercent: 0, medicineBreakdown: [] };
  }

  async getSlowStockReport(): Promise<SlowStockReport> {
    return { asOf: "", totalItems: 0, slowMovingCount: 0, deadStockCount: 0, slowMovingValue: "0.00", deadStockValue: "0.00", items: [] };
  }

  async getReorderSuggestionReport(): Promise<ReorderSuggestionReport> {
    return { branchId: "", totalMedicines: 0, needReorder: 0, items: [] };
  }

  async getSupplierPriceComparison(): Promise<SupplierPriceComparison[]> {
    return [];
  }

  async hydrate() {
    await useWorkspaceStore.persist.rehydrate();
  }

  async lookupPos(query: string) {
    const state = useWorkspaceStore.getState();
    const normalized = query.trim().toLowerCase();
    if (!normalized) return [];
    return state.medicines
      .filter(
        (medicine) =>
          medicine.status === "ACTIVE" &&
          [medicine.brandName, medicine.genericName, medicine.sku, medicine.barcode]
            .some((value) => value.toLowerCase().includes(normalized)),
      )
      .map((medicine) => ({
        barcode: medicine.barcode,
        brandName: medicine.brandName,
        categoryId: medicine.categoryId,
        controlledDrug: false,
        genericName: medicine.genericName,
        id: medicine.id,
        prescriptionRequired: medicine.prescriptionRequired,
        sellingPrice: medicine.sellingPrice,
        sku: medicine.sku,
        stockAvailable: state.batches
          .filter(
            (batch) =>
              batch.medicineId === medicine.id &&
              batch.quantity > 0 &&
              batch.expiryDate > todayIsoDate(),
          )
          .reduce((total, batch) => total + batch.quantity, 0),
      }));
  }

  async openShift(openingFloat: string) {
    return useWorkspaceStore
      .getState()
      .openShift(openingFloat, previewActor().displayName);
  }

  async receiveStock(input: ReceiveStockInput) {
    return useWorkspaceStore
      .getState()
      .receiveStock(input, previewActor().displayName);
  }

  async resetWorkspace() {
    useWorkspaceStore.getState().resetWorkspace();
  }

  async returnSaleItem(input: ReturnInput) {
    useWorkspaceStore.getState().returnSaleItem({
      ...input,
      actor: previewActor().displayName,
    });
  }

  async setMedicineStatus(id: string, status: MedicineInput["status"]) {
    useWorkspaceStore.getState().setMedicineStatus(id, status);
  }

  async setStaffStatus(
    id: string,
    status: StaffUser["status"],
  ) {
    useWorkspaceStore
      .getState()
      .setStaffStatus(id, status, previewActor().username);
  }

  async setSupplierStatus(id: string, status: Supplier["status"]) {
    useWorkspaceStore.getState().setSupplierStatus(id, status);
  }

  subscribe(listener: () => void) {
    return useWorkspaceStore.subscribe(() => listener());
  }

  async updateMedicine(id: string, input: MedicineInput) {
    useWorkspaceStore.getState().updateMedicine(id, input);
  }

  async updateStaff(id: string, input: StaffInput) {
    useWorkspaceStore
      .getState()
      .updateStaff(id, input, previewActor().username);
  }

  async updateSettings(settings: PharmacySettings) {
    useWorkspaceStore.getState().updateSettings(settings);
  }

  async updateSupplier(id: string, input: SupplierInput) {
    useWorkspaceStore.getState().updateSupplier(id, input);
  }
}

export const workspaceGateway: WorkspaceGateway =
  DEMO_AUTH_ENABLED || process.env.NODE_ENV === "test"
    ? new PreviewWorkspaceGateway()
    : createLiveWorkspaceGateway();

export function useWorkspaceQuery<T>(selector: (state: WorkspaceSeed) => T) {
  const snapshot = useSyncExternalStore(
    workspaceGateway.subscribe,
    workspaceGateway.getSnapshot,
    workspaceGateway.getSnapshot,
  );
  return selector(snapshot);
}
