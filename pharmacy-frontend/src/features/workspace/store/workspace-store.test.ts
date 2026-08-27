import { beforeEach, describe, expect, it } from "vitest";

import { createSeedWorkspace } from "@/features/workspace/data/seed-workspace";
import {
  useWorkspaceStore,
  WorkspaceError,
} from "@/features/workspace/store/workspace-store";

describe("workspace transactions", () => {
  beforeEach(() => {
    localStorage.clear();
    useWorkspaceStore.setState(createSeedWorkspace());
  });

  it("allocates stock from the earliest-expiring batch and updates the shift", () => {
    const before = useWorkspaceStore.getState();
    const firstBatch = before.batches.find((batch) => batch.id === "batch-pan-1");
    const secondBatch = before.batches.find((batch) => batch.id === "batch-pan-2");

    const saleId = before.completeSale({
      idempotencyKey: "checkout-fefo",
      cashierName: "Test Cashier",
      items: [{ medicineId: "med-panadol", quantity: 3 }],
      paymentMethod: "CASH",
      mpesaMode: "MANUAL",
      mpesaPhone: "",
      mpesaReference: "",
      pharmacistApproved: false,
    });

    const after = useWorkspaceStore.getState();
    expect(after.batches.find((batch) => batch.id === "batch-pan-1")?.quantity).toBe(
      (firstBatch?.quantity ?? 0) - 3,
    );
    expect(after.batches.find((batch) => batch.id === "batch-pan-2")?.quantity).toBe(
      secondBatch?.quantity,
    );
    expect(after.sales.find((sale) => sale.id === saleId)?.items[0].allocations).toEqual([
      { batchId: "batch-pan-1", batchNumber: "PAN24091", quantity: 3 },
    ]);
    expect(after.shifts.find((shift) => shift.id === "shift-current")?.cashSales).toBe(
      "60.00",
    );
  });

  it("makes a repeated checkout key idempotent", () => {
    const input = {
      idempotencyKey: "checkout-repeat",
      cashierName: "Test Cashier",
      items: [{ medicineId: "med-brufen", quantity: 1 }],
      paymentMethod: "CASH" as const,
      mpesaMode: "MANUAL" as const,
      mpesaPhone: "",
      mpesaReference: "",
      pharmacistApproved: false,
    };
    const initialCount = useWorkspaceStore.getState().sales.length;
    const firstId = useWorkspaceStore.getState().completeSale(input);
    const secondId = useWorkspaceStore.getState().completeSale(input);

    expect(secondId).toBe(firstId);
    expect(useWorkspaceStore.getState().sales).toHaveLength(initialCount + 1);
  });

  it("requires pharmacist approval for prescription medicines", () => {
    expect(() =>
      useWorkspaceStore.getState().completeSale({
        idempotencyKey: "checkout-rx",
        cashierName: "Test Cashier",
        items: [{ medicineId: "med-amoxil", quantity: 1 }],
        paymentMethod: "CASH",
        mpesaMode: "MANUAL",
        mpesaPhone: "",
        mpesaReference: "",
        pharmacistApproved: false,
      }),
    ).toThrowError(WorkspaceError);
    expect(useWorkspaceStore.getState().sales.some((sale) => sale.idempotencyKey === "checkout-rx")).toBe(false);
  });

  it("restores a resalable return and blocks excess quantities", () => {
    const saleId = useWorkspaceStore.getState().completeSale({
      idempotencyKey: "checkout-return",
      cashierName: "Test Cashier",
      items: [{ medicineId: "med-panadol", quantity: 2 }],
      paymentMethod: "CASH",
      mpesaMode: "MANUAL",
      mpesaPhone: "",
      mpesaReference: "",
      pharmacistApproved: false,
    });
    const saleItem = useWorkspaceStore.getState().sales.find((sale) => sale.id === saleId)?.items[0];
    const quantityAfterSale = useWorkspaceStore.getState().batches.find((batch) => batch.id === "batch-pan-1")?.quantity;

    useWorkspaceStore.getState().returnSaleItem({
      idempotencyKey: "return-one",
      saleId,
      saleItemId: saleItem?.id ?? "",
      quantity: 1,
      reason: "Sealed item returned",
      resalable: true,
      actor: "Test Cashier",
    });

    expect(useWorkspaceStore.getState().batches.find((batch) => batch.id === "batch-pan-1")?.quantity).toBe(
      (quantityAfterSale ?? 0) + 1,
    );
    expect(useWorkspaceStore.getState().sales.find((sale) => sale.id === saleId)?.status).toBe(
      "PARTIALLY_RETURNED",
    );
    expect(() =>
      useWorkspaceStore.getState().returnSaleItem({
        idempotencyKey: "return-excess",
        saleId,
        saleItemId: saleItem?.id ?? "",
        quantity: 2,
        reason: "Too many",
        resalable: false,
        actor: "Test Cashier",
      }),
    ).toThrow("You can return up to 1 unit(s).");
  });

  it("records a GRN, batch, and purchase movement together", () => {
    const before = useWorkspaceStore.getState();
    const grn = before.receiveStock(
      {
        idempotencyKey: "receive-stock",
        supplierId: "sup-medsource",
        medicineId: "med-brufen",
        batchNumber: "BRU-NEW",
        expiryDate: "2028-12-31",
        quantity: 12,
        unitCost: "10.00",
      },
      "Inventory Clerk",
    );
    const after = useWorkspaceStore.getState();
    const batch = after.batches.find((candidate) => candidate.batchNumber === "BRU-NEW");

    expect(grn).toBe("GRN-000004");
    expect(batch?.quantity).toBe(12);
    expect(after.goodsReceipts[0]).toMatchObject({ number: grn, totalCost: "120.00" });
    expect(after.movements[0]).toMatchObject({
      batchId: batch?.id,
      quantityDelta: 12,
      reference: grn,
      type: "PURCHASE",
    });
  });

  it("deletes only medicines without stock or sales history", () => {
    const store = useWorkspaceStore.getState();
    const template = store.medicines[0];
    const medicineId = store.addMedicine({
      sku: "MED-UNUSED",
      barcode: "6161100099999",
      brandName: "Unused Test Medicine",
      genericName: template.genericName,
      categoryId: template.categoryId,
      unitId: template.unitId,
      buyingUnitId: null,
      packSize: null,
      manufacturer: template.manufacturer,
      taxCategory: template.taxCategory,
      prescriptionRequired: template.prescriptionRequired,
      controlledDrug: false,
      buyingPrice: template.buyingPrice,
      sellingPrice: template.sellingPrice,
      reorderLevel: template.reorderLevel,
      status: template.status,
    });

    store.deleteMedicine(medicineId);

    expect(
      useWorkspaceStore
        .getState()
        .medicines.some((medicine) => medicine.id === medicineId),
    ).toBe(false);
    expect(() => store.deleteMedicine("med-panadol")).toThrow(
      "This medicine has stock or sales history. Archive it instead.",
    );
    store.setMedicineStatus("med-panadol", "INACTIVE");
    expect(
      useWorkspaceStore
        .getState()
        .medicines.find((medicine) => medicine.id === "med-panadol")?.status,
    ).toBe("INACTIVE");
  });

  it("allows distinct medicines without retail barcodes", () => {
    const store = useWorkspaceStore.getState();
    const template = store.medicines[0];
    const baseInput = {
      barcode: "",
      brandName: "Unbarcoded Medicine",
      genericName: template.genericName,
      categoryId: template.categoryId,
      unitId: template.unitId,
      buyingUnitId: null,
      packSize: null,
      manufacturer: template.manufacturer,
      taxCategory: template.taxCategory,
      prescriptionRequired: template.prescriptionRequired,
      controlledDrug: false,
      buyingPrice: template.buyingPrice,
      sellingPrice: template.sellingPrice,
      reorderLevel: template.reorderLevel,
      status: template.status,
    };

    expect(() =>
      store.addMedicine({ ...baseInput, sku: "NO-BARCODE-1" }),
    ).not.toThrow();
    expect(() =>
      store.addMedicine({ ...baseInput, sku: "NO-BARCODE-2" }),
    ).not.toThrow();
    expect(() =>
      store.addMedicine({ ...baseInput, sku: "NO-BARCODE-1" }),
    ).toThrow("A medicine already uses this SKU or barcode.");
  });

  it("edits, archives, and deletes an unused supplier", () => {
    const store = useWorkspaceStore.getState();
    const supplierId = store.addSupplier({
      name: "Unused Supplier",
      phone: "+254700999999",
      email: "unused@example.test",
    });

    store.updateSupplier(supplierId, {
      name: "Updated Supplier",
      phone: "+254711999999",
      email: "updated@example.test",
    });
    store.setSupplierStatus(supplierId, "INACTIVE");

    expect(
      useWorkspaceStore
        .getState()
        .suppliers.find((supplier) => supplier.id === supplierId),
    ).toMatchObject({ name: "Updated Supplier", status: "INACTIVE" });

    useWorkspaceStore.getState().deleteSupplier(supplierId);
    expect(
      useWorkspaceStore
        .getState()
        .suppliers.some((supplier) => supplier.id === supplierId),
    ).toBe(false);
    expect(() =>
      useWorkspaceStore.getState().deleteSupplier("sup-medsource"),
    ).toThrow(
      "This supplier has stock receiving history. Archive it instead.",
    );
  });

  it("rejects receiving against an archived supplier", () => {
    const store = useWorkspaceStore.getState();
    store.setSupplierStatus("sup-medsource", "INACTIVE");

    expect(() =>
      useWorkspaceStore.getState().receiveStock(
        {
          idempotencyKey: "receive-archived",
          supplierId: "sup-medsource",
          medicineId: "med-brufen",
          batchNumber: "ARCHIVED-SUPPLIER",
          expiryDate: "2028-12-31",
          quantity: 1,
          unitCost: "10.00",
        },
        "Inventory Clerk",
      ),
    ).toThrow("The selected supplier is archived.");
  });

  it("stores multiple access roles for one staff account", () => {
    const staffId = useWorkspaceStore.getState().addStaff({
      displayName: "Test Technician",
      username: "countertech",
      phoneNumber: "0711000001",
      jobTitle: "Pharmacy technician",
      roles: ["CASHIER", "STORE_KEEPER"],
    });

    expect(
      useWorkspaceStore.getState().staff.find((user) => user.id === staffId),
    ).toMatchObject({
      jobTitle: "Pharmacy technician",
      roles: ["CASHIER", "STORE_KEEPER"],
    });
  });

  it("requires at least one access role for a staff account", () => {
    expect(() =>
      useWorkspaceStore.getState().addStaff({
        displayName: "No Role",
        username: "norole",
        phoneNumber: "0711000002",
        jobTitle: "Assistant",
        roles: [],
      }),
    ).toThrow("Assign at least one access role.");
  });

  it("updates staff details and status", () => {
    const store = useWorkspaceStore.getState();

    store.updateStaff("staff-cashier", {
      displayName: "Evening Cashier",
      username: "cashier-evening",
      phoneNumber: "0711000003",
      jobTitle: "Senior cashier",
      roles: ["CASHIER", "STORE_KEEPER"],
    }, "admin@demo.com");
    useWorkspaceStore
      .getState()
      .setStaffStatus("staff-cashier", "DISABLED", "admin@demo.com");

    expect(
      useWorkspaceStore
        .getState()
        .staff.find((user) => user.id === "staff-cashier"),
    ).toMatchObject({
      displayName: "Evening Cashier",
      roles: ["CASHIER", "STORE_KEEPER"],
      status: "DISABLED",
      username: "cashier-evening",
    });
  });

  it("protects the signed-in user and the final active owner", () => {
    const store = useWorkspaceStore.getState();

    expect(() =>
      store.setStaffStatus("staff-owner", "DISABLED", "admin@demo.com"),
    ).toThrow("You cannot disable your signed-in account.");
    expect(() =>
      store.setStaffStatus("staff-owner", "DISABLED", "manager"),
    ).toThrow("Keep at least one active owner account.");
    expect(() =>
      store.updateStaff("staff-owner", {
        displayName: "Pharmacy Owner",
        username: "admin@demo.com",
        phoneNumber: "0700000000",
        jobTitle: "Superintendent pharmacist",
        roles: ["PHARMACIST"],
      }, "manager"),
    ).toThrow("Keep at least one active owner account.");

    expect(() =>
      store.updateStaff("staff-owner", {
        displayName: "Updated Owner",
        username: "admin@demo.com",
        phoneNumber: "0700000000",
        jobTitle: "Owner",
        roles: ["OWNER"],
      }, "admin@demo.com"),
    ).toThrow("You cannot change your own staff account.");
  });
});
