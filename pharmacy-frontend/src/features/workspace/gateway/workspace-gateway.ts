"use client";

import { useSyncExternalStore } from "react";

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
  PharmacySettings,
  PosLookupItem,
  ReceiveStockInput,
  ReturnInput,
  SalesReport,
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
  getSalesReport(from: string, to: string): Promise<SalesReport>;
  hydrate(): Promise<void>;
  lookupPos(query: string): Promise<PosLookupItem[]>;
  openShift(openingFloat: string, actor: string): Promise<string>;
  receiveStock(input: ReceiveStockInput, actor: string): Promise<string>;
  resetWorkspace(): Promise<void>;
  returnSaleItem(input: ReturnInput): Promise<void>;
  setMedicineStatus(
    id: string,
    status: MedicineInput["status"],
  ): Promise<void>;
  setStaffStatus(
    id: string,
    status: StaffUser["status"],
    actorUsername: string,
  ): Promise<void>;
  setSupplierStatus(id: string, status: Supplier["status"]): Promise<void>;
  subscribe(listener: () => void): () => void;
  updateMedicine(id: string, input: MedicineInput): Promise<void>;
  updateStaff(
    id: string,
    input: StaffInput,
    actorUsername: string,
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
    return useWorkspaceStore.getState().completeSale(input);
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

  async openShift(openingFloat: string, actor: string) {
    return useWorkspaceStore.getState().openShift(openingFloat, actor);
  }

  async receiveStock(input: ReceiveStockInput, actor: string) {
    return useWorkspaceStore.getState().receiveStock(input, actor);
  }

  async resetWorkspace() {
    useWorkspaceStore.getState().resetWorkspace();
  }

  async returnSaleItem(input: ReturnInput) {
    useWorkspaceStore.getState().returnSaleItem(input);
  }

  async setMedicineStatus(id: string, status: MedicineInput["status"]) {
    useWorkspaceStore.getState().setMedicineStatus(id, status);
  }

  async setStaffStatus(
    id: string,
    status: StaffUser["status"],
    actorUsername: string,
  ) {
    useWorkspaceStore.getState().setStaffStatus(id, status, actorUsername);
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

  async updateStaff(id: string, input: StaffInput, actorUsername: string) {
    useWorkspaceStore.getState().updateStaff(id, input, actorUsername);
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
