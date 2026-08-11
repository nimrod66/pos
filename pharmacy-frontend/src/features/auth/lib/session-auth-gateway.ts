import type {
  AuthGateway,
  AuthSession,
  LoginCredentials,
} from "@/features/auth/types";
import { ApiClientError, apiRequest } from "@/lib/api-client";
import { isCsrfHeaderName } from "@/lib/api-config";
import { getCsrfToken, setCsrfToken } from "@/lib/csrf-token";

interface CsrfTokenResponse {
  headerName: string;
  token: string;
}

interface BackendAuthSession extends Omit<AuthSession, "user"> {
  user: Omit<AuthSession["user"], "username"> & {
    email: string;
    username?: string;
  };
}

function normalizeSession(
  session: AuthSession | BackendAuthSession,
): AuthSession {
  const user = session.user as AuthSession["user"] & { email?: string };
  const username = user.username ?? user.email;

  if (!username) {
    throw new Error("The backend returned an invalid authenticated user.");
  }

  return {
    ...session,
    user: {
      ...user,
      username,
    },
  };
}

async function refreshCsrfToken() {
  const response = await apiRequest<CsrfTokenResponse>("/auth/csrf", {
    cache: "no-store",
  });

  if (!isCsrfHeaderName(response.data.headerName) || !response.data.token) {
    throw new Error("The backend returned an invalid CSRF token response.");
  }

  setCsrfToken(response.data.token, response.data.headerName);
}

async function ensureCsrfToken() {
  if (!getCsrfToken()) {
    await refreshCsrfToken();
  }
}

export function createSessionAuthGateway(): AuthGateway {
  return {
    async login(credentials: LoginCredentials) {
      await refreshCsrfToken();
      const response = await apiRequest<AuthSession | BackendAuthSession>(
        "/auth/login",
        {
          body: {
            email: credentials.username,
            password: credentials.password,
          },
          cache: "no-store",
          method: "POST",
        },
      );

      // Spring Security rotates the CSRF token when authentication changes.
      await refreshCsrfToken();
      return normalizeSession(response.data);
    },
    async logout() {
      try {
        await ensureCsrfToken();
        await apiRequest<{ signedOut: true }>("/auth/logout", {
          cache: "no-store",
          method: "POST",
        });
      } finally {
        setCsrfToken(null);
      }
    },
    async restore() {
      try {
        const response = await apiRequest<AuthSession | BackendAuthSession>(
          "/auth/me",
          { cache: "no-store" },
        );
        await refreshCsrfToken();
        return normalizeSession(response.data);
      } catch (error) {
        setCsrfToken(null);
        if (error instanceof ApiClientError && error.status === 401) {
          return null;
        }
        throw error;
      }
    },
  };
}
