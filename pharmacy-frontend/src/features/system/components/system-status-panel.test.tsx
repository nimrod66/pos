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
  it("treats disconnected backend services as expected in preview mode", () => {
    render(<SystemStatusPanel />);

    expect(screen.getByText("Backend API")).toBeVisible();
    expect(screen.getByText("Not connected")).toBeVisible();
    expect(screen.getByText("Local preview data")).toBeVisible();
    expect(screen.getByText("Ready")).toBeVisible();
    expect(
      screen.getByText(
        "Preview mode runs without Docker, Spring Boot, or PostgreSQL.",
      ),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Refresh system status" }),
    ).not.toBeInTheDocument();
    expect(apiRequestMock).not.toHaveBeenCalled();
  });
});
