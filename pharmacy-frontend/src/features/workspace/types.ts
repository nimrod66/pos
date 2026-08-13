import type { TenantRole } from "@/features/auth/access-control";

export type MedicineStatus = "ACTIVE" | "INACTIVE";
export type SupplierStatus = "ACTIVE" | "INACTIVE";
export type PaymentMethod = "CASH" | "MPESA";
export type SaleStatus =
  | "COMPLETED"
  | "PARTIALLY_RETURNED"
  | "RETURNED"
  | "CANCELLED"
  | "SUSPENDED"
  | "UNKNOWN";
export type ShiftStatus = "OPEN" | "CLOSED";
export type MovementType = "PURCHASE" | "SALE" | "SALE_RETURN" | "ADJUSTMENT";
export type StaffRole = TenantRole;

export interface Category {
  id: string;
  name: string;
}

export interface Unit {
  id: string;
  name: string;
  symbol: string;
}

export interface Manufacturer {
  id: string;
  name: string;
  country: string;
}

export interface TaxCategory {
  id: string;
  code: Medicine["taxCategory"];
  name: string;
  rate: string;
  active: boolean;
}

export interface Medicine {
  id: string;
  sku: string;
  barcode: string;
  brandName: string;
  genericName: string;
  categoryId: string;
  unitId: string;
  manufacturer: string;
  taxCategory: "EXEMPT" | "VAT_16" | "ZERO_RATED";
  prescriptionRequired: boolean;
  buyingPrice: string;
  sellingPrice: string;
  reorderLevel: number;
  status: MedicineStatus;
  createdAt: string;
}

export interface Supplier {
  id: string;
  name: string;
  phone: string;
  email: string;
  status: SupplierStatus;
  createdAt: string;
}

export interface Batch {
  id: string;
  medicineId: string;
  supplierId: string;
  batchNumber: string;
  expiryDate: string;
  quantity: number;
  unitCost: string;
  receivedAt: string;
}

export interface StockMovement {
  id: string;
  medicineId: string;
  batchId: string;
  type: MovementType;
  quantityDelta: number;
  reference: string;
  actor: string;
  occurredAt: string;
}

export interface GoodsReceipt {
  id: string;
  number: string;
  supplierId: string;
  receivedAt: string;
  totalCost: string;
  itemCount: number;
}

export interface SaleItemAllocation {
  saleItemId?: string;
  batchId: string;
  batchNumber: string;
  quantity: number;
}

export interface SaleItem {
  id: string;
  medicineId: string;
  medicineName: string;
  quantity: number;
  returnedQuantity: number;
  unitPrice: string;
  lineTotal: string;
  allocations: SaleItemAllocation[];
}

export interface SalePayment {
  method: PaymentMethod;
  amount: string;
  reference: string | null;
}

export interface Sale {
  id: string;
  receiptNumber: string;
  shiftId: string;
  completedAt: string;
  cashierName: string;
  status: SaleStatus;
  items: SaleItem[];
  payments: SalePayment[];
  subtotal: string;
  taxTotal: string;
  total: string;
  refundTotal: string;
  idempotencyKey: string;
}

export interface Shift {
  id: string;
  openedAt: string;
  closedAt: string | null;
  openingFloat: string;
  cashSales: string;
  mpesaSales: string;
  cashRefunds: string;
  expectedCash: string;
  actualCash: string | null;
  variance: string | null;
  status: ShiftStatus;
  openedBy: string;
}

export interface StaffUser {
  id: string;
  displayName: string;
  username: string;
  phoneNumber: string;
  jobTitle: string;
  roles: StaffRole[];
  status: "ACTIVE" | "DISABLED";
}

export interface StaffInput {
  displayName: string;
  username: string;
  phoneNumber: string;
  jobTitle: string;
  roles: StaffRole[];
  password?: string;
}

export interface PharmacySettings {
  pharmacyName: string;
  branchName: string;
  phone: string;
  receiptPrefix: string;
  receiptFooter: string;
  receiptPaperWidth: "58MM" | "80MM";
  currency: "KES";
  timezone: "Africa/Nairobi";
  lowStockThresholdDays: number;
  nearExpiryDays: number;
}

export interface MedicineInput {
  sku: string;
  barcode: string;
  brandName: string;
  genericName: string;
  categoryId: string;
  unitId: string;
  manufacturer: string;
  taxCategory: Medicine["taxCategory"];
  prescriptionRequired: boolean;
  buyingPrice: string;
  sellingPrice: string;
  reorderLevel: number;
  status: MedicineStatus;
}

export interface SupplierInput {
  name: string;
  phone: string;
  email: string;
}

export interface ReceiveStockInput {
  idempotencyKey: string;
  supplierId: string;
  medicineId: string;
  batchNumber: string;
  expiryDate: string;
  quantity: number;
  unitCost: string;
  supplierInvoiceNumber?: string;
  remarks?: string;
}

export interface CheckoutInput {
  idempotencyKey: string;
  cashierName: string;
  items: Array<{ lineId?: string; medicineId: string; quantity: number }>;
  paymentMethod: PaymentMethod;
  mpesaReference: string;
  pharmacistApproved: boolean;
  cashTendered?: string;
  prescriptionReferenceId?: string;
}

export interface ReturnInput {
  idempotencyKey: string;
  saleId: string;
  saleItemId: string;
  quantity: number;
  reason: string;
  resalable: boolean;
  actor: string;
  refundMethod?: PaymentMethod;
  refundReference?: string;
}

export interface PosLookupItem {
  id: string;
  sku: string;
  barcode: string;
  brandName: string;
  genericName: string;
  categoryId: string;
  prescriptionRequired: boolean;
  controlledDrug: boolean;
  stockAvailable: number;
  sellingPrice: string;
}

export interface DashboardReport {
  pharmacyWide: boolean;
  date: string;
  completedSalesCount: number;
  grossSales: string;
  refunds: string;
  netSales: string;
  lowStockCount: number;
  totalStockItems: number;
  nearExpiryCount: number;
  expiredCount: number;
  nearExpiryDays: number;
}

export interface SalesReport {
  pharmacyWide: boolean;
  from: string;
  to: string;
  completedSalesCount: number;
  grossSales: string;
  refunds: string;
  netSales: string;
  cashPayments: string;
  mpesaPayments: string;
  otherPayments: string;
  cashRefunds: string;
  mpesaRefunds: string;
  otherRefunds: string;
  topProducts: Array<{
    medicineId: string;
    medicineName: string;
    quantity: number;
    netRevenue: string;
  }>;
}

export interface InventoryReport {
  pharmacyWide: boolean;
  asOf: string;
  stockValue: string;
  lowStockCount: number;
  batchCount: number;
  nearExpiryCount: number;
  expiredCount: number;
  nearExpiryDays: number;
  lowStockItems: Array<{
    branchId: string;
    branchName: string;
    medicineId: string;
    medicineName: string;
    sku: string;
    available: number;
    reorderLevel: number;
  }>;
}
