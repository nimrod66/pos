export const PERMISSIONS = {
  DASHBOARD_READ: "dashboard.read",
  POS_SELL: "pos.sell",
  POS_DISCOUNT_REQUEST: "pos.discount.request",
  POS_DISCOUNT_APPROVE: "pos.discount.approve",
  SALE_READ: "sale.read",
  SALE_RECEIPT_REPRINT: "sale.receipt.reprint",
  SALE_VOID: "sale.void",
  SALE_RETURN: "sale.return",
  MEDICINE_READ: "medicine.read",
  MEDICINE_WRITE: "medicine.write",
  MEDICINE_PRICE_WRITE: "medicine.price.write",
  INVENTORY_READ: "inventory.read",
  INVENTORY_RECEIVE: "inventory.receive",
  INVENTORY_ADJUST_REQUEST: "inventory.adjust.request",
  INVENTORY_ADJUST_APPROVE: "inventory.adjust.approve",
  SUPPLIER_READ: "supplier.read",
  SUPPLIER_WRITE: "supplier.write",
  CUSTOMER_READ: "customer.read",
  CUSTOMER_WRITE: "customer.write",
  PURCHASE_ORDER_READ: "purchase_order.read",
  PURCHASE_ORDER_WRITE: "purchase_order.write",
  SHIFT_OPEN: "shift.open",
  SHIFT_CLOSE: "shift.close",
  SHIFT_VARIANCE_APPROVE: "shift.variance.approve",
  REPORT_SALES_READ: "report.sales.read",
  REPORT_INVENTORY_READ: "report.inventory.read",
  USER_MANAGE: "user.manage",
  SETTINGS_MANAGE: "settings.manage",
  AUDIT_READ: "audit.read",
  TERMINAL_READ: "terminal.read",
  TERMINAL_MANAGE: "terminal.manage",
  PRESCRIPTION_READ: "prescription.read",
  PRESCRIPTION_APPROVE: "prescription.approve",
} as const;

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];

export const ALL_PERMISSIONS = Object.values(PERMISSIONS) as Permission[];

export const TENANT_ROLES = [
  "OWNER",
  "BRANCH_MANAGER",
  "PHARMACIST",
  "CASHIER",
  "STORE_KEEPER",
  "PHARMACY_TECHNICIAN",
] as const;

export type TenantRole = (typeof TENANT_ROLES)[number];

export interface AccessRule {
  allOf?: readonly Permission[];
  anyOf?: readonly Permission[];
}

export interface RoleDefinition {
  code: TenantRole;
  description: string;
  label: string;
  permissions: readonly Permission[];
  scope: "PHARMACY" | "BRANCH";
}

export const ROLE_DEFINITIONS: readonly RoleDefinition[] = [
  {
    code: "OWNER",
    label: "Owner",
    description: "Business administration and pharmacy-wide oversight.",
    scope: "PHARMACY",
    permissions: ALL_PERMISSIONS.filter(
      (permission) => permission !== PERMISSIONS.PRESCRIPTION_APPROVE,
    ),
  },
  {
    code: "BRANCH_MANAGER",
    label: "Branch manager",
    description: "Branch operations, approvals, and staff oversight.",
    scope: "BRANCH",
    permissions: [
      PERMISSIONS.DASHBOARD_READ,
      PERMISSIONS.POS_DISCOUNT_APPROVE,
      PERMISSIONS.SALE_READ,
      PERMISSIONS.SALE_RECEIPT_REPRINT,
      PERMISSIONS.SALE_VOID,
      PERMISSIONS.SALE_RETURN,
      PERMISSIONS.MEDICINE_READ,
      PERMISSIONS.MEDICINE_WRITE,
      PERMISSIONS.MEDICINE_PRICE_WRITE,
      PERMISSIONS.INVENTORY_READ,
      PERMISSIONS.INVENTORY_ADJUST_APPROVE,
      PERMISSIONS.SUPPLIER_READ,
      PERMISSIONS.CUSTOMER_READ,
      PERMISSIONS.CUSTOMER_WRITE,
      PERMISSIONS.PURCHASE_ORDER_READ,
      PERMISSIONS.SHIFT_VARIANCE_APPROVE,
      PERMISSIONS.REPORT_SALES_READ,
      PERMISSIONS.REPORT_INVENTORY_READ,
      PERMISSIONS.TERMINAL_READ,
      PERMISSIONS.TERMINAL_MANAGE,
      PERMISSIONS.PRESCRIPTION_READ,
    ],
  },
  {
    code: "PHARMACIST",
    label: "Pharmacist",
    description: "Prescription approval and clinical dispensing controls.",
    scope: "BRANCH",
    permissions: [
      PERMISSIONS.DASHBOARD_READ,
      PERMISSIONS.POS_SELL,
      PERMISSIONS.SALE_READ,
      PERMISSIONS.MEDICINE_READ,
      PERMISSIONS.INVENTORY_READ,
      PERMISSIONS.CUSTOMER_READ,
      PERMISSIONS.CUSTOMER_WRITE,
      PERMISSIONS.SHIFT_OPEN,
      PERMISSIONS.SHIFT_CLOSE,
      PERMISSIONS.PRESCRIPTION_READ,
      PERMISSIONS.PRESCRIPTION_APPROVE,
    ],
  },
  {
    code: "CASHIER",
    label: "Cashier",
    description: "Sales, receipts, payments, and cashier shifts.",
    scope: "BRANCH",
    permissions: [
      PERMISSIONS.POS_SELL,
      PERMISSIONS.POS_DISCOUNT_REQUEST,
      PERMISSIONS.SALE_READ,
      PERMISSIONS.SALE_RECEIPT_REPRINT,
      PERMISSIONS.SALE_RETURN,
      PERMISSIONS.MEDICINE_READ,
      PERMISSIONS.CUSTOMER_READ,
      PERMISSIONS.CUSTOMER_WRITE,
      PERMISSIONS.SHIFT_OPEN,
      PERMISSIONS.SHIFT_CLOSE,
    ],
  },
  {
    code: "STORE_KEEPER",
    label: "Store keeper",
    description: "Receiving, stock counts, transfers, and expiry work.",
    scope: "BRANCH",
    permissions: [
      PERMISSIONS.DASHBOARD_READ,
      PERMISSIONS.MEDICINE_READ,
      PERMISSIONS.MEDICINE_WRITE,
      PERMISSIONS.MEDICINE_PRICE_WRITE,
      PERMISSIONS.INVENTORY_READ,
      PERMISSIONS.INVENTORY_RECEIVE,
      PERMISSIONS.INVENTORY_ADJUST_REQUEST,
      PERMISSIONS.SUPPLIER_READ,
      PERMISSIONS.SUPPLIER_WRITE,
      PERMISSIONS.PURCHASE_ORDER_READ,
      PERMISSIONS.PURCHASE_ORDER_WRITE,
      PERMISSIONS.REPORT_INVENTORY_READ,
    ],
  },
  {
    code: "PHARMACY_TECHNICIAN",
    label: "Pharmacy technician",
    description: "Assisted dispensing, counter sales, customers, and stock receiving.",
    scope: "BRANCH",
    permissions: [
      PERMISSIONS.DASHBOARD_READ,
      PERMISSIONS.POS_SELL,
      PERMISSIONS.SALE_READ,
      PERMISSIONS.SALE_RECEIPT_REPRINT,
      PERMISSIONS.MEDICINE_READ,
      PERMISSIONS.INVENTORY_READ,
      PERMISSIONS.INVENTORY_RECEIVE,
      PERMISSIONS.SUPPLIER_READ,
      PERMISSIONS.CUSTOMER_READ,
      PERMISSIONS.CUSTOMER_WRITE,
      PERMISSIONS.PURCHASE_ORDER_READ,
      PERMISSIONS.SHIFT_OPEN,
      PERMISSIONS.SHIFT_CLOSE,
      PERMISSIONS.PRESCRIPTION_READ,
    ],
  },
];

const roleDefinitionsByCode = new Map(
  ROLE_DEFINITIONS.map((role) => [role.code, role]),
);

export function roleLabel(role: TenantRole) {
  return roleDefinitionsByCode.get(role)?.label ?? role;
}

export function permissionsForRoles(roles: readonly TenantRole[]) {
  const granted = new Set<Permission>();
  for (const role of roles) {
    for (const permission of roleDefinitionsByCode.get(role)?.permissions ?? []) {
      granted.add(permission);
    }
  }
  return ALL_PERMISSIONS.filter((permission) => granted.has(permission));
}

export function hasPermission(
  permissions: readonly Permission[],
  permission: Permission,
) {
  return permissions.includes(permission);
}

export function canAccess(
  permissions: readonly Permission[],
  rule?: AccessRule,
) {
  if (!rule) return true;
  if (rule.allOf?.some((permission) => !permissions.includes(permission))) {
    return false;
  }
  if (
    rule.anyOf?.length &&
    !rule.anyOf.some((permission) => permissions.includes(permission))
  ) {
    return false;
  }
  return true;
}

const routeAccessRules: Array<{
  exact?: boolean;
  path: string;
  rule: AccessRule;
}> = [
  {
    path: "/medicines/new",
    exact: true,
    rule: {
      allOf: [PERMISSIONS.MEDICINE_WRITE, PERMISSIONS.MEDICINE_PRICE_WRITE],
    },
  },
  {
    path: "/procurement/grn/new",
    exact: true,
    rule: { allOf: [PERMISSIONS.INVENTORY_RECEIVE] },
  },
  {
    path: "/medicines/",
    rule: { allOf: [PERMISSIONS.MEDICINE_WRITE] },
  },
  {
    path: "/dashboard",
    rule: { allOf: [PERMISSIONS.DASHBOARD_READ] },
  },
  {
    path: "/pos",
    rule: { allOf: [PERMISSIONS.POS_SELL] },
  },
  {
    path: "/shifts/current",
    rule: { anyOf: [PERMISSIONS.SHIFT_OPEN, PERMISSIONS.SHIFT_CLOSE] },
  },
  {
    path: "/medicines",
    rule: { allOf: [PERMISSIONS.MEDICINE_READ] },
  },
  {
    path: "/prescriptions",
    rule: { allOf: [PERMISSIONS.PRESCRIPTION_READ] },
  },
  {
    path: "/inventory",
    rule: { allOf: [PERMISSIONS.INVENTORY_READ] },
  },
  {
    path: "/suppliers",
    rule: { allOf: [PERMISSIONS.SUPPLIER_READ] },
  },
  {
    path: "/procurement/purchase-orders",
    rule: { allOf: [PERMISSIONS.PURCHASE_ORDER_READ] },
  },
  {
    path: "/customers",
    rule: { allOf: [PERMISSIONS.CUSTOMER_READ] },
  },
  {
    path: "/sales",
    rule: { allOf: [PERMISSIONS.SALE_READ] },
  },
  {
    path: "/reports",
    rule: {
      anyOf: [PERMISSIONS.REPORT_SALES_READ, PERMISSIONS.REPORT_INVENTORY_READ],
    },
  },
  {
    path: "/admin/users",
    rule: { allOf: [PERMISSIONS.USER_MANAGE] },
  },
  {
    path: "/admin/terminals",
    rule: { allOf: [PERMISSIONS.TERMINAL_READ] },
  },
  {
    path: "/admin/settings",
    rule: { allOf: [PERMISSIONS.SETTINGS_MANAGE] },
  },
  {
    path: "/admin/audit",
    rule: { allOf: [PERMISSIONS.AUDIT_READ] },
  },
  {
    path: "/system",
    rule: { anyOf: [PERMISSIONS.TERMINAL_MANAGE, PERMISSIONS.SETTINGS_MANAGE] },
  },
];

export function accessRuleForPath(pathname: string) {
  return routeAccessRules.find(({ exact, path }) =>
    exact ? pathname === path : pathname === path || pathname.startsWith(path),
  )?.rule;
}

export function canAccessPath(
  pathname: string,
  permissions: readonly Permission[],
) {
  return canAccess(permissions, accessRuleForPath(pathname));
}

export function homePathForPermissions(permissions: readonly Permission[]) {
  if (permissions.includes(PERMISSIONS.DASHBOARD_READ)) return "/dashboard";
  if (permissions.includes(PERMISSIONS.POS_SELL)) return "/pos";
  if (permissions.includes(PERMISSIONS.INVENTORY_READ)) return "/inventory";
  if (permissions.includes(PERMISSIONS.SALE_READ)) return "/sales";
  if (permissions.includes(PERMISSIONS.MEDICINE_READ)) return "/medicines";
  return "/system";
}
