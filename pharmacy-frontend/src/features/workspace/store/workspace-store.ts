import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

import {
  createEmptyWorkspace,
  createSeedWorkspace,
  type WorkspaceSeed,
} from "@/features/workspace/data/seed-workspace";
import {
  addMoney,
  centsToMoney,
  moneyToCents,
  multiplyMoney,
} from "@/features/workspace/lib/money";
import { availableBatches } from "@/features/workspace/lib/workspace-helpers";
import type {
  CheckoutInput,
  MedicineInput,
  ReceiveStockInput,
  ReturnInput,
  Sale,
  SaleItem,
  StaffInput,
  StaffRole,
  StaffUser,
  Supplier,
  SupplierInput,
} from "@/features/workspace/types";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";
import { uuid } from "../../../lib/uuid";

export class WorkspaceError extends Error {
  constructor(
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "WorkspaceError";
  }
}

interface WorkspaceActions {
  addMedicine(input: MedicineInput): string;
  deleteMedicine(id: string): void;
  setMedicineStatus(id: string, status: MedicineInput["status"]): void;
  updateMedicine(id: string, input: MedicineInput): void;
  addSupplier(input: SupplierInput): string;
  deleteSupplier(id: string): void;
  setSupplierStatus(id: string, status: Supplier["status"]): void;
  updateSupplier(id: string, input: SupplierInput): void;
  receiveStock(input: ReceiveStockInput, actor: string): string;
  openShift(openingFloat: string, actor: string): string;
  closeShift(actualCash: string): void;
  completeSale(input: CheckoutInput & { cashierName: string }): string;
  returnSaleItem(input: ReturnInput & { actor: string }): void;
  addStaff(input: StaffInput): string;
  updateStaff(id: string, input: StaffInput, actorUsername: string): void;
  setStaffStatus(
    id: string,
    status: StaffUser["status"],
    actorUsername: string,
  ): void;
  updateSettings(settings: WorkspaceSeed["settings"]): void;
  resetWorkspace(): void;
}

export type WorkspaceState = WorkspaceSeed & WorkspaceActions;

type LegacyStaffRole =
  | "OWNER"
  | "MANAGER"
  | "PHARMACIST"
  | "CASHIER"
  | "INVENTORY";

interface PersistedStaffUser {
  id: string;
  displayName: string;
  username: string;
  phoneNumber?: string;
  jobTitle?: string;
  role?: LegacyStaffRole;
  roles?: StaffRole[];
  status: StaffUser["status"];
}

const legacyRoleMap: Record<LegacyStaffRole, StaffRole> = {
  OWNER: "OWNER",
  MANAGER: "BRANCH_MANAGER",
  PHARMACIST: "PHARMACIST",
  CASHIER: "CASHIER",
  INVENTORY: "STORE_KEEPER",
};

const legacyJobTitleMap: Record<LegacyStaffRole, string> = {
  OWNER: "Pharmacy owner",
  MANAGER: "Branch manager",
  PHARMACIST: "Pharmacist",
  CASHIER: "Cashier",
  INVENTORY: "Store keeper",
};

function id(prefix: string) {
  return `${prefix}-${uuid()}`;
}

function now() {
  return new Date().toISOString();
}

function documentNumber(prefix: string, sequence: number) {
  return `${prefix}-${String(sequence).padStart(6, "0")}`;
}

function expectedCash(shift: {
  openingFloat: string;
  cashSales: string;
  cashRefunds: string;
}) {
  return centsToMoney(
    moneyToCents(shift.openingFloat) +
      moneyToCents(shift.cashSales) -
      moneyToCents(shift.cashRefunds),
  );
}

export const useWorkspaceStore = create<WorkspaceState>()(
  persist(
    (set, get) => ({
      ...(DEMO_AUTH_ENABLED ? createSeedWorkspace() : createEmptyWorkspace()),
      addMedicine(input) {
        const state = get();
        const duplicate = state.medicines.some(
          (medicine) =>
            medicine.sku.toLowerCase() === input.sku.toLowerCase() ||
            (input.barcode !== "" && medicine.barcode === input.barcode),
        );
        if (duplicate) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "A medicine already uses this SKU or barcode.",
          );
        }

        const medicineId = id("med");
        set({
          medicines: [
            { ...input, id: medicineId, buyingUnitName: null, createdAt: now() },
            ...state.medicines,
          ],
        });
        return medicineId;
      },
      updateMedicine(medicineId, input) {
        const state = get();
        const duplicate = state.medicines.some(
          (medicine) =>
            medicine.id !== medicineId &&
            (medicine.sku.toLowerCase() === input.sku.toLowerCase() ||
              (input.barcode !== "" && medicine.barcode === input.barcode)),
        );
        if (duplicate) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "A medicine already uses this SKU or barcode.",
          );
        }
        set({
          medicines: state.medicines.map((medicine) =>
            medicine.id === medicineId ? { ...medicine, ...input } : medicine,
          ),
        });
      },
      setMedicineStatus(medicineId, status) {
        const state = get();
        if (!state.medicines.some((medicine) => medicine.id === medicineId)) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The medicine could not be found.",
          );
        }
        set({
          medicines: state.medicines.map((medicine) =>
            medicine.id === medicineId ? { ...medicine, status } : medicine,
          ),
        });
      },
      deleteMedicine(medicineId) {
        const state = get();
        if (!state.medicines.some((medicine) => medicine.id === medicineId)) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The medicine could not be found.",
          );
        }
        const hasHistory =
          state.batches.some((batch) => batch.medicineId === medicineId) ||
          state.movements.some((movement) => movement.medicineId === medicineId) ||
          state.sales.some((sale) =>
            sale.items.some((item) => item.medicineId === medicineId),
          );
        if (hasHistory) {
          throw new WorkspaceError(
            "RESOURCE_HAS_HISTORY",
            "This medicine has stock or sales history. Archive it instead.",
          );
        }
        set({
          medicines: state.medicines.filter(
            (medicine) => medicine.id !== medicineId,
          ),
        });
      },
      addSupplier(input) {
        const state = get();
        if (
          state.suppliers.some(
            (supplier) =>
              supplier.name.toLowerCase() === input.name.trim().toLowerCase(),
          )
        ) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "A supplier already uses this name.",
          );
        }
        const supplierId = id("sup");
        set({
          suppliers: [
            {
              ...input,
              id: supplierId,
              status: "ACTIVE",
              createdAt: now(),
            },
            ...state.suppliers,
          ],
        });
        return supplierId;
      },
      updateSupplier(supplierId, input) {
        const state = get();
        if (!state.suppliers.some((supplier) => supplier.id === supplierId)) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The supplier could not be found.",
          );
        }
        if (
          state.suppliers.some(
            (supplier) =>
              supplier.id !== supplierId &&
              supplier.name.toLowerCase() === input.name.trim().toLowerCase(),
          )
        ) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "A supplier already uses this name.",
          );
        }
        set({
          suppliers: state.suppliers.map((supplier) =>
            supplier.id === supplierId
              ? {
                  ...supplier,
                  name: input.name.trim(),
                  phone: input.phone.trim(),
                  email: input.email.trim(),
                }
              : supplier,
          ),
        });
      },
      setSupplierStatus(supplierId, status) {
        const state = get();
        if (!state.suppliers.some((supplier) => supplier.id === supplierId)) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The supplier could not be found.",
          );
        }
        set({
          suppliers: state.suppliers.map((supplier) =>
            supplier.id === supplierId ? { ...supplier, status } : supplier,
          ),
        });
      },
      deleteSupplier(supplierId) {
        const state = get();
        if (!state.suppliers.some((supplier) => supplier.id === supplierId)) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The supplier could not be found.",
          );
        }
        const hasHistory =
          state.batches.some((batch) => batch.supplierId === supplierId) ||
          state.goodsReceipts.some(
            (receipt) => receipt.supplierId === supplierId,
          );
        if (hasHistory) {
          throw new WorkspaceError(
            "RESOURCE_HAS_HISTORY",
            "This supplier has stock receiving history. Archive it instead.",
          );
        }
        set({
          suppliers: state.suppliers.filter(
            (supplier) => supplier.id !== supplierId,
          ),
        });
      },
      receiveStock(input, actor) {
        const state = get();
        const supplier = state.suppliers.find(
          (candidate) => candidate.id === input.supplierId,
        );
        const medicine = state.medicines.find(
          (candidate) => candidate.id === input.medicineId,
        );
        if (!supplier || !medicine) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The selected supplier or medicine is unavailable.",
          );
        }
        if (supplier.status !== "ACTIVE") {
          throw new WorkspaceError(
            "RESOURCE_INACTIVE",
            "The selected supplier is archived.",
          );
        }
        if (medicine.status !== "ACTIVE") {
          throw new WorkspaceError(
            "RESOURCE_INACTIVE",
            "The selected medicine is archived.",
          );
        }
        if (input.quantity <= 0) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Received quantity must be greater than zero.",
          );
        }

        const grnNumber = documentNumber("GRN", state.grnSequence);
        const receivedAt = now();
        const existingBatch = state.batches.find(
          (batch) =>
            batch.medicineId === input.medicineId &&
            batch.batchNumber.toLowerCase() === input.batchNumber.toLowerCase(),
        );
        const batchId = existingBatch?.id ?? id("batch");
        const batches = existingBatch
          ? state.batches.map((batch) =>
              batch.id === existingBatch.id
                ? {
                    ...batch,
                    expiryDate: input.expiryDate,
                    quantity: batch.quantity + input.quantity,
                    supplierId: input.supplierId,
                    unitCost: input.unitCost,
                  }
                : batch,
            )
          : [
              {
                id: batchId,
                medicineId: input.medicineId,
                supplierId: input.supplierId,
                batchNumber: input.batchNumber,
                expiryDate: input.expiryDate,
                quantity: input.quantity,
                unitCost: input.unitCost,
                receivedAt,
              },
              ...state.batches,
            ];

        set({
          batches,
          goodsReceipts: [
            {
              id: id("grn"),
              number: grnNumber,
              supplierId: input.supplierId,
              receivedAt,
              totalCost: multiplyMoney(input.unitCost, input.quantity),
              itemCount: 1,
            },
            ...state.goodsReceipts,
          ],
          grnSequence: state.grnSequence + 1,
          movements: [
            {
              id: id("movement"),
              medicineId: input.medicineId,
              batchId,
              type: "PURCHASE",
              quantityDelta: input.quantity,
              reference: grnNumber,
              actor,
              occurredAt: receivedAt,
            },
            ...state.movements,
          ],
        });
        return grnNumber;
      },
      openShift(openingFloat, actor) {
        const state = get();
        if (state.currentShiftId) {
          throw new WorkspaceError(
            "SHIFT_ALREADY_OPEN",
            "Close the current shift before opening another one.",
          );
        }
        if (moneyToCents(openingFloat) < 0) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Opening cash cannot be negative.",
          );
        }
        const shiftId = id("shift");
        const shift = {
          id: shiftId,
          openedAt: now(),
          closedAt: null,
          openingFloat: centsToMoney(moneyToCents(openingFloat)),
          cashSales: "0.00",
          mpesaSales: "0.00",
          cashRefunds: "0.00",
          expectedCash: centsToMoney(moneyToCents(openingFloat)),
          actualCash: null,
          variance: null,
          status: "OPEN" as const,
          openedBy: actor,
        };
        set({ currentShiftId: shiftId, shifts: [shift, ...state.shifts] });
        return shiftId;
      },
      closeShift(actualCash) {
        const state = get();
        const shift = state.shifts.find(
          (candidate) => candidate.id === state.currentShiftId,
        );
        if (!shift) {
          throw new WorkspaceError(
            "SHIFT_NOT_OPEN",
            "There is no open shift to close.",
          );
        }
        const normalizedActual = centsToMoney(moneyToCents(actualCash));
        const expected = expectedCash(shift);
        const variance = centsToMoney(
          moneyToCents(normalizedActual) - moneyToCents(expected),
        );
        set({
          currentShiftId: null,
          shifts: state.shifts.map((candidate) =>
            candidate.id === shift.id
              ? {
                  ...candidate,
                  actualCash: normalizedActual,
                  closedAt: now(),
                  expectedCash: expected,
                  status: "CLOSED",
                  variance,
                }
              : candidate,
          ),
        });
      },
      completeSale(input) {
        const state = get();
        const completedSaleId =
          state.completedCheckoutKeys[input.idempotencyKey];
        if (completedSaleId) {
          return completedSaleId;
        }
        const shift = state.shifts.find(
          (candidate) => candidate.id === state.currentShiftId,
        );
        if (!shift || shift.status !== "OPEN") {
          throw new WorkspaceError(
            "SHIFT_NOT_OPEN",
            "Open a cashier shift before checkout.",
          );
        }
        if (input.items.length === 0) {
          throw new WorkspaceError("EMPTY_CART", "Add at least one item.");
        }
        if (input.paymentMethod === "MPESA" && !input.mpesaReference.trim()) {
          throw new WorkspaceError(
            "PAYMENT_REFERENCE_REQUIRED",
            "Enter the confirmed M-Pesa reference.",
          );
        }

        let batches = state.batches.map((batch) => ({ ...batch }));
        const movements: WorkspaceState["movements"] = [];
        const saleItems: SaleItem[] = [];
        let totalCents = 0;
        let taxCents = 0;
        const receiptNumber = documentNumber(
          state.settings.receiptPrefix,
          state.receiptSequence,
        );
        const completedAt = now();

        for (const requestedItem of input.items) {
          const medicine = state.medicines.find(
            (candidate) => candidate.id === requestedItem.medicineId,
          );
          if (!medicine || medicine.status !== "ACTIVE") {
            throw new WorkspaceError(
              "RESOURCE_NOT_FOUND",
              "A cart item is no longer available.",
            );
          }
          if (medicine.prescriptionRequired && !input.pharmacistApproved) {
            throw new WorkspaceError(
              "PHARMACIST_APPROVAL_REQUIRED",
              `${medicine.brandName} requires pharmacist approval.`,
            );
          }

          let remaining = requestedItem.quantity;
          const allocations: SaleItem["allocations"] = [];
          const candidates = availableBatches(batches, medicine.id);
          for (const batch of candidates) {
            if (remaining === 0) {
              break;
            }
            const allocated = Math.min(remaining, batch.quantity);
            remaining -= allocated;
            allocations.push({
              batchId: batch.id,
              batchNumber: batch.batchNumber,
              quantity: allocated,
            });
            batches = batches.map((candidate) =>
              candidate.id === batch.id
                ? { ...candidate, quantity: candidate.quantity - allocated }
                : candidate,
            );
            movements.push({
              id: id("movement"),
              medicineId: medicine.id,
              batchId: batch.id,
              type: "SALE",
              quantityDelta: -allocated,
              reference: receiptNumber,
              actor: input.cashierName,
              occurredAt: completedAt,
            });
          }
          if (remaining > 0) {
            throw new WorkspaceError(
              "INSUFFICIENT_STOCK",
              `${medicine.brandName} no longer has enough stock.`,
            );
          }

          const lineTotal = multiplyMoney(
            medicine.sellingPrice,
            requestedItem.quantity,
          );
          const lineCents = moneyToCents(lineTotal);
          const lineTax =
            medicine.taxCategory === "VAT_16"
              ? Math.round((lineCents * 16) / 116)
              : 0;
          totalCents += lineCents;
          taxCents += lineTax;
          saleItems.push({
            id: id("sale-item"),
            medicineId: medicine.id,
            medicineName: medicine.brandName,
            quantity: requestedItem.quantity,
            returnedQuantity: 0,
            unitPrice: medicine.sellingPrice,
            lineTotal,
            allocations,
          });
        }

        const total = centsToMoney(totalCents);
        const saleId = id("sale");
        const sale: Sale = {
          id: saleId,
          receiptNumber,
          shiftId: shift.id,
          completedAt,
          cashierName: input.cashierName,
          status: "COMPLETED",
          items: saleItems,
          payments: [
            {
              method: input.paymentMethod,
              amount: total,
              reference:
                input.paymentMethod === "MPESA"
                  ? input.mpesaReference.trim().toUpperCase()
                  : null,
            },
          ],
          subtotal: centsToMoney(totalCents - taxCents),
          taxTotal: centsToMoney(taxCents),
          total,
          refundTotal: "0.00",
          cashTendered:
            input.paymentMethod === "CASH"
              ? input.cashTendered ?? total
              : null,
          changeDue:
            input.paymentMethod === "CASH" && input.cashTendered
              ? centsToMoney(
                  Math.max(0, moneyToCents(input.cashTendered) - totalCents),
                )
              : "0.00",
          idempotencyKey: input.idempotencyKey,
        };
        const updatedShift = {
          ...shift,
          cashSales:
            input.paymentMethod === "CASH"
              ? addMoney(shift.cashSales, total)
              : shift.cashSales,
          mpesaSales:
            input.paymentMethod === "MPESA"
              ? addMoney(shift.mpesaSales, total)
              : shift.mpesaSales,
        };
        updatedShift.expectedCash = expectedCash(updatedShift);

        set({
          batches,
          completedCheckoutKeys: {
            ...state.completedCheckoutKeys,
            [input.idempotencyKey]: saleId,
          },
          movements: [...movements, ...state.movements],
          receiptSequence: state.receiptSequence + 1,
          sales: [sale, ...state.sales],
          shifts: state.shifts.map((candidate) =>
            candidate.id === shift.id ? updatedShift : candidate,
          ),
        });
        return saleId;
      },
      returnSaleItem(input) {
        const state = get();
        const sale = state.sales.find((candidate) => candidate.id === input.saleId);
        const saleItem = sale?.items.find(
          (candidate) => candidate.id === input.saleItemId,
        );
        if (!sale || !saleItem) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The selected sale item could not be found.",
          );
        }
        const returnable = saleItem.quantity - saleItem.returnedQuantity;
        if (input.quantity <= 0 || input.quantity > returnable) {
          throw new WorkspaceError(
            "RETURN_QUANTITY_EXCEEDED",
            `You can return up to ${returnable} unit(s).`,
          );
        }
        if (!input.reason.trim()) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Enter a reason for the return.",
          );
        }

        const refund = multiplyMoney(saleItem.unitPrice, input.quantity);
        const updatedItems = sale.items.map((candidate) =>
          candidate.id === saleItem.id
            ? {
                ...candidate,
                returnedQuantity:
                  candidate.returnedQuantity + input.quantity,
              }
            : candidate,
        );
        const fullyReturned = updatedItems.every(
          (item) => item.returnedQuantity === item.quantity,
        );
        let batches = state.batches;
        let movements = state.movements;
        if (input.resalable && saleItem.allocations[0]) {
          const allocation = saleItem.allocations[0];
          batches = state.batches.map((batch) =>
            batch.id === allocation.batchId
              ? { ...batch, quantity: batch.quantity + input.quantity }
              : batch,
          );
          movements = [
            {
              id: id("movement"),
              medicineId: saleItem.medicineId,
              batchId: allocation.batchId,
              type: "SALE_RETURN",
              quantityDelta: input.quantity,
              reference: sale.receiptNumber,
              actor: input.actor,
              occurredAt: now(),
            },
            ...state.movements,
          ];
        }

        const originalPayment = sale.payments[0]?.method;
        const shift = state.shifts.find(
          (candidate) => candidate.id === state.currentShiftId,
        );
        const shifts = shift && originalPayment === "CASH"
          ? state.shifts.map((candidate) => {
              if (candidate.id !== shift.id) {
                return candidate;
              }
              const updated = {
                ...candidate,
                cashRefunds: addMoney(candidate.cashRefunds, refund),
              };
              updated.expectedCash = expectedCash(updated);
              return updated;
            })
          : state.shifts;

        set({
          batches,
          movements,
          sales: state.sales.map((candidate) =>
            candidate.id === sale.id
              ? {
                  ...candidate,
                  items: updatedItems,
                  refundTotal: addMoney(candidate.refundTotal, refund),
                  status: fullyReturned ? "RETURNED" : "PARTIALLY_RETURNED",
                }
              : candidate,
          ),
          shifts,
        });
      },
      addStaff(input) {
        const state = get();
        const normalizedPhone = input.phoneNumber.replace(/[\s-]/g, "");
        if (!/^\+?\d{10,15}$/.test(normalizedPhone)) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Enter a valid phone number with 10 to 15 digits.",
          );
        }
        if (input.roles.length === 0) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Assign at least one access role.",
          );
        }
        if (
          state.staff.some(
            (user) => user.username.toLowerCase() === input.username.toLowerCase(),
          )
        ) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "This username is already assigned.",
          );
        }
        const staffId = id("staff");
        set({
          staff: [
            {
              ...input,
              id: staffId,
              phoneNumber: input.phoneNumber.trim(),
              jobTitle: input.jobTitle.trim(),
              roles: [...new Set(input.roles)],
              status: "ACTIVE",
            },
            ...state.staff,
          ],
        });
        return staffId;
      },
      updateStaff(staffId, input, actorUsername) {
        const state = get();
        const staffUser = state.staff.find((user) => user.id === staffId);
        if (!staffUser) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The staff account could not be found.",
          );
        }
        if (
          staffUser.username.toLowerCase() === actorUsername.toLowerCase()
        ) {
          throw new WorkspaceError(
            "SELF_MANAGE_NOT_ALLOWED",
            "You cannot change your own staff account.",
          );
        }
        if (input.roles.length === 0) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Assign at least one access role.",
          );
        }
        const normalizedPhone = input.phoneNumber.replace(/[\s-]/g, "");
        if (!/^\+?\d{10,15}$/.test(normalizedPhone)) {
          throw new WorkspaceError(
            "VALIDATION_FAILED",
            "Enter a valid phone number with 10 to 15 digits.",
          );
        }
        if (
          state.staff.some(
            (user) =>
              user.id !== staffId &&
              user.username.toLowerCase() === input.username.toLowerCase(),
          )
        ) {
          throw new WorkspaceError(
            "DUPLICATE_RESOURCE",
            "This username is already assigned.",
          );
        }

        const removesLastActiveOwner =
          staffUser.status === "ACTIVE" &&
          staffUser.roles.includes("OWNER") &&
          !input.roles.includes("OWNER") &&
          state.staff.filter(
            (user) => user.status === "ACTIVE" && user.roles.includes("OWNER"),
          ).length === 1;
        if (removesLastActiveOwner) {
          throw new WorkspaceError(
            "LAST_ACTIVE_OWNER",
            "Keep at least one active owner account.",
          );
        }

        set({
          staff: state.staff.map((user) =>
            user.id === staffId
              ? {
                  ...user,
                  ...input,
                  displayName: input.displayName.trim(),
                  username: input.username.trim(),
                  phoneNumber: input.phoneNumber.trim(),
                  jobTitle: input.jobTitle.trim(),
                  roles: [...new Set(input.roles)],
                }
              : user,
          ),
        });
      },
      setStaffStatus(staffId, status, actorUsername) {
        const state = get();
        const staffUser = state.staff.find((user) => user.id === staffId);
        if (!staffUser) {
          throw new WorkspaceError(
            "RESOURCE_NOT_FOUND",
            "The staff account could not be found.",
          );
        }
        if (
          status === "DISABLED" &&
          staffUser.username.toLowerCase() === actorUsername.toLowerCase()
        ) {
          throw new WorkspaceError(
            "SELF_DISABLE_NOT_ALLOWED",
            "You cannot disable your signed-in account.",
          );
        }
        const disablesLastActiveOwner =
          status === "DISABLED" &&
          staffUser.status === "ACTIVE" &&
          staffUser.roles.includes("OWNER") &&
          state.staff.filter(
            (user) => user.status === "ACTIVE" && user.roles.includes("OWNER"),
          ).length === 1;
        if (disablesLastActiveOwner) {
          throw new WorkspaceError(
            "LAST_ACTIVE_OWNER",
            "Keep at least one active owner account.",
          );
        }

        set({
          staff: state.staff.map((user) =>
            user.id === staffId ? { ...user, status } : user,
          ),
        });
      },
      updateSettings(settings) {
        set({ settings });
      },
      resetWorkspace() {
        set(DEMO_AUTH_ENABLED ? createSeedWorkspace() : createEmptyWorkspace());
      },
    }),
    {
      name: "pharmacy-pos:workspace-preview",
      storage: createJSONStorage(() =>
        DEMO_AUTH_ENABLED
          ? window.localStorage
          : {
              getItem: () => null,
              removeItem: () => undefined,
              setItem: () => undefined,
            },
      ),
      skipHydration: true,
      migrate(persistedState, version) {
        if (version >= 4) {
          return persistedState as WorkspaceState;
        }

        const state = persistedState as Omit<WorkspaceState, "staff"> & {
          staff?: PersistedStaffUser[];
        };
        return {
          ...state,
          settings: {
            ...state.settings,
            receiptPaperWidth: state.settings.receiptPaperWidth ?? "80MM",
          },
          staff: (state.staff ?? []).map((user) => {
            const legacyRole = user.role ?? "CASHIER";
            return {
              id: user.id,
              displayName: user.displayName,
              username: user.username,
              phoneNumber: user.phoneNumber ?? "",
              jobTitle: user.jobTitle ?? legacyJobTitleMap[legacyRole],
              roles: user.roles?.length
                ? user.roles
                : [legacyRoleMap[legacyRole]],
              status: user.status,
            };
          }),
        } as WorkspaceState;
      },
      version: 4,
    },
  ),
);
