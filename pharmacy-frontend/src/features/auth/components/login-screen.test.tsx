import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAuthStore } from "@/features/auth/store/auth-store";

import { LoginScreen } from "./login-screen";

const router = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

describe("LoginScreen", () => {
  beforeEach(() => {
    router.replace.mockReset();
    useAuthStore.setState({
      error: null,
      session: null,
      status: "anonymous",
    });
  });

  it("shows accessible validation messages for an empty submission", async () => {
    const user = userEvent.setup();
    render(<LoginScreen />);

    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText("Enter your staff email address."),
    ).toBeVisible();
    expect(
      screen.getByText("Password must contain at least 8 characters."),
    ).toBeVisible();
    expect(screen.getByLabelText("Email address")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("toggles password visibility without changing the entered value", async () => {
    const user = userEvent.setup();
    render(<LoginScreen />);
    const password = screen.getByLabelText("Password");

    await user.type(password, "securepass");
    await user.click(screen.getByRole("button", { name: "Show password" }));

    expect(password).toHaveAttribute("type", "text");
    expect(password).toHaveValue("securepass");
    expect(screen.getByRole("button", { name: "Hide password" })).toBeVisible();
  });
});
