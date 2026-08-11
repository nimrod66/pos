import type { CsrfHeaderName } from "@/lib/api-config";

let csrfToken: string | null = null;
let csrfHeaderName: CsrfHeaderName | null = null;

export function getCsrfToken() {
  return csrfToken;
}

export function getCsrfHeaderName() {
  return csrfHeaderName;
}

export function setCsrfToken(
  token: string | null,
  headerName: CsrfHeaderName | null = null,
) {
  csrfToken = token;
  csrfHeaderName = token ? headerName : null;
}
