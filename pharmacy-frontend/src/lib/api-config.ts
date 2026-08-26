declare global {
  interface Window {
    __POS_CONFIG?: { apiBaseUrl?: string };
  }
}

const BUILD_TIME_API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090/api/v1"
).replace(/\/$/, "");

/**
 * Resolved at request time, in priority order:
 *
 * 1. window.__POS_CONFIG.apiBaseUrl  (explicit server-side override via
 *    POS_API_BASE_URL env on the frontend container)
 * 2. Auto-derived from the browser's hostname — whoever served the page
 *    also hosts the API. Works from localhost AND from any LAN device
 *    (phone/handheld) with zero configuration.
 * 3. Build-time NEXT_PUBLIC_API_BASE_URL fallback.
 */
export function getApiBaseUrl(): string {
  if (typeof window !== "undefined") {
    if (window.__POS_CONFIG?.apiBaseUrl) {
      return window.__POS_CONFIG.apiBaseUrl.replace(/\/$/, "");
    }
    // Auto-derive: same host that served this page also runs the API.
    const proto = window.location.protocol;
    const host = window.location.hostname;
    if (host && host !== "localhost" && host !== "127.0.0.1") {
      return `${proto}//${host}:9090/api/v1`;
    }
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
