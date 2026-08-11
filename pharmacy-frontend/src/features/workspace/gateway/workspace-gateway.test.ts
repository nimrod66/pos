import { beforeEach, describe, expect, it } from "vitest";

import { createSeedWorkspace } from "@/features/workspace/data/seed-workspace";
import { workspaceGateway } from "@/features/workspace/gateway/workspace-gateway";
import { useWorkspaceStore } from "@/features/workspace/store/workspace-store";

describe("preview workspace gateway", () => {
  beforeEach(() => {
    localStorage.clear();
    useWorkspaceStore.setState(createSeedWorkspace());
  });

  it("exposes preview mutations through async commands", async () => {
    const staffId = await workspaceGateway.addStaff({
      displayName: "Relief Cashier",
      username: "relief",
      phoneNumber: "0711000004",
      jobTitle: "Relief cashier",
      roles: ["CASHIER"],
    });
    await workspaceGateway.setStaffStatus(
      staffId,
      "DISABLED",
      "admin@demo.com",
    );

    const snapshot = workspaceGateway.getSnapshot();
    expect(snapshot.staff.find((user) => user.id === staffId)).toMatchObject({
      displayName: "Relief Cashier",
      status: "DISABLED",
    });
  });
});
