import {
  permissionsForRoles,
  type TenantRole,
} from "@/features/auth/access-control";
import type { AuthSession } from "@/features/auth/types";

export interface DemoAccount {
  displayName: string;
  id: string;
  label: string;
  password: string;
  roles: TenantRole[];
  username: string;
}

export const DEMO_ACCOUNTS: readonly DemoAccount[] = [
  {
    id: "10000000-0000-4000-8000-000000000001",
    username: "admin@demo.com",
    password: "admin123",
    displayName: "System Admin",
    label: "Owner",
    roles: ["OWNER"],
  },
  {
    id: "10000000-0000-4000-8000-000000000002",
    username: "manager@demo.com",
    password: "manager123",
    displayName: "Branch Manager",
    label: "Branch manager",
    roles: ["BRANCH_MANAGER"],
  },
  {
    id: "10000000-0000-4000-8000-000000000003",
    username: "pharmacist@demo.com",
    password: "pharmacist123",
    displayName: "Duty Pharmacist",
    label: "Pharmacist",
    roles: ["PHARMACIST"],
  },
  {
    id: "10000000-0000-4000-8000-000000000004",
    username: "cashier@demo.com",
    password: "cashier123",
    displayName: "Main Cashier",
    label: "Cashier",
    roles: ["CASHIER"],
  },
  {
    id: "10000000-0000-4000-8000-000000000005",
    username: "storekeeper@demo.com",
    password: "stock1234",
    displayName: "Store Keeper",
    label: "Store keeper",
    roles: ["STORE_KEEPER"],
  },
  {
    id: "10000000-0000-4000-8000-000000000006",
    username: "technician@demo.com",
    password: "tech12345",
    displayName: "Pharmacy Technician",
    label: "Pharmacy technician",
    roles: ["PHARMACY_TECHNICIAN"],
  },
];

export function findDemoAccount(username: string) {
  const normalized = username.trim().toLowerCase();
  return DEMO_ACCOUNTS.find((account) => account.username === normalized);
}

export function createDemoSession(account: DemoAccount): AuthSession {
  return {
    expiresAt: new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString(),
    user: {
      id: account.id,
      username: account.username,
      displayName: account.displayName,
      pharmacyId: "30000000-0000-4000-8000-000000000001",
      pharmacyName: "Pharmacy POS",
      activeBranch: {
        id: "20000000-0000-4000-8000-000000000001",
        code: "MAIN",
        name: "Main branch",
      },
      roles: account.roles,
      permissions: permissionsForRoles(account.roles),
      featureFlags: {
        customers: false,
        insurance: false,
        loyalty: false,
      },
    },
  };
}
