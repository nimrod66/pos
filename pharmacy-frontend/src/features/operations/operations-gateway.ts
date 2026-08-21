import { apiRequest } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type ApiPath = `/${string}`;

interface BackendPage<T> {
  content: T[];
  totalPages: number;
}

export interface Customer {
  id: string;
  pharmacyId: string;
  firstName: string;
  lastName: string | null;
  phoneNumber: string | null;
  email: string | null;
  address: string | null;
  loyaltyPoints: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export type CustomerInput = Pick<
  Customer,
  "firstName" | "lastName" | "phoneNumber" | "email" | "address" | "notes"
>;

export interface PurchaseOrderItem {
  id: string;
  medicineId: string;
  medicineName: string;
  quantity: number;
  buyingPrice: number;
  discount: number;
  tax: number;
  total: number;
}

export interface PurchaseOrder {
  id: string;
  supplierId: string;
  supplierName: string;
  branchId: string;
  branchName: string;
  orderedById: string;
  orderedByName: string;
  approvedById: string | null;
  status: "ORDERED" | "IN_PROGRESS" | "DELIVERED" | "FAILED";
  orderDate: string;
  expectedDeliveryDate: string | null;
  deliveryDate: string | null;
  items: PurchaseOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseOrderInput {
  supplierId: string;
  branchId: string;
  orderedById: string;
  expectedDeliveryDate: string | null;
  items: Array<{
    medicineId: string;
    quantity: number;
    buyingPrice: number;
    discount: number;
    tax: number;
  }>;
}

export interface PurchaseOrderReceipt {
  id: string;
  purchaseOrderId: string;
  supplierInvoiceNumber: string | null;
  receivedAt: string;
  lines: Array<{
    id: string;
    purchaseOrderLineId: string | null;
    medicineId: string;
    medicineName: string;
    batchNumber: string;
    expiryDate: string | null;
    quantity: number;
    unitCost: number;
  }>;
}

export interface ReceivePurchaseOrderInput {
  supplierId: string;
  purchaseOrdersId: string;
  supplierInvoiceNumber: string | null;
  remarks: string | null;
  lines: Array<{
    medicineId: string;
    purchaseOrderLineId: string;
    batchNumber: string;
    expiryDate: string;
    quantity: number;
    unitCost: number;
  }>;
}

export interface AuditLogEntry {
  id: string;
  userId: string | null;
  pharmacyId: string;
  branchId: string | null;
  userName: string | null;
  tableName: string;
  recordId: string | null;
  action: string;
  createdAt: string;
}

export interface AuditLogFilters {
  tableName?: string;
  recordId?: string;
  userId?: string;
}

interface OperationsGateway {
  approvePurchaseOrder(id: string, userId: string): Promise<PurchaseOrder>;
  createCustomer(input: CustomerInput): Promise<Customer>;
  createPurchaseOrder(input: PurchaseOrderInput): Promise<PurchaseOrder>;
  deleteCustomer(id: string): Promise<void>;
  getPurchaseOrder(id: string): Promise<PurchaseOrder>;
  listAuditLogs(filters?: AuditLogFilters): Promise<AuditLogEntry[]>;
  listCustomers(query?: string): Promise<Customer[]>;
  listPurchaseOrderReceipts(id: string): Promise<PurchaseOrderReceipt[]>;
  listPurchaseOrders(branchId: string): Promise<PurchaseOrder[]>;
  receivePurchaseOrder(
    input: ReceivePurchaseOrderInput,
    idempotencyKey: string,
  ): Promise<PurchaseOrderReceipt>;
  updateCustomer(id: string, input: CustomerInput): Promise<Customer>;
}

function path(value: string) {
  return value as ApiPath;
}

async function getAllPages<T>(endpoint: string) {
  const separator = endpoint.includes("?") ? "&" : "?";
  const sizedEndpoint = `${endpoint}${separator}size=100&sort=createdAt,desc`;
  const first = await apiRequest<BackendPage<T>>(path(sizedEndpoint), {
    cache: "no-store",
  });
  const rows = [...first.data.content];
  for (let page = 1; page < first.data.totalPages; page += 1) {
    const response = await apiRequest<BackendPage<T>>(
      path(`${sizedEndpoint}&page=${page}`),
      { cache: "no-store" },
    );
    rows.push(...response.data.content);
  }
  return rows;
}

class LiveOperationsGateway implements OperationsGateway {
  async approvePurchaseOrder(id: string, userId: string) {
    const response = await apiRequest<PurchaseOrder>(
      path(`/purchase-orders/${id}/approve?userId=${encodeURIComponent(userId)}`),
      { method: "PATCH" },
    );
    return response.data;
  }

  async createCustomer(input: CustomerInput) {
    const response = await apiRequest<Customer>("/customers", {
      body: input,
      method: "POST",
    });
    return response.data;
  }

  async createPurchaseOrder(input: PurchaseOrderInput) {
    const response = await apiRequest<PurchaseOrder>("/purchase-orders", {
      body: input,
      method: "POST",
    });
    return response.data;
  }

  async deleteCustomer(id: string) {
    await apiRequest(path(`/customers/${id}`), { method: "DELETE" });
  }

  async getPurchaseOrder(id: string) {
    const response = await apiRequest<PurchaseOrder>(path(`/purchase-orders/${id}`), {
      cache: "no-store",
    });
    return response.data;
  }

  async listAuditLogs(filters: AuditLogFilters = {}) {
    const params = new URLSearchParams();
    if (filters.tableName) params.set("tableName", filters.tableName);
    if (filters.recordId) params.set("recordId", filters.recordId);
    if (filters.userId) params.set("userId", filters.userId);
    const query = params.size ? `?${params.toString()}` : "";
    return getAllPages<AuditLogEntry>(`/audit-logs${query}`);
  }

  async listCustomers(query = "") {
    const normalized = query.trim();
    return getAllPages<Customer>(
      normalized
        ? `/customers/search?q=${encodeURIComponent(normalized)}`
        : "/customers",
    );
  }

  async listPurchaseOrderReceipts(id: string) {
    return getAllPages<PurchaseOrderReceipt>(
      `/goods-received?poId=${encodeURIComponent(id)}`,
    );
  }

  async listPurchaseOrders(branchId: string) {
    return getAllPages<PurchaseOrder>(
      `/purchase-orders?branchId=${encodeURIComponent(branchId)}`,
    );
  }

  async receivePurchaseOrder(
    input: ReceivePurchaseOrderInput,
    idempotencyKey: string,
  ) {
    const response = await apiRequest<PurchaseOrderReceipt>("/goods-received", {
      body: input,
      idempotencyKey,
      method: "POST",
    });
    return response.data;
  }

  async updateCustomer(id: string, input: CustomerInput) {
    const response = await apiRequest<Customer>(path(`/customers/${id}`), {
      body: input,
      method: "PUT",
    });
    return response.data;
  }
}

interface PreviewOperationsState {
  auditLogs: AuditLogEntry[];
  customers: Customer[];
  purchaseOrders: PurchaseOrder[];
  receipts: PurchaseOrderReceipt[];
}

const PREVIEW_KEY = "pharmacy-pos:operations-preview";

function previewSeed(): PreviewOperationsState {
  const createdAt = new Date().toISOString();
  return {
    auditLogs: [
      {
        action: "LOGIN_SUCCESS",
        branchId: "preview-main",
        createdAt,
        id: "audit-preview-1",
        pharmacyId: "preview-pharmacy",
        recordId: null,
        tableName: "Authentication",
        userId: "owner-preview",
        userName: "Pharmacy Owner",
      },
    ],
    customers: [
      {
        address: "Nairobi",
        createdAt,
        email: "jane@example.com",
        firstName: "Jane",
        id: "customer-preview-1",
        lastName: "Wanjiku",
        loyaltyPoints: 40,
        notes: null,
        pharmacyId: "preview-pharmacy",
        phoneNumber: "0712345678",
        updatedAt: createdAt,
      },
    ],
    purchaseOrders: [],
    receipts: [],
  };
}

function loadPreviewState() {
  if (typeof window === "undefined") return previewSeed();
  const stored = window.localStorage.getItem(PREVIEW_KEY);
  if (!stored) return previewSeed();
  try {
    return JSON.parse(stored) as PreviewOperationsState;
  } catch {
    return previewSeed();
  }
}

function savePreviewState(state: PreviewOperationsState) {
  window.localStorage.setItem(PREVIEW_KEY, JSON.stringify(state));
}

function appendPreviewAudit(
  state: PreviewOperationsState,
  tableName: string,
  recordId: string,
  action: string,
) {
  state.auditLogs.unshift({
    action,
    branchId: "preview-main",
    createdAt: new Date().toISOString(),
    id: crypto.randomUUID(),
    pharmacyId: "preview-pharmacy",
    recordId,
    tableName,
    userId: "owner-preview",
    userName: "Pharmacy Owner",
  });
}

class PreviewOperationsGateway implements OperationsGateway {
  async approvePurchaseOrder(id: string, userId: string) {
    const state = loadPreviewState();
    const order = state.purchaseOrders.find((candidate) => candidate.id === id);
    if (!order) throw new Error("Purchase order not found.");
    order.status = "IN_PROGRESS";
    order.approvedById = userId;
    order.updatedAt = new Date().toISOString();
    appendPreviewAudit(state, "PurchaseOrder", id, "APPROVE_PURCHASE_ORDER");
    savePreviewState(state);
    return order;
  }

  async createCustomer(input: CustomerInput) {
    const state = loadPreviewState();
    const now = new Date().toISOString();
    const customer: Customer = {
      ...input,
      createdAt: now,
      id: crypto.randomUUID(),
      loyaltyPoints: 0,
      pharmacyId: "preview-pharmacy",
      updatedAt: now,
    };
    state.customers.unshift(customer);
    appendPreviewAudit(state, "Customer", customer.id, "CREATE_CUSTOMER");
    savePreviewState(state);
    return customer;
  }

  async createPurchaseOrder(input: PurchaseOrderInput) {
    const state = loadPreviewState();
    const now = new Date().toISOString();
    const order: PurchaseOrder = {
      approvedById: null,
      branchId: input.branchId,
      branchName: "Main branch",
      createdAt: now,
      deliveryDate: null,
      expectedDeliveryDate: input.expectedDeliveryDate,
      id: crypto.randomUUID(),
      items: input.items.map((item) => ({
        ...item,
        id: crypto.randomUUID(),
        medicineName: "Medicine",
        total:
          item.buyingPrice * item.quantity - item.discount + item.tax,
      })),
      orderDate: now,
      orderedById: input.orderedById,
      orderedByName: "Pharmacy Owner",
      status: "ORDERED",
      supplierId: input.supplierId,
      supplierName: "Supplier",
      updatedAt: now,
    };
    state.purchaseOrders.unshift(order);
    appendPreviewAudit(state, "PurchaseOrder", order.id, "CREATE_PURCHASE_ORDER");
    savePreviewState(state);
    return order;
  }

  async deleteCustomer(id: string) {
    const state = loadPreviewState();
    const index = state.customers.findIndex((customer) => customer.id === id);
    if (index < 0) throw new Error("Customer not found.");
    state.customers.splice(index, 1);
    appendPreviewAudit(state, "Customer", id, "DELETE_CUSTOMER");
    savePreviewState(state);
  }

  async getPurchaseOrder(id: string) {
    const order = loadPreviewState().purchaseOrders.find(
      (candidate) => candidate.id === id,
    );
    if (!order) throw new Error("Purchase order not found.");
    return order;
  }

  async listAuditLogs(filters: AuditLogFilters = {}) {
    return loadPreviewState().auditLogs.filter(
      (entry) =>
        (!filters.tableName || entry.tableName === filters.tableName) &&
        (!filters.recordId || entry.recordId === filters.recordId) &&
        (!filters.userId || entry.userId === filters.userId),
    );
  }

  async listCustomers(query = "") {
    const normalized = query.trim().toLowerCase();
    return loadPreviewState().customers.filter((customer) =>
      !normalized
        ? true
        : [
            customer.firstName,
            customer.lastName,
            customer.phoneNumber,
            customer.email,
          ].some((value) => value?.toLowerCase().includes(normalized)),
    );
  }

  async listPurchaseOrderReceipts(id: string) {
    return loadPreviewState().receipts.filter(
      (receipt) => receipt.purchaseOrderId === id,
    );
  }

  async listPurchaseOrders(branchId: string) {
    return loadPreviewState().purchaseOrders.filter(
      (order) => order.branchId === branchId,
    );
  }

  async receivePurchaseOrder(
    input: ReceivePurchaseOrderInput,
    idempotencyKey: string,
  ) {
    const state = loadPreviewState();
    const existing = state.receipts.find(
      (receipt) => receipt.id === idempotencyKey,
    );
    if (existing) return existing;
    const now = new Date().toISOString();
    const receipt: PurchaseOrderReceipt = {
      id: idempotencyKey,
      lines: input.lines.map((line) => ({
        ...line,
        id: crypto.randomUUID(),
        medicineName: "Medicine",
      })),
      purchaseOrderId: input.purchaseOrdersId,
      receivedAt: now,
      supplierInvoiceNumber: input.supplierInvoiceNumber,
    };
    state.receipts.unshift(receipt);
    const order = state.purchaseOrders.find(
      (candidate) => candidate.id === input.purchaseOrdersId,
    );
    if (order) {
      order.status = "DELIVERED";
      order.deliveryDate = now;
      order.updatedAt = now;
    }
    appendPreviewAudit(
      state,
      "GoodsReceivedNote",
      receipt.id,
      "RECEIVE_PURCHASE_ORDER",
    );
    savePreviewState(state);
    return receipt;
  }

  async updateCustomer(id: string, input: CustomerInput) {
    const state = loadPreviewState();
    const customer = state.customers.find((candidate) => candidate.id === id);
    if (!customer) throw new Error("Customer not found.");
    Object.assign(customer, input, { updatedAt: new Date().toISOString() });
    appendPreviewAudit(state, "Customer", id, "UPDATE_CUSTOMER");
    savePreviewState(state);
    return customer;
  }
}

export const operationsGateway: OperationsGateway = DEMO_AUTH_ENABLED
  ? new PreviewOperationsGateway()
  : new LiveOperationsGateway();
