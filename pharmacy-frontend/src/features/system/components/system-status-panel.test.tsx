import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

const { apiRequestMock } = vi.hoisted(() => ({
  apiRequestMock: vi.fn(),
}));

vi.mock("@/lib/api-client", () => ({
  apiRequest: apiRequestMock,
}));

vi.mock("@/lib/api-config", () => ({
  DEMO_AUTH_ENABLED: true,
}));

import { SystemStatusPanel } from "./system-status-panel";

describe("SystemStatusPanel", () => {
  it("loads preview status through the shared terminal gateway", async () => {
    render(<SystemStatusPanel />);

    expect(screen.getByText("Backend API")).toBeVisible();
    expect(screen.getByText("Not connected")).toBeVisible();
    expect(screen.getByText("Local preview data")).toBeVisible();
    expect(screen.getByText("Ready")).toBeVisible();
    expect(
      await screen.findByText(
        "Preview mode runs without Docker, Spring Boot, or PostgreSQL.",
      ),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Refresh system status" }),
    ).toBeVisible();
    expect(apiRequestMock).not.toHaveBeenCalled();
  });
});
