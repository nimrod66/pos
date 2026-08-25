declare global {
  interface Window {
    __POS_CONFIG?: { apiBaseUrl?: string };
  }
}

const BUILD_TIME_API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090/api/v1"
).replace(/\/$/, "");

/**
 * Resolved at request time so a container started with POS_API_BASE_URL
 * (injected into window.__POS_CONFIG by the root layout) can point the
 * same build at any backend without rebuilding.
 */
export function getApiBaseUrl(): string {
  if (typeof window !== "undefined" && window.__POS_CONFIG?.apiBaseUrl) {
    return window.__POS_CONFIG.apiBaseUrl.replace(/\/$/, "");
  }
  return BUILD_TIME_API_BASE_URL;
}

export const API_BASE_URL = BUILD_TIME_API_BASE_URL;

export const CSRF_HEADER_NAMES = ["X-XSRF-TOKEN", "X-CSRF-TOKEN"] as const;

export type CsrfHeaderName = (typeof CSRF_HEADER_NAMES)[number];

export function isCsrfHeaderName(value: string): value is CsrfHeaderName {
  return CSRF_HEADER_NAMES.some((headerName) => headerName === value);
}

export const CSRF_HEADER_NAME: CsrfHeaderName =
  process.env.NEXT_PUBLIC_CSRF_HEADER_NAME === "X-CSRF-TOKEN"
    ? "X-CSRF-TOKEN"
    : "X-XSRF-TOKEN";

const demoAuthSetting = process.env.NEXT_PUBLIC_DEMO_AUTH;

export const DEMO_AUTH_ENABLED = demoAuthSetting === "true";

export const DEMO_ACCOUNTS_VISIBLE =
  process.env.NEXT_PUBLIC_SHOW_DEMO_ACCOUNTS === "true";
