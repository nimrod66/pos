import { afterEach, describe, expect, it, vi } from "vitest";

import { setCsrfToken } from "@/lib/csrf-token";

import { ApiClientError, apiRequest } from "./api-client";

describe("apiRequest", () => {
  afterEach(() => {
    setCsrfToken(null);
    vi.unstubAllGlobals();
  });

  it("sends request correlation headers and returns the success envelope", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          data: { status: "UP" },
          meta: { requestId: "10000000-0000-4000-8000-000000000001" },
        }),
        {
          headers: { "Content-Type": "application/json" },
          status: 200,
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    const response = await apiRequest<{ status: string }>("/system/status");

    expect(response.data.status).toBe("UP");
    expect(fetchMock).toHaveBeenCalledOnce();
    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(options.credentials).toBe("include");
    const headers = new Headers(options.headers);
    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("X-Request-ID")).toMatch(
      /^[0-9a-f-]{36}$/,
    );
  });

  it("normalizes the backend success envelope", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            success: true,
            data: { status: "UP" },
            timestamp: "2026-07-21T17:00:00Z",
          }),
          {
            headers: {
              "Content-Type": "application/json",
              "X-Request-ID": "10000000-0000-4000-8000-000000000004",
            },
            status: 200,
          },
        ),
      ),
    );

    const response = await apiRequest<{ status: string }>("/system/status");

    expect(response).toEqual({
      data: { status: "UP" },
      meta: { requestId: "10000000-0000-4000-8000-000000000004" },
    });
  });

  it("adds the in-memory CSRF token to unsafe requests", async () => {
    setCsrfToken("csrf-test-token", "X-CSRF-TOKEN");
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          data: { signedOut: true },
          meta: { requestId: "10000000-0000-4000-8000-000000000003" },
        }),
        {
          headers: { "Content-Type": "application/json" },
          status: 200,
        },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/auth/logout", { method: "POST" });

    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe(
      "csrf-test-token",
    );
  });

  it("blocks unsafe requests until CSRF is initialized", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest("/auth/logout", { method: "POST" }),
    ).rejects.toMatchObject({ code: "CSRF_TOKEN_MISSING", status: 0 });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("converts the standard error envelope into ApiClientError", async () => {
    setCsrfToken("csrf-test-token");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            error: {
              code: "VALIDATION_FAILED",
              fieldErrors: [
                {
                  code: "REQUIRED",
                  field: "username",
                  message: "Username is required.",
                },
              ],
              message: "Review the highlighted field.",
            },
            meta: {
              requestId: "10000000-0000-4000-8000-000000000002",
              timestamp: "2026-07-13T08:00:00Z",
            },
          }),
          {
            headers: { "Content-Type": "application/json" },
            status: 400,
          },
        ),
      ),
    );

    const request = apiRequest("/auth/login", {
      body: { username: "" },
      method: "POST",
    });

    await expect(request).rejects.toBeInstanceOf(ApiClientError);
    await expect(request).rejects.toMatchObject({
      code: "VALIDATION_FAILED",
      fieldErrors: [expect.objectContaining({ field: "username" })],
      status: 400,
    });
  });

  it("normalizes the backend validation error envelope", async () => {
    setCsrfToken("csrf-test-token");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            success: false,
            message: "Validation Failed",
            errorCode: "VALIDATION_ERROR",
            status: 400,
            validationErrors: [
              { field: "email", message: "must be a well-formed email address" },
            ],
          }),
          {
            headers: { "Content-Type": "application/json" },
            status: 400,
          },
        ),
      ),
    );

    await expect(
      apiRequest("/auth/login", {
        body: { email: "invalid" },
        method: "POST",
      }),
    ).rejects.toMatchObject({
      code: "VALIDATION_ERROR",
      fieldErrors: [
        expect.objectContaining({
          code: "VALIDATION_ERROR",
          field: "email",
        }),
      ],
      status: 400,
    });
  });

  it("normalizes network failures", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    );

    await expect(apiRequest("/system/status")).rejects.toMatchObject({
      code: "NETWORK_ERROR",
      message: "The service is unreachable. Check the connection and try again.",
      status: 0,
    });
  });
});
