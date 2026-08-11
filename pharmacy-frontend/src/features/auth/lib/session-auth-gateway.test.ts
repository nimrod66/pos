import { afterEach, describe, expect, it, vi } from "vitest";

import { getCsrfToken, setCsrfToken } from "@/lib/csrf-token";

import { createSessionAuthGateway } from "./session-auth-gateway";

const meta = {
  requestId: "10000000-0000-4000-8000-000000000001",
};

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    headers: { "Content-Type": "application/json" },
    status,
  });
}

describe("session auth gateway", () => {
  afterEach(() => {
    setCsrfToken(null);
    vi.unstubAllGlobals();
  });

  it("gets CSRF before login and refreshes it after authentication", async () => {
    const session = {
      expiresAt: "2026-07-15T20:00:00Z",
      user: {
        id: "user-1",
        email: "admin@demo.com",
        displayName: "Pharmacy Owner",
        pharmacyId: "pharmacy-1",
        pharmacyName: "Pharmacy POS",
        activeBranch: { id: "branch-1", code: "MAIN", name: "Main branch" },
        roles: ["OWNER"],
        permissions: ["pos.sell"],
        featureFlags: {},
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          data: { headerName: "X-CSRF-TOKEN", token: "before-login" },
          meta,
        }),
      )
      .mockResolvedValueOnce(jsonResponse({ data: session, meta }))
      .mockResolvedValueOnce(
        jsonResponse({
          data: { headerName: "X-XSRF-TOKEN", token: "after-login" },
          meta,
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    const result = await createSessionAuthGateway().login({
      username: "admin@demo.com",
      password: "admin123",
    });

    expect(result).toEqual({
      ...session,
      user: {
        ...session.user,
        username: "admin@demo.com",
      },
    });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const [, loginOptions] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(new Headers(loginOptions.headers).get("X-CSRF-TOKEN")).toBe(
      "before-login",
    );
    expect(loginOptions.body).toBe(
      JSON.stringify({ email: "admin@demo.com", password: "admin123" }),
    );
    expect(loginOptions.credentials).toBe("include");
    expect(getCsrfToken()).toBe("after-login");
  });

  it("signs out through the backend envelope and clears the CSRF token", async () => {
    setCsrfToken("logout-token", "X-XSRF-TOKEN");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        data: { signedOut: true },
        meta,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(createSessionAuthGateway().logout()).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/auth/logout");
    expect(options.method).toBe("POST");
    expect(new Headers(options.headers).get("X-XSRF-TOKEN")).toBe(
      "logout-token",
    );
    expect(getCsrfToken()).toBeNull();
  });

  it("treats an unauthorized me response as an anonymous session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            error: {
              code: "AUTHENTICATION_REQUIRED",
              fieldErrors: [],
              message: "Authentication is required.",
            },
            meta: { ...meta, timestamp: "2026-07-15T08:00:00Z" },
          },
          401,
        ),
      ),
    );

    await expect(createSessionAuthGateway().restore()).resolves.toBeNull();
    expect(getCsrfToken()).toBeNull();
  });
});
