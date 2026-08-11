"use client";

import { useSyncExternalStore } from "react";

import type { WorkspaceSeed } from "@/features/workspace/data/seed-workspace";
import {
  useWorkspaceStore,
  WorkspaceError,
} from "@/features/workspace/store/workspace-store";
import type {
  CheckoutInput,
  MedicineInput,
  PharmacySettings,
  ReceiveStockInput,
  ReturnInput,
  StaffInput,
  StaffUser,
  Supplier,
  SupplierInput,
} from "@/features/workspace/types";
import { ApiClientError } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";
import { createLiveWorkspaceGateway } from "@/features/workspace/gateway/live-workspace-gateway";

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
  hydrate(): Promise<void>;
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

  async hydrate() {
    await useWorkspaceStore.persist.rehydrate();
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
