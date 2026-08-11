import { beforeEach, describe, expect, it } from "vitest";

import { createDemoAuthGateway } from "./demo-auth-gateway";

describe("demo auth gateway", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  it("creates a cookie-style session model and restores the preview marker", async () => {
    const gateway = createDemoAuthGateway(0);
    const session = await gateway.login({
      username: "admin@demo.com",
      password: "admin123",
    });

    expect(session).not.toHaveProperty("accessToken");
    expect(session.user.displayName).toBe("System Admin");
    expect(session.user.activeBranch.code).toBe("MAIN");
    expect(session.user.roles).toEqual(["OWNER"]);
    expect(session.user.permissions).toContain("pos.sell");
    await expect(gateway.restore()).resolves.toMatchObject({
      user: { username: "admin@demo.com" },
    });
  });

  it("clears the preview session during logout", async () => {
    const gateway = createDemoAuthGateway(0);
    await gateway.login({ username: "admin@demo.com", password: "admin123" });

    await gateway.logout();

    await expect(gateway.restore()).resolves.toBeNull();
  });

  it("returns the permission bundle for the selected preview account", async () => {
    const gateway = createDemoAuthGateway(0);
    const cashier = await gateway.login({
      username: "cashier@demo.com",
      password: "cashier123",
    });

    expect(cashier.user.roles).toEqual(["CASHIER"]);
    expect(cashier.user.permissions).toContain("pos.sell");
    expect(cashier.user.permissions).not.toContain("inventory.receive");
  });

  it("rejects unknown accounts and incorrect passwords", async () => {
    const gateway = createDemoAuthGateway(0);

    await expect(
      gateway.login({ username: "cashier@demo.com", password: "wrongpass" }),
    ).rejects.toThrow("Invalid username or password.");
  });
});
