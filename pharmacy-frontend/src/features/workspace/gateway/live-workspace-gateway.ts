"use client";

import {
  PERMISSIONS,
  TENANT_ROLES,
  type TenantRole,
} from "@/features/auth/access-control";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { createEmptyWorkspace } from "@/features/workspace/data/seed-workspace";
import type { WorkspaceGateway } from "@/features/workspace/gateway/workspace-gateway";
import type {
  BackendBatch,
  BackendBranch,
  BackendCategory,
  BackendDashboardReport,
  BackendGoodsReceipt,
  BackendInventoryReport,
  BackendManufacturer,
  BackendMedicine,
  BackendPage,
  BackendPaymentGatewayResponse,
  BackendPharmacy,
  BackendPosLookupItem,
  BackendSale,
  BackendSaleReturn,
  BackendSetting,
  BackendSalesReport,
  BackendShift,
  BackendStock,
  BackendStockMovement,
  BackendSupplier,
  BackendTaxCategory,
  BackendUnit,
  BackendUser,
  PosQuickItem,
} from "@/features/workspace/gateway/backend-workspace-types";
import { useWorkspaceStore, WorkspaceError } from "@/features/workspace/store/workspace-store";
import type {
  Batch,
  CheckoutInput,
  DashboardReport,
  InventoryReport,
  Medicine,
  MedicineInput,
  PaymentMethod,
  PaymentCapabilities,
  PharmacySettings,
  PosLookupItem,
  ReceiveStockInput,
  ReturnInput,
  Sale,
  SalesReport,
  Shift,
  StaffInput,
  StaffRole,
  StaffUser,
  Supplier,
  SupplierInput,
  TaxCategory,
} from "@/features/workspace/types";
import { apiRequest } from "@/lib/api-client";

type ApiPath = `/${string}`;

interface BackendRole {
  id: string;
  roleName: string;
}

interface BackendUserBranchRole {
  id: string;
  userId: string;
  roleId: string;
  roleName: string;
}

const ROLE_SET = new Set<string>(TENANT_ROLES);

function path(value: string) {
  return value as ApiPath;
}

function amount(value: number | string | null | undefined) {
  const parsed = typeof value === "number" ? value : Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed.toFixed(2) : "0.00";
}

function settingNumber(value: string | undefined, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function localDateTime() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 19);
}

function isRole(value: string): value is TenantRole {
  return ROLE_SET.has(value);
}

function taxCode(tax: Pick<BackendTaxCategory, "code" | "taxRate" | "taxType">): TaxCategory["code"] {
  const code = tax.code.toUpperCase();
  const type = tax.taxType.toUpperCase();
  if (code.includes("16") || tax.taxRate === 16 || type === "VAT_STANDARD") {
    return "VAT_16";
  }
  if (code.includes("ZERO") || type === "VAT_ZERO") return "ZERO_RATED";
  return "EXEMPT";
}

function saleStatus(status: string): Sale["status"] {
  const normalized = status.toUpperCase();
  if (normalized.includes("PARTIAL")) return "PARTIALLY_RETURNED";
  if (normalized.includes("RETURN")) return "RETURNED";
  if (normalized === "COMPLETED" || normalized === "DONE") return "COMPLETED";
  if (normalized === "CANCELLED") return "CANCELLED";
  if (normalized === "SUSPENDED") return "SUSPENDED";
  return "UNKNOWN";
}

function paymentMethod(method: string): PaymentMethod {
  return method.toUpperCase().includes("MPESA") || method.toUpperCase().includes("M_PESA")
    ? "MPESA"
    : "CASH";
}

function movementType(value: string) {
  const normalized = value.toUpperCase();
  if (normalized.includes("RETURN")) return "SALE_RETURN" as const;
  if (normalized.includes("SALE")) return "SALE" as const;
  if (normalized.includes("PURCHASE") || normalized.includes("RECEIV")) {
    return "PURCHASE" as const;
  }
  return "ADJUSTMENT" as const;
}

async function getPage<T>(endpoint: string) {
  const first = await apiRequest<BackendPage<T>>(path(endpoint), {
    cache: "no-store",
  });
  if (first.data.totalPages <= 1) return first.data.content;

  const separator = endpoint.includes("?") ? "&" : "?";
  const content = [...first.data.content];
  for (let page = 1; page < first.data.totalPages; page += 1) {
    const response = await apiRequest<BackendPage<T>>(
      path(`${endpoint}${separator}page=${page}`),
      { cache: "no-store" },
    );
    content.push(...response.data.content);
  }
  return content;
}

function has(permission: string) {
  return Boolean(
    useAuthStore
      .getState()
      .session?.user.permissions.some((candidate) => candidate === permission),
  );
}

export class LiveWorkspaceGateway implements WorkspaceGateway {
  private activeBranch: BackendBranch | null = null;
  private activePharmacy: BackendPharmacy | null = null;
  private roleIds = new Map<TenantRole, string>();
  private settingsByKey = new Map<string, BackendSetting>();

  async addMedicine(input: MedicineInput) {
    const response = await apiRequest<BackendMedicine>("/medicines", {
      body: this.medicinePayload(input),
      method: "POST",
    });
    await this.hydrate();
    return response.data.id;
  }

  async addStaff(input: StaffInput) {
    const session = this.requireSession();
    if (!input.password || input.password.length < 8) {
      throw new WorkspaceError(
        "PASSWORD_REQUIRED",
        "Enter a temporary password with at least 8 characters.",
      );
    }
    const names = input.displayName.trim().split(/\s+/);
    const firstName = names.shift() ?? "Staff";
    const lastName = names.join(" ") || "User";
    const response = await apiRequest<BackendUser>("/users", {
      body: {
        branchId: session.user.activeBranch.id,
        email: input.username.trim(),
        firstName,
        lastName,
        password: input.password,
        phoneNumber: input.phoneNumber.trim(),
        status: "ACTIVE",
      },
      method: "POST",
    });
    await this.assignRoles(response.data.id, input.roles);
    await this.hydrate();
    return response.data.id;
  }

  async addSupplier(input: SupplierInput) {
    const response = await apiRequest<BackendSupplier>("/suppliers", {
      body: this.supplierPayload(input, "ACTIVE"),
      method: "POST",
    });
    await this.hydrate();
    return response.data.id;
  }

  async closeShift(actualCash: string) {
    const shiftId = useWorkspaceStore.getState().currentShiftId;
    if (!shiftId) {
      throw new WorkspaceError("SHIFT_NOT_OPEN", "There is no open shift to close.");
    }
    await apiRequest<BackendShift>(path(`/shifts/${shiftId}/close`), {
      body: { actualCash: Number(actualCash), remarks: "Drawer counted", status: "CLOSED" },
      method: "PATCH",
    });
    await this.hydrate();
  }

  async completeSale(input: CheckoutInput) {
    const state = useWorkspaceStore.getState();
    if (!state.currentShiftId) {
      throw new WorkspaceError("SHIFT_NOT_OPEN", "Open a shift before checkout.");
    }
    const lines = input.items.map((line) => {
      const medicine = state.medicines.find((candidate) => candidate.id === line.medicineId);
      if (!medicine) {
        throw new WorkspaceError("RESOURCE_NOT_FOUND", "A cart medicine is no longer available.");
      }
      if (!line.lineId) {
        throw new WorkspaceError("INVALID_CART", "Remove and add the cart item again before checkout.");
      }
      return {
        expectedUnitPrice: Number(medicine.sellingPrice),
        lineId: line.lineId,
        medicineId: line.medicineId,
        quantity: line.quantity,
        requestedBatchId: null,
        sellingUnitId: medicine.unitId || null,
      };
    });
    const total = lines.reduce(
      (sum, line) => sum + line.expectedUnitPrice * line.quantity,
      0,
    );
    const method = input.paymentMethod === "MPESA"
      ? input.mpesaMode === "STK" ? "M_PESA" : "MPESA_MANUAL"
      : "CASH";
    const response = await apiRequest<BackendSale>("/sales", {
      body: {
        cashTendered:
          input.paymentMethod === "CASH"
            ? Number(input.cashTendered ?? total.toFixed(2))
            : null,
        clientSaleId: input.idempotencyKey,
        customerId: input.customerId || null,
        items: lines,
        note: null,
        payments: [
          {
            amount: total,
            method,
            reference:
              input.paymentMethod === "MPESA" && input.mpesaMode === "MANUAL"
                ? input.mpesaReference.trim()
                : null,
          },
        ],
        prescriptionReferenceId: input.prescriptionReferenceId || null,
        shiftId: state.currentShiftId,
      },
      idempotencyKey: input.idempotencyKey,
      method: "POST",
    });
    const saleId = response.data.id ?? response.data.saleId;
    if (input.paymentMethod === "MPESA" && input.mpesaMode === "STK") {
      const payment = response.data.payments.find(
        (candidate) => candidate.paymentMethod === "M_PESA",
      );
      if (!payment?.id) {
        throw new WorkspaceError(
          "MPESA_PAYMENT_MISSING",
          "The sale was reserved but its M-Pesa payment could not be loaded.",
        );
      }
      if (["FAILED", "CANCELLED"].includes(payment.paymentStatus)) {
        throw new WorkspaceError(
          `MPESA_${payment.paymentStatus}`,
          "The previous M-Pesa request did not complete. Start checkout again.",
        );
      }

      const initiated = await apiRequest<BackendPaymentGatewayResponse>(
        path(`/payments/${payment.id}/process?phoneNumber=${encodeURIComponent(input.mpesaPhone)}`),
        { method: "POST" },
      );
      if (initiated.data.status === "PENDING") {
        await this.hydrate();
        throw new WorkspaceError(
          "MPESA_PENDING",
          initiated.data.responseDescription ??
            "M-Pesa did not return a definitive response. Verify the payment before retrying.",
        );
      }
      if (!initiated.data.success && initiated.data.status !== "PROCESSING") {
        await this.hydrate();
        throw new WorkspaceError(
          `MPESA_FINAL_${initiated.data.responseCode ?? "STK_FAILED"}`,
          initiated.data.responseDescription ?? "M-Pesa could not start the payment request.",
        );
      }

      for (let attempt = 0; attempt < 30; attempt += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 2_500));
        const status = await apiRequest<BackendPaymentGatewayResponse>(
          path(`/payments/${payment.id}/status`),
          { cache: "no-store" },
        );
        if (status.data.status === "COMPLETED") {
          await this.hydrate();
          return saleId;
        }
        if (status.data.status === "FAILED" || status.data.status === "CANCELLED") {
          await this.hydrate();
          throw new WorkspaceError(
            `MPESA_${status.data.status}`,
            status.data.responseDescription ??
              (status.data.status === "CANCELLED"
                ? "The M-Pesa request was cancelled."
                : "M-Pesa did not complete the payment."),
          );
        }
      }
      await this.hydrate();
      throw new WorkspaceError(
        "MPESA_PENDING",
        "M-Pesa is still confirming this payment. Retry checkout to continue checking without sending another prompt.",
      );
    }
    await this.hydrate();
    return saleId;
  }

  async deleteMedicine(id: string) {
    await apiRequest<void>(path(`/medicines/${id}`), { method: "DELETE" });
    await this.hydrate();
  }

  async deleteSupplier(id: string) {
    await apiRequest<void>(path(`/suppliers/${id}`), { method: "DELETE" });
    await this.hydrate();
  }

  getSnapshot() {
    return useWorkspaceStore.getState();
  }

  async getDashboardReport(date?: string): Promise<DashboardReport> {
    const session = this.requireSession();
    const query = date ? `&date=${encodeURIComponent(date)}` : "";
    const scope = session.user.roles.includes("OWNER")
      ? "&pharmacyWide=true"
      : "";
    const response = await apiRequest<BackendDashboardReport>(
      path(`/reports/dashboard?branchId=${session.user.activeBranch.id}${query}${scope}`),
      { cache: "no-store" },
    );
    return {
      ...response.data,
      grossSales:
        response.data.grossSales == null
          ? null
          : amount(response.data.grossSales),
      netSales:
        response.data.netSales == null ? null : amount(response.data.netSales),
      refunds:
        response.data.refunds == null ? null : amount(response.data.refunds),
    };
  }

  async getInventoryReport(asOf?: string): Promise<InventoryReport> {
    const session = this.requireSession();
    const query = asOf ? `&asOf=${encodeURIComponent(asOf)}` : "";
    const scope = session.user.roles.includes("OWNER")
      ? "&pharmacyWide=true"
      : "";
    const response = await apiRequest<BackendInventoryReport>(
      path(`/reports/inventory-summary?branchId=${session.user.activeBranch.id}${query}${scope}`),
      { cache: "no-store" },
    );
    return {
      ...response.data,
      stockValue: amount(response.data.stockValue),
    };
  }

  async getPaymentCapabilities(): Promise<PaymentCapabilities> {
    return (await apiRequest<PaymentCapabilities>("/payments/capabilities", {
      cache: "no-store",
    })).data;
  }

  async getSalesReport(from: string, to: string): Promise<SalesReport> {
    const session = this.requireSession();
    const scope = session.user.roles.includes("OWNER")
      ? "&pharmacyWide=true"
      : "";
    const response = await apiRequest<BackendSalesReport>(
      path(
        `/reports/sales-summary?branchId=${session.user.activeBranch.id}` +
          `&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}${scope}`,
      ),
      { cache: "no-store" },
    );
    return {
      ...response.data,
      cashPayments: amount(response.data.cashPayments),
      cashRefunds: amount(response.data.cashRefunds),
      grossSales: amount(response.data.grossSales),
      mpesaPayments: amount(response.data.mpesaPayments),
      mpesaRefunds: amount(response.data.mpesaRefunds),
      netSales: amount(response.data.netSales),
      otherPayments: amount(response.data.otherPayments),
      otherRefunds: amount(response.data.otherRefunds),
      refunds: amount(response.data.refunds),
      topProducts: response.data.topProducts.map((product) => ({
        ...product,
        netRevenue: amount(product.netRevenue),
      })),
    };
  }

  async hydrate() {
    const session = useAuthStore.getState().session;
    if (!session) {
      useWorkspaceStore.setState(createEmptyWorkspace());
      return;
    }

    useWorkspaceStore.setState({ loadError: null, loadStatus: "loading" });
    try {
      const canReadMedicines = has(PERMISSIONS.MEDICINE_READ);
      const canReadInventory = has(PERMISSIONS.INVENTORY_READ);
      const canSell = has(PERMISSIONS.POS_SELL);
      const canReadSales = has(PERMISSIONS.SALE_READ);
      const canReadSuppliers = has(PERMISSIONS.SUPPLIER_READ);
      const canReadShifts =
        has(PERMISSIONS.SHIFT_OPEN) ||
        has(PERMISSIONS.SHIFT_CLOSE) ||
        has(PERMISSIONS.SHIFT_VARIANCE_APPROVE);
      const canReadOperationalSettings =
        has(PERMISSIONS.SETTINGS_MANAGE) ||
        has(PERMISSIONS.INVENTORY_READ) ||
        has(PERMISSIONS.DASHBOARD_READ);

      const [
        categories,
        units,
        manufacturers,
        taxes,
        medicines,
        suppliers,
        goodsReceipts,
        stocks,
        backendBatches,
        movements,
        shifts,
        backendSales,
        quickItems,
        users,
        assignments,
        roles,
        settings,
        activeBranch,
        activePharmacy,
      ] = await Promise.all([
        canReadMedicines ? getPage<BackendCategory>("/categories?size=200&sort=categoryName,asc") : [],
        canReadMedicines ? getPage<BackendUnit>("/units?size=200&sort=unitName,asc") : [],
        canReadMedicines ? getPage<BackendManufacturer>("/manufacturers?size=200&sort=manufacturerName,asc") : [],
        canReadMedicines ? getPage<BackendTaxCategory>("/tax-categories?activeOnly=true&size=100") : [],
        canReadMedicines ? getPage<BackendMedicine>("/medicines?size=500&sort=brandName,asc") : [],
        canReadSuppliers ? getPage<BackendSupplier>("/suppliers?size=500&sort=supplierName,asc") : [],
        canReadInventory ? getPage<BackendGoodsReceipt>("/goods-received?size=500&sort=receivedAt,desc") : [],
        canReadInventory ? getPage<BackendStock>("/stock?size=1000") : [],
        canReadInventory
          ? getPage<BackendBatch>(
              `/batches?branchId=${session.user.activeBranch.id}&size=1000&sort=expirationDate,asc`,
            )
          : [],
        canReadInventory ? getPage<BackendStockMovement>("/stock-movements?size=500&sort=movementDate,desc") : [],
        canReadShifts
          ? apiRequest<BackendShift[]>(path(`/shifts?userId=${session.user.id}`), { cache: "no-store" }).then((value) => value.data)
          : [],
        canReadSales ? getPage<BackendSale>("/sales?size=200&sort=completedAt,desc") : [],
        !canReadInventory && canSell
          ? apiRequest<PosQuickItem[]>("/pos/quick-items", { cache: "no-store" }).then((value) => value.data)
          : [],
        has(PERMISSIONS.USER_MANAGE)
          ? getPage<BackendUser>(`/users?branchId=${session.user.activeBranch.id}&size=300&sort=firstName,asc`)
          : [],
        has(PERMISSIONS.USER_MANAGE)
          ? apiRequest<BackendUserBranchRole[]>(path(`/user-branch-roles?branchId=${session.user.activeBranch.id}`), { cache: "no-store" }).then((value) => value.data)
          : [],
        has(PERMISSIONS.USER_MANAGE)
          ? apiRequest<BackendRole[]>("/roles", { cache: "no-store" }).then((value) => value.data)
          : [],
        has(PERMISSIONS.SETTINGS_MANAGE)
          ? apiRequest<BackendSetting[]>(path(`/system-settings?pharmacyId=${session.user.pharmacyId}`), { cache: "no-store" }).then((value) => value.data)
          : canReadOperationalSettings
            ? this.loadOperationalSettings()
            : [],
        has(PERMISSIONS.SETTINGS_MANAGE)
          ? apiRequest<BackendBranch>(path(`/branches/${session.user.activeBranch.id}`), { cache: "no-store" }).then((value) => value.data)
          : null,
        has(PERMISSIONS.SETTINGS_MANAGE)
          ? apiRequest<BackendPharmacy>(path(`/pharmacies/${session.user.pharmacyId}`), { cache: "no-store" }).then((value) => value.data)
          : null,
      ]);

      this.roleIds = new Map(
        roles
          .filter((role) => isRole(role.roleName))
          .map((role) => [role.roleName as TenantRole, role.id]),
      );

      const taxCategories = taxes.map((tax) => ({
        active: tax.active,
        code: taxCode(tax),
        id: tax.id,
        name: tax.taxName,
        rate: amount(tax.taxRate),
      }));
      const mappedMedicines = medicines.map((medicine) =>
        this.mapMedicine(medicine, taxCategories),
      );
      const mappedBatches = canReadInventory
        ? this.mapBatches(backendBatches, stocks, goodsReceipts)
        : this.mapQuickItemBatches(quickItems);
      const mappedSales = backendSales.map((sale) => this.mapSale(sale));
      const mappedShifts = shifts.map((shift) => this.mapShift(shift));
      const activeShift = mappedShifts.find(
        (shift) =>
          shift.status === "OPEN" &&
          shifts.find((candidate) => candidate.id === shift.id)?.userId === session.user.id,
      );

      this.activeBranch = activeBranch;
      this.activePharmacy = activePharmacy;
      this.settingsByKey = new Map();
      for (const setting of settings.filter((candidate) => candidate.branchId == null)) {
        this.settingsByKey.set(setting.settingKey, setting);
      }
      for (const setting of settings.filter(
        (candidate) => candidate.branchId === session.user.activeBranch.id,
      )) {
        this.settingsByKey.set(setting.settingKey, setting);
      }
      const settingValue = (key: string) => this.settingsByKey.get(key)?.settingValue;
      const currentSettings = useWorkspaceStore.getState().settings;
      useWorkspaceStore.setState({
        batches: mappedBatches,
        categories: categories.map((category) => ({ id: category.id, name: category.categoryName })),
        completedCheckoutKeys: {},
        currentShiftId: activeShift?.id ?? null,
        goodsReceipts: goodsReceipts.map((receipt) => this.mapGoodsReceipt(receipt)),
        loadError: null,
        loadStatus: "ready",
        manufacturers: manufacturers.map((manufacturer) => ({
          country: manufacturer.manufacturerCountry ?? "",
          id: manufacturer.id,
          name: manufacturer.manufacturerName,
        })),
        medicines: mappedMedicines,
        movements: movements.map((movement) => ({
          actor: movement.userName ?? "System",
          batchId: movement.medicineBatchesId,
          id: movement.id,
          medicineId: movement.medicineId,
          occurredAt: movement.movementDate ?? movement.createdAt,
          quantityDelta: movement.quantity,
          reference: movement.referenceId ?? movement.referenceType ?? "Stock movement",
          type: movementType(movement.movementType),
        })),
        sales: mappedSales,
        settings: {
          ...currentSettings,
          branchName: activeBranch?.branchName ?? session.user.activeBranch.name,
          lowStockThresholdDays: settingNumber(
            settingValue("inventory.low_stock_threshold"),
            currentSettings.lowStockThresholdDays,
          ),
          nearExpiryDays: settingNumber(
            settingValue("inventory.expiry_alert_days"),
            currentSettings.nearExpiryDays,
          ),
          pharmacyName: activePharmacy?.name ?? session.user.pharmacyName,
          phone: activeBranch?.phoneNumber ?? currentSettings.phone,
          receiptFooter:
            settingValue("receipt.footer_text") ?? currentSettings.receiptFooter,
          receiptPaperWidth:
            settingValue("receipt.paper_width") === "58MM" ? "58MM" : "80MM",
          receiptPrefix: (
            settingValue("invoice.prefix") ?? currentSettings.receiptPrefix
          ).replace(/-+$/, ""),
        },
        shifts: mappedShifts,
        staff: users.map((user) => this.mapStaff(user, assignments)),
        suppliers: suppliers.map((supplier) => this.mapSupplier(supplier)),
        taxCategories,
        units: units.map((unit) => ({
          id: unit.id,
          name: unit.unitName,
          symbol: unit.unitAbbreviation,
        })),
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : "Workspace data could not be loaded.";
      useWorkspaceStore.setState({ loadError: message, loadStatus: "error" });
      throw error;
    }
  }

  async openShift(openingFloat: string) {
    const response = await apiRequest<BackendShift>("/shifts", {
      body: {
        openingFloat: Number(openingFloat),
        remarks: "Drawer counted",
        shiftName: "Till shift",
      },
      method: "POST",
    });
    await this.hydrate();
    return response.data.id;
  }

  async receiveStock(input: ReceiveStockInput) {
    const response = await apiRequest<BackendGoodsReceipt>("/goods-received", {
      body: {
        lines: [
          {
            batchNumber: input.batchNumber.trim(),
            expiryDate: input.expiryDate,
            medicineId: input.medicineId,
            purchaseOrderLineId: null,
            quantity: input.quantity,
            unitCost: Number(input.unitCost),
          },
        ],
        purchaseOrdersId: null,
        receivedAt: localDateTime(),
        remarks: input.remarks?.trim() || null,
        supplierId: input.supplierId,
        supplierInvoiceNumber: input.supplierInvoiceNumber?.trim() || null,
      },
      idempotencyKey: input.idempotencyKey,
      method: "POST",
    });
    await this.hydrate();
    return `GRN-${response.data.id.slice(0, 8).toUpperCase()}`;
  }

  async resetWorkspace() {
    await this.hydrate();
  }

  async returnSaleItem(input: ReturnInput) {
    const sale = useWorkspaceStore
      .getState()
      .sales.find((candidate) => candidate.id === input.saleId);
    const item = sale?.items.find((candidate) => candidate.id === input.saleItemId);
    if (!sale || !item || item.allocations.length === 0) {
      throw new WorkspaceError(
        "RESOURCE_NOT_FOUND",
        "The selected sale allocation could not be found.",
      );
    }
    let previouslyReturned = item.returnedQuantity;
    let remaining = input.quantity;
    const returnLines = [];
    for (const allocation of item.allocations) {
      const consumed = Math.min(previouslyReturned, allocation.quantity);
      previouslyReturned -= consumed;
      const available = allocation.quantity - consumed;
      const quantity = Math.min(remaining, available);
      if (quantity > 0) {
        returnLines.push({
          medicineBatchesId: allocation.batchId,
          quantity,
          saleItemId: allocation.saleItemId ?? item.id,
        });
        remaining -= quantity;
      }
      if (remaining === 0) break;
    }
    if (remaining > 0) {
      throw new WorkspaceError(
        "RETURN_QUANTITY_EXCEEDED",
        "The requested quantity is greater than the quantity available to return.",
      );
    }
    const method = input.refundMethod ?? sale.payments[0]?.method ?? "CASH";
    await apiRequest<BackendSaleReturn>("/sale-returns", {
      body: {
        clientReturnId: input.idempotencyKey,
        items: returnLines,
        reason: input.reason.trim(),
        refundMethod: method === "MPESA" ? "MPESA_MANUAL" : "CASH",
        refundReference:
          method === "MPESA" ? input.refundReference?.trim() || null : null,
        saleId: input.saleId,
      },
      idempotencyKey: input.idempotencyKey,
      method: "POST",
    });
    await this.hydrate();
  }

  async setMedicineStatus(id: string, status: MedicineInput["status"]) {
    const medicine = this.requireMedicine(id);
    await this.updateMedicine(id, {
      ...medicine,
      status,
    });
  }

  async setStaffStatus(id: string, status: StaffUser["status"]) {
    await apiRequest<BackendUser>(path(`/users/${id}/status`), {
      body: { status: status === "ACTIVE" ? "ACTIVE" : "INACTIVE" },
      method: "PATCH",
    });
    await this.hydrate();
  }

  async setSupplierStatus(id: string, status: Supplier["status"]) {
    const supplier = this.requireSupplier(id);
    await apiRequest<BackendSupplier>(path(`/suppliers/${id}`), {
      body: this.supplierPayload(supplier, status),
      method: "PUT",
    });
    await this.hydrate();
  }

  subscribe(listener: () => void) {
    return useWorkspaceStore.subscribe(() => listener());
  }

  async updateMedicine(id: string, input: MedicineInput) {
    await apiRequest<BackendMedicine>(path(`/medicines/${id}`), {
      body: this.medicinePayload(input),
      method: "PUT",
    });
    await this.hydrate();
  }

  async updateStaff(id: string, input: StaffInput) {
    const session = this.requireSession();
    const names = input.displayName.trim().split(/\s+/);
    const firstName = names.shift() ?? "Staff";
    const lastName = names.join(" ") || "User";
    await apiRequest<BackendUser>(path(`/users/${id}`), {
      body: {
        branchId: session.user.activeBranch.id,
        email: input.username.trim(),
        firstName,
        lastName,
        phoneNumber: input.phoneNumber.trim(),
      },
      method: "PUT",
    });
    await this.assignRoles(id, input.roles);
    await this.hydrate();
  }

  async updateSettings(settings: PharmacySettings) {
    const session = this.requireSession();
    if (!this.activeBranch || !this.activePharmacy) {
      throw new WorkspaceError(
        "SETTINGS_NOT_READY",
        "Reload the workspace before changing pharmacy settings.",
      );
    }

    if (settings.pharmacyName !== this.activePharmacy.name) {
      this.activePharmacy = (
        await apiRequest<BackendPharmacy>(path(`/pharmacies/${this.activePharmacy.id}`), {
          body: {
            address: this.activePharmacy.address,
            email: this.activePharmacy.email,
            kraPin: this.activePharmacy.kraPin,
            licenseNumber: this.activePharmacy.licenseNumber,
            name: settings.pharmacyName.trim(),
            phoneNumber: this.activePharmacy.phoneNumber,
          },
          method: "PUT",
        })
      ).data;
    }

    if (
      settings.branchName !== this.activeBranch.branchName ||
      settings.phone !== this.activeBranch.phoneNumber
    ) {
      this.activeBranch = (
        await apiRequest<BackendBranch>(path(`/branches/${this.activeBranch.id}`), {
          body: {
            branchCode: this.activeBranch.branchCode,
            branchName: settings.branchName.trim(),
            email: this.activeBranch.email,
            location: this.activeBranch.location,
            pharmacyId: session.user.pharmacyId,
            phoneNumber: settings.phone.trim(),
            status: this.activeBranch.status,
          },
          method: "PUT",
        })
      ).data;
    }

    await Promise.all([
      this.upsertSetting("invoice.prefix", settings.receiptPrefix, "Receipt number prefix"),
      this.upsertSetting("receipt.footer_text", settings.receiptFooter, "Receipt footer"),
      this.upsertSetting("receipt.paper_width", settings.receiptPaperWidth, "Receipt paper width"),
      this.upsertSetting(
        "inventory.low_stock_threshold",
        String(settings.lowStockThresholdDays),
        "Low-stock threshold",
      ),
      this.upsertSetting(
        "inventory.expiry_alert_days",
        String(settings.nearExpiryDays),
        "Near-expiry alert window",
      ),
    ]);

    useAuthStore.setState({
      session: {
        ...session,
        user: {
          ...session.user,
          activeBranch: {
            ...session.user.activeBranch,
            name: this.activeBranch.branchName,
          },
          pharmacyName: this.activePharmacy.name,
        },
      },
    });
    useWorkspaceStore.setState({ settings });
  }

  async updateSupplier(id: string, input: SupplierInput) {
    const current = this.requireSupplier(id);
    await apiRequest<BackendSupplier>(path(`/suppliers/${id}`), {
      body: this.supplierPayload(input, current.status),
      method: "PUT",
    });
    await this.hydrate();
  }

  async lookupPos(query: string): Promise<PosLookupItem[]> {
    const normalized = query.trim();
    if (!normalized) return [];
    const response = await apiRequest<BackendPosLookupItem[]>(
      path(`/pos/lookup?name=${encodeURIComponent(normalized)}`),
      { cache: "no-store" },
    );
    const medicines = useWorkspaceStore.getState().medicines;
    return response.data.map((item) => {
      const medicine = medicines.find((candidate) => candidate.id === item.id);
      return {
        barcode: item.barcode ?? medicine?.barcode ?? "",
        brandName: item.brandName,
        categoryId: item.categoryId ?? medicine?.categoryId ?? "",
        controlledDrug: item.isControlledDrug,
        genericName: [item.genericName, item.strength].filter(Boolean).join(" "),
        id: item.id,
        prescriptionRequired: item.requiresPrescription,
        sellingPrice: amount(item.sellingPrice),
        sku: item.sku ?? medicine?.sku ?? "",
        stockAvailable: item.stockAvailable,
      };
    });
  }

  private async loadOperationalSettings() {
    const session = this.requireSession();
    const keys = ["inventory.low_stock_threshold", "inventory.expiry_alert_days"];
    const settings = await Promise.all(
      keys.map((key) =>
        apiRequest<BackendSetting | null>(
          path(
            `/system-settings/resolve?key=${encodeURIComponent(key)}` +
              `&branchId=${session.user.activeBranch.id}` +
              `&pharmacyId=${session.user.pharmacyId}`,
          ),
          { cache: "no-store" },
        ).then((response) => response.data),
      ),
    );
    return settings.filter((setting): setting is BackendSetting => setting !== null);
  }

  private async assignRoles(userId: string, roles: StaffRole[]) {
    const session = this.requireSession();
    if (this.roleIds.size === 0) {
      const backendRoles = await apiRequest<BackendRole[]>("/roles", { cache: "no-store" });
      this.roleIds = new Map(
        backendRoles.data
          .filter((role) => isRole(role.roleName))
          .map((role) => [role.roleName as TenantRole, role.id]),
      );
    }
    const current = await apiRequest<BackendUserBranchRole[]>(
      path(`/user-branch-roles?userId=${userId}&branchId=${session.user.activeBranch.id}`),
      { cache: "no-store" },
    );
    const desired = new Set(roles);
    for (const assignment of current.data) {
      if (isRole(assignment.roleName) && !desired.has(assignment.roleName)) {
        await apiRequest<void>(path(`/user-branch-roles/${assignment.id}`), {
          method: "DELETE",
        });
      }
    }
    const assigned = new Set(
      current.data.map((assignment) => assignment.roleName).filter(isRole),
    );
    for (const role of roles) {
      if (assigned.has(role)) continue;
      const roleId = this.roleIds.get(role);
      if (!roleId) {
        throw new WorkspaceError("ROLE_NOT_FOUND", `The ${role} role is not configured.`);
      }
      await apiRequest<BackendUserBranchRole>("/user-branch-roles", {
        body: {
          branchId: session.user.activeBranch.id,
          roleId,
          userId,
        },
        method: "POST",
      });
    }
  }

  private mapBatches(
    batches: BackendBatch[],
    stocks: BackendStock[],
    goodsReceipts: BackendGoodsReceipt[],
  ): Batch[] {
    const stockByBatch = new Map(stocks.map((stock) => [stock.medicineBatchesId, stock]));
    const supplierByBatch = new Map<string, string>();
    for (const receipt of goodsReceipts) {
      for (const line of receipt.lines ?? []) {
        if (line.batchId && !supplierByBatch.has(line.batchId)) {
          supplierByBatch.set(line.batchId, receipt.supplierId);
        }
      }
    }
    return batches.map((batch) => ({
      batchNumber: batch.batchNumber,
      expiryDate: batch.expirationDate,
      id: batch.id,
      medicineId: batch.medicineId,
      quantity: stockByBatch.get(batch.id)?.quantityAvailable ?? 0,
      receivedAt: batch.createdAt,
      supplierId: supplierByBatch.get(batch.id) ?? "",
      unitCost: amount(batch.buyingPrice),
    }));
  }

  private mapGoodsReceipt(receipt: BackendGoodsReceipt) {
    const total = receipt.lines.reduce(
      (sum, line) => sum + Number(line.unitCost) * line.quantity,
      0,
    );
    return {
      id: receipt.id,
      itemCount: receipt.lines.length,
      number:
        receipt.supplierInvoiceNumber?.trim() ||
        `GRN-${receipt.id.slice(0, 8).toUpperCase()}`,
      receivedAt: receipt.receivedAt ?? receipt.createdAt,
      supplierId: receipt.supplierId,
      totalCost: amount(total),
    };
  }

  private mapMedicine(medicine: BackendMedicine, taxes: TaxCategory[]): Medicine {
    const tax = taxes.find((candidate) => candidate.id === medicine.taxId);
    return {
      barcode: medicine.barcode ?? "",
      brandName: medicine.brandName ?? "Unnamed medicine",
      buyingPrice: amount(medicine.buyingPrice),
      categoryId: medicine.medicineCategoriesId ?? "",
      createdAt: medicine.createdAt,
      genericName: [medicine.genericName, medicine.strength].filter(Boolean).join(" "),
      id: medicine.id,
      manufacturer: medicine.manufacturerName ?? "Unknown manufacturer",
      prescriptionRequired: medicine.requiresPrescription,
      reorderLevel: medicine.reorderLevel ?? 0,
      sellingPrice: amount(medicine.sellingPrice),
      sku: medicine.sku ?? "",
      status: medicine.status === "AVAILABLE" ? "ACTIVE" : "INACTIVE",
      taxCategory: tax?.code ?? "EXEMPT",
      unitId: medicine.unitId ?? "",
    };
  }

  private mapQuickItemBatches(items: PosQuickItem[]): Batch[] {
    return items.map((item) => ({
      batchNumber: item.batchNumber,
      expiryDate: item.expirationDate,
      id: item.batchId,
      medicineId: item.medicineId,
      quantity: item.available,
      receivedAt: new Date().toISOString(),
      supplierId: "",
      unitCost: "0.00",
    }));
  }

  private mapSale(sale: BackendSale): Sale {
    const id = sale.id ?? sale.saleId;
    const mappedItems = (sale.items ?? []).map((item) => {
      const itemId = item.id ?? item.allocations?.[0]?.saleItemId;
      return {
        allocations: (item.allocations ?? []).map((allocation) => ({
          batchId: allocation.batchId,
          batchNumber: allocation.batchNumber,
          quantity: allocation.quantity,
          saleItemId: allocation.saleItemId,
        })),
        id: itemId,
        lineTotal: amount(item.lineTotal ?? item.total),
        medicineId: item.medicineId,
        medicineName: item.medicineName,
        quantity: item.quantity,
        returnedQuantity: item.returnedQuantity ?? 0,
        unitPrice: amount(item.unitPrice ?? item.price),
      };
    });
    const soldQuantity = mappedItems.reduce((sum, item) => sum + item.quantity, 0);
    const returnedQuantity = mappedItems.reduce(
      (sum, item) => sum + item.returnedQuantity,
      0,
    );
    const status =
      returnedQuantity === 0
        ? saleStatus(sale.saleStatus || sale.status)
        : returnedQuantity >= soldQuantity
          ? "RETURNED"
          : "PARTIALLY_RETURNED";
    return {
      cashierName: sale.userName ?? "Pharmacy user",
      completedAt: sale.completedAt ?? sale.createdAt,
      id,
      idempotencyKey: id,
      items: mappedItems,
      payments: (sale.payments ?? []).map((payment) => ({
        amount: amount(payment.amount),
        method: paymentMethod(payment.paymentMethod),
        reference: payment.transactionReference ?? null,
      })),
      receiptNumber:
        sale.receipt?.receiptNumber ?? sale.invoiceNumber ?? sale.saleNumber ?? id.slice(0, 8),
      refundTotal: amount(sale.refundTotal),
      cashTendered:
        sale.cashTendered == null ? null : amount(sale.cashTendered),
      changeDue: sale.changeDue == null ? null : amount(sale.changeDue),
      shiftId: sale.shiftId,
      status,
      subtotal: amount(sale.subtotal),
      taxTotal: amount(sale.taxTotal ?? sale.tax),
      total: amount(sale.total),
    };
  }

  private mapShift(shift: BackendShift): Shift {
    return {
      actualCash: shift.actualCash == null ? null : amount(shift.actualCash),
      cashRefunds: amount(shift.cashRefunds),
      cashSales: amount(shift.cashSales),
      closedAt: shift.shiftEndTime,
      expectedCash: amount(shift.expectedCash ?? shift.openingFloat),
      id: shift.id,
      mpesaSales: amount(shift.mpesaSales),
      openedAt: shift.shiftStartTime ?? shift.createdAt,
      openedBy: shift.userName,
      openingFloat: amount(shift.openingFloat),
      status: shift.status === "ACTIVE" ? "OPEN" : "CLOSED",
      variance: shift.variance == null ? null : amount(shift.variance),
    };
  }

  private mapStaff(user: BackendUser, assignments: BackendUserBranchRole[]): StaffUser {
    const roles = assignments
      .filter((assignment) => assignment.userId === user.id && isRole(assignment.roleName))
      .map((assignment) => assignment.roleName as TenantRole);
    return {
      displayName: [user.firstName, user.lastName].filter(Boolean).join(" "),
      id: user.id,
      jobTitle: roles.map((role) => role.replaceAll("_", " ").toLowerCase()).join(", "),
      phoneNumber: user.phoneNumber,
      roles,
      status: user.status === "ACTIVE" ? "ACTIVE" : "DISABLED",
      username: user.email,
    };
  }

  private mapSupplier(supplier: BackendSupplier): Supplier {
    return {
      createdAt: supplier.createdAt,
      email: supplier.email ?? "",
      id: supplier.id,
      name: supplier.supplierName,
      phone: supplier.phoneNumber ?? "",
      status: supplier.status === "INACTIVE" ? "INACTIVE" : "ACTIVE",
    };
  }

  private medicinePayload(input: MedicineInput) {
    const state = useWorkspaceStore.getState();
    const manufacturer = state.manufacturers.find(
      (candidate) => candidate.name.toLowerCase() === input.manufacturer.trim().toLowerCase(),
    );
    const tax = state.taxCategories.find((candidate) => candidate.code === input.taxCategory);
    if (!manufacturer || !tax) {
      throw new WorkspaceError(
        "REFERENCE_DATA_MISSING",
        "Choose a configured manufacturer and tax category.",
      );
    }
    return {
      barcode: input.barcode.trim() || null,
      brandName: input.brandName.trim(),
      buyingPrice: Number(input.buyingPrice),
      genericName: input.genericName.trim(),
      isControlledDrug: false,
      manufacturerId: manufacturer.id,
      medicineCategoriesId: input.categoryId,
      reorderLevel: input.reorderLevel,
      requiresPrescription: input.prescriptionRequired,
      requiresRefrigeration: false,
      sellingPrice: Number(input.sellingPrice),
      sku: input.sku.trim(),
      status: input.status === "ACTIVE" ? "AVAILABLE" : "NOT_AVAILABLE",
      taxId: tax.id,
      trackBatch: true,
      trackExpiry: true,
      trackSerialNumber: false,
      unitId: input.unitId,
    };
  }

  private requireMedicine(id: string) {
    const medicine = useWorkspaceStore.getState().medicines.find((candidate) => candidate.id === id);
    if (!medicine) {
      throw new WorkspaceError("RESOURCE_NOT_FOUND", "The medicine could not be found.");
    }
    return medicine;
  }

  private requireSession() {
    const session = useAuthStore.getState().session;
    if (!session) throw new WorkspaceError("UNAUTHENTICATED", "Sign in to continue.");
    return session;
  }

  private requireSupplier(id: string) {
    const supplier = useWorkspaceStore.getState().suppliers.find((candidate) => candidate.id === id);
    if (!supplier) {
      throw new WorkspaceError("RESOURCE_NOT_FOUND", "The supplier could not be found.");
    }
    return supplier;
  }

  private supplierPayload(input: SupplierInput, status: Supplier["status"]) {
    return {
      address: null,
      contactPerson: null,
      email: input.email.trim() || null,
      licenseNumber: null,
      paymentTerms: null,
      phoneNumber: input.phone.trim() || null,
      status,
      supplierName: input.name.trim(),
    };
  }

  private async upsertSetting(key: string, value: string, description: string) {
    const session = this.requireSession();
    const existing = this.settingsByKey.get(key);
    const branchSetting =
      existing?.branchId === session.user.activeBranch.id ? existing : null;
    const response = await apiRequest<BackendSetting>(
      branchSetting ? path(`/system-settings/${branchSetting.id}`) : "/system-settings",
      {
        body: {
          branchId: session.user.activeBranch.id,
          description,
          pharmacyId: session.user.pharmacyId,
          settingKey: key,
          settingValue: value,
        },
        method: branchSetting ? "PUT" : "POST",
      },
    );
    this.settingsByKey.set(key, response.data);
  }
}

export function createLiveWorkspaceGateway() {
  return new LiveWorkspaceGateway();
}
