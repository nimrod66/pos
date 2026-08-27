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
  parentUnitId: string | null;
  parentUnitName: string | null;
  conversionFactor: number | null;
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
  buyingUnitId: string | null;
  buyingUnitName: string | null;
  packSize: number | null;
  manufacturer: string;
  taxCategory: "EXEMPT" | "VAT_16" | "ZERO_RATED";
  prescriptionRequired: boolean;
  controlledDrug: boolean;
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
  cashTendered: string | null;
  changeDue: string | null;
  customerKraPin?: string | null;
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
  branchId?: string | null;
  branchName?: string | null;
}

export interface StaffInput {
  displayName: string;
  username: string;
  phoneNumber: string;
  jobTitle: string;
  roles: StaffRole[];
  password?: string;
  branchId?: string;
}

export interface PharmacySettings {
  pharmacyName: string;
  branchName: string;
  phone: string;
  kraPin: string;
  address: string;
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
  buyingUnitId: string | null;
  packSize: number | null;
  manufacturer: string;
  taxCategory: "EXEMPT" | "VAT_16" | "ZERO_RATED";
  prescriptionRequired: boolean;
  controlledDrug: boolean;
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
  customerId?: string;
  items: Array<{ lineId?: string; medicineId: string; quantity: number; discountPercent?: number }>;
  paymentMethod: PaymentMethod;
  mpesaMode: "STK" | "MANUAL";
  mpesaPhone: string;
  mpesaReference: string;
  pharmacistApproved: boolean;
  cashTendered?: string;
  prescriptionReferenceId?: string;
}

export interface PaymentCapabilities {
  mpesaStkConfigured: boolean;
  mpesaEnvironment: string;
  pollingSupported: boolean;
}

export interface ReturnInput {
  idempotencyKey: string;
  saleId: string;
  saleItemId: string;
  quantity: number;
  reason: string;
  resalable: boolean;
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
  completedSalesCount: number | null;
  grossSales: string | null;
  refunds: string | null;
  netSales: string | null;
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

export interface PluRow {
  medicineId: string;
  medicineName: string;
  sku: string;
  unitPrice: string;
  quantitySold: number;
  revenue: string;
  remainingStock: number;
}

export interface StockCountItem {
  id: string;
  medicineBatchId: string;
  batchNumber: string;
  medicineName: string;
  systemQuantity: number;
  countedQuantity: number | null;
  variance: number | null;
  status: string;
}

export interface StockCount {
  id: string;
  countNumber: string;
  branchId: string;
  branchName: string;
  status: "DRAFT" | "IN_PROGRESS" | "COMPLETED" | "RECONCILED";
  notes: string | null;
  countedById: string;
  countedByName: string;
  itemCount: number;
  varianceCount: number;
  createdAt: string;
  completedAt: string | null;
  items: StockCountItem[];
}

export interface StockCountItemInput {
  medicineBatchId: string;
  countedQuantity: number;
}

export interface StockCountInput {
  notes?: string;
  items: StockCountItemInput[];
}

export interface StockTransferItem {
  id: string;
  medicineBatchId: string;
  batchNumber: string;
  medicineName: string;
  quantity: number;
}

export interface StockTransfer {
  id: string;
  transferNumber: string;
  sourceBranchId: string;
  sourceBranchName: string;
  destBranchId: string;
  destBranchName: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "IN_TRANSIT" | "RECEIVED";
  notes: string | null;
  requestedByName: string;
  approvedByName: string | null;
  receivedByName: string | null;
  itemCount: number;
  createdAt: string;
  approvedAt: string | null;
  receivedAt: string | null;
  items: StockTransferItem[];
}

export interface StockTransferItemInput {
  medicineBatchId: string;
  quantity: number;
}

export interface StockTransferInput {
  destBranchId: string;
  notes?: string;
  items: StockTransferItemInput[];
}

export interface ProfitMedicine {
  medicineId: string;
  medicineName: string;
  sku: string;
  quantitySold: number;
  revenue: string;
  costOfGoods: string;
  grossProfit: string;
  marginPercent: number;
}

export interface ProfitReport {
  from: string;
  to: string;
  totalRevenue: string;
  totalCostOfGoods: string;
  totalGrossProfit: string;
  overallMarginPercent: number;
  medicines: ProfitMedicine[];
}

export interface SlowStockItem {
  medicineId: string;
  medicineName: string;
  sku: string;
  currentStock: number;
  lastSoldAt: string | null;
  totalSold90d: number;
  velocity: number;
  category: "DEAD" | "SLOW";
}

export interface SlowStockReport {
  asOf: string;
  totalDead: number;
  totalSlow: number;
  items: SlowStockItem[];
}

export interface ReorderItem {
  medicineId: string;
  medicineName: string;
  sku: string;
  currentStock: number;
  reorderLevel: number;
  avgWeeklySales: number;
  suggestedOrderQty: number;
  urgency: "CRITICAL" | "HIGH" | "MEDIUM";
}

export interface ReorderSuggestionReport {
  asOf: string;
  totalCritical: number;
  totalHigh: number;
  totalMedium: number;
  items: ReorderItem[];
}

export interface SupplierPriceRow {
  supplierId: string;
  supplierName: string;
  medicineId: string;
  medicineName: string;
  sku: string;
  lastCost: string;
  avgCost: string;
  totalPurchased: number;
  purchaseCount: number;
}

export interface SupplierPriceComparison {
  from: string;
  to: string;
  rows: SupplierPriceRow[];
}

export interface CustomerSaleItem {
  medicineName: string;
  quantity: number;
  unitPrice: string;
  lineTotal: string;
}

export interface CustomerSale {
  id: string;
  completedAt: string;
  total: string;
  paymentMethod: string;
  items: CustomerSaleItem[];
}

export interface CustomerPrescriptionItem {
  medicineName: string;
  dosage: string | null;
  quantity: number;
}

export interface CustomerPrescription {
  id: string;
  prescriptionNumber: string;
  doctorName: string;
  diagnosis: string | null;
  issuedDate: string;
  status: string;
  items: CustomerPrescriptionItem[];
}

export interface CustomerHistory {
  customerId: string;
  fullName: string;
  phoneNumber: string | null;
  loyaltyPoints: number;
  totalSales: number;
  totalSpent: string;
  recentSales: CustomerSale[];
  prescriptions: CustomerPrescription[];
}
