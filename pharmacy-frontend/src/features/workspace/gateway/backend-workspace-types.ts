export interface BackendPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface BackendCategory {
  id: string;
  categoryName: string;
}

export interface BackendUnit {
  id: string;
  unitName: string;
  unitAbbreviation: string;
}

export interface BackendManufacturer {
  id: string;
  manufacturerName: string;
  manufacturerCountry: string | null;
}

export interface BackendTaxCategory {
  id: string;
  code: string;
  taxName: string;
  taxRate: number;
  taxType: string;
  active: boolean;
}

export interface BackendBranch {
  id: string;
  branchName: string;
  branchCode: string;
  phoneNumber: string;
  email: string | null;
  location: string;
  status: string;
  pharmacyId: string;
}

export interface BackendPharmacy {
  id: string;
  name: string;
  address: string;
  email: string;
  phoneNumber: string;
  licenseNumber: string;
  kraPin: string;
}

export interface BackendMedicine {
  id: string;
  barcode: string | null;
  sku: string | null;
  brandName: string | null;
  genericName: string | null;
  strength: string | null;
  buyingPrice: number;
  sellingPrice: number;
  reorderLevel: number;
  status: string;
  manufacturerId: string;
  manufacturerName: string | null;
  medicineCategoriesId: string;
  categoryName: string | null;
  unitId: string;
  unitName: string | null;
  buyingUnitId: string | null;
  buyingUnitName: string | null;
  packSize: number | null;
  taxId: string;
  taxName: string | null;
  requiresPrescription: boolean;
  controlledDrug: boolean;
  createdAt: string;
}

export interface BackendSupplier {
  id: string;
  supplierName: string;
  phoneNumber: string | null;
  email: string | null;
  status: string | null;
  createdAt: string;
}

export interface BackendStock {
  id: string;
  medicineBatchesId: string;
  batchNumber: string;
  medicineId: string;
  medicineName: string;
  quantityAvailable: number;
  quantityQuarantined: number;
  reorderLevel: number;
  createdAt: string;
  updatedAt: string;
}

export interface BackendBatch {
  id: string;
  medicineId: string;
  medicineName: string;
  batchNumber: string;
  expirationDate: string;
  initialQuantity: number;
  buyingPrice: number;
  sellingPrice: number;
  createdAt: string;
}

export interface BackendStockMovement {
  id: string;
  movementType: string;
  medicineBatchesId: string;
  batchNumber: string;
  medicineId: string;
  medicineName: string;
  userName: string | null;
  referenceType: string | null;
  referenceId: string | null;
  movementDate: string;
  quantity: number;
  createdAt: string;
}

export interface BackendShift {
  id: string;
  shiftName: string;
  status: string;
  userId: string;
  userName: string;
  shiftStartTime: string;
  shiftEndTime: string | null;
  remarks: string | null;
  createdAt: string;
  openingFloat?: number;
  cashSales?: number;
  mpesaSales?: number;
  cashRefunds?: number;
  expectedCash?: number;
  actualCash?: number | null;
  variance?: number | null;
}

export interface BackendSaleAllocation {
  saleItemId: string;
  batchId: string;
  batchNumber: string;
  quantity: number;
}

export interface BackendSaleItem {
  id: string;
  lineId: string;
  medicineId: string;
  medicineName: string;
  quantity: number;
  returnedQuantity: number;
  unitPrice: number;
  price: number;
  total: number;
  lineTotal: number;
  allocations: BackendSaleAllocation[];
}

export interface BackendPayment {
  id: string;
  paymentMethod: string;
  amount: number;
  transactionReference: string | null;
  merchantRequestId: string | null;
  checkoutRequestId: string | null;
  paymentStatus: string;
}

export interface BackendPaymentGatewayResponse {
  success: boolean;
  transactionReference: string | null;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED";
  responseCode: string | null;
  responseDescription: string | null;
  merchantRequestId: string | null;
  checkoutRequestId: string | null;
}

export interface BackendSale {
  id: string;
  saleId: string;
  invoiceNumber: string | null;
  saleNumber: string | null;
  status: string;
  saleStatus: string;
  subtotal: number;
  tax: number;
  taxTotal: number;
  total: number;
  refundTotal: number;
  paidTotal: number;
  cashTendered: number | null;
  changeDue: number | null;
  shiftId: string;
  customerKraPin: string | null;
  userName: string;
  items: BackendSaleItem[];
  payments: BackendPayment[];
  createdAt: string;
  completedAt: string | null;
  receipt?: { receiptNumber: string } | null;
}

export interface BackendSaleReturnItem {
  saleItemId: string;
  medicineBatchesId: string;
  quantity: number;
}

export interface BackendSaleReturn {
  id: string;
  saleId: string;
  refundAmount: number;
  refundMethod: string;
  items: BackendSaleReturnItem[];
}

export interface BackendGoodsReceipt {
  id: string;
  supplierId: string;
  supplierName: string;
  supplierInvoiceNumber: string | null;
  receivedAt: string;
  lines: Array<{
    id: string;
    medicineId: string;
    batchId: string;
    batchNumber: string;
    expiryDate: string;
    quantity: number;
    unitCost: number;
  }>;
  createdAt: string;
}

export interface BackendSetting {
  id: string;
  settingKey: string;
  settingValue: string;
  description: string | null;
  branchId: string | null;
  pharmacyId: string;
}

export interface BackendUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  status: string;
  branchId?: string | null;
  branchName?: string | null;
  roles?: Array<{ roleName?: string }> | string[];
  createdAt: string;
}

export interface PosQuickItem {
  batchId: string;
  batchNumber: string;
  medicineId: string;
  barcode: string | null;
  name: string;
  strength: string | null;
  available: number;
  sellingPrice: number;
  expirationDate: string;
}

export interface BackendPosLookupItem {
  id: string;
  sku: string | null;
  barcode: string | null;
  brandName: string;
  genericName: string | null;
  strength: string | null;
  categoryId: string | null;
  requiresPrescription: boolean;
  isControlledDrug: boolean;
  stockAvailable: number;
  sellingPrice: number;
  batches: Array<{
    batchId: string;
    batchNumber: string;
    available: number;
    sellingPrice: number;
    expirationDate: string | null;
  }>;
}

export interface BackendDashboardReport {
  branchId: string | null;
  pharmacyWide: boolean;
  date: string;
  completedSalesCount: number | null;
  grossSales: number | null;
  refunds: number | null;
  netSales: number | null;
  lowStockCount: number;
  totalStockItems: number;
  nearExpiryCount: number;
  expiredCount: number;
  nearExpiryDays: number;
}

export interface BackendSalesReport {
  branchId: string | null;
  pharmacyWide: boolean;
  from: string;
  to: string;
  completedSalesCount: number;
  grossSales: number;
  refunds: number;
  netSales: number;
  cashPayments: number;
  mpesaPayments: number;
  otherPayments: number;
  cashRefunds: number;
  mpesaRefunds: number;
  otherRefunds: number;
  topProducts: Array<{
    medicineId: string;
    medicineName: string;
    quantity: number;
    netRevenue: number;
  }>;
}

export interface BackendInventoryReport {
  branchId: string | null;
  pharmacyWide: boolean;
  asOf: string;
  stockValue: number;
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
