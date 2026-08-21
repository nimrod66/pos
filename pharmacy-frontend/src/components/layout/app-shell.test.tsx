import { describe, expect, it } from "vitest";

import { PERMISSIONS } from "@/features/auth/access-control";

import { appNavigation, visibleNavigation } from "./app-shell";

describe("application navigation permissions", () => {
  it("uses the canonical POS permission", () => {
    expect(
      appNavigation.find((item) => item.href === "/pos")?.access?.allOf,
    ).toContain(PERMISSIONS.POS_SELL);
  });

  it("hides workspaces that are not granted to the active account", () => {
    const navigation = visibleNavigation(appNavigation, [
      PERMISSIONS.DASHBOARD_READ,
      PERMISSIONS.POS_SELL,
    ]);

    expect(navigation.map((item) => item.href)).toContain("/pos");
    expect(navigation.map((item) => item.href)).not.toContain("/admin/users");
    expect(navigation.map((item) => item.href)).not.toContain("/admin/terminals");
  });

  it("shows terminal administration only to terminal readers", () => {
    const navigation = visibleNavigation(appNavigation, [
      PERMISSIONS.TERMINAL_READ,
    ]);

    expect(navigation.map((item) => item.href)).toContain("/admin/terminals");
  });
});
