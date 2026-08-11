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

  it("requires price permission when creating a medicine", () => {
    const storeKeeper = permissionsForRoles(["STORE_KEEPER"]);
    const owner = permissionsForRoles(["OWNER"]);

    expect(canAccessPath("/medicines/new", storeKeeper)).toBe(false);
    expect(canAccessPath("/medicines/med-1", storeKeeper)).toBe(true);
    expect(canAccessPath("/medicines/new", owner)).toBe(true);
  });

  it("chooses an allowed landing page for operational roles", () => {
    expect(homePathForPermissions(permissionsForRoles(["CASHIER"]))).toBe(
      "/pos",
    );
    expect(homePathForPermissions(permissionsForRoles(["STORE_KEEPER"]))).toBe(
      "/inventory",
    );
  });
});
