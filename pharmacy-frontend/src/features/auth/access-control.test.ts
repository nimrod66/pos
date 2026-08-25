import { describe, expect, it } from "vitest";

import {
  canAccessPath,
  homePathForPermissions,
  PERMISSIONS,
  permissionsForRoles,
} from "./access-control";

describe("access control", () => {
  it("does not grant clinical approval to the owner role by itself", () => {
    const permissions = permissionsForRoles(["OWNER"]);

    expect(permissions).toContain(PERMISSIONS.USER_MANAGE);
    expect(permissions).not.toContain(PERMISSIONS.PRESCRIPTION_APPROVE);
  });

  it("unions permissions when one account holds multiple roles", () => {
    const permissions = permissionsForRoles(["PHARMACIST", "CASHIER"]);

    expect(permissions).toContain(PERMISSIONS.PRESCRIPTION_APPROVE);
    expect(permissions).toContain(PERMISSIONS.POS_SELL);
  });

  it("lets store keepers create medicines with prices, cashiers cannot", () => {
    const storeKeeper = permissionsForRoles(["STORE_KEEPER"]);
    const cashier = permissionsForRoles(["CASHIER"]);
    const owner = permissionsForRoles(["OWNER"]);

    expect(canAccessPath("/medicines/new", storeKeeper)).toBe(true);
    expect(canAccessPath("/medicines/med-1", storeKeeper)).toBe(true);
    expect(canAccessPath("/medicines/new", cashier)).toBe(false);
    expect(canAccessPath("/medicines/new", owner)).toBe(true);
  });

  it("chooses an allowed landing page for operational roles", () => {
    expect(homePathForPermissions(permissionsForRoles(["CASHIER"]))).toBe(
      "/pos",
    );
    expect(homePathForPermissions(permissionsForRoles(["STORE_KEEPER"]))).toBe(
      "/dashboard",
    );
  });

  it("uses dedicated customer permissions instead of sales permissions", () => {
    const cashier = permissionsForRoles(["CASHIER"]);
    const storeKeeper = permissionsForRoles(["STORE_KEEPER"]);

    expect(cashier).toContain(PERMISSIONS.CUSTOMER_READ);
    expect(cashier).toContain(PERMISSIONS.CUSTOMER_WRITE);
    expect(canAccessPath("/customers", cashier)).toBe(true);
    expect(canAccessPath("/customers", storeKeeper)).toBe(false);
  });

  it("separates purchase-order access from supplier access", () => {
    const manager = permissionsForRoles(["BRANCH_MANAGER"]);
    const storeKeeper = permissionsForRoles(["STORE_KEEPER"]);

    expect(manager).toContain(PERMISSIONS.PURCHASE_ORDER_READ);
    expect(manager).not.toContain(PERMISSIONS.PURCHASE_ORDER_WRITE);
    expect(storeKeeper).toContain(PERMISSIONS.PURCHASE_ORDER_WRITE);
    expect(canAccessPath("/procurement/purchase-orders", manager)).toBe(true);
  });
});
