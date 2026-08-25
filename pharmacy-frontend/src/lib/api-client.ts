import {
  getApiBaseUrl,
  CSRF_HEADER_NAME,
} from "@/lib/api-config";
import { getCsrfHeaderName, getCsrfToken } from "@/lib/csrf-token";
import type {
  ApiErrorResponse,
  ApiFieldError,
  ApiResponse,
  BackendApiErrorResponse,
  BackendApiResponse,
} from "@/types/api";

interface ApiRequestOptions extends Omit<RequestInit, "body" | "headers"> {
  body?: unknown;
  headers?: HeadersInit;
  idempotencyKey?: string;
}

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);
export const SESSION_EXPIRED_EVENT = "pharmacy-pos:session-expired";

export class ApiClientError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string,
    readonly fieldErrors: ApiFieldError[] = [],
    readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== "object" || !("error" in value)) {
    return false;
  }

  const error = value.error;
  return Boolean(
    error &&
      typeof error === "object" &&
      "code" in error &&
      "message" in error,
  );
}

function isApiResponse<T>(value: unknown): value is ApiResponse<T> {
  if (!value || typeof value !== "object" || !("data" in value)) {
    return false;
  }

  return Boolean(
    "meta" in value &&
      value.meta &&
      typeof value.meta === "object" &&
      "requestId" in value.meta &&
      typeof value.meta.requestId === "string",
  );
}

function isBackendApiResponse<T>(
  value: unknown,
): value is BackendApiResponse<T> {
  return Boolean(
    value &&
      typeof value === "object" &&
      "success" in value &&
      value.success === true &&
      "data" in value,
  );
}

function isBackendApiErrorResponse(
  value: unknown,
): value is BackendApiErrorResponse {
  return Boolean(
    value &&
      typeof value === "object" &&
      "success" in value &&
      value.success === false &&
      "message" in value &&
      typeof value.message === "string",
  );
}

export async function apiRequest<T>(
  path: `/${string}`,
  options: ApiRequestOptions = {},
): Promise<ApiResponse<T>> {
  const {
    body,
    headers: suppliedHeaders,
    idempotencyKey,
    ...requestInit
  } = options;
  const headers = new Headers(suppliedHeaders);
  const method = (requestInit.method ?? "GET").toUpperCase();
  const csrfToken = getCsrfToken();
  const requestId = crypto.randomUUID();

  headers.set("Accept", "application/json");
  headers.set("X-Request-ID", requestId);

  if (body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (!SAFE_METHODS.has(method)) {
    if (!csrfToken) {
      throw new ApiClientError(
        "The secure request token is not ready. Refresh the page and try again.",
        0,
        "CSRF_TOKEN_MISSING",
      );
    }
    headers.set(getCsrfHeaderName() ?? CSRF_HEADER_NAME, csrfToken);
  }
  if (idempotencyKey) {
    headers.set("Idempotency-Key", idempotencyKey);
  }

  let response: Response;
  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      ...requestInit,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "include",
      headers,
    });
  } catch {
    throw new ApiClientError(
      "The service is unreachable. Check the connection and try again.",
      0,
      "NETWORK_ERROR",
    );
  }

  let payload: unknown = null;
  if (response.status !== 204) {
    try {
      payload = await response.json();
    } catch {
      // The response validation below handles empty or non-JSON payloads.
    }
  }

  if (!response.ok || isBackendApiErrorResponse(payload)) {
    if (response.status === 401 && typeof window !== "undefined") {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    if (isApiErrorResponse(payload)) {
      throw new ApiClientError(
        payload.error.message,
        response.status,
        payload.error.code,
        payload.error.fieldErrors ?? [],
        payload.meta?.requestId,
      );
    }

    if (isBackendApiErrorResponse(payload)) {
      const errorCode = payload.errorCode ?? "API_REQUEST_FAILED";
      throw new ApiClientError(
        payload.message,
        payload.status ?? response.status,
        errorCode,
        (payload.validationErrors ?? []).map((error) => ({
          ...error,
          code: errorCode,
        })),
        response.headers.get("X-Request-ID") ?? requestId,
      );
    }

    throw new ApiClientError(
      "The service could not complete the request.",
      response.status,
      "UNEXPECTED_API_RESPONSE",
      [],
      response.headers.get("X-Request-ID") ?? requestId,
    );
  }

  if (response.status === 204) {
    return {
      data: undefined as T,
      meta: { requestId: response.headers.get("X-Request-ID") ?? requestId },
    };
  }

  if (isApiResponse<T>(payload)) {
    return payload;
  }

  if (isBackendApiResponse<T>(payload)) {
    return {
      data: payload.data,
      meta: { requestId: response.headers.get("X-Request-ID") ?? requestId },
    };
  }

  throw new ApiClientError(
    "The service returned an unexpected response.",
    response.status,
    "UNEXPECTED_API_RESPONSE",
    [],
    response.headers.get("X-Request-ID") ?? requestId,
  );
}
